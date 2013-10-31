/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.service.kml;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;

/**
 * Bean to provide a mapping based on {@link Metacard} {@link Attribute}s to supply custom style
 * configuration.
 * 
 * @author kcwire
 * 
 */
public class KmlStyleMapper {

    // Map<Entry<AttribueName, AttributeValue>, StyleUrl>
    private Map<Entry<String, String>, String> styleMap = new ConcurrentHashMap<Entry<String, String>, String>();

    private static final Logger LOGGER = LoggerFactory.getLogger(KmlStyleMapper.class);
    
    private static final String ATTRIBUTE_NAME = "attributeName";

    private static final String ATTRIBUTE_VALUE = "attributeValue";

    private static final String STYLE_URL = "styleUrl";

    private static final String CURRENT_MAPPINGS = "currentMappings";

    private static final String PID = "org.codice.ddf.spatial.kml.style.properties";

    private String attributeName;

    private String attributeValue;

    private String styleUrl;

    private String[] mappings;

    private BundleContext context;

    private Pattern entryPattern = Pattern.compile("(.*)=(.*);(.*)");

    public KmlStyleMapper(BundleContext bundleContext) {
        this.context = bundleContext;
    }

    public void updateMap(Map<String, ?> props) {
        if (props != null) {
            String[] currentMappings = (String[]) props.get(CURRENT_MAPPINGS);
            if (currentMappings.length != styleMap.size()) {
                styleMap.clear();
                loadFromStringArray(currentMappings);
            }
            String name = (String) props.get(ATTRIBUTE_NAME);
            String value = (String) props.get(ATTRIBUTE_VALUE);
            String url = (String) props.get(STYLE_URL);
            if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(value)
                    && StringUtils.isNotBlank(url)) {
                addMapEntry(name, value, url);
                updateConfigAdminProperties();
            }
        }
    }

    public String getStyleForMetacard(Metacard metacard) {
        for (Entry<Entry<String, String>, String> styleEntry : styleMap.entrySet()) {
            Attribute attribute = metacard.getAttribute(styleEntry.getKey().getKey());
            if (attribute != null) {
                if (attribute.getValue() != null) {
                    AttributeDescriptor descriptor = metacard.getMetacardType()
                            .getAttributeDescriptor(attribute.getName());
                    if (compareAttributeByFormat(descriptor.getType().getAttributeFormat(),
                            attribute.getValue(), styleEntry.getKey().getValue())) {
                        return styleEntry.getValue();
                    }
                }
            }
        }
        return null;
    }
    
    protected void addMapEntry(String name, String value, String url) {
        LOGGER.debug("ENTERING: addMapEntry");
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(value)) {
            styleMap.put(new AbstractMap.SimpleImmutableEntry<String, String>(name, value), url);
            LOGGER.debug("Map Size = " + styleMap.size());
        }
    }
    
    private String entryToString(Entry<Entry<String, String>, String> entry) {
        return entry.getKey().getKey() + "=" + entry.getKey().getValue() + ";" + entry.getValue();
    }

    private void loadFromStringArray(String[] currentMappings) {
        for (String entry : currentMappings) {
            Matcher matcher = entryPattern.matcher(entry);
            if (matcher.find()) {
                addMapEntry(matcher.group(1), matcher.group(2), matcher.group(3));
            }
        }
    }


    private void updateConfigAdminProperties() {
        Configuration config = getManagedConfig();
        Dictionary<String, Object> props = config.getProperties();
        props.put(ATTRIBUTE_NAME, "");
        props.put(ATTRIBUTE_VALUE, "");
        props.put(STYLE_URL, "");
        String[] mappings = getMapAsStringArray();
        props.put(CURRENT_MAPPINGS, mappings);
        try {
            config.update(props);
        } catch (IOException e) {
            LOGGER.warn("Unable to update KML Style Mappings", e);
        }
    }

    private String[] getMapAsStringArray() {
        List<String> list = new ArrayList<String>();
        for (Entry<Entry<String, String>, String> entry : styleMap.entrySet()) {
            list.add(entryToString(entry));
        }
        return list.toArray(new String[list.size()]);
    }

    private boolean compareAttributeByFormat(AttributeFormat format, Serializable metacardValue,
            String mappedValue) {
        switch (format) {
        case STRING:
        case XML:
        case GEOMETRY:
            return mappedValue.equals(metacardValue);
        case BOOLEAN:
            return Boolean.valueOf(mappedValue).equals(metacardValue);
        case DATE:
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String mappedDate = dateFormat.format(dateFormat.parse(mappedValue));
                String metacardDate = dateFormat.format((Date) metacardValue);
                return mappedDate.equals(metacardDate);
            } catch (ParseException e) {
                LOGGER.warn("Unable to parese date and perform comparison.", e);
                return false;
            }
        case SHORT:
            return Short.valueOf(mappedValue).equals(metacardValue);
        case INTEGER:
            return Integer.valueOf(mappedValue).equals(metacardValue);
        case LONG:
            return Long.valueOf(mappedValue).equals(metacardValue);
        case FLOAT:
            return Float.valueOf(mappedValue).equals(metacardValue);
        case DOUBLE:
            return Double.valueOf(mappedValue).equals(metacardValue);
        case BINARY:
        case OBJECT:
        default:
            LOGGER.warn("Unsupported Attribute Format was attempted for KML Style Mapping.");
            return false;
        }

    }

    private Configuration getManagedConfig() {
        Configuration managedConfig = null;
        ServiceReference configurationAdminReference = context
                .getServiceReference(ConfigurationAdmin.class.getName());
        if (configurationAdminReference != null) {
            ConfigurationAdmin confAdmin = (ConfigurationAdmin) context
                    .getService(configurationAdminReference);
            try {
                managedConfig = confAdmin.getConfiguration(PID);
            } catch (IOException e) {
                LOGGER.warn("{}: Failed to capture KML Stlye Mapping Config.", PID);
            }
        }

        return managedConfig;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public String getStyleUrl() {
        return styleUrl;
    }

    public void setStyleUrl(String styleUrl) {
        this.styleUrl = styleUrl;
    }

    public String[] getMappings() {
        return mappings;
    }

    public void setMappings(String[] mappings) {
        this.mappings = mappings;
    }
    
}

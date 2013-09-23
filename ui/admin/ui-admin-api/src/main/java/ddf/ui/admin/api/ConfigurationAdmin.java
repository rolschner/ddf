/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.ui.admin.api;

import org.apache.aries.jmx.codec.PropertyData;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static org.osgi.jmx.JmxConstants.PROPERTIES_TYPE;

/**
 * @author Scott Tustison
 */
public class ConfigurationAdmin implements ConfigurationAdminMBean
{
    private final XLogger logger = new XLogger(LoggerFactory.getLogger(ConfigurationAdmin.class));

    private static final String SERVICE_PID = "service.pid";
    private static final String SERVICE_FACTORYPID = "service.factorypid";

    private org.osgi.service.cm.ConfigurationAdmin configurationAdmin;
    private ConfigurationAdminExt configAdminSupport;
    private BundleContext bundleContext;
    private ObjectName objectName;
    private MBeanServer mBeanServer;
    private List<String> filterList;

    /**
     * Constructs a ConfigurationAdmin implementation
     *
     * @param configurationAdmin instance of org.osgi.service.cm.ConfigurationAdmin service
     */
    public ConfigurationAdmin(BundleContext bundleContext, org.osgi.service.cm.ConfigurationAdmin configurationAdmin)
    {
        this.configurationAdmin = configurationAdmin;
        this.bundleContext = bundleContext;
        configAdminSupport = new ConfigurationAdminExt(bundleContext, configurationAdmin);
    }

    public void init()
    {
        try
        {
            if (objectName == null)
            {
                objectName = new ObjectName(ConfigurationAdminMBean.OBJECTNAME);
            }
            if (mBeanServer == null)
            {
                mBeanServer = ManagementFactory.getPlatformMBeanServer();
            }
            try
            {
                mBeanServer.registerMBean(this, objectName);
            }
            catch (InstanceAlreadyExistsException iaee)
            {
                // Try to remove and re-register
                logger.info("Re-registering SchemaLookup MBean");
                mBeanServer.unregisterMBean(objectName);
                mBeanServer.registerMBean(this, objectName);
            }
        }
        catch (Exception e)
        {
            logger.warn("Exception during initialization: ", e);
            throw new RuntimeException(e);
        }
    }

    public void destroy()
    {
        try
        {
            if (objectName != null && mBeanServer != null)
            {
                mBeanServer.unregisterMBean(objectName);
            }
        }
        catch (Exception e)
        {
            logger.warn("Exception unregistering mbean: ", e);
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> listDefaultFilteredFactoryConfigurations()
    {
        List<Map<String, Object>> factoryConfigurations = listFactoryConfigurations(getDefaultLdapFilter());

        return factoryConfigurations;
    }

    public List<Map<String, Object>> listConfigurations(String pidFilter)
    {
        List<Map<String, Object>> json = new ArrayList<Map<String, Object>>();

        configAdminSupport.listConfigurations(json, pidFilter);
        for(Map<String, Object> configuration : json)
        {
            try
            {
                TabularData properties = getProperties((String) configuration.get("id"));
                configuration.put("properties", properties);
            }
            catch (IOException e)
            {
                logger.error("Unable to get properties for: "+configuration.get("id"), e);
            }
        }

        return json;
    }

    public List<Map<String, Object>> listFactoryConfigurations(String pidFilter)
    {
        List<Map<String, Object>> json = new ArrayList<Map<String, Object>>();

        configAdminSupport.listFactoryConfigurations(json, pidFilter);
        for(Map<String, Object> factoryConfiguration : json)
        {
            List<Map<String, Object>> configurations = listConfigurations("(" + SERVICE_FACTORYPID + "=" + factoryConfiguration.get("id") + ")");
            factoryConfiguration.put("configurations", configurations);
        }

        return json;
    }

    private String getDefaultLdapFilter()
    {
        if(filterList != null)
        {
            StringBuilder ldapFilter = new StringBuilder();
            ldapFilter.append("(");
            ldapFilter.append("|");

            for(String fpid : filterList)
            {
                ldapFilter.append("(");
                ldapFilter.append(SERVICE_FACTORYPID);
                ldapFilter.append("=");
                ldapFilter.append(fpid);
                ldapFilter.append(")");
            }

            ldapFilter.append(")");

            return ldapFilter.toString();
        }
        return "("+SERVICE_FACTORYPID+"="+"*)";
    }

    /**
     * @see ddf.ui.admin.api.ConfigurationAdminMBean#createFactoryConfiguration(java.lang.String)
     */
    public String createFactoryConfiguration(String factoryPid) throws IOException
    {
        return createFactoryConfigurationForLocation(factoryPid, null);
    }

    /**
     * @see ddf.ui.admin.api.ConfigurationAdminMBean#createFactoryConfigurationForLocation(java.lang.String, java.lang.String)
     */
    public String createFactoryConfigurationForLocation(String factoryPid, String location) throws IOException {
        if (factoryPid == null || factoryPid.length() < 1) {
            throw new IOException("Argument factoryPid cannot be null or empty");
        }
        Configuration config = configurationAdmin.createFactoryConfiguration(factoryPid);
        config.setBundleLocation(location);
        return config.getPid();
    }

    /**
     * @see ddf.ui.admin.api.ConfigurationAdminMBean#delete(java.lang.String)
     */
    public void delete(String pid) throws IOException {
        deleteForLocation(pid, null);
    }

    /**
     * @see ddf.ui.admin.api.ConfigurationAdminMBean#deleteForLocation(java.lang.String, java.lang.String)
     */
    public void deleteForLocation(String pid, String location) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, location);
        config.delete();
    }

    /**
     * @see ddf.ui.admin.api.ConfigurationAdminMBean#deleteConfigurations(java.lang.String)
     */
    public void deleteConfigurations(String filter) throws IOException {
        if (filter == null || filter.length() < 1) {
            throw new IOException("Argument filter cannot be null or empty");
        }
        Configuration[] configuations = null;
        try {
            configuations = configurationAdmin.listConfigurations(filter);
        } catch (InvalidSyntaxException e) {
            throw new IOException("Invalid filter [" + filter + "] : " + e);
        }
        if (configuations != null) {
            for (Configuration config : configuations) {
                config.delete();
            }
        }
    }

    /**
     * @see ddf.ui.admin.api.ConfigurationAdminMBean#getBundleLocation(java.lang.String)
     */
    public String getBundleLocation(String pid) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, null);
        String bundleLocation = (config.getBundleLocation() == null) ? "Configuration is not yet bound to a bundle location" : config.getBundleLocation();
        return bundleLocation;
    }

    /**
     * @see ddf.ui.admin.api.ConfigurationAdminMBean#getConfigurations(java.lang.String)
     */
    public String[][] getConfigurations(String filter) throws IOException {
        if (filter == null || filter.length() < 1) {
            throw new IOException("Argument filter cannot be null or empty");
        }
        List<String[]> result = new ArrayList<String[]>();
        Configuration[] configurations = null;
        try {
            configurations = configurationAdmin.listConfigurations(filter);
        } catch (InvalidSyntaxException e) {
            throw new IOException("Invalid filter [" + filter + "] : " + e);
        }
        if (configurations != null) {
            for (Configuration config : configurations) {
                result.add(new String[] { config.getPid(), config.getBundleLocation() });
            }
        }
        return result.toArray(new String[result.size()][]);
    }

    /**
     * @see ddf.ui.admin.api.ConfigurationAdminMBean#getFactoryPid(java.lang.String)
     */
    public String getFactoryPid(String pid) throws IOException {
        return getFactoryPidForLocation(pid, null);
    }

    /**
     * @see ddf.ui.admin.api.ConfigurationAdminMBean#getFactoryPidForLocation(java.lang.String, java.lang.String)
     */
    public String getFactoryPidForLocation(String pid, String location) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, location);
        return config.getFactoryPid();
    }

    /**
     * @see ddf.ui.admin.api.ConfigurationAdminMBean#getProperties(java.lang.String)
     */
    public TabularData getProperties(String pid) throws IOException {
        return getPropertiesForLocation(pid, null);
    }

    /**
     * @see ddf.ui.admin.api.ConfigurationAdminMBean#getPropertiesForLocation(java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public TabularData getPropertiesForLocation(String pid, String location) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        TabularData propertiesTable = null;
        Configuration config = configurationAdmin.getConfiguration(pid, location);
        Dictionary<String, Object> properties = config.getProperties();
        if (properties != null) {
            propertiesTable = new TabularDataSupport(PROPERTIES_TYPE);
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                propertiesTable.put(PropertyData.newInstance(key, properties.get(key)).toCompositeData());
            }
        }
        return propertiesTable;
    }

    /**
     * @see ddf.ui.admin.api.ConfigurationAdminMBean#setBundleLocation(java.lang.String, java.lang.String)
     */
    public void setBundleLocation(String pid, String location) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument factoryPid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, null);
        config.setBundleLocation(location);
    }

    /**
     * @see ddf.ui.admin.api.ConfigurationAdminMBean#update(java.lang.String, javax.management.openmbean.TabularData)
     */
    public void update(String pid, TabularData configurationTable) throws IOException {
        updateForLocation(pid, null, configurationTable);
    }

    /**
     * @see ddf.ui.admin.api.ConfigurationAdminMBean#updateForLocation(java.lang.String, java.lang.String, javax.management.openmbean.TabularData)
     */
    @SuppressWarnings("unchecked")
    public void updateForLocation(String pid, String location, TabularData configurationTable) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        if (configurationTable == null) {
            throw new IOException("Argument configurationTable cannot be null");
        }

        if (!PROPERTIES_TYPE.equals(configurationTable.getTabularType())) {
            throw new IOException("Invalid TabularType ["  + configurationTable.getTabularType() + "]");
        }
        Dictionary<String, Object> configurationProperties = new Hashtable<String, Object>();
        Collection<CompositeData> compositeData = (Collection<CompositeData>) configurationTable.values();
        for (CompositeData row: compositeData) {
            PropertyData<? extends Class> propertyData = PropertyData.from(row);
            configurationProperties.put(propertyData.getKey(), propertyData.getValue());
        }
        Configuration config = configurationAdmin.getConfiguration(pid, location);
        config.update(configurationProperties);
    }

    public List<String> getFilterList()
    {
        return filterList;
    }

    public void setFilterList(List<String> filterList)
    {
        this.filterList = filterList;
    }
}

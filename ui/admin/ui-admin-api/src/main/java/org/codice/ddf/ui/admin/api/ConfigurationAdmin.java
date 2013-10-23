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
package org.codice.ddf.ui.admin.api;

import org.codice.ddf.ui.admin.api.plugin.ConfigurationAdminPlugin;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * @author Scott Tustison
 */
public class ConfigurationAdmin implements ConfigurationAdminMBean {
    private static final String DISABLED = "_disabled";

    private final XLogger logger = new XLogger(LoggerFactory.getLogger(ConfigurationAdmin.class));

    private static final String SERVICE_PID = "service.pid";

    private static final String SERVICE_FACTORYPID = "service.factorypid";

    private final org.osgi.service.cm.ConfigurationAdmin configurationAdmin;

    private final ConfigurationAdminExt configurationAdminExt;

    private ObjectName objectName;

    private MBeanServer mBeanServer;

    private List<String> filterList;

    private List<ConfigurationAdminPlugin> configurationAdminPluginList;

    /**
     * Constructs a ConfigurationAdmin implementation
     * 
     * @param configurationAdmin
     *            instance of org.osgi.service.cm.ConfigurationAdmin service
     */
    public ConfigurationAdmin(BundleContext bundleContext,
            org.osgi.service.cm.ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
        configurationAdminExt = new ConfigurationAdminExt(bundleContext, configurationAdmin);
    }

    /**
     * Initialize this MBean and register it with the MBean server
     */
    public void init() {
        try {
            if (objectName == null) {
                objectName = new ObjectName(ConfigurationAdminMBean.OBJECTNAME);
            }
            if (mBeanServer == null) {
                mBeanServer = ManagementFactory.getPlatformMBeanServer();
            }
            try {
                mBeanServer.registerMBean(this, objectName);
            } catch (InstanceAlreadyExistsException iaee) {
                // Try to remove and re-register
                logger.info("Re-registering SchemaLookup MBean");
                mBeanServer.unregisterMBean(objectName);
                mBeanServer.registerMBean(this, objectName);
            }
        } catch (Exception e) {
            logger.warn("Exception during initialization: ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Unregister this MBean with the MBean server
     */
    public void destroy() {
        try {
            if (objectName != null && mBeanServer != null) {
                mBeanServer.unregisterMBean(objectName);
            }
        } catch (Exception e) {
            logger.warn("Exception unregistering mbean: ", e);
            throw new RuntimeException(e);
        }
    }

    public List<ConfigurationAdminPlugin> getConfigurationAdminPluginList() {
        return configurationAdminPluginList;
    }

    public void setConfigurationAdminPluginList(
            List<ConfigurationAdminPlugin> configurationAdminPluginList) {
        this.configurationAdminPluginList = configurationAdminPluginList;
        configurationAdminExt.setConfigurationAdminPluginList(configurationAdminPluginList);
    }

    /**
     * @see ConfigurationAdminMBean#listDefaultFilteredFactoryConfigurations()
     */
    public List<Map<String, Object>> listDefaultFilteredFactoryConfigurations() {
        List<Map<String, Object>> factoryConfigurations = listFactoryConfigurations(getDefaultFactoryLdapFilter());

        return factoryConfigurations;
    }

    /**
     * @see ConfigurationAdminMBean#listConfigurations(String)
     */
    public List<Map<String, Object>> listDefaultFilteredConfigurations() {
        return listConfigurations(getDefaultLdapFilter());
    }

    /**
     * @see ConfigurationAdminMBean#listConfigurations(String)
     */
    public List<Map<String, Object>> listConfigurations(String pidFilter) {
        List<Map<String, Object>> json = new ArrayList<Map<String, Object>>();

        configurationAdminExt.listConfigurations(json, pidFilter);
        for (Map<String, Object> configuration : json) {
            try {
                Map<String, Object> properties = getProperties((String) configuration.get("id"));
                configuration.put("properties", properties);
            } catch (IOException e) {
                logger.error("Unable to get properties for: " + configuration.get("id"), e);
            }
        }

        return json;
    }

    /**
     * @see ConfigurationAdminMBean#listFactoryConfigurations(String)
     */
    public List<Map<String, Object>> listFactoryConfigurations(String pidFilter) {
        List<Map<String, Object>> json = new ArrayList<Map<String, Object>>();

        configurationAdminExt.listFactoryConfigurations(json, pidFilter);
        for (Map<String, Object> factoryConfiguration : json) {
            List<Map<String, Object>> configurations = listConfigurations("(" + SERVICE_FACTORYPID
                    + "=" + factoryConfiguration.get("id") + ")");
            factoryConfiguration.put("configurations", configurations);
        }

        return json;
    }

    private String getDefaultFactoryLdapFilter() {
        if (CollectionUtils.isNotEmpty(filterList)) {
            StringBuilder ldapFilter = new StringBuilder();
            ldapFilter.append("(");
            ldapFilter.append("|");

            for (String fpid : filterList) {
                ldapFilter.append("(");
                ldapFilter.append(SERVICE_FACTORYPID);
                ldapFilter.append("=");
                ldapFilter.append(fpid);
                ldapFilter.append(")");
            }

            ldapFilter.append(")");

            return ldapFilter.toString();
        }
        return "(" + SERVICE_FACTORYPID + "=" + "*)";
    }

    private String getDefaultLdapFilter() {
        if (CollectionUtils.isNotEmpty(filterList)) {
            StringBuilder ldapFilter = new StringBuilder();
            ldapFilter.append("(");
            ldapFilter.append("|");

            for (String fpid : filterList) {
                ldapFilter.append("(");
                ldapFilter.append(SERVICE_PID);
                ldapFilter.append("=");
                ldapFilter.append(fpid);
                ldapFilter.append("*");
                ldapFilter.append(")");
            }

            ldapFilter.append(")");

            return ldapFilter.toString();
        }
        return "(" + SERVICE_PID + "=" + "*)";
    }

    /**
     * @see ConfigurationAdminMBean#createFactoryConfiguration(java.lang.String)
     */
    public String createFactoryConfiguration(String factoryPid) throws IOException {
        return createFactoryConfigurationForLocation(factoryPid, null);
    }

    /**
     * @see ConfigurationAdminMBean#createFactoryConfigurationForLocation(java.lang.String,
     *      java.lang.String)
     */
    public String createFactoryConfigurationForLocation(String factoryPid, String location)
        throws IOException {
        if (factoryPid == null || factoryPid.length() < 1) {
            throw new IOException("Argument factoryPid cannot be null or empty");
        }
        Configuration config = configurationAdmin.createFactoryConfiguration(factoryPid);
        config.setBundleLocation(location);
        return config.getPid();
    }

    /**
     * @see ConfigurationAdminMBean#delete(java.lang.String)
     */
    public void delete(String pid) throws IOException {
        deleteForLocation(pid, null);
    }

    /**
     * @see ConfigurationAdminMBean#deleteForLocation(java.lang.String,
     *      java.lang.String)
     */
    public void deleteForLocation(String pid, String location) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, location);
        config.delete();
    }

    /**
     * @see ConfigurationAdminMBean#deleteConfigurations(java.lang.String)
     */
    public void deleteConfigurations(String filter) throws IOException {
        if (filter == null || filter.length() < 1) {
            throw new IOException("Argument filter cannot be null or empty");
        }
        Configuration[] configuations;
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
     * @see ConfigurationAdminMBean#getBundleLocation(java.lang.String)
     */
    public String getBundleLocation(String pid) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, null);
        String bundleLocation = (config.getBundleLocation() == null) ? "Configuration is not yet bound to a bundle location"
                : config.getBundleLocation();
        return bundleLocation;
    }

    /**
     * @see ConfigurationAdminMBean#getConfigurations(java.lang.String)
     */
    public String[][] getConfigurations(String filter) throws IOException {
        if (filter == null || filter.length() < 1) {
            throw new IOException("Argument filter cannot be null or empty");
        }
        List<String[]> result = new ArrayList<String[]>();
        Configuration[] configurations;
        try {
            configurations = configurationAdmin.listConfigurations(filter);
        } catch (InvalidSyntaxException e) {
            throw new IOException("Invalid filter [" + filter + "] : " + e);
        }
        if (configurations != null) {
            for (Configuration config : configurations) {
                result.add(new String[] {config.getPid(), config.getBundleLocation()});
            }
        }
        return result.toArray(new String[result.size()][]);
    }

    /**
     * @see ConfigurationAdminMBean#getFactoryPid(java.lang.String)
     */
    public String getFactoryPid(String pid) throws IOException {
        return getFactoryPidForLocation(pid, null);
    }

    /**
     * @see ConfigurationAdminMBean#getFactoryPidForLocation(java.lang.String,
     *      java.lang.String)
     */
    public String getFactoryPidForLocation(String pid, String location) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, location);
        return config.getFactoryPid();
    }

    /**
     * @see ConfigurationAdminMBean#getProperties(java.lang.String)
     */
    public Map<String, Object> getProperties(String pid) throws IOException {
        return getPropertiesForLocation(pid, null);
    }

    /**
     * @see ConfigurationAdminMBean#getPropertiesForLocation(java.lang.String,
     *      java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPropertiesForLocation(String pid, String location)
        throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        Map<String, Object> propertiesTable = new HashMap<String, Object>();
        Configuration config = configurationAdmin.getConfiguration(pid, location);
        Dictionary<String, Object> properties = config.getProperties();
        if (properties != null) {
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                propertiesTable.put(key, properties.get(key));
            }
        }
        return propertiesTable;
    }

    /**
     * @see ConfigurationAdminMBean#setBundleLocation(java.lang.String,
     *      java.lang.String)
     */
    public void setBundleLocation(String pid, String location) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument factoryPid cannot be null or empty");
        }
        Configuration config = configurationAdmin.getConfiguration(pid, null);
        config.setBundleLocation(location);
    }

    /**
     * @see ConfigurationAdminMBean#update(java.lang.String, java.util.Map)
     */
    public void update(String pid, Map<String, Object> configurationTable) throws IOException {
        updateForLocation(pid, null, configurationTable);
    }

    /**
     * @see ConfigurationAdminMBean#updateForLocation(java.lang.String,
     *      java.lang.String, java.util.Map)
     */
    @SuppressWarnings("unchecked")
    public void updateForLocation(String pid, String location,
            Map<String, Object> configurationTable) throws IOException {
        if (pid == null || pid.length() < 1) {
            throw new IOException("Argument pid cannot be null or empty");
        }
        if (configurationTable == null) {
            throw new IOException("Argument configurationTable cannot be null");
        }

        for (String key : configurationTable.keySet()) // sanity check to make sure no values are
                                                       // null
        {
            if (configurationTable.get(key) == null) {
                configurationTable.put(key, "");
            }
        }

        Dictionary<String, Object> configurationProperties = new Hashtable<String, Object>(
                configurationTable);
        Configuration config = configurationAdmin.getConfiguration(pid, location);
        config.update(configurationProperties);
    }

    public List<String> getFilterList() {
        return filterList;
    }

    public void setFilterList(List<String> filterList) {
        this.filterList = filterList;
    }

    public void disableConfiguration(String servicePid) throws IOException {
        if (StringUtils.isEmpty(servicePid)) {
            throw new IOException(
                    "Service PID of Source to be disabled must be specified.  Service PID provided: "
                            + servicePid);
        }

        Configuration originalConfig = configurationAdminExt.getConfiguration(servicePid);

        if (originalConfig == null) {
            throw new IOException("No Source exists with the service PID: " + servicePid);
        }

        Dictionary properties = originalConfig.getProperties();
        String originalFactoryPid = (String) properties
                .get(org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID);
        if (StringUtils.endsWith(originalFactoryPid, DISABLED)) {
            throw new IOException("Source is already disabled.");
        }

        // Copy configuration from the original configuration and change its factory PID to end with
        // "disabled"
        String disabledServiceFactoryPid = originalFactoryPid + DISABLED;
        properties.put(org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID,
                disabledServiceFactoryPid);
        Configuration disabledConfig = configurationAdmin.createFactoryConfiguration(
                disabledServiceFactoryPid, null);
        disabledConfig.update(properties);

        // remove original configuration
        originalConfig.delete();
    }

    public void enableConfiguration(String servicePid) throws IOException {
        if (StringUtils.isEmpty(servicePid)) {
            throw new IOException(
                    "Service PID of Source to be disabled must be specified.  Service PID provided: "
                            + servicePid);
        }

        Configuration disabledConfig = configurationAdminExt.getConfiguration(servicePid);

        if (disabledConfig == null) {
            throw new IOException("No Source exists with the service PID: " + servicePid);
        }

        Dictionary properties = disabledConfig.getProperties();
        String disabledFactoryPid = (String) properties
                .get(org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID);
        if (!StringUtils.endsWith(disabledFactoryPid, DISABLED)) {
            throw new IOException("Source is already enabled.");
        }

        String enabledFactoryPid = StringUtils.removeEnd(disabledFactoryPid, DISABLED);
        properties
                .put(org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID, enabledFactoryPid);
        Configuration enabledConfiguration = configurationAdmin.createFactoryConfiguration(
                enabledFactoryPid, null);
        enabledConfiguration.update(properties);

        disabledConfig.delete();
    }
}

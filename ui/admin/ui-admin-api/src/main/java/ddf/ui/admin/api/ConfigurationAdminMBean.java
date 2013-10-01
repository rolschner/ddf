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

import javax.management.openmbean.TabularData;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Scott Tustison
 */
public interface ConfigurationAdminMBean
{
    static String OBJECTNAME = "ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0";

    /**
     * Lists all ConfigurationAdmin Configurations that have been created. Filter is
     * an LDAP style filter.
     * @param pidFilter the string representation of the
     *        <code>org.osgi.framework.Filter</code>
     * @return the List of all Configurations
     */
    List<Map<String, Object>> listConfigurations(String pidFilter);

    /**
     * Lists all default filtered Configuration instances.
     * @return the List of all filtered Configurations
     */
    public List<Map<String, Object>> listDefaultFilteredConfigurations();

    /**
     * Lists all ManagedServiceFactory instances with associated Configurations. Filter is
     * an LDAP style filter.
     * @param pidFilter the string representation of the
     *        <code>org.osgi.framework.Filter</code>
     * @return the List of all ManagedServiceFactory with associated Configurations
     */
    List<Map<String, Object>> listFactoryConfigurations(String pidFilter);

    /**
     * Lists all default filtered ManagedServiceFactory instances with associated Configurations.
     * @return the List of all filtered ManagedServiceFactory with associated Configurations
     */
    List<Map<String, Object>> listDefaultFilteredFactoryConfigurations();

    /**
     * Create a new configuration instance for the supplied persistent id of the
     * factory, answering the PID of the created configuration
     *
     * @param factoryPid the persistent id of the factory
     * @return the PID of the created configuration
     * @throws java.io.IOException if the operation failed
     */
    String createFactoryConfiguration(String factoryPid) throws IOException;

    /**
     * Create a factory configuration for the supplied persistent id of the
     * factory and the bundle location bound to bind the created configuration
     * to, answering the PID of the created configuration
     *
     * @param factoryPid the persistent id of the factory
     * @param location the bundle location
     * @return the pid of the created configuation
     * @throws IOException if the operation failed
     */
    String createFactoryConfigurationForLocation(String factoryPid, String location)
            throws IOException;

    /**
     * Delete the configuration
     *
     * @param pid the persistent identifier of the configuration
     * @throws IOException if the operation fails
     */
    void delete(String pid) throws IOException;

    /**
     * Delete the configuration
     *
     * @param pid the persistent identifier of the configuration
     * @param location the bundle location
     * @throws IOException if the operation fails
     */
    void deleteForLocation(String pid, String location) throws IOException;

    /**
     * Delete the configurations matching the filter specification.
     *
     * @param filter the string representation of the
     *        <code>org.osgi.framework.Filter</code>
     * @throws IOException if the operation failed
     * @throws IllegalArgumentException if the filter is invalid
     */
    void deleteConfigurations(String filter) throws IOException;

    /**
     * Answer the bundle location the configuration is bound to
     *
     * @param pid the persistent identifier of the configuration
     * @return the bundle location
     * @throws IOException if the operation fails
     */
    String getBundleLocation(String pid) throws IOException;

    /**
     * Answer the factory PID if the configuration is a factory configuration,
     * null otherwise.
     *
     * @param pid the persistent identifier of the configuration
     * @return the factory PID
     * @throws IOException if the operation fails
     */
    String getFactoryPid(String pid) throws IOException;

    /**
     * Answer the factory PID if the configuration is a factory configuration,
     * null otherwise.
     *
     * @param pid the persistent identifier of the configuration
     * @param location the bundle location
     * @return the factory PID
     * @throws IOException if the operation fails
     */
    String getFactoryPidForLocation(String pid, String location) throws IOException;

    /**
     * Answer the contents of the configuration <p/>
     *
     * @see org.osgi.jmx.JmxConstants#PROPERTIES_TYPE for the details of the TabularType
     *
     * @param pid the persistent identifier of the configuration
     * @return the table of contents
     * @throws IOException if the operation fails
     */

    Map<String, Object> getProperties(String pid) throws IOException;

    /**
     * Answer the contents of the configuration <p/>
     *
     * @see org.osgi.jmx.JmxConstants#PROPERTIES_TYPE for the details of the TabularType
     *
     * @param pid the persistent identifier of the configuration
     * @param location the bundle location
     * @return the table of contents
     * @throws IOException if the operation fails
     */
    Map<String, Object> getPropertiesForLocation(String pid, String location) throws IOException;

    /**
     * Answer the list of PID/Location pairs of the configurations managed by
     * this service
     *
     * @param filter the string representation of the
     *        <code>org.osgi.framework.Filter</code>
     * @return the list of configuration PID/Location pairs
     * @throws IOException if the operation failed
     * @throws IllegalArgumentException if the filter is invalid
     */
    String[][] getConfigurations(String filter) throws IOException;

    /**
     * Set the bundle location the configuration is bound to
     *
     * @param pid the persistent identifier of the configuration
     * @param location the bundle location
     * @throws IOException if the operation fails
     */
    void setBundleLocation(String pid, String location) throws IOException;

    /**
     * Update the configuration with the supplied properties For each property
     * entry, the following row is supplied <p/>
     *
     * @see org.osgi.jmx.JmxConstants#PROPERTIES_TYPE for the details of the TabularType
     *
     * @param pid the persistent identifier of the configuration
     * @param configurationTable the table of properties
     * @throws IOException if the operation fails
     */
    void update(String pid, Map<String, Object> configurationTable) throws IOException;

    /**
     * Update the configuration with the supplied properties For each property
     * entry, the following row is supplied <p/>
     *
     * @see org.osgi.jmx.JmxConstants#PROPERTIES_TYPE for the details of the TabularType
     *
     * @param pid the persistent identifier of the configuration
     * @param location the bundle location
     * @param configurationTable the table of properties
     * @throws IOException if the operation fails
     */
    void updateForLocation(String pid, String location, Map<String, Object> configurationTable)
            throws IOException;
    
    /**
     * Enables a previously disabled {@link Source} to be able to be queried by the {@link CatalogFramework}.
     * 
     * @param servicePid that uniquely identifies the source to enable.
     * @throws IOException if the Source to be enabled cannot be located via the provided service PID.
     */
    void enableSource(String servicePid) throws IOException;
    
    /**
     * Disables a {@link Source} to prevent it from being queried by the {@link CatalogFramework} but preserves
     * the configuration to be enabled at a later time.
     * 
     * @param servicePid that uniquely identifies the source to disable.
     * @throws IOException if the Source to be disabled cannot be located via the provided service PID.
     */
    void disableSource(String servicePid) throws IOException;
}

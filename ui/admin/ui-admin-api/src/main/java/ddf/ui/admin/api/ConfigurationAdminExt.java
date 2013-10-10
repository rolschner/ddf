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


import ddf.ui.admin.api.plugin.ConfigurationAdminPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;


class ConfigurationAdminExt
{
    static final String META_TYPE_NAME = "org.osgi.service.metatype.MetaTypeService";
    private final XLogger logger = new XLogger(LoggerFactory.getLogger(ConfigurationAdminExt.class));

    private final BundleContext bundleContext;
    private final ConfigurationAdmin service;

    private final Map<String, ServiceTracker> services = new HashMap<String, ServiceTracker>();

    private List<ConfigurationAdminPlugin> configurationAdminPluginList;

    /**
     * @param bundleContext
     * @param service
     * @throws ClassCastException if {@code service} is not a MetaTypeService instances
     */
    ConfigurationAdminExt(final BundleContext bundleContext, final Object service)
    {
        this.bundleContext = bundleContext;
        this.service = (ConfigurationAdmin) service;
    }


    BundleContext getBundleContext()
    {
        return bundleContext;
    }

    final Configuration getConfiguration(String pid)
    {
        if (pid != null)
        {
            try
            {
                // we use listConfigurations to not create configuration
                // objects persistently without the user providing actual
                // configuration
                String filter = '(' + Constants.SERVICE_PID + '=' + pid + ')';
                Configuration[] configs = this.service.listConfigurations(filter);
                if (configs != null && configs.length > 0)
                {
                    return configs[0];
                }
            }
            catch (InvalidSyntaxException ise)
            {
                // should print message
            }
            catch (IOException ioe)
            {
                // should print message
            }
        }

        // fallback to no configuration at all
        return null;
    }

    private final Bundle getBoundBundle(Configuration config)
    {
        if (null == config)
            return null;
        final String location = config.getBundleLocation();
        if (null == location)
            return null;

        final Bundle bundles[] = getBundleContext().getBundles();
        for (int i = 0; bundles != null && i < bundles.length; i++)
        {
            if (bundles[i].getLocation().equals(location))
                return bundles[i];

        }
        return null;
    }


    final void listConfigurations(List<Map<String, Object>> json, String pidFilter)
    {
        try
        {
            // Get ManagedService instances
            List<Map<String, Object>> serviceList = getServices(ManagedService.class.getName(), pidFilter, true);
            json.addAll(serviceList);
            // Get Metatypes
            List<Map<String, Object>> metatypeList = addMetaTypeNamesToMap(getPidObjectClasses(), pidFilter, Constants.SERVICE_PID);
            json.addAll(metatypeList);
            // Get configurations
            Configuration[] cfgs = service.listConfigurations(pidFilter);
            for (int i = 0; cfgs != null && i < cfgs.length; i++)
            {

                // ignore configuration object if an entry already exists in the map
                // or if it is invalid
                final String pid = cfgs[i].getPid();
                if(!isAllowedPid(pid))
                {
                    continue;
                }
                else
                {
                    boolean skip = false;
                    for(Map<String, Object> data : json)
                    {
                        if(data.get("id").equals(pid))
                        {
                            skip = true;
                        }
                    }
                    if(skip)
                    {
                        continue;
                    }
                }

                // insert an entry for the PID
                try
                {
                    ObjectClassDefinition ocd = getObjectClassDefinition(cfgs[i]);
                    if (ocd != null)
                    {
                        Map<String, Object> data = new HashMap<String, Object>();
                        data.put("id", pid);
                        data.put("name", ocd.getName());
                        json.add(data);
                        continue;
                    }
                }
                catch (IllegalArgumentException t)
                {
                    // Catch exception thrown by getObjectClassDefinition so other configurations are displayed
                }

                // no object class definition, use plain PID
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("id", pid);
                data.put("name", pid);
                json.add(data);
            }

            // add configuration data
            for(Map<String, Object> data : json)
            {
                Object id = data.get("id");
                final Configuration config = this.getConfiguration((String) id);
                if (null != config)
                {
                    final String fpid = config.getFactoryPid();
                    if (null != fpid)
                    {
                        data.put("fpid", fpid);
                    }

                    final Bundle bundle = getBoundBundle(config);
                    if (null != bundle)
                    {
                        data.put("bundle", bundle.getBundleId());
                        data.put("bundle_name", getName(bundle));
                    }

                    Map<String, Object> pluginDataMap = getConfigurationPluginData(id.toString(), Collections.unmodifiableMap(data));
                    if(pluginDataMap != null && !pluginDataMap.isEmpty())
                    {
                        data.putAll(pluginDataMap);
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.error("listConfigurations: Unexpected problem encountered", e);
        }
    }

    private Map<String, Object> getConfigurationPluginData(String servicePid, Map<String, Object> dataMap)
    {
        Map<String, Object> allPluginMap = new HashMap<String, Object>();
        if(configurationAdminPluginList != null)
        {
            for(ConfigurationAdminPlugin plugin : configurationAdminPluginList)
            {
                Map<String, Object> pluginDataMap = plugin.getConfigurationData(servicePid, dataMap, bundleContext);
                allPluginMap.putAll(pluginDataMap);
            }
        }
        return allPluginMap;
    }

    /**
     * Return a display name for the given <code>bundle</code>:
     * <ol>
     * <li>If the bundle has a non-empty <code>Bundle-Name</code> manifest
     * header that value is returned.</li>
     * <li>Otherwise the symbolic name is returned if set</li>
     * <li>Otherwise the bundle's location is returned if defined</li>
     * <li>Finally, as a last resort, the bundles id is returned</li>
     * </ol>
     *
     * @param bundle the bundle which name to retrieve
     * @return the bundle name - see the description of the method for more details.
     */
    String getName(Bundle bundle)
    {
        Locale locale = Locale.getDefault();
        final String loc = locale == null ? null : locale.toString();
        String name = (String) bundle.getHeaders(loc).get(Constants.BUNDLE_NAME);
        if (name == null || name.length() == 0)
        {
            name = bundle.getSymbolicName();
            if (name == null)
            {
                name = bundle.getLocation();
                if (name == null)
                {
                    name = String.valueOf(bundle.getBundleId());
                }
            }
        }
        return name;
    }

    final boolean isAllowedPid(final String pid)
    {
        for (int i = 0; i < pid.length(); i++)
        {
            final char c = pid.charAt(i);
            if (c == '&' || c == '<' || c == '>' || c == '"' || c == '\'')
            {
                return false;
            }
        }
        return true;
    }

    public void setConfigurationAdminPluginList(List<ConfigurationAdminPlugin> configurationAdminPluginList)
    {
        this.configurationAdminPluginList = configurationAdminPluginList;
    }

    /**
     * The <code>IdGetter</code> interface is an internal helper to abstract
     * retrieving object class definitions from all bundles for either
     * pids or factory pids.
     *
     * @see #PID_GETTER
     * @see #FACTORY_PID_GETTER
     */
    private static interface IdGetter
    {
        String[] getIds(MetaTypeInformation metaTypeInformation);
    }

    /**
     * The implementation of the {@link IdGetter} interface returning the PIDs
     * listed in the meta type information.
     *
     * @see #getPidObjectClasses()
     */
    private static final IdGetter PID_GETTER = new IdGetter()
    {
        public String[] getIds(MetaTypeInformation metaTypeInformation)
        {
            return metaTypeInformation.getPids();
        }
    };

    /**
     * The implementation of the {@link IdGetter} interface returning the
     * factory PIDs listed in the meta type information.
     */
    private static final IdGetter FACTORY_PID_GETTER = new IdGetter()
    {
        public String[] getIds(MetaTypeInformation metaTypeInformation)
        {
            return metaTypeInformation.getFactoryPids();
        }
    };

    /**
     * Returns a map of PIDs and providing bundles of MetaType information. The
     * map is indexed by PID and the value of each entry is the bundle providing
     * the MetaType information for that PID.
     *
     * @return see the method description
     */
    Map getPidObjectClasses()
    {
        return getObjectClassDefinitions(PID_GETTER);
    }

    /**
     * Returns the <code>ObjectClassDefinition</code> objects for the IDs
     * returned by the <code>idGetter</code>. Depending on the
     * <code>idGetter</code> implementation this will be for factory PIDs or
     * plain PIDs.
     *
     * @param idGetter The {@link IdGetter} used to get the list of factory PIDs
     *                 or PIDs from <code>MetaTypeInformation</code> objects.
     * @return Map of <code>ObjectClassDefinition</code> objects indexed by the
     *         PID (or factory PID) to which they pertain
     */
    private Map getObjectClassDefinitions(final IdGetter idGetter)
    {
        Locale locale = Locale.getDefault();
        final Map objectClassesDefinitions = new HashMap();
        final MetaTypeService mts = this.getMetaTypeService();
        if (mts != null)
        {
            final Bundle[] bundles = this.getBundleContext().getBundles();
            for (int i = 0; i < bundles.length; i++)
            {
                final MetaTypeInformation mti = mts.getMetaTypeInformation(bundles[i]);
                if (mti != null)
                {
                    final String[] idList = idGetter.getIds(mti);
                    for (int j = 0; idList != null && j < idList.length; j++)
                    {
                        // After getting the list of PIDs, a configuration  might be
                        // removed. So the getObjectClassDefinition will throw
                        // an exception, and this will prevent ALL configuration from
                        // being displayed. By catching it, the configurations will be
                        // visible
                        ObjectClassDefinition ocd = null;
                        try
                        {
                            ocd = mti.getObjectClassDefinition(idList[j], locale.toString());
                        }
                        catch (IllegalArgumentException ignore)
                        {
                            // ignore - just don't show this configuration
                        }
                        if (ocd != null)
                        {
                            objectClassesDefinitions.put(idList[j], ocd);
                        }
                    }
                }
            }
        }
        return objectClassesDefinitions;
    }

    ObjectClassDefinition getObjectClassDefinition(Configuration config)
    {
        // if the configuration is bound, try to get the object class
        // definition from the bundle installed from the given location
        if (config.getBundleLocation() != null)
        {
            Bundle bundle = getBundle(this.getBundleContext(), config.getBundleLocation());
            if (bundle != null)
            {
                String id = config.getFactoryPid();
                if (null == id)
                {
                    id = config.getPid();
                }
                return getObjectClassDefinition(bundle, id);
            }
        }

        // get here if the configuration is not bound or if no
        // bundle with the bound location is installed. We search
        // all bundles for a matching [factory] PID
        // if the configuration is a factory one, use the factory PID
        if (config.getFactoryPid() != null)
        {
            return getObjectClassDefinition(config.getFactoryPid());
        }

        // otherwise use the configuration PID
        return getObjectClassDefinition(config.getPid());
    }

    ObjectClassDefinition getObjectClassDefinition(Bundle bundle, String pid)
    {
        Locale locale = Locale.getDefault();
        if (bundle != null)
        {
            MetaTypeService mts = this.getMetaTypeService();
            if (mts != null)
            {
                MetaTypeInformation mti = mts.getMetaTypeInformation(bundle);
                if (mti != null)
                {
                    // see #getObjectClasses( final IdGetter idGetter, final String locale )
                    try
                    {
                        return mti.getObjectClassDefinition(pid, locale.toString());
                    }
                    catch (IllegalArgumentException e)
                    {
                        // MetaTypeProvider.getObjectClassDefinition might throw illegal
                        // argument exception. So we must catch it here, otherwise the
                        // other configurations will not be shown
                        // See https://issues.apache.org/jira/browse/FELIX-2390
                        // https://issues.apache.org/jira/browse/FELIX-3694
                    }
                }
            }
        }

        // fallback to nothing found
        return null;
    }

    MetaTypeService getMetaTypeService()
    {
        return (MetaTypeService) getService(META_TYPE_NAME);
    }

    /**
     * Gets the service with the specified class name. Will create a new
     * {@link ServiceTracker} if the service is not already retrieved.
     *
     * @param serviceName the service name to obtain
     * @return the service or <code>null</code> if missing.
     */
    final Object getService(String serviceName)
    {
        ServiceTracker serviceTracker = services.get(serviceName);
        if (serviceTracker == null)
        {
            serviceTracker = new ServiceTracker(getBundleContext(), serviceName, null);
            serviceTracker.open();

            services.put(serviceName, serviceTracker);
        }

        return serviceTracker.getService();
    }

    ObjectClassDefinition getObjectClassDefinition(String pid)
    {
        Bundle[] bundles = this.getBundleContext().getBundles();
        for (int i = 0; i < bundles.length; i++)
        {
            try
            {
                ObjectClassDefinition ocd = this.getObjectClassDefinition(bundles[i], pid);
                if (ocd != null)
                {
                    return ocd;
                }
            }
            catch (IllegalArgumentException iae)
            {
                // don't care
            }
        }
        return null;
    }

    static Bundle getBundle(final BundleContext bundleContext, final String bundleLocation)
    {
        if (bundleLocation == null)
        {
            return null;
        }

        Bundle[] bundles = bundleContext.getBundles();
        for (int i = 0; i < bundles.length; i++)
        {
            if (bundleLocation.equals(bundles[i].getLocation()))
            {
                return bundles[i];
            }
        }

        return null;
    }


    final void listFactoryConfigurations(List<Map<String, Object>> json, String pidFilter)
    {
        try
        {
            List<Map<String, Object>> serviceList = getServices(ManagedServiceFactory.class.getName(),
                    pidFilter, true);
            List<Map<String, Object>> metatypeList = addMetaTypeNamesToMap(getFactoryPidObjectClasses(), pidFilter,
                    ConfigurationAdmin.SERVICE_FACTORYPID);

            json.addAll(serviceList);
            json.addAll(metatypeList);
        }
        catch (Exception e)
        {
            logger.error("listFactoryConfigurations: Unexpected problem encountered", e);
        }
    }

    /**
     * Returns a map of factory PIDs and providing bundles of MetaType
     * information. The map is indexed by factory PID and the value of each
     * entry is the bundle providing the MetaType information for that factory
     * PID.
     *
     * @return see the method description
     */
    Map getFactoryPidObjectClasses()
    {
        return getObjectClassDefinitions(FACTORY_PID_GETTER);
    }

    List<Map<String, Object>> getServices(String serviceClass, String serviceFilter,
                          boolean ocdRequired) throws InvalidSyntaxException
    {
        List<Map<String, Object>> serviceList = new ArrayList<Map<String, Object>>();

        // find all ManagedServiceFactories to get the factoryPIDs
        ServiceReference[] refs = this.getBundleContext().getServiceReferences(serviceClass, serviceFilter);
        for (int i = 0; refs != null && i < refs.length; i++)
        {
            Object pidObject = refs[i].getProperty(Constants.SERVICE_PID);
            // only include valid PIDs
            if (pidObject instanceof String && isAllowedPid((String) pidObject))
            {
                String pid = (String) pidObject;
                String name = pid;
                boolean haveOcd = !ocdRequired;
                final ObjectClassDefinition ocd = getObjectClassDefinition(refs[i].getBundle(), pid);
                if (ocd != null)
                {
                    name = ocd.getName();
                    haveOcd = true;
                }

                if (haveOcd)
                {
                    Map<String, Object> service = new HashMap<String, Object>();
                    service.put("id", pid);
                    service.put("name", name);
                    serviceList.add(service);
                }
            }
        }

        return serviceList;
    }

    private List<Map<String, Object>> addMetaTypeNamesToMap(final Map ocdCollection, final String filterSpec, final String type)
    {
        Filter filter = null;
        if (filterSpec != null)
        {
            try
            {
                filter = getBundleContext().createFilter(filterSpec);
            }
            catch (InvalidSyntaxException not_expected)
            {
                /* filter is correct */
            }
        }

        List<Map<String, Object>> metatypeList = new ArrayList<Map<String, Object>>();
        for (Iterator ei = ocdCollection.entrySet().iterator(); ei.hasNext(); )
        {
            Entry ociEntry = (Entry) ei.next();
            final String pid = (String) ociEntry.getKey();
            final ObjectClassDefinition ocd = (ObjectClassDefinition) ociEntry.getValue();
            if (filter == null)
            {
                Map<String, Object> metatype = new HashMap<String, Object>();
                metatype.put("id", pid);
                metatype.put("name", ocd.getName());
                AttributeDefinition[] defs = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
                metatype.put("metatype", createMetatypeMap(defs));
                metatypeList.add(metatype);
            }
            else
            {
                final Dictionary props = new Hashtable();
                props.put(type, pid);
                if (filter.match(props))
                {
                    Map<String, Object> metatype = new HashMap<String, Object>();
                    metatype.put("id", pid);
                    metatype.put("name", ocd.getName());
                    AttributeDefinition[] defs = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
                    metatype.put("metatype", createMetatypeMap(defs));
                    metatypeList.add(metatype);
                }
            }
        }
        return metatypeList;
    }

    private List<Map<String, Object>> createMetatypeMap(AttributeDefinition[] definitions)
    {
        List<Map<String, Object>> metatypeList = new ArrayList<Map<String, Object>>();

        if(definitions != null)
        {
            for(AttributeDefinition definition : definitions)
            {
                Map<String, Object> attributeMap = new HashMap<String, Object>();
                attributeMap.put("id",definition.getID());
                attributeMap.put("name", definition.getName());
                attributeMap.put("cardinality", definition.getCardinality());
                attributeMap.put("defaultValue", definition.getDefaultValue());
                attributeMap.put("description", definition.getDescription());
                attributeMap.put("type", definition.getType());
                attributeMap.put("optionLabels", definition.getOptionLabels());
                attributeMap.put("optionValues", definition.getOptionValues());
                metatypeList.add(attributeMap);
            }
        }

        return metatypeList;
    }

}

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


import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
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
    static final String PLACEHOLDER_PID = "[Temporary PID replaced by real PID upon save]";
    static final String FACTORY_PID = "factoryPid";
    private final XLogger logger = new XLogger(LoggerFactory.getLogger(ConfigurationAdminExt.class));

    private final BundleContext bundleContext;
    private final ConfigurationAdmin service;

    // used to obtain services. Structure is: service name -> ServiceTracker
    private final Map services = new HashMap();

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


    public BundleContext getBundleContext()
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


    final Configuration getConfiguration(String pid, String factoryPid) throws IOException
    {
        if (factoryPid != null && (pid == null || pid.equals(PLACEHOLDER_PID)))
        {
            return this.service.createFactoryConfiguration(factoryPid, null);
        }

        return this.service.getConfiguration(pid, null);
    }


    Configuration getPlaceholderConfiguration(final String factoryPid)
    {
        return new PlaceholderConfiguration(factoryPid);
    }

    String getPlaceholderPid()
    {
        return PLACEHOLDER_PID;
    }
/*
    String applyConfiguration( HttpServletRequest request, String pid )
        throws IOException
    {
        if ( request.getParameter( ConfigManager.ACTION_DELETE ) != null ) 
        {
            // only delete if the PID is not our place holder
            if ( !ConfigManager.PLACEHOLDER_PID.equals( pid ) )
            {
                configManager.log( "applyConfiguration: Deleting configuration " + pid );
                Configuration config = service.getConfiguration( pid, null );
                config.delete();
            }
            return null; // return request.getHeader( "Referer" );
        }

        String factoryPid = request.getParameter( ConfigManager.FACTORY_PID );
        Configuration config = null;

        String propertyList = request.getParameter( ConfigManager.PROPERTY_LIST ); 
        if ( propertyList == null )
        {
            // FIXME: this would be a bug !!
        }
        else
        {
            config = getConfiguration( pid, factoryPid );

            Dictionary props = config.getProperties();
            if ( props == null )
            {
                props = new Hashtable();
            }

            final MetaTypeServiceSupport mtss = getMetaTypeSupport();
            final Map adMap = ( mtss != null ) ? mtss.getAttributeDefinitionMap( config, null ) : new HashMap();
            final StringTokenizer propTokens = new StringTokenizer( propertyList, "," ); 
            while ( propTokens.hasMoreTokens() )
            {
                String propName = propTokens.nextToken();
                PropertyDescriptor ad = (PropertyDescriptor) adMap.get( propName );

                // try to derive from current value
                if (ad == null) {
                    Object currentValue = props.get( propName );
                    ad = MetaTypeSupport.createAttributeDefinition( propName, currentValue );
                }

                int attributeType = MetaTypeSupport.getAttributeType( ad );

                if ( ad == null
                    || ( ad.getCardinality() == 0 && ( attributeType == AttributeDefinition.STRING || attributeType == MetaTypeServiceSupport.ATTRIBUTE_TYPE_PASSWORD ) ) )
                {
                    String prop = request.getParameter( propName );
                    if ( prop != null
                        && ( attributeType != MetaTypeSupport.ATTRIBUTE_TYPE_PASSWORD || !MetaTypeSupport.PASSWORD_PLACEHOLDER_VALUE.equals( prop ) ) )
                    {
                        props.put( propName, prop );
                    }
                }
                else if ( ad.getCardinality() == 0 )
                {
                    // scalar of non-string
                    String prop = request.getParameter( propName );
                    if ( prop != null )
                    {
                        try
                        {
                            props.put( propName, MetaTypeSupport.toType( attributeType, prop ) );
                        }
                        catch ( NumberFormatException nfe )
                        {
                            // don't care
                        }
                    }
                }
                else
                {
                    // array or vector of any type
                    Vector vec = new Vector();

                    String[] properties = request.getParameterValues( propName );
                    if ( properties != null )
                    {
                        if ( attributeType == MetaTypeSupport.ATTRIBUTE_TYPE_PASSWORD )
                        {
                            MetaTypeSupport.setPasswordProps( vec, properties, props.get( propName ) );
                        }
                        else
                        {
                            for ( int i = 0; i < properties.length; i++ )
                            {
                                try
                                {
                                    vec.add( MetaTypeSupport.toType( attributeType, properties[i] ) );
                                }
                                catch ( NumberFormatException nfe )
                                {
                                    // don't care
                                }
                            }
                        }
                    }

                    // but ensure size (check for positive value since
                    // abs(Integer.MIN_VALUE) is still INTEGER.MIN_VALUE)
                    int maxSize = Math.abs( ad.getCardinality() );
                    if ( vec.size() > maxSize && maxSize > 0 )
                    {
                        vec.setSize( maxSize );
                    }

                    if ( ad.getCardinality() < 0 )
                    {
                        // keep the vector, but only add if not empty
                        if ( vec.isEmpty() )
                        {
                            props.remove( propName );
                        }
                        else
                        {
                            props.put( propName, vec );
                        }
                    }
                    else
                    {
                        // convert to an array
                        props.put( propName, MetaTypeSupport.toArray( attributeType, vec ) );
                    }
                }
            }

            config.update( props );
        }

        // redirect to the new configuration (if existing)
        return (config != null) ? config.getPid() : ""; 
    }


    void printConfigurationJson( PrintWriter pw, String pid, Configuration config, String pidFilter,
        String locale )
    {

        JSONWriter result = new JSONWriter( pw );

        if ( pid != null )
        {
            try
            {
                result.object();
                this.configForm( result, pid, config, pidFilter, locale );
                result.endObject();
            }
            catch ( Exception e )
            {
                logger.error( "Error reading configuration PID " + pid, e );
            }
        }

    }


    void configForm( JSONWriter json, String pid, Configuration config, String pidFilter, String locale )
        throws JSONException
    {

        json.key( ConfigManager.PID );
        json.value( pid );

        if ( pidFilter != null )
        {
            json.key( ConfigManager.PID_FILTER );
            json.value( pidFilter );
        }

        Dictionary props = null;
        if ( config != null )
        {
            props = config.getProperties(); // unchecked
        }
        if ( props == null )
        {
            props = new Hashtable();
        }

        boolean doSimpleMerge = true;
        final MetaTypeServiceSupport mtss = getMetaTypeSupport();
        if ( mtss != null )
        {
            ObjectClassDefinition ocd = null;
            if ( config != null )
            {
                ocd = mtss.getObjectClassDefinition( config, locale );
            }
            if ( ocd == null )
            {
                ocd = mtss.getObjectClassDefinition( pid, locale );
            }
            if ( ocd != null )
            {
                mtss.mergeWithMetaType( props, ocd, json );
                doSimpleMerge = false;
            }
        }

        if (doSimpleMerge)
        {
            json.key( "title" ).value( pid ); 
            json.key( "description" ).value( 
                "This form is automatically generated from existing properties because no property "
                    + "descriptors are available for this configuration. This may be cause by the absence "
                    + "of the OSGi Metatype Service or the absence of a MetaType descriptor for this configuration." );

            json.key( "properties" ).object(); 
            for ( Enumeration pe = props.keys(); pe.hasMoreElements(); )
            {
                final String id = ( String ) pe.nextElement();

                // ignore well known special properties
                if ( !id.equals( Constants.SERVICE_PID ) && !id.equals( Constants.SERVICE_DESCRIPTION )
                    && !id.equals( Constants.SERVICE_ID ) && !id.equals( Constants.SERVICE_VENDOR )
                    && !id.equals( ConfigurationAdmin.SERVICE_BUNDLELOCATION )
                    && !id.equals( ConfigurationAdmin.SERVICE_FACTORYPID ) )
                {
                    final Object value = props.get( id );
                    final PropertyDescriptor ad = MetaTypeServiceSupport.createAttributeDefinition( id, value );
                    json.key( id );
                    MetaTypeServiceSupport.attributeToJson( json, ad, value );
                }
            }
            json.endObject();
        }

        if ( config != null )
        {
            this.addConfigurationInfo( config, json, locale );
        }
    }
*/

    void addConfigurationInfo(Configuration config, JSONWriter json, String locale) throws JSONException
    {

        if (config.getFactoryPid() != null)
        {
            json.key(FACTORY_PID);
            json.value(config.getFactoryPid());
        }

        String location;
        if (config.getBundleLocation() == null)
        {
            location = ""; 
        }
        else
        {
            // if the configuration is bound to a bundle location which
            // is not related to an installed bundle, we just print the
            // raw bundle location binding
            Bundle bundle = getBundle(this.getBundleContext(), config.getBundleLocation());
            if (bundle == null)
            {
                location = config.getBundleLocation();
            }
            else
            {
                Dictionary headers = bundle.getHeaders(locale);
                String name = (String) headers.get(Constants.BUNDLE_NAME);
                if (name == null)
                {
                    location = bundle.getSymbolicName();
                }
                else
                {
                    location = name + " (" + bundle.getSymbolicName() + ')'; 
                }

                Version v = Version.parseVersion((String) headers.get(Constants.BUNDLE_VERSION));
                location += ", Version " + v.toString();
            }
        }
        json.key("bundleLocation"); 
        json.value(location);
        // raw bundle location and service locations
        final String pid = config.getPid();
        String serviceLocation = ""; 
        try
        {
            final ServiceReference[] refs = getBundleContext().getServiceReferences(
                    null,
                    "(&(" + Constants.OBJECTCLASS + '=' + ManagedService.class.getName() 
                            + ")(" + Constants.SERVICE_PID + '=' + pid + "))");  //$NON-NLS-2$
            if (refs != null && refs.length > 0)
            {
                serviceLocation = refs[0].getBundle().getLocation();
            }
        }
        catch (Throwable t)
        {
            logger.error("Error getting service associated with configuration " + pid, t);
        }
        json.key("bundle_location"); 
        json.value(config.getBundleLocation());
        json.key("service_location"); 
        json.value(serviceLocation);
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
                }
            }
        }
        catch (Exception e)
        {
            logger.error("listConfigurations: Unexpected problem encountered", e);
        }
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
    public String getName(Bundle bundle)
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
     * {@link ServiceTracker} if the service is not already got.
     *
     * @param serviceName the service name to obtain
     * @return the service or <code>null</code> if missing.
     */
    public final Object getService(String serviceName)
    {
        ServiceTracker serviceTracker = (ServiceTracker) services.get(serviceName);
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

    private Map<String, Object> createMetatypeMap(AttributeDefinition[] definitions)
    {
        Map<String, Object> metatypeMap = new HashMap<String, Object>();

        if(definitions != null)
        {
            for(AttributeDefinition definition : definitions)
            {
                Map<String, Object> attributeMap = new HashMap<String, Object>();
                attributeMap.put("name", definition.getName());
                attributeMap.put("cardinality", definition.getCardinality());
                attributeMap.put("defaultValue", definition.getDefaultValue());
                attributeMap.put("description", definition.getDescription());
                attributeMap.put("type", definition.getType());
                attributeMap.put("optionLabels", definition.getOptionLabels());
                attributeMap.put("optionValues", definition.getOptionValues());
                metatypeMap.put(definition.getID(), attributeMap);
            }
        }

        return metatypeMap;
    }

    private static class PlaceholderConfiguration implements Configuration
    {

        private final String factoryPid;
        private String bundleLocation;


        PlaceholderConfiguration(String factoryPid)
        {
            this.factoryPid = factoryPid;
        }


        public String getPid()
        {
            return PLACEHOLDER_PID;
        }


        public String getFactoryPid()
        {
            return factoryPid;
        }


        public void setBundleLocation(String bundleLocation)
        {
            this.bundleLocation = bundleLocation;
        }


        public String getBundleLocation()
        {
            return bundleLocation;
        }


        public Dictionary getProperties()
        {
            // dummy configuration has no properties
            return null;
        }


        public void update()
        {
            // dummy configuration cannot be updated
        }


        public void update(Dictionary properties)
        {
            // dummy configuration cannot be updated
        }


        public void delete()
        {
            // dummy configuration cannot be deleted
        }

    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.configresource.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * The <code>ConfigResourceProvider</code> is a resource provider which maps
 * configuration into the virtual resource tree.
 * <p>
 * The provider instance is configured with one properties: The location in the
 * resource tree where resources are provided ({@link ResourceProvider#ROOTS})
 *
 * @scr.component
 * @scr.service
 * @scr.property name="service.description" value="Sling ConfigAdmin Resource
 *               Provider"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property nameRef="ResourceProvider.ROOTS" valueRef="DEFAULT_ROOT"
 */
public class ConfigResourceProvider implements ResourceProvider {

    public static final String DEFAULT_ROOT = "/test/configs";

    // The location in the resource tree where the resources are mapped
    private String providerRoot;

    // providerRoot + "/" to be used for prefix matching of paths
    private String providerRootPrefix;

    /** @scr.reference */
    private ConfigurationAdmin configAdmin;

    /** @scr.reference cardinality="0..1 */
    private MetaTypeService metatypeService;

    /** The component context. */
    private ComponentContext componentContext;

    /**
     * Same as {@link #getResource(ResourceResolver, String)}, i.e. the
     * <code>request</code> parameter is ignored.
     *
     * @see #getResource(ResourceResolver, String)
     */
    public Resource getResource(ResourceResolver   resourceResolver,
                                HttpServletRequest request,
                                String             path) {
        return getResource(resourceResolver, path);
    }

    /**
     * Returns a resource wrapping a filesystem file or folder for the given
     * path. If the <code>path</code> is equal to the configured resource tree
     * location of this provider, the configured file system file or folder is
     * used for the resource. Otherwise the configured resource tree location
     * prefix is removed from the path and the remaining relative path is used
     * to access the file or folder. If no such file or folder exists, this
     * method returns <code>null</code>.
     */
    public Resource getResource(ResourceResolver resourceResolver, String path) {
        // check root
        if ( path.equals(this.providerRoot) ) {
            return new RootResource(path, resourceResolver);
        }
        if ( !path.startsWith(this.providerRootPrefix) ) {
            return null;
        }
        // check container
        if ( path.equals(this.providerRootPrefix + "configurations") ) {
            return new ConfigurationContainerResource(path, resourceResolver);
        }
        if ( path.equals(this.providerRootPrefix + "factories") ) {
            return new FactoryContainerResource(path, resourceResolver);
        }
        if ( path.startsWith(this.providerRootPrefix + "configurations/") ) {
            final String pid = path.substring((this.providerRootPrefix + "configurations/").length());
            if ( pid.length() == 0 ) {
                return null;
            }
            try {
                final Configuration c = this.configAdmin.getConfiguration(pid);
                if ( c.getProperties() == null ) {
                    return null;
                }
                return new ConfigurationResource(path, resourceResolver, c);
            } catch (IOException e) {
                return null;
            }
        }
        // TODO factories
        if ( path.startsWith(this.providerRootPrefix + "factories/") ) {
            final String pid = path.substring((this.providerRootPrefix + "factories/").length());
            if ( pid.length() == 0 ) {
                return null;
            }
            try {
                final Configuration c = this.configAdmin.getConfiguration(pid);
                if ( c.getProperties() == null ) {
                    return null;
                }
                return new FactoryResource(path, resourceResolver, c);
            } catch (IOException e) {
                return null;
            }
        }

        // not applicable or not existing
        return null;
    }

    /**
     * Returns an iterator of resources.
     */
    public Iterator<Resource> listChildren(final Resource parent) {
        if ( parent instanceof RootResource ) {
            final List<Resource> children = new ArrayList<Resource>();
            children.add(new ConfigurationContainerResource(parent.getPath() + "/configurations", parent.getResourceResolver()));
            children.add(new FactoryContainerResource(parent.getPath() + "/factories", parent.getResourceResolver()));
            return children.iterator();
        }
        if ( parent instanceof FactoryContainerResource ) {
            // TODO
            return null;
        }
        if ( parent instanceof ConfigurationContainerResource ) {
            try {
                final Map<String, String> optionsPlain = this.listConfigurations();
                final List<Configuration> configList = new ArrayList<Configuration>();
                if ( optionsPlain != null ) {
                    for(String key : optionsPlain.keySet()) {
                        final Configuration c = this.configAdmin.getConfiguration(key);
                        if ( c != null && c.getProperties() != null ) {
                            configList.add(c);
                        } else {
                            final ObjectClassDefinition ocd = this.getObjectClassDefinition( key );
                            if ( ocd != null ) {
                                configList.add(getConfiguration(key, ocd));
                            }
                        }
                    }
                }
                final Configuration[] configs = configList.toArray(new Configuration[configList.size()]);
                return new Iterator<Resource>() {

                    int index = 0;

                    /**
                     * @see java.util.Iterator#hasNext()
                     */
                    public boolean hasNext() {
                        return index < configs.length;
                    }

                    /**
                     * @see java.util.Iterator#next()
                     */
                    public Resource next() {
                        if ( index >= configs.length ) {
                            throw new NoSuchElementException();
                        }
                        final Configuration c = configs[index];
                        index++;
                        return new ConfigurationResource(providerRootPrefix + "configurations/" + c.getPid(),
                                parent.getResourceResolver(), c);
                    }

                    /**
                     * @see java.util.Iterator#remove()
                     */
                    public void remove() {
                        if ( index <= configs.length && configs.length > 0 ) {
                            try {
                                configs[index-1].delete();
                            } catch (IOException e) {
                                throw new UnsupportedOperationException("Unable to remove element.");
                            }
                        }
                        throw new IllegalStateException("Unable to remove element.");
                    }

                };
            } catch (IOException e) {
                return null;
            }
        }
        // no children
        return null;
    }

    private Configuration getConfiguration( String pid, ObjectClassDefinition ocd ) {
        final Dictionary props = new Hashtable();
        props.put("title", ocd.getName());

        if ( ocd.getDescription() != null ) {
            props.put("description", ocd.getDescription());
        }

        final AttributeDefinition[] ad = ocd.getAttributeDefinitions( ObjectClassDefinition.ALL );
        if ( ad != null ) {

            for ( int i = 0; i < ad.length; i++ ) {
                final String key = ad[i].getID();
                props.put(key, ad[i].getDefaultValue());
            }
        }
        return new PlaceholderConfiguration(pid, null, props);
    }

    private static class PlaceholderConfiguration implements Configuration {

        private final String pid;
        private final String factoryPid;
        private final Dictionary props;

        private String bundleLocation;

        PlaceholderConfiguration( String pid, String factoryPid, Dictionary properties ) {
            this.pid = pid;
            this.factoryPid = factoryPid;
            this.props = properties;
        }

        /**
         * @see org.osgi.service.cm.Configuration#getPid()
         */
        public String getPid() {
            return this.pid;
        }

        /**
         * @see org.osgi.service.cm.Configuration#getFactoryPid()
         */
        public String getFactoryPid() {
            return factoryPid;
        }


        /**
         * @see org.osgi.service.cm.Configuration#setBundleLocation(java.lang.String)
         */
        public void setBundleLocation( String bundleLocation ) {
            this.bundleLocation = bundleLocation;
        }

        /**
         * @see org.osgi.service.cm.Configuration#getBundleLocation()
         */
        public String getBundleLocation() {
            return bundleLocation;
        }

        /**
         * @see org.osgi.service.cm.Configuration#getProperties()
         */
        public Dictionary getProperties() {
            return this.props;
        }

        /**
         * @see org.osgi.service.cm.Configuration#update()
         */
        public void update() {
            // dummy configuration cannot be updated
        }

        /**
         * @see org.osgi.service.cm.Configuration#update(java.util.Dictionary)
         */
        public void update( Dictionary properties ) {
            // dummy configuration cannot be updated
        }

        /**
         * @see org.osgi.service.cm.Configuration#delete()
         */
        public void delete() {
            // dummy configuration cannot be deleted
        }
    }

    /**
     * Activate this component.
     * @param context The component context.
     */
    protected void activate(ComponentContext context) {
        this.componentContext = context;
        Dictionary<?, ?> props = context.getProperties();

        String providerRoot = (String) props.get(ROOTS);
        if (providerRoot == null || providerRoot.length() == 0) {
            throw new IllegalArgumentException(ROOTS + " property must be set");
        }

        this.providerRoot = providerRoot;
        this.providerRootPrefix = providerRoot.concat("/");
    }

    /**
     * Deactivate this component.
     * @param context The component context.
     */
    protected void deactivate(ComponentContext context) {
        this.providerRoot = null;
        this.providerRootPrefix = null;
        this.componentContext = null;
    }

    private Map<String, String> listConfigurations() {
        try {
            // start with ManagedService instances
            SortedMap<String, String> optionsPlain = getServices( ManagedService.class.getName() );

            // add in existing configuration (not duplicating ManagedServices)
            Configuration[] cfgs = this.configAdmin.listConfigurations( null );
            for ( int i = 0; cfgs != null && i < cfgs.length; i++ ) {

                // ignore configuration object if an entry already exists in the
                // map
                String pid = cfgs[i].getPid();
                if ( optionsPlain.containsKey( pid ) ) {
                    continue;
                }

                // insert and entry for the pid
                ObjectClassDefinition ocd = this.getObjectClassDefinition( cfgs[i] );
                String name;
                if ( ocd != null ) {
                    name = ocd.getName() + " (";
                    name += pid + ")";
                } else {
                    name = pid;
                }

                optionsPlain.put( pid, name );
            }
            return optionsPlain;
        } catch ( Exception e ) {
            // write a message or ignore
        }
        return null;
    }


    private void listFactoryConfigurations( ) {
        try
        {
            SortedMap<String, String> optionsFactory = getServices( ManagedServiceFactory.class.getName());
        }  catch ( Exception e ) {
            // write a message or ignore
        }
    }


    private SortedMap<String, String> getServices( String serviceClass )
    throws InvalidSyntaxException {
        // sorted map of options
        final SortedMap<String, String> optionsFactory = new TreeMap<String, String>( String.CASE_INSENSITIVE_ORDER );

        final ServiceReference[] refs = this.getBundleContext().getServiceReferences( serviceClass, null );
        for ( int i = 0; refs != null && i < refs.length; i++ ) {
            Object pidObject = refs[i].getProperty( Constants.SERVICE_PID );
            if ( pidObject instanceof String ) {
                String pid = ( String ) pidObject;
                String name;
                ObjectClassDefinition ocd = this.getObjectClassDefinition( refs[i].getBundle(), pid );
                if ( ocd != null ) {
                    name = ocd.getName() + " (";
                    name += pid + ")";
                } else {
                    name = pid;
                }

                optionsFactory.put( pid, name );
            }
        }

        return optionsFactory;
    }



    private ObjectClassDefinition getObjectClassDefinition( Bundle bundle, String pid )
    {
        if ( bundle != null )
        {
            MetaTypeService mts = this.metatypeService;
            if ( mts != null )
            {
                MetaTypeInformation mti = mts.getMetaTypeInformation( bundle );
                if ( mti != null )
                {
                    return mti.getObjectClassDefinition( pid, null );
                }
            }
        }

        // fallback to nothing found
        return null;
    }

    private ObjectClassDefinition getObjectClassDefinition( Configuration config ) {

        // if the configuration is not bound, search in the bundles
        if ( config.getBundleLocation() == null ) {
            // if the configuration is a factory one, use the factory PID
            if ( config.getFactoryPid() != null ) {
                return this.getObjectClassDefinition( config.getFactoryPid() );
            }

            // otherwise use the configuration PID
            return this.getObjectClassDefinition( config.getPid() );
        }

        MetaTypeService mts = this.getMetaTypeService();
        if ( mts != null ) {
            Bundle bundle = this.getBundle( config.getBundleLocation() );
            if ( bundle != null )
            {
                MetaTypeInformation mti = mts.getMetaTypeInformation( bundle );
                if ( mti != null )
                {
                    // check by factory PID
                    if ( config.getFactoryPid() != null )
                    {
                        return mti.getObjectClassDefinition( config.getFactoryPid(), null );
                    }

                    // otherwise check by configuration PID
                    return mti.getObjectClassDefinition( config.getPid(), null );
                }
            }
        }

        // fallback to nothing found
        return null;
    }

    private MetaTypeService getMetaTypeService() {
        return this.metatypeService;
    }

    private Bundle getBundle( String bundleLocation ) {
        if ( bundleLocation == null ) {
            return null;
        }

        Bundle[] bundles = this.getBundleContext().getBundles();
        for ( int i = 0; i < bundles.length; i++ ) {
            if ( bundleLocation.equals( bundles[i].getLocation() ) ) {
                return bundles[i];
            }
        }

        return null;
    }

    private BundleContext getBundleContext() {
        return this.componentContext.getBundleContext();
    }

    private ObjectClassDefinition getObjectClassDefinition( String pid )
    {
        Bundle[] bundles = this.componentContext.getBundleContext().getBundles();
        for ( int i = 0; i < bundles.length; i++ )
        {
            try
            {
                ObjectClassDefinition ocd = this.getObjectClassDefinition( bundles[i], pid);
                if ( ocd != null )
                {
                    return ocd;
                }
            }
            catch ( IllegalArgumentException iae )
            {
                // don't care
            }
        }
        return null;
    }
}

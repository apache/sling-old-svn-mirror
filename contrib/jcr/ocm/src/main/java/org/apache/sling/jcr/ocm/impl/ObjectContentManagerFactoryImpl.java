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
package org.apache.sling.jcr.ocm.impl;

import static org.apache.sling.jcr.ocm.OcmConstants.EVENT_MAPPING_ADDED;
import static org.apache.sling.jcr.ocm.OcmConstants.EVENT_MAPPING_REMOVED;
import static org.apache.sling.jcr.ocm.OcmConstants.MAPPER_BUNDLE_HEADER;
import static org.apache.sling.jcr.ocm.OcmConstants.MAPPING_CLASS;
import static org.apache.sling.jcr.ocm.OcmConstants.MAPPING_NODE_TYPE;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.cache.ObjectCache;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.ObjectConverterImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.ProxyManagerImpl;
import org.apache.jackrabbit.ocm.mapper.model.MappingDescriptor;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.query.impl.QueryManagerImpl;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.ocm.ObjectContentManagerFactory;
import org.apache.sling.jcr.ocm.impl.classloader.MapperClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

/**
 * The <code>ObjectContentManagerFactory</code> TODO
 *
 */
@Component
@Service(value=ObjectContentManagerFactory.class)
@Properties({
    @Property(name="service.description", value="Sling Object Content Manager Factory"),
    @Property(name="service.vendor", value="The Apache Software Foundation")
})
public class ObjectContentManagerFactoryImpl implements ObjectContentManagerFactory, SynchronousBundleListener {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(ObjectContentManagerFactoryImpl.class);

    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY, policy=ReferencePolicy.DYNAMIC)
    private volatile EventAdmin eventAdmin;

    /**
     * The class loader used by the Jackrabbit OCM ReflectionUtils class to load
     * classes for mapping. The class loader is set on the ReflectionUtils
     * before the new mappings are loaded.
     *
     * @see #loadMappings()
     */
    private MapperClassLoader mapperClassLoader;

    private BundleMapper mapper;

    private ClassDescriptorReader descriptorReader;

    private AtomicTypeConverterProvider converterProvider;

    private OcmAdapterFactory adapterFactory;

    private OcmFactoryAdapterFactory factoryAdapterFactory;

    public ObjectContentManagerFactoryImpl() {

        // prepare the data converters and query manager
        this.converterProvider = new SlingAtomicTypeConverterProvider();
    }

    /**
     * @see org.apache.sling.jcr.ocm.ObjectContentManagerFactory#getObjectContentManager(javax.jcr.Session)
     */
    public ObjectContentManager getObjectContentManager(Session session) {

        ValueFactory valueFactory;
        try {
            valueFactory = session.getValueFactory();
        } catch (RepositoryException re) {
            log.info(
                "getObjectContentManager: Cannot get ValueFactory from Session ("
                    + session.getUserID() + "), using default factory", re);
            valueFactory = ValueFactoryImpl.getInstance();
        }

        ObjectCache objectCache = new ObservingObjectCache(session);

        QueryManager queryManager = new QueryManagerImpl(mapper,
            converterProvider.getAtomicTypeConverters(), valueFactory);

        ObjectConverter objectConverter = new ObjectConverterImpl(mapper,
            converterProvider, new ProxyManagerImpl(), objectCache);

        return new ObjectContentManagerImpl(mapper, objectConverter,
            queryManager, objectCache, session);
    }

    private ClassDescriptorReader getDescriptorReader() {
        if (descriptorReader == null) {
            ClassDescriptorReader ddr = new ClassDescriptorReader();
            // ddr.setResolver(null /* TODO resolve URL :
            // graffito-jcr-mapping.dtd */);
            // ddr.setValidating(false);
            descriptorReader = ddr;
        } else {
            descriptorReader.reset();
        }

        return descriptorReader;
    }

    // TODO: New Implementation --------------------------------------

    private List<Bundle> bundles = new ArrayList<Bundle>();

    /**
     * Loads the components of the given bundle. If the bundle has no
     * <i>Service-Component</i> header, this method has no effect. The
     * fragments of a bundle are not checked for the header (112.4.1).
     * <p>
     * This method calls the {@link #getBundleContext(Bundle)} method to find
     * the <code>BundleContext</code> of the bundle. If the context cannot be
     * found, this method does not load components for the bundle.
     */
    private synchronized void addBundle(Bundle bundle) {
        // ignore bundle without mappings
        if (bundle.getHeaders().get(MAPPER_BUNDLE_HEADER) == null) {
            return;
        }

        if (bundles.contains(bundle)) {
            // ignore existing bundle
            return;
        }

        bundles.add(bundle);

        loadMappings();

        // fire mapping event
        fireEvent(bundle, EVENT_MAPPING_ADDED);
    }

    private synchronized void removeBundle(Bundle bundle) {
        if (!bundles.remove(bundle)) {
            // bundle not known
            return;
        }

        loadMappings();

        // fire mapping event
        fireEvent(bundle, EVENT_MAPPING_REMOVED);
    }

    private void loadMappings() {
        MapperClassLoader newMapperClassLoader = new MapperClassLoader();
        ReflectionUtils.setClassLoader(newMapperClassLoader);

        ArrayList<URL> urlList = new ArrayList<URL>();
        for (Iterator<Bundle> bi = bundles.iterator(); bi.hasNext();) {
            Bundle bundle = bi.next();

            String mapperHeader = (String) bundle.getHeaders().get(
                MAPPER_BUNDLE_HEADER);
            if (mapperHeader == null) {
                // no components in the bundle, abandon
                log.debug("registerMapperClient: Bundle {} has no mappings",
                    bundle.getSymbolicName());
                continue;
            }

            newMapperClassLoader.registerBundle(bundle);

            StringTokenizer tokener = new StringTokenizer(mapperHeader, ",");
            while (tokener.hasMoreTokens()) {
                String mapping = tokener.nextToken().trim();
                URL mappingURL = bundle.getResource(mapping);
                if (mappingURL == null) {
                    log.warn("Mapping {} not found in bundle {}", mapping,
                        bundle.getSymbolicName());
                } else {
                    urlList.add(mappingURL);
                }
            }
        }

        // nothing to do if there are not streams
        if (urlList.isEmpty()) {
            return;
        }

        MappingDescriptor md;
        try {
            ClassDescriptorReader cdr = getDescriptorReader();
            cdr.parse(urlList);
            md = cdr.getMappingDescriptor();
        } catch (XmlPullParserException xppe) {
            log.error("Failed parsing descriptors", xppe);
            return;
        } catch (IOException ioe) {
            log.error("Failed reading descriptors", ioe);
            return;
        }

        BundleMapper newMapper = new BundleMapper(md);

        // dispose off old class loader before using new loader
        if (mapperClassLoader != null) {
            // note: the mapperClassLoader is used by the Jackrabbit OCM
            // ReflectionUtils class. This class has already been reset to
            // use the new class loader, so we can dispose off the old
            // class loader here safely
            mapperClassLoader.dispose();
        }

        mapperClassLoader = newMapperClassLoader;
        mapper = newMapper;

        // update adapters
        if (adapterFactory != null) {
            adapterFactory.updateAdapterClasses(mapper.getMappedClasses());
        }
    }

    // ---------- BundleListener -----------------------------------------------

    /**
     * Loads and unloads any components provided by the bundle whose state
     * changed. If the bundle has been started, the components are loaded. If
     * the bundle is about to stop, the components are unloaded.
     *
     * @param event The <code>BundleEvent</code> representing the bundle state
     *            change.
     */
    public void bundleChanged(BundleEvent event) {

        //
        // NOTE:
        // This is synchronous - take care to not block the system !!
        //

        switch (event.getType()) {

            case BundleEvent.STARTING: // STARTED:
                // register mappings before the bundle gets activated
                addBundle(event.getBundle());
                break;

            case BundleEvent.STOPPED:
                // remove mappings after the bundle has stopped
                removeBundle(event.getBundle());
                break;

        }
    }

    // ---------- EventAdmin Event Dispatching ---------------------------------

    /**
     * Fires an OSGi event through the EventAdmin service.
     *
     * @param sourceBundle The Bundle from which the event originates. This may
     *            be <code>null</code> if there is no originating bundle.
     * @param eventName The name of the event
     * @throws NullPointerException if eventName or props is <code>null</code>.
     */
    public void fireEvent(Bundle sourceBundle, String eventName) {

        // check event admin service, return if not available
        EventAdmin ea = eventAdmin;
        BundleMapper mapper = this.mapper;
        if (ea == null || mapper == null) {
            return;
        }

        // only fire, if there is a (new) mapper
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(MAPPING_CLASS, mapper.getMappedClasses());
        props.put(MAPPING_NODE_TYPE, mapper.getMappedNodeTypes());

        // create and fire the event
        Event event = OsgiUtil.createEvent(sourceBundle, null, eventName, props);
        ea.postEvent(event);
    }

    // ---------- SCR Integration ---------------------------------------------

    /** Activates this component, called by SCR before registering as a service */
    protected void activate(ComponentContext componentContext) {

        componentContext.getBundleContext().addBundleListener(this);

        try {
            Bundle[] bundles = componentContext.getBundleContext().getBundles();
            for (Bundle bundle : bundles) {
                if (bundle.getState() == Bundle.ACTIVE) {
                    // register active bundles with the mapper client
                    addBundle(bundle);
                }
            }
        } catch (Throwable t) {
            log.error("activate: Problem while loading initial content and"
                + " registering mappings for existing bundles", t);
        }

        // register the OcmAdapterFactory
        String[] mappedClasses = (mapper != null)
                ? mapper.getMappedClasses()
                : new String[0];
        adapterFactory = new OcmAdapterFactory(this,
            componentContext.getBundleContext(), mappedClasses);
        factoryAdapterFactory = new OcmFactoryAdapterFactory(this, componentContext.getBundleContext());
    }

    /** Deativates this component, called by SCR to take out of service */
    protected void deactivate(ComponentContext componentContext) {
        if (adapterFactory != null) {
            adapterFactory.dispose();
            adapterFactory = null;
        }
        if ( factoryAdapterFactory != null ) {
            factoryAdapterFactory.dispose();
            factoryAdapterFactory = null;
        }

        componentContext.getBundleContext().removeBundleListener(this);

        synchronized (this) {
            if (mapperClassLoader != null) {
                mapperClassLoader.dispose();
                mapperClassLoader = null;
            }
        }
    }

}

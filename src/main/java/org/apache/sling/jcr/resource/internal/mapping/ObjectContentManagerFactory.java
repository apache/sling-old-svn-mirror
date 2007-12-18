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
package org.apache.sling.jcr.resource.internal.mapping;

import static org.apache.sling.jcr.resource.JcrResourceConstants.EVENT_MAPPING_ADDED;
import static org.apache.sling.jcr.resource.JcrResourceConstants.EVENT_MAPPING_REMOVED;
import static org.apache.sling.jcr.resource.JcrResourceConstants.MAPPER_BUNDLE_HEADER;
import static org.apache.sling.jcr.resource.JcrResourceConstants.MAPPING_CLASS;
import static org.apache.sling.jcr.resource.JcrResourceConstants.MAPPING_NODE_TYPE;

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
import org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl;
import org.apache.sling.jcr.resource.internal.mapping.classloader.MapperClassLoader;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

/**
 * The <code>ObjectContentManagerFactory</code> TODO
 */
public class ObjectContentManagerFactory {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(ObjectContentManagerFactory.class);

    private JcrResourceResolverFactoryImpl jcrResourceResolverFactory;

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

    public ObjectContentManagerFactory(
            JcrResourceResolverFactoryImpl jcrResourceResolverFactory) {
        this.jcrResourceResolverFactory = jcrResourceResolverFactory;

        // prepare the data converters and query manager
        this.converterProvider = new SlingAtomicTypeConverterProvider();
    }

    public synchronized void dispose() {
        if (mapperClassLoader != null) {
            mapperClassLoader.dispose();
            mapperClassLoader = null;
        }
    }

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

    // ---------- Bundle registration and unregistration -----------------------

    /**
     * Loads the components of the given bundle. If the bundle has no
     * <i>Service-Component</i> header, this method has no effect. The
     * fragments of a bundle are not checked for the header (112.4.1).
     * <p>
     * This method calls the {@link #getBundleContext(Bundle)} method to find
     * the <code>BundleContext</code> of the bundle. If the context cannot be
     * found, this method does not load components for the bundle.
     */
    public void registerMapperClient(Bundle bundle) {
        addBundle(bundle);
    }

    public void unregisterMapperClient(Bundle bundle) {
        removeBundle(bundle);
    }

    // TODO: New Implementation --------------------------------------

    private List<Bundle> bundles = new ArrayList<Bundle>();

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
        fireMappingEvent(bundle, EVENT_MAPPING_ADDED);
    }

    private synchronized void removeBundle(Bundle bundle) {
        if (!bundles.remove(bundle)) {
            // bundle not known
            return;
        }

        loadMappings();

        // fire mapping event
        fireMappingEvent(bundle, EVENT_MAPPING_REMOVED);
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
    }

    private void fireMappingEvent(Bundle sourceBundle, String eventName) {
        if (mapper != null) {
            // only fire, if there is a (new) mapper
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(MAPPING_CLASS, mapper.getMappedClasses());
            props.put(MAPPING_NODE_TYPE, mapper.getMappedNodeTypes());
            jcrResourceResolverFactory.fireEvent(sourceBundle, eventName, props);
        }
    }
}

/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.content.jcr.internal.mapping;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.mapper.model.MappingDescriptor;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;
import org.apache.sling.content.jcr.Constants;
import org.apache.sling.content.jcr.JcrContentManager;
import org.apache.sling.content.jcr.internal.JcrContentHelper;
import org.apache.sling.content.jcr.internal.mapping.classloader.MapperClassLoader;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;


/**
 * The <code>PersistenceManagerProviderImpl</code> TODO
 */
public class PersistenceManagerProviderImpl {

    public static final String MAPPER_BUNDLE_HEADER = "Sling-Mappings";

    /** default log */
    private static final Logger log =
        LoggerFactory.getLogger(PersistenceManagerProviderImpl.class);

    private JcrContentHelper jcrContentHelper;
    private MapperClassLoader mapperClassLoader;

    private BundleMapper mapper;
    private ClassDescriptorReader descriptorReader;
    private AtomicTypeConverterProvider converterProvider;
    private Map<String, Session> adminSessions;

    // issued persistence managers indexed by session
    private Map<Session, JcrContentManager> managers;
    private final Object managersLock = new Object();

    private Thread reaper;

    public PersistenceManagerProviderImpl(JcrContentHelper jcrContentHelper) {
        this.jcrContentHelper = jcrContentHelper;

        // prepare the data converters and query manager
        this.converterProvider = new MapperAtomicTypeConverterProvider();

        this.managers = new IdentityHashMap<Session, JcrContentManager>();

        this.reaper = new Thread("PersistenceManager Reaper") {
            public void run() {
                while (PersistenceManagerProviderImpl.this.reaper != null) {
                    try {
                        Thread.sleep(60*1000L);
                    } catch (InterruptedException ie) {
                        // don't care
                    }

                    synchronized (PersistenceManagerProviderImpl.this.managersLock) {
                        if (PersistenceManagerProviderImpl.this.managers != null) {
                            for (Iterator<Session> mi=PersistenceManagerProviderImpl.this.managers.keySet().iterator(); mi.hasNext(); ) {
                                Session session = mi.next();
                                if (!session.isLive()) {
                                    mi.remove();
                                }
                            }
                        }
                    }
                }
            }
        };

        this.adminSessions = new HashMap<String, Session>();
    }

    public synchronized void dispose() {
        synchronized (this.managersLock) {
            if (this.managers != null) {
                this.managers.clear();
                this.managers = null;
            }
        }

        if (this.reaper != null) {
            Thread thread = this.reaper;
            this.reaper = null;
            thread.interrupt();
        }

        if (this.mapperClassLoader != null) {
            this.mapperClassLoader.dispose();
            this.mapperClassLoader = null;
        }

        if (this.adminSessions != null) {
            Object[] sessions = this.adminSessions.values().toArray();
            this.adminSessions = null;
            for (int i=0; i < sessions.length; i++) {
                ((Session) sessions[i]).logout();
            }
        }
    }

    /**
     * @throws IllegalStateException If this provider is not operational
     */
    public JcrContentManager getContentManager(Session session) {
        if (this.managers == null) {
            throw new IllegalStateException("Already disposed");
        }

        synchronized (this.managersLock) {
            JcrContentManager pm = this.managers.get(session);

            // create if not existing yet
            if (pm == null) {
                // ensure namespace mapping for graffito:
                if (this.ensureNamespaces(session)) {
                    try {
                        pm = new ContentManagerImpl(this, this.mapper, this.converterProvider,
                            session);
                        this.managers.put(session, pm);
                    } catch (RepositoryException re) {
                        // TODO: do better than that !!
                        throw new org.apache.jackrabbit.ocm.exception.RepositoryException(re);
                    }
                }
            }

            return pm;
        }
    }

    private boolean ensureNamespaces(Session session) {
        NamespaceRegistry nsr;
        try {
            nsr = session.getWorkspace().getNamespaceRegistry();
            this.ensureNamespace(nsr, Constants.GRAFFITO_NS_PREFIX, Constants.GRAFFITO_NS_URI);
            this.ensureNamespace(nsr, Constants.SLING_NS_PREFIX, Constants.SLING_NS_URI);
            return true;
        } catch (RepositoryException re) {
            // TODO: log general error, fail
            log.error("Problem checking for namespace " + Constants.GRAFFITO_NS_PREFIX, re);
            return false;
        }
    }

    private void ensureNamespace(NamespaceRegistry nsr, String prefix,
            String uri) throws RepositoryException {
        try {
            if (nsr.getURI(prefix) != null) {
                return;
            }
        } catch (NamespaceException nse) {
            // TODO: log No such name space, create
            log.info("Namespace {} does not seem to exist, creating", prefix);
        }

        nsr.registerNamespace(prefix, uri);
    }

    private ClassDescriptorReader getDescriptorReader() {
        if (this.descriptorReader == null) {
            ClassDescriptorReader ddr = new ClassDescriptorReader();
//            ddr.setResolver(null /* TODO resolve URL : graffito-jcr-mapping.dtd */);
//            ddr.setValidating(false);
            this.descriptorReader = ddr;
        } else {
            this.descriptorReader.reset();
        }

        return this.descriptorReader;
    }

    boolean itemReallyExists(Session clientSession, String path)
            throws RepositoryException {

        Session adminSession;
        synchronized (this.adminSessions) {
            String workSpace = clientSession.getWorkspace().getName();
            adminSession = this.adminSessions.get(workSpace);
            if (adminSession == null) {
                adminSession = this.jcrContentHelper.getRepository().loginAdministrative(workSpace);
                this.adminSessions.put(workSpace, adminSession);
            }
        }

        // assume this session has more access rights than the client Session
        return adminSession.itemExists(path);
    }

    //---------- Bundle registration and unregistration -----------------------

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
        this.addBundle(bundle);
    }

    public void unregisterMapperClient(Bundle bundle) {
        this.removeBundle(bundle);
    }

    // TODO: New Implementation --------------------------------------

    private List<Bundle> bundles = new ArrayList<Bundle>();

    private synchronized void addBundle(Bundle bundle) {
        // ignore bundle without mappings
        if (bundle.getHeaders().get(MAPPER_BUNDLE_HEADER) == null) {
            return;
        }

        if (this.bundles.contains(bundle)) {
            // ignore existing bundle
            return;
        }

        this.bundles.add(bundle);

        this.loadMappings();

        // fire mapping event
        this.fireMappingEvent(bundle, Constants.EVENT_MAPPING_ADDED);
    }

    private synchronized void removeBundle(Bundle bundle) {
        if (!this.bundles.remove(bundle)) {
            // bundle not known
            return;
        }

        this.loadMappings();

        // fire mapping event
        this.fireMappingEvent(bundle, Constants.EVENT_MAPPING_REMOVED);
    }

    private void loadMappings() {
        MapperClassLoader newMapperClassLoader = new MapperClassLoader();
        ReflectionUtils.setClassLoader(newMapperClassLoader);

        ArrayList<URL> urlList = new ArrayList<URL>();
        for (Iterator<Bundle> bi=this.bundles.iterator(); bi.hasNext(); ) {
            Bundle bundle = bi.next();

            String mapperHeader = (String) bundle.getHeaders().get(MAPPER_BUNDLE_HEADER);
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
                    log.warn("Mapping {} not found in bundle {}", mapping, bundle.getSymbolicName());
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
            ClassDescriptorReader cdr = this.getDescriptorReader();
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

        synchronized (this.managersLock) {
            // dispose off old class loader before using new loader
            if (this.mapperClassLoader != null) {
                this.mapperClassLoader.dispose();
            }

            this.mapperClassLoader = newMapperClassLoader;
            this.mapper = newMapper;

            this.managers.clear();
        }
    }

    private void fireMappingEvent(Bundle sourceBundle, String eventName) {
        if (this.mapper != null) {
            // only fire, if there is a (new) mapper
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(Constants.MAPPING_CLASS, this.mapper.getMappedClasses());
            props.put(Constants.MAPPING_NODE_TYPE, this.mapper.getMappedNodeTypes());
            this.jcrContentHelper.fireEvent(sourceBundle, eventName, props);
        }
    }
}

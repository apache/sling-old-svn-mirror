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
package org.apache.sling.jcr.base.internal.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.jcr.api.NamespaceMapper;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.NodeTypeLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>Loader</code> TODO
 */
public class Loader implements NamespaceMapper, BundleListener {

    public static final String NODETYPES_BUNDLE_HEADER = "Sling-Nodetypes";

    public static final String NAMESPACES_BUNDLE_HEADER = "Sling-Namespaces";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(Loader.class);

    private final BundleContext bundleContext;

    private final SlingRepository slingRepository;

    // bundles whose registration failed and should be retried
    private final List<Bundle> delayedBundles;

    /** Namespace prefix table. */
    private final Map<Long, NamespaceEntry[]> namespaceTable = new HashMap<Long, NamespaceEntry[]>();

    public Loader(final SlingRepository repository, final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.slingRepository = repository;
        this.delayedBundles = new ArrayList<Bundle>();

        // scan existing bundles
        bundleContext.addBundleListener(this);
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getState() != Bundle.UNINSTALLED) {
                registerBundle(bundle);
            }
        }

    }

    public void dispose() {
        bundleContext.removeBundleListener(this);

        synchronized (delayedBundles) {
            delayedBundles.clear();
        }
    }

    //---------- NamespaceMapper interface

    public void defineNamespacePrefixes(Session session)
    throws RepositoryException {
        final Iterator<NamespaceEntry[]> iter = this.namespaceTable.values().iterator();
        while ( iter.hasNext() ) {
            final NamespaceEntry[] entries = iter.next();
            for(int i=0; i<entries.length; i++) {

                // the namespace prefixing is a little bit tricky:
                String mappedPrefix = null;
                // first, we check if the namespace is registered with a prefix
                try {
                    mappedPrefix = session.getNamespacePrefix(entries[i].namespace);
                } catch (NamespaceException ne) {
                    // the namespace is not registered yet, so we should do this
                    // can we directly use the desired prefix?
                    mappedPrefix = entries[i].prefix + "_new";
                    try {
                        session.getNamespaceURI(entries[i].prefix);
                    } catch (NamespaceException ne2) {
                        // as an exception occured we can directly use the new prefix
                        mappedPrefix = entries[i].prefix;
                    }
                    session.getWorkspace().getNamespaceRegistry().registerNamespace(mappedPrefix, entries[i].namespace);
                }
                // do we have to remap?
                if ( mappedPrefix != null && !mappedPrefix.equals(entries[i].prefix ) ) {
                    // check if the prefix is already used?
                    String oldUri = null;
                    try {
                        oldUri = session.getNamespaceURI(entries[i].prefix);
                        session.setNamespacePrefix(entries[i].prefix + "_old", oldUri);
                    } catch (NamespaceException ne) {
                        // ignore: prefix is not used
                    }
                    // finally set prefix
                    session.setNamespacePrefix(entries[i].prefix, entries[i].namespace);
                }
            }
        }
    }

    // ---------- BundleListener ------------------------------------

    /**
     * Loads and unloads any components provided by the bundle whose state
     * changed. If the bundle has been started, the components are loaded. If
     * the bundle is about to stop, the components are unloaded.
     *
     * @param event The <code>BundleEvent</code> representing the bundle state
     *            change.
     */
    public final void bundleChanged(BundleEvent event) {
        // Take care: This is synchronous - take care to not block the system !!
        switch (event.getType()) {
            case BundleEvent.INSTALLED:
                // register types when the bundle gets installed
                registerBundle(event.getBundle());
                break;

            case BundleEvent.UNINSTALLED:
                unregisterBundle(event.getBundle());
                break;

            case BundleEvent.UPDATED:
                updateBundle(event.getBundle());
        }
    }

    //---------- internal

    private void registerBundle(Bundle bundle) {
        this.registerNamespaces(bundle);
        if (this.registerBundleInternal(bundle, false)) {
            // handle delayed bundles, might help now
            int currentSize = -1;
            for (int i=this.delayedBundles.size(); i > 0 && currentSize != this.delayedBundles.size() && !this.delayedBundles.isEmpty(); i--) {
                for (Iterator<Bundle> di=this.delayedBundles.iterator(); di.hasNext(); ) {
                    Bundle delayed = di.next();
                    if (this.registerBundleInternal(delayed, true)) {
                        di.remove();
                    }
                }
                currentSize = this.delayedBundles.size();
            }
        } else {
            synchronized (delayedBundles) {
                delayedBundles.add(bundle);
            }
        }
    }

    private void unregisterBundle(Bundle bundle) {
        this.unregisterNamespaces(bundle);
        synchronized (delayedBundles) {
            delayedBundles.remove(bundle);
        }
    }

    private void updateBundle(Bundle bundle) {
        unregisterBundle(bundle);
        registerBundle(bundle);
    }

    /**
     * Register namespaces defined in the bundle in the namespace table.
     * @param bundle The bundle.
     */
    private void registerNamespaces(Bundle bundle) {
        final String definition = (String) bundle.getHeaders().get(NAMESPACES_BUNDLE_HEADER);
        if ( definition != null ) {
            log.debug("registerNamespaces: Bundle {} tries to register: {}",
                    bundle.getSymbolicName(), definition);
            final StringTokenizer st = new StringTokenizer(definition, ",");
            final List<NamespaceEntry>entries = new ArrayList<NamespaceEntry>();

            while ( st.hasMoreTokens() ) {
                final String token = st.nextToken().trim();
                int pos = token.indexOf('=');
                if ( pos == -1 ) {
                    log.warn("registerNamespaces: Bundle {} has an invalid namespace manifest header entry: {}",
                            bundle.getSymbolicName(), token);
                } else {
                    final String prefix = token.substring(0, pos).trim();
                    final String namespace = token.substring(pos+1).trim();
                    entries.add(new NamespaceEntry(prefix, namespace));
                }
            }
            if ( entries.size() > 0 ) {
                this.namespaceTable.put(bundle.getBundleId(), entries.toArray(new NamespaceEntry[entries.size()]));
            }
        }
    }

    /**
     * Unregister namespaces defined in the bundle.
     * @param bundle The bundle.
     */
    protected void unregisterNamespaces(Bundle bundle) {
        this.namespaceTable.remove(bundle.getBundleId());
    }

    private boolean registerBundleInternal (Bundle bundle, boolean isRetry) {
        try {
            if (this.registerNodeTypes(bundle, isRetry)) {
                return true;
            }
        } catch (RepositoryException re) {
            if ( isRetry ) {
                log.error("Cannot register node types for bundle {}: {}",
                    bundle.getSymbolicName(), re);
            } else {
                log.debug("Retrying to register node types failed for bundle {}: {}",
                        bundle.getSymbolicName(), re);
            }
        }

        return false;
    }

    private boolean registerNodeTypes(Bundle bundle, boolean isRetry) throws RepositoryException {
        // TODO: define header referring to mapper files
        String typesHeader = (String) bundle.getHeaders().get(NODETYPES_BUNDLE_HEADER);
        if (typesHeader == null) {
            // no node types in the bundle, return with success
            log.debug("registerNodeTypes: Bundle {} has no nodetypes",
                bundle.getSymbolicName());
            return true;
        }

        boolean success = true;
        Session session = this.getSession();
        try {
            StringTokenizer tokener = new StringTokenizer(typesHeader, ",");
            while (tokener.hasMoreTokens()) {
                String nodeTypeFile = tokener.nextToken().trim();
                Map<String,String> nodeTypeFileParams = new HashMap<String,String>();
                nodeTypeFileParams.put("reregister", "true");

                if (nodeTypeFile.contains(";")) {
                    int idx = nodeTypeFile.indexOf(';');
                    String nodeTypeFileParam = nodeTypeFile.substring(idx + 1);
                    String[] params = nodeTypeFileParam.split(":=");
                    nodeTypeFileParams.put(params[0], params[1]);
                    nodeTypeFile = nodeTypeFile.substring(0, idx);

                }

                URL mappingURL = bundle.getEntry(nodeTypeFile);
                if (mappingURL == null) {
                    // if we are retrying we already logged this message once, so we won't log it again
                    if ( !isRetry ) {
                        log.warn("Custom node type definition {} not found in bundle {}", nodeTypeFile, bundle.getSymbolicName());
                    }
                    continue;
                }

                InputStream ins = null;
                try {
                    // laod the node types
                    ins = mappingURL.openStream();
                    String reregister = nodeTypeFileParams.get("reregister");
                    boolean reregisterBool = Boolean.valueOf(reregister);
                    NodeTypeLoader.registerNodeType(session, mappingURL.toString(), new InputStreamReader(ins), reregisterBool);
                    // log a message if retry is successful
                    if ( isRetry ) {
                        log.info("Retrying to register node types from {} in bundle {} succeeded.",
                           new Object[]{ nodeTypeFile, bundle.getSymbolicName()});
                    }
                } catch (IOException ioe) {
                    success = false;
                    // if we are retrying we already logged this message once, so we won't log it again
                    if ( !isRetry ) {
                        log.warn("Cannot read node types {} from bundle {}: {}",
                            new Object[]{ nodeTypeFile, bundle.getSymbolicName(), ioe });
                        log.warn("Stacktrace ", ioe);
                    }
                } catch (Exception e) {
                    success = false;
                    // if we are retrying we already logged this message once, so we won't log it again
                    if ( !isRetry ) {
                        log.error("Error loading node types {} from bundle {}: {}",
                            new Object[]{ nodeTypeFile, bundle.getSymbolicName(), e });
                        log.error("Stacktrace ", e);
                    }
                } finally {
                    if (ins != null) {
                        try {
                            ins.close();
                        } catch (IOException ioe) {
                            // ignore
                        }
                    }
                }
            }
        } finally {
            this.ungetSession(session);
        }

        return success;
    }

    private Session getSession() throws RepositoryException {
        // TODO: Should really use loginService !!
        return this.slingRepository.loginAdministrative(null);
    }

    private void ungetSession(Session session) {
        if (session != null) {
            session.logout();
        }
    }

    private static class NamespaceEntry {

        public final String prefix;
        public final String namespace;

        public NamespaceEntry(String p, String n) {
            this.prefix = p;
            this.namespace = n;
        }
    }
}

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

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.NodeTypeLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>Loader</code> TODO
 */
public class Loader implements BundleListener {

    public static final String NODETYPES_BUNDLE_HEADER = "Sling-Nodetypes";

    public static final String NAMESPACES_BUNDLE_HEADER = "Sling-Namespaces";

    /** default log */
    private final Logger logger = LoggerFactory.getLogger(Loader.class);

    private final BundleContext bundleContext;

    private final SlingRepository slingRepository;

    // bundles whose registration failed and should be retried
    private final List<Bundle> delayedBundles;

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

    private String getBundleIdentifier(final Bundle bundle) {
        final StringBuilder sb = new StringBuilder();
        sb.append(bundle.getSymbolicName());
        sb.append(':');
        sb.append(bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        sb.append(" (");
        sb.append(bundle.getBundleId());
        sb.append(')');

        return sb.toString();
    }

    //---------- NamespaceMapper interface

    private void defineNamespacePrefixes(final Bundle bundle, final Session session, final List<NamespaceEntry> entries)
    throws RepositoryException {
        for(final NamespaceEntry entry : entries) {

            // the namespace prefixing is a little bit tricky:
            String mappedPrefix = null;
            // first, we check if the namespace is registered with a prefix
            try {
                mappedPrefix = session.getNamespacePrefix(entry.namespace);
            } catch (final NamespaceException ne) {
                try {
                    session.getWorkspace().getNamespaceRegistry().registerNamespace(entry.prefix, entry.namespace);
                } catch (final NamespaceException ne2) {
                    logger.warn("Unable to register namespace {}:{} from bundle {} : {}",
                            new Object[] {entry.prefix, entry.namespace, entry.prefix, getBundleIdentifier(bundle), ne2.getMessage()});
                }
            }
            if ( mappedPrefix != null && !mappedPrefix.equals(entry.prefix ) ) {
                logger.warn("Namespace for {} is already registered with prefix {}. Ignoring prefix {} from bundle {}",
                        new Object[] {entry.namespace, mappedPrefix, entry.prefix, getBundleIdentifier(bundle)});
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
    @Override
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
    private void registerNamespaces(final Bundle bundle) throws RepositoryException {
        final String definition = (String) bundle.getHeaders().get(NAMESPACES_BUNDLE_HEADER);
        if ( definition != null ) {
            logger.debug("registerNamespaces: Bundle {} tries to register: {}",
                    getBundleIdentifier(bundle), definition);
            final StringTokenizer st = new StringTokenizer(definition, ",");
            final List<NamespaceEntry>entries = new ArrayList<NamespaceEntry>();

            while ( st.hasMoreTokens() ) {
                final String token = st.nextToken().trim();
                int pos = token.indexOf('=');
                if ( pos == -1 ) {
                    logger.warn("registerNamespaces: Bundle {} has an invalid namespace manifest header entry: {}",
                            getBundleIdentifier(bundle), token);
                } else {
                    final String prefix = token.substring(0, pos).trim();
                    final String namespace = token.substring(pos+1).trim();
                    entries.add(new NamespaceEntry(prefix, namespace));
                }
            }
            if ( entries.size() > 0 ) {
                final Session session = this.getSession();
                try {
                    this.defineNamespacePrefixes(bundle, session, entries);
                } finally {
                    this.ungetSession(session);
                }
            }
        }
    }

    private boolean registerBundleInternal (Bundle bundle, boolean isRetry) {
        try {
            this.registerNamespaces(bundle);
            if (this.registerNodeTypes(bundle, isRetry)) {
                return true;
            }
        } catch (RepositoryException re) {
            if ( isRetry ) {
                logger.error("Cannot register node types for bundle {}: {}",
                    getBundleIdentifier(bundle), re);
            } else {
                logger.debug("Retrying to register node types failed for bundle {}: {}",
                        getBundleIdentifier(bundle), re);
            }
        }

        return false;
    }

    private boolean registerNodeTypes(Bundle bundle, boolean isRetry) throws RepositoryException {
        // TODO: define header referring to mapper files
        String typesHeader = (String) bundle.getHeaders().get(NODETYPES_BUNDLE_HEADER);
        if (typesHeader == null) {
            // no node types in the bundle, return with success
            logger.debug("registerNodeTypes: Bundle {} has no nodetypes",
                getBundleIdentifier(bundle));
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
                        logger.warn("Custom node type definition {} not found in bundle {}", nodeTypeFile, getBundleIdentifier(bundle));
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
                        logger.info("Retrying to register node types from {} in bundle {} succeeded.",
                           new Object[]{ nodeTypeFile, getBundleIdentifier(bundle)});
                    }
                } catch (IOException ioe) {
                    success = false;
                    // if we are retrying we already logged this message once, so we won't log it again
                    if ( !isRetry ) {
                        logger.warn("Cannot read node types {} from bundle {}: {}",
                            new Object[]{ nodeTypeFile, getBundleIdentifier(bundle), ioe });
                        logger.warn("Stacktrace ", ioe);
                    }
                } catch (Exception e) {
                    success = false;
                    // if we are retrying we already logged this message once, so we won't log it again
                    if ( !isRetry ) {
                        logger.error("Error loading node types {} from bundle {}: {}",
                            new Object[]{ nodeTypeFile, getBundleIdentifier(bundle), e });
                        logger.error("Stacktrace ", e);
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

    private void ungetSession(final Session session) {
        if (session != null) {
            session.logout();
        }
    }

    private static class NamespaceEntry {

        public final String prefix;
        public final String namespace;

        public NamespaceEntry(final String p, final String n) {
            this.prefix = p;
            this.namespace = n;
        }
    }
}

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
package org.apache.sling.jcr.internal.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.jcr.AbstractSlingRepository;
import org.apache.sling.jcr.NodeTypeLoader;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>Loader</code> TODO
 */
public class Loader {

    public static final String NODETYPES_BUNDLE_HEADER = "Sling-Nodetypes";

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(Loader.class);

    private AbstractSlingRepository slingRepository;

    // bundles whose registration failed and should be retried
    private List<Bundle> delayedBundles;

    public Loader(AbstractSlingRepository repository) {
        this.slingRepository = repository;
        this.delayedBundles = new LinkedList<Bundle>();
    }

    public void dispose() {
        if (this.delayedBundles != null) {
            this.delayedBundles.clear();
            this.delayedBundles = null;
        }
        this.slingRepository = null;
    }

    public void registerBundle(Bundle bundle) {
        if (this.registerBundleInternal(bundle)) {
            // handle delayed bundles, might help now
            int currentSize = -1;
            for (int i=this.delayedBundles.size(); i > 0 && currentSize != this.delayedBundles.size() && !this.delayedBundles.isEmpty(); i--) {
                for (Iterator<Bundle> di=this.delayedBundles.iterator(); di.hasNext(); ) {
                    Bundle delayed = di.next();
                    if (this.registerBundleInternal(delayed)) {
                        di.remove();
                    }
                }
                currentSize = this.delayedBundles.size();
            }
        } else {
            // add to delayed bundles
            this.delayedBundles.add(bundle);
        }
    }

    public void unregisterBundle(Bundle bundle) {
        if ( this.delayedBundles.contains(bundle) ) {
            this.delayedBundles.remove(bundle);
        }
    }

    private boolean registerBundleInternal (Bundle bundle) {
        try {
            if (this.registerNodeTypes(bundle)) {
                return true;
            }
        } catch (RepositoryException re) {
            log.error("Cannot register node types for bundle {}: {}",
                bundle.getSymbolicName(), re);
        }

        return false;
    }

    private boolean registerNodeTypes(Bundle bundle) throws RepositoryException {
        // TODO: define header referring to mapper files
        String typesHeader = (String) bundle.getHeaders().get(NODETYPES_BUNDLE_HEADER);
        if (typesHeader == null) {
            // no components in the bundle, return with success
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

                URL mappingURL = bundle.getEntry(nodeTypeFile);
                if (mappingURL == null) {
                    log.warn("Mapping {} not found in bundle {}", nodeTypeFile, bundle.getSymbolicName());
                    continue;
                }

                InputStream ins = null;
                try {
                    // laod the descriptors
                    ins = mappingURL.openStream();
                    NodeTypeLoader.registerNodeType(session, ins);
                } catch (IOException ioe) {
                    success = false;
//                    log.error("Cannot read node types " + nodeTypeFile
//                        + " from bundle " + bundle.getSymbolicName(), ioe);
                    log.warn("Cannot read node types {} from bundle {}: {}",
                        new Object[]{ nodeTypeFile, bundle.getSymbolicName(), ioe });
                } catch (Exception e) {
                    success = false;
//                    log.error("Error loading node types " + nodeTypeFile
//                        + " from bundle " + bundle.getSymbolicName(), e);
                    log.error("Error loading node types {} from bundle {}: {}",
                        new Object[]{ nodeTypeFile, bundle.getSymbolicName(), e });
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
        return this.slingRepository.loginAdministrative(null);
    }

    private void ungetSession(Session session) {
        if (session != null) {
            session.logout();
        }
    }
}

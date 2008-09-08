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
package org.apache.sling.jcr.jcrinstall;

import static org.apache.sling.jcr.jcrinstall.JcrBundlesConstants.BUNDLES_NODENAME;
import static org.apache.sling.jcr.jcrinstall.JcrBundlesConstants.STATUS_BASE_PATH;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JcrBundlesManager service, manages OSGi bundles and
 * configurations stored in the JCR Repository.
 *
 * @scr.service
 * @scr.component immediate="true"
 * metatype="no"
 * @scr.property name="service.description"
 * value="Sling JCR Bundles Manager Service"
 * @scr.property name="service.vendor"
 * value="The Apache Software Foundation"
 */

public class JcrBundlesManager implements Runnable, SynchronousBundleListener {
    /**
     * Ordered list of root paths to observe for bundles and configs
     * TODO should be configurable
     */
    private String[] bundleRoots = {"/libs", "/apps"};

    /**
     * List of processors for our bundles and configs
     */
    private List<NodeProcessor> processors;

    /**
     * Set of BundleFolders that we manage
     */
    private Set<BundlesFolder> folders;

    /**
     * @scr.reference
     */
    private SlingRepository repository;

    /**
     * @scr.reference
     */
    private PackageAdmin padmin;

    /**
     * @scr.reference
     */
    private ConfigurationAdmin cadmin;

    /**
     * Listeners for new BundleFolders under our bundleRoots
     */
    private List<BundlesFolderCreationListener> listeners = new LinkedList<BundlesFolderCreationListener>();

    private Session session;
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private boolean running;
    private boolean processPendingBundles;
    private long loopDelay;

    private final Map<String, Bundle> pendingBundles = new HashMap<String, Bundle>();

    /**
     * When activated, collect the list of bundle folders to scan
     * and register as a listener for future updates.
     */
    protected void activate(ComponentContext context) throws RepositoryException {

        // Listen to bundle events to find out if our pending bundles list
        // needs processing
        context.getBundleContext().addBundleListener(this);
        
        // setup our processors
        processors = new LinkedList<NodeProcessor>();
        processors.add(new BundleNodeProcessor(this, context, padmin));
        processors.add(new ConfigNodeProcessor(cadmin));
        processors.add(new NodeProcessor() {
            public boolean accepts(Node n) throws RepositoryException {
                return true;
            }

            public void process(Node n, Map<String, Boolean> flags) throws RepositoryException {
                log.debug("Node {} ignored in process() call, no NodeProcessor accepts it", n.getPath());
            }

            public void checkDeletions(Node statusNode, Map<String, Boolean> flags) throws Exception {
                log.debug("Node {} ignored in checkDeletions() call, no NodeProcessor accepts it", statusNode.getPath());
            }

        });

        // find "bundles" folders and add them to processing queue
        folders = new HashSet<BundlesFolder>();
        for (String rootPath : bundleRoots) {
            folders.addAll(BundlesFolder.findBundlesFolders(repository, rootPath, processors));
        }

        // Listen for any new "bundles" folders created after activation
        session = repository.loginAdministrative(repository.getDefaultWorkspace());
        final int eventTypes = Event.NODE_ADDED;
        final boolean isDeep = true;
        final boolean noLocal = true;
        for (String path : this.bundleRoots) {
            final BundlesFolderCreationListener bfc = new BundlesFolderCreationListener();
            listeners.add(bfc);
            session.getWorkspace().getObservationManager().addEventListener(bfc, eventTypes, path,
                    isDeep, null, null, noLocal);
        }

        // start queue processing
        final Thread t = new Thread(this, getClass().getSimpleName() + "_" + System.currentTimeMillis());
        t.setDaemon(true);
        running = true;
        t.start();
    }

    /**
     * Cleanup
     */
    protected void deactivate(ComponentContext oldContext) {
        running = false;

        oldContext.getBundleContext().removeBundleListener(this);
        
        for (BundlesFolder bf : folders) {
            try {
                bf.cleanup();
            } catch (RepositoryException e) {
                log.warn("RepositoryException in deactivate/cleanup", e);
            }
        }

        folders.clear();

        if (session != null) {
            try {
                for (BundlesFolderCreationListener bfc : listeners) {
                    session.getWorkspace().getObservationManager().removeEventListener(bfc);
                }
            } catch (RepositoryException re) {
                log.warn("RepositoryException in deactivate()/removeEventListener", re);
            }

            listeners.clear();

            session.logout();
            session = null;
        }
    }

    protected void addPendingBundle(String path, Bundle bundle) {
        synchronized (pendingBundles) {
            processPendingBundles = true;
            pendingBundles.put(path, bundle);
            log.debug("Bundle {} added to list of pending bundles.", path);
        }
    }
    
    protected void removePendingBundle(String path) {
        synchronized (pendingBundles) {
            processPendingBundles = true;
            if (pendingBundles.remove(path) != null) {
                log.debug("Removed bundle {} from pending list.", path);
            }
        }
    }
    
    public void bundleChanged(BundleEvent event) {
        processPendingBundles = true;
    }

    /**
     * Scan paths once their timer expires
     */
    public void run() {
        log.info("{} thread {} starts", getClass().getSimpleName(), Thread.currentThread().getName());
        Session s = null;

        // First check for deletions, using the status folder root path
        BundlesFolder statusFolder = null;
        try {
            statusFolder = new BundlesFolder(repository, STATUS_BASE_PATH, processors);
            final Map<String, Boolean> flags = new HashMap<String, Boolean>();
            statusFolder.checkDeletions(flags);
            refreshAndResolve(flags);
        } catch (Exception e) {
            log.error("Exception during initial scanning of " + STATUS_BASE_PATH, e);
        } finally {
            if (statusFolder != null) {
                try {
                    statusFolder.cleanup();
                } catch (Exception e) {
                    log.error("Exception during BundlesFolder cleanup of " + STATUS_BASE_PATH, e);
                }
            }
        }

        // We could use the scheduler service but that makes things harder to test
        while (running) {
            loopDelay = 1000L;
            try {
                s = repository.loginAdministrative(repository.getDefaultWorkspace());
                runOneCycle(s);
            } catch (IllegalArgumentException ie) {
                log.warn("IllegalArgumentException  in " + getClass().getSimpleName(), ie);
            } catch (RepositoryException re) {
                log.warn("RepositoryException in " + getClass().getSimpleName(), re);
            } catch (Throwable t) {
                log.error("Unhandled Throwable in runOneCycle() - "
                        + getClass().getSimpleName() + " thread will be stopped", t);
            } finally {
                if (s != null) {
                    s.logout();
                    s = null;
                }
                try {
                    Thread.sleep(loopDelay);
                } catch (InterruptedException ignore) {
                    // ignore
                }
            }
        }

        log.info("{} thread {} ends", getClass().getSimpleName(), Thread.currentThread().getName());
    }

    /**
     * Run one cycle of processing our scanTimes queue
     */
    void runOneCycle(Session s) throws Exception {

        // Add new bundle folders that onEvent created
        for (BundlesFolderCreationListener bfc : listeners) {
            for (String path : bfc.getAndClearPaths()) {
                log.info("New \"" + BUNDLES_NODENAME + "\" node was detected at {}, creating BundlesFolder to watch it", path);
                folders.add(new BundlesFolder(repository, path, processors));
            }
        }

        final Map<String, Boolean> flags = new HashMap<String, Boolean>();
        
        // Let our BundlesFolders do their work
        for (BundlesFolder bf : folders) {
            if (!running) {
                break;
            }
            bf.scanIfNeeded(flags);
        }

        // Process bundles that could not be started or resolved, if any
        processPendingBundles(flags);
        
        // Refresh/resolve packages if needed
        refreshAndResolve(flags);
    }
    
    /** Process pending bundles if needed */ 
    private void processPendingBundles(Map<String, Boolean> flags) throws BundleException {
        synchronized (pendingBundles) {
            
            // Do nothing if list is empty or if no interesting events happened
            // since last processed
            if(!processPendingBundles || pendingBundles.isEmpty()) {
                return;
            }
            processPendingBundles = false;
            
            log.debug("Processing {} pending bundles", pendingBundles.size());
            
            // If we start or resolve any bundles, trigger the next
            // scanning loop immediately, as that might allow more
            // bundles to start
            boolean scanImmediately = false;

            // Walk the list of pending bundles
            final Iterator<String> iter = pendingBundles.keySet().iterator();
            while (iter.hasNext()) {
                String location = iter.next();
                Bundle bundle = pendingBundles.get(location);
                log.debug("Checking bundle {}, current state={}", location, bundle.getState());
                
                if ((bundle.getState() & Bundle.ACTIVE) > 0) {
                    log.info("Bundle {} is active.", location);
                    flags.put("refresh.packages", Boolean.TRUE);
                    flags.put("resolve.bundles", Boolean.TRUE);
                    scanImmediately = true;
                    processPendingBundles = true;
                    iter.remove();
                    
                } else if ((bundle.getState() & Bundle.STARTING) > 0) {
                    log.info("Bundle {} is starting.", location);
                    
                } else if ((bundle.getState() & Bundle.STOPPING) > 0) {
                    log.info("Bundle {} is stopping.", location);
                    
                } else if ((bundle.getState() & Bundle.UNINSTALLED) > 0) {
                    log.info("Bundle {} is uninstalled.", location);
                    processPendingBundles = true;
                    iter.remove();
                    
                } else if ((bundle.getState() & Bundle.RESOLVED) > 0) {
                    log.info("Bundle {} is resolved, trying to start it.", location);
                    flags.put("resolve.bundles", Boolean.TRUE);
                    bundle.start();
                    scanImmediately = true;
                    
                } else if ((bundle.getState() & Bundle.INSTALLED) > 0) {
                    log.info("Bundle {} is installed but not resolved.", location);
                    flags.put("resolve.bundles", Boolean.TRUE);
                    scanImmediately = true;
                }
            }
            
            log.debug("Done processing pending bundles, {} bundles left in list", pendingBundles.size());
            
            if(scanImmediately) {
                loopDelay = 0;
            }
        }
    }

    /** If flags say so, refresh/resolve packages */
    void refreshAndResolve(Map<String, Boolean> flags) {
        if (Boolean.TRUE.equals(flags.get("resolve.bundles"))) {
            log.debug("Resolving bundles");
            padmin.resolveBundles(null);
        }
        if (Boolean.TRUE.equals(flags.get("refresh.packages"))) {
            log.debug("Refreshing packages");
            padmin.refreshPackages(null);
        }
    }

}
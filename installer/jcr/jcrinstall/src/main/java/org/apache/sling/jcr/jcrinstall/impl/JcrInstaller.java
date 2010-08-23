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
package org.apache.sling.jcr.jcrinstall.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main class of jcrinstall, runs as a service, observes the
 * 	repository for changes in folders having names that match
 * 	configurable regular expressions, and registers resources
 *  found in those folders with the OSGi installer for installation.
 *
 * @scr.component
 *  label="%jcrinstall.name"
 *  description="%jcrinstall.description"
 *  immediate="true"
 *  @scr.property
 *      name="service.description"
 *      value="Sling Jcrinstall Service"
 *  @scr.property
 *      name="service.vendor"
 *      value="The Apache Software Foundation"
 */
public class JcrInstaller implements EventListener {
	public static final long RUN_LOOP_DELAY_MSEC = 500L;
	public static final String URL_SCHEME = "jcrinstall";

	private final Logger log = LoggerFactory.getLogger(getClass());

	/** Counters, used for statistics and testing */
	private final long [] counters = new long[COUNTERS_COUNT];
	public static final int SCAN_FOLDERS_COUNTER = 0;
    public static final int UPDATE_FOLDERS_LIST_COUNTER = 1;
    public static final int RUN_LOOP_COUNTER = 2;
    public static final int COUNTERS_COUNT = 3;

    /**	This class watches the repository for installable resources
     * @scr.reference
     */
    private SlingRepository repository;

    /** Additional installation folders are activated based
     *  on the current RunMode. For example, /libs/foo/install.dev
     *  if the current run mode is "dev".
     *  @scr.reference
     */
    private SlingSettingsService settings;

    /**	The OsgiInstaller installs resources in the OSGi framework.
     * 	@scr.reference
     */
    private OsgiInstaller installer;

    /** Default regexp for watched folders */
    public static final String DEFAULT_FOLDER_NAME_REGEXP = ".*/install$";

    /** ComponentContext property that overrides the folder name regexp
     * 	@scr.property valueRef="DEFAULT_FOLDER_NAME_REGEXP"
     */
    public static final String FOLDER_NAME_REGEXP_PROPERTY = "sling.jcrinstall.folder.name.regexp";

    /** Configurable max. path depth for watched folders
     *  @scr.property valueRef="DEFAULT_FOLDER_MAX_DEPTH" type="Integer"
     */
    public static final String PROP_INSTALL_FOLDER_MAX_DEPTH = "sling.jcrinstall.folder.max.depth";

    /**	Configurable search path, with per-path priorities.
     *  We could get it from the ResourceResolver, but introducing a dependency on this just to get those
     *  values is too much for this module that's meant to bootstrap other services.
     *
     * 	@scr.property values.1="/libs:100" values.2="/apps:200"
     */
    public static final String PROP_SEARCH_PATH = "sling.jcrinstall.search.path";
    public static final String [] DEFAULT_SEARCH_PATH = { "/libs:100", "/apps:200" };

    public static final int DEFAULT_FOLDER_MAX_DEPTH = 4;
    private int maxWatchedFolderDepth;

    /** Filter for folder names */
    private FolderNameFilter folderNameFilter;

    /** List of watched folders */
    private List<WatchedFolder> watchedFolders;

    /** Session shared by all WatchedFolder */
    private Session session;

    /** The root folders that we watch */
    private String [] roots;

    /** Convert Nodes to InstallableResources */
    static interface NodeConverter {
    	InstallableResource convertNode(Node n, int priority)
    	throws Exception;
    }

    /** Our NodeConverters*/
    private final Collection <NodeConverter> converters = new ArrayList<NodeConverter>();

    /** Detect newly created folders that we must watch */
    private final List<RootFolderListener> listeners = new LinkedList<RootFolderListener>();

    /** Timer used to call updateFoldersList() */
    private final RescanTimer updateFoldersListTimer = new RescanTimer();

    /** Thread that can be cleanly stopped with a flag */
    static int bgThreadCounter;
    class StoppableThread extends Thread {
        boolean active = true;
        StoppableThread() {
            synchronized (JcrInstaller.class) {
                setName("JcrInstaller." + (++bgThreadCounter));
            }
            setDaemon(true);
        }

        @Override
        public final void run() {
            log.info("Background thread {} starting", Thread.currentThread().getName());
            while(active) {
                runOneCycle();
            }
            log.info("Background thread {} done", Thread.currentThread().getName());
            counters[RUN_LOOP_COUNTER] = -1;
        }
    };
    private StoppableThread backgroundThread;

    protected void activate(ComponentContext context) throws Exception {
        log.info("Activating Apache Sling JCR Installer");

        // open session
    	session = repository.loginAdministrative(repository.getDefaultWorkspace());

    	// Setup converters
    	converters.add(new FileNodeConverter());
    	converters.add(new ConfigNodeConverter());

    	// Configurable max depth, system property (via bundle context) overrides default value
    	final Object obj = getPropertyValue(context, PROP_INSTALL_FOLDER_MAX_DEPTH);
    	if (obj != null) {
    		// depending on where it's coming from, obj might be a string or integer
    		maxWatchedFolderDepth = Integer.valueOf(String.valueOf(obj)).intValue();
            log.debug("Using configured ({}) folder name max depth '{}'", PROP_INSTALL_FOLDER_MAX_DEPTH, maxWatchedFolderDepth);
    	} else {
            maxWatchedFolderDepth = DEFAULT_FOLDER_MAX_DEPTH;
            log.debug("Using default folder max depth {}, not provided by {}", maxWatchedFolderDepth, PROP_INSTALL_FOLDER_MAX_DEPTH);
    	}

    	// Configurable folder regexp, system property overrides default value
    	String folderNameRegexp = (String)getPropertyValue(context, FOLDER_NAME_REGEXP_PROPERTY);
    	if(folderNameRegexp != null) {
    		folderNameRegexp = folderNameRegexp.trim();
            log.debug("Using configured ({}) folder name regexp '{}'", FOLDER_NAME_REGEXP_PROPERTY, folderNameRegexp);
    	} else {
    	    folderNameRegexp = DEFAULT_FOLDER_NAME_REGEXP;
            log.debug("Using default folder name regexp '{}', not provided by {}", folderNameRegexp, FOLDER_NAME_REGEXP_PROPERTY);
    	}

    	// Setup folder filtering and watching
        folderNameFilter = new FolderNameFilter(OsgiUtil.toStringArray(context.getProperties().get(PROP_SEARCH_PATH), DEFAULT_SEARCH_PATH),
                folderNameRegexp, settings.getRunModes());
        roots = folderNameFilter.getRootPaths();
        for (String path : roots) {
            listeners.add(new RootFolderListener(session, folderNameFilter, path, updateFoldersListTimer));
            log.debug("Configured root folder: {}", path);
        }

        // Watch for events on the root - that might be one of our root folders
        session.getWorkspace().getObservationManager().addEventListener(this,
                Event.NODE_ADDED | Event.NODE_REMOVED,
                "/",
                false, // isDeep
                null,
                null,
                true); // noLocal
        log.debug("Watching for node events on / to detect removal/add of our root folders");


    	// Find paths to watch and create WatchedFolders to manage them
    	watchedFolders = new LinkedList<WatchedFolder>();
    	for(String root : roots) {
    		findPathsToWatch(root, watchedFolders);
    	}

    	// Scan watchedFolders and register resources with installer
    	final List<InstallableResource> resources = new LinkedList<InstallableResource>();
    	for(WatchedFolder f : watchedFolders) {
    		final WatchedFolder.ScanResult r = f.scan();
    		log.debug("Startup: {} provides resources {}", f, r.toAdd);
    		resources.addAll(r.toAdd);
    	}

    	log.debug("Registering {} resources with OSGi installer: {}", resources.size(), resources);
    	installer.registerResources(URL_SCHEME, resources);

    	if (backgroundThread != null) {
    	    throw new IllegalStateException("Expected backgroundThread to be null in activate()");
    	}
        backgroundThread = new StoppableThread();
        backgroundThread.start();
    }

    protected void deactivate(ComponentContext context) {
    	log.info("Deactivating Apache Sling JCR Installer");

    	final long timeout = 30000L;
    	try {
            backgroundThread.active = false;
            log.debug("Waiting for " + backgroundThread.getName() + " Thread to end...");
            backgroundThread.join(timeout);
            backgroundThread = null;
    	} catch(InterruptedException iex) {
    	    throw new IllegalStateException("backgroundThread.join interrupted after " + timeout + " msec");
    	}

        try {
            folderNameFilter = null;
            watchedFolders = null;
            converters.clear();
            if(session != null) {
                for(RootFolderListener wfc : listeners) {
                    wfc.cleanup(session);
                }
                session.getWorkspace().getObservationManager().removeEventListener(this);
                session.logout();
                session = null;
            }
            listeners.clear();
        } catch(Exception e) {
            log.warn("Exception in deactivate()", e);
        }
    }

    /** Get a property value from the component context or bundle context */
    protected Object getPropertyValue(ComponentContext ctx, String name) {
        Object result = ctx.getBundleContext().getProperty(name);
        if(result == null) {
            result = ctx.getProperties().get(name);
        }
        return result;
    }

    /** Find the paths to watch under rootPath, according to our folderNameFilter,
     * 	and add them to result */
    void findPathsToWatch(final String rootPath, final List<WatchedFolder> result) throws RepositoryException {
        Session s = null;

        try {
            s = repository.loginAdministrative(repository.getDefaultWorkspace());
            if (!s.itemExists(rootPath) || !s.getItem(rootPath).isNode() ) {
                log.info("Bundles root node {} not found, ignored", rootPath);
            } else {
                log.debug("Bundles root node {} found, looking for bundle folders inside it", rootPath);
                final Node n = (Node)s.getItem(rootPath);
                findPathsUnderNode(n, result);
            }
        } finally {
            if (s != null) {
                s.logout();
            }
        }
    }

    /**
     * Add n to result if it is a folder that we must watch, and recurse into its children
     * to do the same.
     */
    void findPathsUnderNode(final Node n, final List<WatchedFolder> result) throws RepositoryException {
        final String path = n.getPath();
        final int priority = folderNameFilter.getPriority(path);
        if (priority > 0) {
            result.add(new WatchedFolder(session, path, priority, converters));
        }
        final int depth = path.split("/").length;
        if(depth > maxWatchedFolderDepth) {
            log.debug("Not recursing into {} due to maxWatchedFolderDepth={}", path, maxWatchedFolderDepth);
            return;
        }
        final NodeIterator it = n.getNodes();
        while (it.hasNext()) {
            findPathsUnderNode(it.nextNode(), result);
        }
    }

    /** Add WatchedFolder to our list if it doesn't exist yet */
    private void addWatchedFolder(WatchedFolder toAdd) {
        WatchedFolder existing = null;
        for(WatchedFolder wf : watchedFolders) {
            if(wf.getPath().equals(toAdd.getPath())) {
                existing = wf;
                break;
            }
        }
        if(existing == null) {
            watchedFolders.add(toAdd);
            toAdd.scheduleScan();
        }
    }

    /** Add new folders to watch if any have been detected
     *  @return a list of InstallableResource that must be unregistered,
     *  	for folders that have been removed
     */
    private List<String> updateFoldersList() throws Exception {
        log.debug("Updating folder list.");
        
        final List<String> result = new LinkedList<String>();

        final List<WatchedFolder> newFolders = new ArrayList<WatchedFolder>();
	    for(String root : roots) {
	        findPathsToWatch(root, newFolders);
	    }
	    for(WatchedFolder wf : newFolders) {
	        addWatchedFolder(wf);
	    }

        // Check all WatchedFolder, in case some were deleted
        final List<WatchedFolder> toRemove = new ArrayList<WatchedFolder>();
        for(WatchedFolder wf : watchedFolders) {
            log.debug("Item {} exists? {}", wf.getPath(), session.itemExists(wf.getPath()));

            if(!session.itemExists(wf.getPath())) {
                result.addAll(wf.scan().toRemove);
                wf.cleanup();
                toRemove.add(wf);
            }
        }
        for(WatchedFolder wf : toRemove) {
            log.info("Deleting {}, path does not exist anymore", wf);
            watchedFolders.remove(wf);
        }

        return result;
    }

    public void onEvent(EventIterator it) {
        // Got a DELETE or ADD on root - schedule folders rescan if one
        // of our root folders is impacted
        try {
            while(it.hasNext()) {
                final Event e = it.nextEvent();
                log.debug("Got event {}", e);

                for(String root : roots) {
                    if (e.getPath().startsWith(root)) {
                        log.info("Got event for root {}, scheduling scanning of new folders", root);
                        updateFoldersListTimer.scheduleScan();
                    }
                }
            }
        } catch(RepositoryException re) {
            log.warn("RepositoryException in onEvent", re);
        }
    }

    /** Run periodic scans of our watched folders, and watch for folders creations/deletions */
    public void runOneCycle() {
        log.debug("Running watch cycle.");
        
        try {
            boolean didRefresh = true;

            // Rescan WatchedFolders if needed
            final boolean scanWf = WatchedFolder.getRescanTimer().expired();
            if(scanWf) {
                session.refresh(false);
                didRefresh = true;
                for(WatchedFolder wf : watchedFolders) {
                    if(!wf.needsScan()) {
                        continue;
                    }
                    WatchedFolder.getRescanTimer().reset();
                    counters[SCAN_FOLDERS_COUNTER]++;
                    final WatchedFolder.ScanResult sr = wf.scan();
                    for(String r : sr.toRemove) {
                        log.info("Removing resource from OSGi installer: {}",r);
                        installer.removeResource(URL_SCHEME, r);
                    }
                    for(InstallableResource r : sr.toAdd) {
                        log.info("Registering resource with OSGi installer: {}",r);
                        installer.addResource(URL_SCHEME, r);
                    }
                }
            }

            // Update list of WatchedFolder if we got any relevant events,
            // or if there were any WatchedFolder events
            if(scanWf || updateFoldersListTimer.expired()) {
                if (!didRefresh) {
                    session.refresh(false);
                    didRefresh = true;
                }
                updateFoldersListTimer.reset();
                counters[UPDATE_FOLDERS_LIST_COUNTER]++;
                final List<String> toRemove = updateFoldersList();
                for(String r : toRemove) {
                    log.info("Removing resource from OSGi installer (folder deleted): {}",r);
                    installer.removeResource(URL_SCHEME, r);
                }
            }

            try {
                Thread.sleep(RUN_LOOP_DELAY_MSEC);
            } catch(InterruptedException ignore) {
            }

        } catch(Exception e) {
            log.warn("Exception in run()", e);
            try {
                Thread.sleep(RUN_LOOP_DELAY_MSEC);
            } catch(InterruptedException ignore) {
            }
        }
        counters[RUN_LOOP_COUNTER]++;
    }

    long [] getCounters() {
        return counters;
    }

}
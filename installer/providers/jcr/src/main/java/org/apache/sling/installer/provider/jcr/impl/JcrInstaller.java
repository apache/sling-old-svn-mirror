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
package org.apache.sling.installer.provider.jcr.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.UpdateHandler;
import org.apache.sling.installer.api.UpdateResult;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class of jcrinstall, runs as a service, observes the
 * repository for changes in folders having names that match
 * configurable regular expressions, and registers resources
 * found in those folders with the OSGi installer for installation.
 */
@Component(label="%jcrinstall.name", description="%jcrinstall.description", immediate=true, metatype=true)
@Properties({
    @Property(name=Constants.SERVICE_DESCRIPTION, value="Sling JCR Install Service"),
    @Property(name=Constants.SERVICE_VENDOR, value="The Apache Software Foundation"),
    @Property(name=UpdateHandler.PROPERTY_SCHEMES, value=JcrInstaller.URL_SCHEME, unbounded=PropertyUnbounded.ARRAY),
    @Property(name=Constants.SERVICE_RANKING, intValue=100)
})
@Service(value=UpdateHandler.class)
public class JcrInstaller implements UpdateHandler, ManagedService {

	public static final long RUN_LOOP_DELAY_MSEC = 500L;
	public static final String URL_SCHEME = "jcrinstall";

	/** PID before refactoring. */
	public static final String OLD_PID = "org.apache.sling.jcr.install.impl.JcrInstaller";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/** Counters, used for statistics and testing */
	private final long [] counters = new long[COUNTERS_COUNT];
	public static final int SCAN_FOLDERS_COUNTER = 0;
    public static final int UPDATE_FOLDERS_LIST_COUNTER = 1;
    public static final int RUN_LOOP_COUNTER = 2;
    public static final int COUNTERS_COUNT = 3;

    private static final String NT_FILE = "nt:file";
    private static final String NT_RESOURCE = "nt:resource";
    private static final String PROP_DATA = "jcr:data";
    private static final String PROP_MODIFIED = "jcr:lastModified";
    private static final String PROP_ENC = "jcr:encoding";
    private static final String PROP_MIME = "jcr:mimeType";
    private static final String MIME_TXT = "text/plain";
    private static final String ENCODING = "UTF-8";

    /** Default regexp for watched folders */
    public static final String DEFAULT_FOLDER_NAME_REGEXP = ".*/install|config$";

    /**
     * ComponentContext property that overrides the folder name regexp
     */
    @Property(value=DEFAULT_FOLDER_NAME_REGEXP)
    public static final String FOLDER_NAME_REGEXP_PROPERTY = "sling.jcrinstall.folder.name.regexp";

    public static final int DEFAULT_FOLDER_MAX_DEPTH = 4;

    /**
     * Configurable max. path depth for watched folders
     */
    @Property(intValue=DEFAULT_FOLDER_MAX_DEPTH)
    public static final String PROP_INSTALL_FOLDER_MAX_DEPTH = "sling.jcrinstall.folder.max.depth";

    /**
     * Configurable search path, with per-path priorities.
     * We could get it from the ResourceResolver, but introducing a dependency on this just to get those
     * values is too much for this module that's meant to bootstrap other services.
     */
    @Property(value={"/libs:100", "/apps:200"}, unbounded=PropertyUnbounded.ARRAY)
    public static final String PROP_SEARCH_PATH = "sling.jcrinstall.search.path";
    public static final String [] DEFAULT_SEARCH_PATH = { "/libs:100", "/apps:200" };

    public static final String DEFAULT_NEW_CONFIG_PATH = "sling/install";
    @Property(value=DEFAULT_NEW_CONFIG_PATH)
    public static final String PROP_NEW_CONFIG_PATH = "sling.jcrinstall.new.config.path";

    public static final String PAUSE_SCAN_NODE_PATH = "/system/sling/installer/jcr/pauseInstallation";
    @Property(value= PAUSE_SCAN_NODE_PATH)
    public static final String PROP_SCAN_PROP_PATH = "sling.jcrinstall.signal.path";

    private volatile boolean pauseMessageLogged = false;

    public static final boolean DEFAULT_ENABLE_WRITEBACK = true;
    @Property(boolValue=DEFAULT_ENABLE_WRITEBACK)
    public static final String PROP_ENABLE_WRITEBACK = "sling.jcrinstall.enable.writeback";

    /**
     * This class watches the repository for installable resources
     */
    @Reference
    private SlingRepository repository;

    /**
     * Additional installation folders are activated based
     * on the current RunMode. For example, /libs/foo/install.dev
     * if the current run mode is "dev".
     */
    @Reference
    private SlingSettingsService settings;

    /**
     * The OsgiInstaller installs resources in the OSGi framework.
     */
    @Reference
    private OsgiInstaller installer;

    /** The component context. */
    private volatile ComponentContext componentContext;

    /** Service reg for managed service. */
    private volatile ServiceRegistration managedServiceRef;

    /** Configuration from managed service (old pid) */
    private volatile Dictionary<?, ?> oldConfiguration;

    /** Convert Nodes to InstallableResources */
    static interface NodeConverter {
    	InstallableResource convertNode(Node n, int priority)
    	throws RepositoryException;
    }

    /** Timer used to call updateFoldersList() */
    private final RescanTimer updateFoldersListTimer = new RescanTimer();

    /** Thread that can be cleanly stopped with a flag */
    private final static AtomicInteger bgThreadCounter = new AtomicInteger();

    class StoppableThread extends Thread implements EventListener {

        /** Used for synchronizing. */
        final Object lock = new Object();

        final AtomicBoolean active = new AtomicBoolean(false);

        private final AtomicBoolean running = new AtomicBoolean(false);

        private final InstallerConfig cfg;

        /** Detect newly created folders that we must watch */
        private final List<RootFolderListener> listeners = new LinkedList<RootFolderListener>();

        private volatile RootFolderMoveListener moveEventListener;

        /** Session shared by all WatchedFolder */
        private volatile Session session;

        StoppableThread(final InstallerConfig cfg) throws RepositoryException {
            this.cfg = cfg;
            setName("JcrInstaller." + String.valueOf(bgThreadCounter.incrementAndGet()));
            setDaemon(true);

            try {
                // open session
                session = repository.loginAdministrative(repository.getDefaultWorkspace());

                for (final String path : cfg.getRoots()) {
                    listeners.add(new RootFolderListener(session, path, updateFoldersListTimer, cfg));
                    logger.debug("Configured root folder: {}", path);
                }

                // Watch for events on the root - that might be one of our root folders
                session.getWorkspace().getObservationManager().addEventListener(this,
                        Event.NODE_ADDED | Event.NODE_REMOVED,
                        "/",
                        false, // isDeep
                        null,
                        null,
                        true); // noLocal
                // add special observation listener for move events
                if(cfg.getRoots() != null && cfg.getRoots().length > 0) {
                    moveEventListener = new RootFolderMoveListener(session, cfg.getRoots(),  updateFoldersListTimer);
                }

                logger.debug("Watching for node events on / to detect removal/add of our root folders");


                // Find paths to watch and create WatchedFolders to manage them
                for(final String root : cfg.getRoots()) {
                    findPathsToWatch(cfg, session, root);
                }

                // Scan watchedFolders and register resources with installer
                final List<InstallableResource> resources = cfg.scanWatchedFolders();
                logger.debug("Registering {} resources with OSGi installer: {}", resources.size(), resources);
                installer.registerResources(URL_SCHEME, resources.toArray(new InstallableResource[resources.size()]));
                this.active.set(true);
            } finally {
                if ( !this.active.get() ) {
                    shutdown();
                }
            }
        }

        public void shutdown() {
            while ( running.get() ) {
                try {
                    Thread.sleep(10);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            try {
                if (session != null) {
                    for(final RootFolderListener wfc : listeners) {
                        wfc.cleanup(session);
                    }
                    session.getWorkspace().getObservationManager().removeEventListener(this);
                    if (moveEventListener != null) {
                        moveEventListener.cleanup(session);
                        moveEventListener = null;
                    }
                }
            } catch (final RepositoryException e) {
                logger.warn("Exception in stop()", e);
            }
            if ( session != null ) {
                session.logout();
                session = null;
            }
            listeners.clear();
        }

        /**
         * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
         */
        public void onEvent(final EventIterator it) {
            // Got a DELETE or ADD on root - schedule folders rescan if one
            // of our root folders is impacted
            try {
                while(it.hasNext()) {
                    final Event e = it.nextEvent();
                    logger.debug("Got event {}", e);

                    this.checkChanges(e.getPath());
                }
            } catch(final RepositoryException re) {
                logger.warn("RepositoryException in onEvent", re);
            }
        }

        /**
         * Check for changes in any of the root folders
         */
        private void checkChanges(final String path) {
            for(final String root : cfg.getRoots()) {
                if (path.startsWith(root)) {
                    logger.info("Got event for root {}, scheduling scanning of new folders", root);
                    updateFoldersListTimer.scheduleScan();
                }
            }
        }

        @Override
        public final void run() {
            logger.info("Background thread {} starting", Thread.currentThread().getName());
            while (this.active.get()) {
                running.set(true);
                try {
                    runOneCycle(cfg, session);
                } finally {
                    running.set(false);
                }
            }
            logger.info("Background thread {} done", Thread.currentThread().getName());
            counters[RUN_LOOP_COUNTER] = -1;
        }

        public InstallerConfig getConfiguration() {
            return this.cfg;
        }
    };
    private volatile StoppableThread backgroundThread;

    /**
     * Activate this component.
     */
    protected void activate(final ComponentContext context) {
        this.componentContext = context;
        this.start();
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, OLD_PID);
        this.managedServiceRef = this.componentContext.getBundleContext().registerService(ManagedService.class.getName(),
                this,
                props);
    }

    private void start() {
        logger.info("Activating Apache Sling JCR Installer");
        final InstallerConfig cfg = new InstallerConfig(logger, componentContext, oldConfiguration, settings);

        try {
            this.backgroundThread = new StoppableThread(cfg);
            backgroundThread.start();
        } catch (final RepositoryException re) {
            logger.error("Repository exception during startup - deactivating installer!", re);
            final ComponentContext ctx = componentContext;
            if ( ctx  != null ) {
                final String name = (String) componentContext.getProperties().get(
                        ComponentConstants.COMPONENT_NAME);
                ctx.disableComponent(name);
            }
        }
    }

    /**
     * Deactivate this component
     */
    protected void deactivate(final ComponentContext context) {
        if ( this.managedServiceRef != null ) {
            this.managedServiceRef.unregister();
            this.managedServiceRef = null;
        }
        this.stop();
        this.componentContext = null;
    }

    private void stop() {
    	logger.info("Deactivating Apache Sling JCR Installer");

    	if ( backgroundThread != null ) {
    	    synchronized ( backgroundThread.lock ) {
    	        backgroundThread.active.set(false);
    	        backgroundThread.lock.notify();
    	    }
            logger.debug("Waiting for " + backgroundThread.getName() + " Thread to end...");

            this.backgroundThread.shutdown();
            this.backgroundThread = null;
    	}
    }


    /**
     * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
     */
    public void updated(@SuppressWarnings("rawtypes") Dictionary properties)
    throws ConfigurationException {
        final boolean restart;
        if ( this.oldConfiguration == null ) {
            restart = properties != null;
        } else {
            restart = true;
        }
        this.oldConfiguration = properties;
        if ( restart ) {
            try {
                this.stop();
                this.start();
            } catch (final Exception e) {
                logger.error("Error restarting", e);
            }
        }
    }

    /** Find the paths to watch under rootPath, according to our folderNameFilter,
     * 	and add them to result */
    private void findPathsToWatch(final InstallerConfig cfg, final Session session,
            final String rootPath) throws RepositoryException {
        Session s = null;

        try {
            s = repository.loginAdministrative(repository.getDefaultWorkspace());
            if (!s.itemExists(rootPath) || !s.getItem(rootPath).isNode() ) {
                logger.info("Bundles root node {} not found, ignored", rootPath);
            } else {
                logger.debug("Bundles root node {} found, looking for bundle folders inside it", rootPath);
                final Node n = (Node)s.getItem(rootPath);
                findPathsUnderNode(cfg, session, n);
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
    void findPathsUnderNode(final InstallerConfig cfg, final Session session,
            final Node n) throws RepositoryException {
        final String path = n.getPath();
        final int priority = cfg.getFolderNameFilter().getPriority(path);
        if (priority > 0) {
            cfg.addWatchedFolder(new WatchedFolder(session, path, priority, cfg.getConverters()));
        }
        final int depth = path.split("/").length;
        if(depth > cfg.getMaxWatchedFolderDepth()) {
            logger.debug("Not recursing into {} due to maxWatchedFolderDepth={}", path, cfg.getMaxWatchedFolderDepth());
            return;
        }
        final NodeIterator it = n.getNodes();
        while (it.hasNext()) {
            findPathsUnderNode(cfg, session, it.nextNode());
        }
    }

    /**
     * Add new folders to watch if any have been detected
     * @return a list of InstallableResource that must be unregistered,
     *  	for folders that have been removed
     */
    private List<String> updateFoldersList(final InstallerConfig cfg, final Session session) throws Exception {
        logger.debug("Updating folder list.");

	    for(final String root : cfg.getRoots()) {
	        findPathsToWatch(cfg, session, root);
	    }

        // Check all WatchedFolder, in case some were deleted
        final List<String> removedResources = cfg.checkForRemovedWatchedFolders(session);

        return removedResources;
    }

    InstallerConfig getConfiguration() {
        InstallerConfig cfg = null;
        final StoppableThread st = this.backgroundThread;
        if ( st != null ) {
            cfg = st.getConfiguration();
        }
        return cfg;
    }

    Session getSession() {
        return this.backgroundThread.session;
    }

    /**
     * Run periodic scans of our watched folders, and watch for folders creations/deletions.
     */
    public void runOneCycle(final InstallerConfig cfg, final Session session) {
        logger.debug("Running watch cycle.");

        try {
            boolean didRefresh = false;

            if (cfg.anyWatchFolderNeedsScan()) {
                session.refresh(false);
                didRefresh = true;
                if (scanningIsPaused(cfg, session)) {
                    if (!pauseMessageLogged) {
                        //Avoid flooding the logs every 500 msec so log at info level once
                        logger.info("Detected signal for pausing the JCR Provider i.e. child nodes found under path {}. " +
                                "JCR Provider scanning would not be performed", cfg.getPauseScanNodePath());
                        pauseMessageLogged = true;
                    }
                    return;
                } else if (pauseMessageLogged) {
                    pauseMessageLogged = false;
                }
            }

            // Rescan WatchedFolders if needed
            boolean scanWf = false;
            for(final WatchedFolder wf : cfg.cloneWatchedFolders()) {
                if (!wf.needsScan()) {
                    continue;
                }
                scanWf = true;
                if ( !didRefresh ) {
                    session.refresh(false);
                    didRefresh = true;
                }
                counters[SCAN_FOLDERS_COUNTER]++;
                final WatchedFolder.ScanResult sr = wf.scan();
                boolean toDo = false;
                if ( sr.toAdd.size() > 0 ) {
                    logger.info("Registering resource with OSGi installer: {}",sr.toAdd);
                    toDo = true;
                }
                if ( sr.toRemove.size() > 0 ) {
                    logger.info("Removing resource from OSGi installer: {}", sr.toRemove);
                    toDo = true;
                }
                if ( toDo ) {
                    installer.updateResources(URL_SCHEME, sr.toAdd.toArray(new InstallableResource[sr.toAdd.size()]),
                        sr.toRemove.toArray(new String[sr.toRemove.size()]));
                }
            }

            // Update list of WatchedFolder if we got any relevant events,
            // or if there were any WatchedFolder events
            if (scanWf || updateFoldersListTimer.expired()) {
                if (!didRefresh) {
                    session.refresh(false);
                    didRefresh = true;
                }
                updateFoldersListTimer.reset();
                counters[UPDATE_FOLDERS_LIST_COUNTER]++;
                final List<String> toRemove = updateFoldersList(cfg, session);
                if ( toRemove.size() > 0 ) {
                    logger.info("Removing resource from OSGi installer (folder deleted): {}", toRemove);
                    installer.updateResources(URL_SCHEME, null,
                            toRemove.toArray(new String[toRemove.size()]));
                }
            }


        } catch (final Exception e) {
            logger.warn("Exception in runOneCycle()", e);
        }

        if ( backgroundThread.active.get() ) {
            synchronized ( backgroundThread.lock ) {
                try {
                    backgroundThread.lock.wait(RUN_LOOP_DELAY_MSEC);
                } catch (final InterruptedException ignore) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        counters[RUN_LOOP_COUNTER]++;
    }

    boolean scanningIsPaused(final InstallerConfig cfg, final Session session) throws RepositoryException {
        if (session.nodeExists(cfg.getPauseScanNodePath())) {
            Node node = session.getNode(cfg.getPauseScanNodePath());
            boolean result = node.hasNodes();
            if (result && logger.isDebugEnabled()) {
                List<String> nodeNames = new ArrayList<String>();
                NodeIterator childItr = node.getNodes();
                while (childItr.hasNext()) {
                    nodeNames.add(childItr.nextNode().getName());
                }
                logger.debug("Found child nodes {} at path {}. Scanning would be paused", nodeNames, cfg.getPauseScanNodePath());
            }
            return result;
        }
        return false;
    }

    long [] getCounters() {
        return counters;
    }

    /**
     * @see org.apache.sling.installer.api.UpdateHandler#handleRemoval(java.lang.String, java.lang.String, java.lang.String)
     */
    public UpdateResult handleRemoval(final String resourceType,
            final String id,
            final String url) {
        // get configuration
        final InstallerConfig cfg = this.getConfiguration();
        if ( cfg == null || !cfg.isWriteBack() ) {
            return null;
        }
        final int pos = url.indexOf(':');
        final String path = url.substring(pos + 1);

        // check path (SLING-2407)
        // 0. Check protocol
        if ( !url.startsWith(URL_SCHEME) ) {
            logger.debug("Not removing unmanaged artifact from repository: {}", url);
            return null;
        }
        // 1. Is this a system configuration then don't delete
        final String[] rootPaths = cfg.getFolderNameFilter().getRootPaths();
        final String systemConfigRootPath = rootPaths[rootPaths.length - 1];
        if ( path.startsWith(systemConfigRootPath) ) {
            logger.debug("Not removing system artifact from repository at {}", path);
            return null;
        }
        // 2. Is this configuration provisioned by us
        boolean found = false;
        int lastSlash = path.lastIndexOf('/');
        while (!found && lastSlash > 1) {
            final String prefix = path.substring(0, lastSlash);
            if ( cfg.getFolderNameFilter().getPriority(prefix) != -1 ) {
                found = true;
            } else {
                lastSlash = prefix.lastIndexOf('/');
            }
        }
        if ( found ) {
            // remove
            logger.debug("Removing artifact at {}", path);
            Session session = null;
            try {
                session = this.repository.loginAdministrative(null);
                if ( session.itemExists(path) ) {
                    session.getItem(path).remove();
                    session.save();
                }
            } catch (final RepositoryException re) {
                logger.error("Unable to remove resource from " + path, re);
                return null;
            } finally {
                if ( session != null ) {
                    session.logout();
                }
            }
            return new UpdateResult(url);
        }
        // not provisioned by us
        logger.debug("Not removing unmanaged artifact from repository at {}", path);
        return null;
    }

    /**
     * @see org.apache.sling.installer.api.UpdateHandler#handleUpdate(java.lang.String, java.lang.String, java.lang.String, java.util.Dictionary, Map)
     */
    public UpdateResult handleUpdate(final String resourceType,
            final String id,
            final String url,
            final Dictionary<String, Object> dict,
            final Map<String, Object> attributes) {
        return this.handleUpdate(resourceType, id, url, null, dict, attributes);
    }

    /**
     * @see org.apache.sling.installer.api.UpdateHandler#handleUpdate(java.lang.String, java.lang.String, java.lang.String, java.io.InputStream, Map)
     */
    public UpdateResult handleUpdate(final String resourceType,
            final String id,
            final String url,
            final InputStream is,
            final Map<String, Object> attributes) {
        return this.handleUpdate(resourceType, id, url, is, null, attributes);
    }

    private String getPathWithHighestPrio(final InstallerConfig cfg, final String oldPath) {
        final String path;
        // check root path, we use the path with highest prio
        final String rootPath = cfg.getFolderNameFilter().getRootPaths()[0] + '/';
        if ( !oldPath.startsWith(rootPath) ) {
            final int slashPos = oldPath.indexOf('/', 1);
            path = rootPath + oldPath.substring(slashPos + 1);
        } else {
            path = oldPath;
        }
        return path;
    }

    /**
     * Internal implementation of update handling
     */
    private UpdateResult handleUpdate(final String resourceType,
            final String id,
            final String url,
            final InputStream is,
            final Dictionary<String, Object> dict,
            final Map<String, Object> attributes) {
        // get configuration
        final InstallerConfig cfg = this.getConfiguration();
        if ( cfg == null || !cfg.isWriteBack() ) {
            return null;
        }

        // we only handle add/update of configs for now
        if ( !resourceType.equals(InstallableResource.TYPE_CONFIG) ) {
            return null;
        }

        Session session = null;
        try {
            session = this.repository.loginAdministrative(null);

            final String path;
            boolean resourceIsMoved = true;
            if ( url != null ) {
                // update
                final int pos = url.indexOf(':');
                final String oldPath = url.substring(pos + 1);

                // calculate the new node path
                final String nodePath;
                if ( url.startsWith(URL_SCHEME + ':') ) {
                    nodePath = getPathWithHighestPrio(cfg, oldPath);
                } else {
                    final int lastSlash = url.lastIndexOf('/');
                    final int lastPos = url.lastIndexOf('.');
                    final String name;
                    if ( lastSlash == -1 || lastPos < lastSlash ) {
                        name = id;
                    } else {
                        name = url.substring(lastSlash + 1, lastPos);
                    }
                    nodePath = getPathWithHighestPrio(cfg, cfg.getNewConfigPath() + name + ".config");
                }
                // ensure extension 'config'
                if ( !nodePath.endsWith(".config") ) {
                    if ( session.itemExists(nodePath) ) {
                        session.getItem(nodePath).remove();
                    }
                    path = nodePath + ".config";
                } else {
                    path = nodePath;
                }

                resourceIsMoved = nodePath.equals(oldPath);
                logger.debug("Update of {} at {}", resourceType, path);
            } else {
                // add
                final String name;
                if ( attributes != null && attributes.get(InstallableResource.RESOURCE_URI_HINT) != null ) {
                    name = (String)attributes.get(InstallableResource.RESOURCE_URI_HINT);
                } else {
                    name = id;
                }
                path = cfg.getNewConfigPath() + name + ".config";
                logger.debug("Add of {} at {}", resourceType, path);
            }

            // write to a byte array stream
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write("# Configuration created by Apache Sling JCR Installer\n".getBytes("UTF-8"));
            ConfigurationHandler.write(baos, dict);
            baos.close();

            // get or create file node
            JcrUtil.createPath(session, path, NT_FILE);
            // get or create resource node
            final Node dataNode = JcrUtil.createPath(session, path + "/jcr:content", NT_RESOURCE);

            dataNode.setProperty(PROP_DATA, new ByteArrayInputStream(baos.toByteArray()));
            dataNode.setProperty(PROP_MODIFIED, Calendar.getInstance());
            dataNode.setProperty(PROP_ENC, ENCODING);
            dataNode.setProperty(PROP_MIME, MIME_TXT);
            session.save();

            final UpdateResult result = new UpdateResult(JcrInstaller.URL_SCHEME + ':' + path);
            // priority
            final int lastSlash = path.lastIndexOf('/');
            final String parentPath = path.substring(0, lastSlash);
            result.setPriority(cfg.getFolderNameFilter().getPriority(parentPath));
            result.setResourceIsMoved(resourceIsMoved);
            return result;
        } catch (final RepositoryException re) {
            logger.error("Unable to add/update resource " + resourceType + ':' + id, re);
            return null;
        } catch (final IOException e) {
            logger.error("Unable to add/update resource " + resourceType + ':' + id, e);
            return null;
        } finally {
            if ( session != null ) {
                session.logout();
            }
        }
    }

}
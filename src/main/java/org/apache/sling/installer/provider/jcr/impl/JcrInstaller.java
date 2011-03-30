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
import java.util.Collection;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.UpdateHandler;
import org.apache.sling.installer.api.UpdateResult;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.settings.SlingSettingsService;
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
    @Property(name="service.description", value="Sling JCR Install Service"),
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name=UpdateHandler.PROPERTY_SCHEMES, value=JcrInstaller.URL_SCHEME),
    @Property(name="service.ranking", intValue=100)
})
@Service(value=UpdateHandler.class)
public class JcrInstaller implements EventListener, UpdateHandler {

	public static final long RUN_LOOP_DELAY_MSEC = 500L;
	public static final String URL_SCHEME = "jcrinstall";

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

    /** Default regexp for watched folders */
    public static final String DEFAULT_FOLDER_NAME_REGEXP = ".*/install$";

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

    private int maxWatchedFolderDepth;

    /** Filter for folder names */
    private FolderNameFilter folderNameFilter;

    /** List of watched folders */
    private List<WatchedFolder> watchedFolders;

    /** Session shared by all WatchedFolder */
    private Session session;

    /** The root folders that we watch */
    private String [] roots;

    private ComponentContext componentContext;

    private static final String DEFAULT_NEW_CONFIG_PATH = "/apps/sling/config";
    @Property(value=DEFAULT_NEW_CONFIG_PATH)
    private static final String PROP_NEW_CONFIG_PATH = "sling.jcrinstall.new.config.path";

    /** The path for new configurations. */
    private String newConfigPath;

    private static final boolean DEFAULT_ENABLE_WRITEBACK = true;
    @Property(boolValue=DEFAULT_ENABLE_WRITEBACK)
    private static final String PROP_ENABLE_WRITEBACK = "sling.jcrinstall.enable.writeback";

    /** Write back enabled? */
    private boolean writeBack;

    /** Convert Nodes to InstallableResources */
    static interface NodeConverter {
    	InstallableResource convertNode(Node n, int priority)
    	throws RepositoryException;
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
            logger.info("Background thread {} starting", Thread.currentThread().getName());
            try {
                // open session
                session = repository.loginAdministrative(repository.getDefaultWorkspace());

                for (String path : roots) {
                    listeners.add(new RootFolderListener(session, folderNameFilter, path, updateFoldersListTimer));
                    logger.debug("Configured root folder: {}", path);
                }

                // Watch for events on the root - that might be one of our root folders
                session.getWorkspace().getObservationManager().addEventListener(JcrInstaller.this,
                        Event.NODE_ADDED | Event.NODE_REMOVED,
                        "/",
                        false, // isDeep
                        null,
                        null,
                        true); // noLocal
                logger.debug("Watching for node events on / to detect removal/add of our root folders");


                // Find paths to watch and create WatchedFolders to manage them
                watchedFolders = new LinkedList<WatchedFolder>();
                for(String root : roots) {
                    findPathsToWatch(root, watchedFolders);
                }

                // Scan watchedFolders and register resources with installer
                final List<InstallableResource> resources = new LinkedList<InstallableResource>();
                for(WatchedFolder f : watchedFolders) {
                    final WatchedFolder.ScanResult r = f.scan();
                    logger.debug("Startup: {} provides resources {}", f, r.toAdd);
                    resources.addAll(r.toAdd);
                }

                logger.debug("Registering {} resources with OSGi installer: {}", resources.size(), resources);
                installer.registerResources(URL_SCHEME, resources.toArray(new InstallableResource[resources.size()]));
            } catch (final RepositoryException re) {
                logger.error("Repository exception during startup - deactivating installer!", re);
                active = false;
                final ComponentContext ctx = componentContext;
                if ( ctx  != null ) {
                    final String name = (String) componentContext.getProperties().get(
                            ComponentConstants.COMPONENT_NAME);
                    ctx.disableComponent(name);
                }
            }
            while (active) {
                runOneCycle();
            }
            logger.info("Background thread {} done", Thread.currentThread().getName());
            counters[RUN_LOOP_COUNTER] = -1;
        }
    };
    private StoppableThread backgroundThread;

    /**
     * Activate this component.
     */
    protected void activate(final ComponentContext context) {
        if (backgroundThread != null) {
            throw new IllegalStateException("Expected backgroundThread to be null in activate()");
        }
        this.componentContext = context;
        logger.info("Activating Apache Sling JCR Installer");

        this.newConfigPath = OsgiUtil.toString(context.getProperties().get(PROP_NEW_CONFIG_PATH), DEFAULT_NEW_CONFIG_PATH);
        if ( !newConfigPath.endsWith("/") ) {
            this.newConfigPath = this.newConfigPath.concat("/");
        }
        this.writeBack = OsgiUtil.toBoolean(context.getProperties().get(PROP_ENABLE_WRITEBACK), DEFAULT_ENABLE_WRITEBACK);

    	// Setup converters
    	converters.add(new FileNodeConverter());
    	converters.add(new ConfigNodeConverter());

    	// Configurable max depth, system property (via bundle context) overrides default value
    	final Object obj = getPropertyValue(context, PROP_INSTALL_FOLDER_MAX_DEPTH);
    	if (obj != null) {
    		// depending on where it's coming from, obj might be a string or integer
    		maxWatchedFolderDepth = Integer.valueOf(String.valueOf(obj)).intValue();
            logger.debug("Using configured ({}) folder name max depth '{}'", PROP_INSTALL_FOLDER_MAX_DEPTH, maxWatchedFolderDepth);
    	} else {
            maxWatchedFolderDepth = DEFAULT_FOLDER_MAX_DEPTH;
            logger.debug("Using default folder max depth {}, not provided by {}", maxWatchedFolderDepth, PROP_INSTALL_FOLDER_MAX_DEPTH);
    	}

    	// Configurable folder regexp, system property overrides default value
    	String folderNameRegexp = (String)getPropertyValue(context, FOLDER_NAME_REGEXP_PROPERTY);
    	if(folderNameRegexp != null) {
    		folderNameRegexp = folderNameRegexp.trim();
            logger.debug("Using configured ({}) folder name regexp '{}'", FOLDER_NAME_REGEXP_PROPERTY, folderNameRegexp);
    	} else {
    	    folderNameRegexp = DEFAULT_FOLDER_NAME_REGEXP;
            logger.debug("Using default folder name regexp '{}', not provided by {}", folderNameRegexp, FOLDER_NAME_REGEXP_PROPERTY);
    	}

    	// Setup folder filtering and watching
        folderNameFilter = new FolderNameFilter(OsgiUtil.toStringArray(context.getProperties().get(PROP_SEARCH_PATH), DEFAULT_SEARCH_PATH),
                folderNameRegexp, settings.getRunModes());
        roots = folderNameFilter.getRootPaths();
        backgroundThread = new StoppableThread();
        backgroundThread.start();
    }

    /**
     * Deactivate this component
     */
    protected void deactivate(final ComponentContext context) {
    	logger.info("Deactivating Apache Sling JCR Installer");

    	final long timeout = 30000L;
        backgroundThread.active = false;
        logger.debug("Waiting for " + backgroundThread.getName() + " Thread to end...");
        backgroundThread.interrupt();
    	try {
            backgroundThread.join(timeout);
    	} catch(InterruptedException iex) {
    	    // ignore this as we want to shutdown
    	}
        backgroundThread = null;

        folderNameFilter = null;
        watchedFolders = null;
        converters.clear();
        try {
            if (session != null) {
                for(RootFolderListener wfc : listeners) {
                    wfc.cleanup(session);
                }
                session.getWorkspace().getObservationManager().removeEventListener(this);
            }
        } catch (final RepositoryException e) {
            logger.warn("Exception in deactivate()", e);
        }
        if ( session != null ) {
            session.logout();
            session = null;
        }
        listeners.clear();
        this.componentContext = null;
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
                logger.info("Bundles root node {} not found, ignored", rootPath);
            } else {
                logger.debug("Bundles root node {} found, looking for bundle folders inside it", rootPath);
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
            logger.debug("Not recursing into {} due to maxWatchedFolderDepth={}", path, maxWatchedFolderDepth);
            return;
        }
        final NodeIterator it = n.getNodes();
        while (it.hasNext()) {
            findPathsUnderNode(it.nextNode(), result);
        }
    }

    /** Add WatchedFolder to our list if it doesn't exist yet */
    private void addWatchedFolder(final WatchedFolder toAdd) {
        WatchedFolder existing = null;
        for(WatchedFolder wf : watchedFolders) {
            if (wf.getPath().equals(toAdd.getPath())) {
                existing = wf;
                break;
            }
        }
        if (existing == null) {
            watchedFolders.add(toAdd);
            toAdd.scheduleScan();
        } else {
            toAdd.cleanup();
        }
    }

    /** Add new folders to watch if any have been detected
     *  @return a list of InstallableResource that must be unregistered,
     *  	for folders that have been removed
     */
    private List<String> updateFoldersList() throws Exception {
        logger.debug("Updating folder list.");

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
            logger.debug("Item {} exists? {}", wf.getPath(), session.itemExists(wf.getPath()));

            if(!session.itemExists(wf.getPath())) {
                result.addAll(wf.scan().toRemove);
                wf.cleanup();
                toRemove.add(wf);
            }
        }
        for(WatchedFolder wf : toRemove) {
            logger.info("Deleting {}, path does not exist anymore", wf);
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
                logger.debug("Got event {}", e);

                for(String root : roots) {
                    if (e.getPath().startsWith(root)) {
                        logger.info("Got event for root {}, scheduling scanning of new folders", root);
                        updateFoldersListTimer.scheduleScan();
                    }
                }
            }
        } catch(RepositoryException re) {
            logger.warn("RepositoryException in onEvent", re);
        }
    }

    /** Run periodic scans of our watched folders, and watch for folders creations/deletions */
    public void runOneCycle() {
        logger.debug("Running watch cycle.");

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
                    logger.info("Registering resource with OSGi installer: {}",sr.toAdd);
                    logger.info("Removing resource from OSGi installer: {}", sr.toRemove);
                    installer.updateResources(URL_SCHEME, sr.toAdd.toArray(new InstallableResource[sr.toAdd.size()]),
                            sr.toRemove.toArray(new String[sr.toRemove.size()]));
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
                logger.info("Removing resource from OSGi installer (folder deleted): {}", toRemove);
                installer.updateResources(URL_SCHEME, null,
                        toRemove.toArray(new String[toRemove.size()]));
            }

            try {
                Thread.sleep(RUN_LOOP_DELAY_MSEC);
            } catch(InterruptedException ignore) {
            }

        } catch(Exception e) {
            logger.warn("Exception in run()", e);
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

    /**
     * @see org.apache.sling.installer.api.UpdateHandler#handleRemoval(java.lang.String, java.lang.String, java.lang.String)
     */
    public UpdateResult handleRemoval(final String resourceType,
            final String id,
            final String url) {
        if ( !this.writeBack ) {
            return null;
        }
        final int pos = url.indexOf(':');
        final String path = url.substring(pos + 1);
        // remove
        logger.debug("Removal of {}", path);
        Session session = null;
        try {
            session = this.repository.loginAdministrative(null);
            if ( session.nodeExists(path) ) {
                session.getNode(path).remove();
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

    private String getPathWithHighestPrio(final String oldPath) {
        final String path;
        // check root path, we use the path with highest prio
        final String rootPath = this.folderNameFilter.getRootPaths()[0] + '/';
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
        if ( !this.writeBack ) {
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
                final String nodePath = getPathWithHighestPrio(oldPath);
                // ensure extension 'config'
                if ( !nodePath.endsWith(".config") && session.itemExists(nodePath) ) {
                    session.getItem(nodePath).remove();
                    path = nodePath + ".config";
                } else {
                    path = nodePath;
                }
                resourceIsMoved = nodePath.equals(oldPath);
                logger.debug("Update of {} at {}", resourceType, path);
            } else {
                // check for path hint
                String hint = null;
                if ( attributes != null ) {
                    hint = (String)attributes.get(InstallableResource.INSTALLATION_HINT);
                    if ( hint != null && hint.startsWith(URL_SCHEME + ':')) {
                        hint = hint.substring(URL_SCHEME.length() + 1);
                        final int lastSlash = hint.lastIndexOf('/');
                        if ( lastSlash < 1 ) {
                            hint = null;
                        } else {
                            int slashPos = hint.lastIndexOf('/', lastSlash - 1);
                            final String dirName = hint.substring(slashPos + 1, lastSlash);
                            // we prefer having "config" as the dir name
                            if ( !"config".equals(dirName) ) {
                                hint = this.getPathWithHighestPrio(hint.substring(0, slashPos + 1) + "config");
                                // check if config is in regexp
                                if ( this.folderNameFilter.getPriority(hint) > -1 ) {
                                    hint = hint + '/';
                                } else {
                                    hint = this.getPathWithHighestPrio(hint);
                                }
                            } else {
                                hint = this.getPathWithHighestPrio(hint);
                            }
                        }
                    }
                }
                // add
                path = (hint != null ? hint : this.newConfigPath) + id + ".config";
                logger.debug("Add of {} at {}", resourceType, path);
            }

            // write to a byte array stream
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
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
            result.setPriority(this.folderNameFilter.getPriority(path));
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
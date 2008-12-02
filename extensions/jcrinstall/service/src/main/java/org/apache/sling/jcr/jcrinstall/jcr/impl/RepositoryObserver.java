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
package org.apache.sling.jcr.jcrinstall.jcr.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.jcrinstall.jcr.NodeConverter;
import org.apache.sling.jcr.jcrinstall.osgi.OsgiController;
import org.apache.sling.jcr.jcrinstall.osgi.ResourceOverrideRules;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.startlevel.StartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Observes a set of folders in the JCR repository, to
 *  detect added or updated resources that might be of
 *  interest to the OsgiController.
 *  
 *  Calls the OsgiController to install/remove resources.
 *   
 * @scr.component 
 *  label="%jcrinstall.name" 
 *  description="%jcrinstall.description"
 *  immediate="true"
 *  @scr.service
 *  @scr.property 
 *      name="service.description" 
 *      value="Sling jcrinstall OsgiController Service"
 *  @scr.property 
 *      name="service.vendor" 
 *      value="The Apache Software Foundation"
 * @scr.property name="sling.servlet.paths" value="/system/sling/jcrinstall"
 */
@SuppressWarnings("serial")
public class RepositoryObserver extends SlingAllMethodsServlet implements Runnable, FrameworkListener {

    private final SortedSet<WatchedFolder> folders = new TreeSet<WatchedFolder>();
    private RegexpFilter folderNameFilter;
    private RegexpFilter filenameFilter;
    private ResourceOverrideRules roRules;
    private ComponentContext componentContext;
    private final PropertiesUtil propertiesUtil = new PropertiesUtil();
    private boolean observationCycleActive;
    private boolean activated;
    
    public static final String POST_ENABLE_PARAM = "enabled";
    
    /** @scr.reference */
    protected OsgiController osgiController;
    
    /** @scr.reference cardinality="0..1" policy="dynamic" */
    protected SlingRepository repository;
    
    /** @scr.reference */
    protected StartLevel startLevel;
    
    /** @scr.property type="Integer" valueRef="DEFAULT_BUNDLES_START_LEVEL" */
    private static final String BUNDLES_START_LEVEL__PROPERTY = "bundles.startlevel";
    private static final int DEFAULT_BUNDLES_START_LEVEL = 30;
    private int installedBundlesStartLevel = DEFAULT_BUNDLES_START_LEVEL;

    private Session session;
    private File serviceDataFile;
    private int startLevelToSetAtStartup;
    
    private final List<NodeConverter> converters = new ArrayList<NodeConverter>();
    
    private final List<WatchedFolderCreationListener> listeners = new LinkedList<WatchedFolderCreationListener>();
    
    /** Default set of root folders to watch */
    public static String[] DEFAULT_ROOTS = {"/libs", "/apps"};
    
    /** Default regexp for watched folders */
    public static final String DEFAULT_FOLDER_NAME_REGEXP = ".*/install$";
    
    /** ComponentContext property that overrides the folder name regepx */
    public static final String FOLDER_NAME_REGEXP_PROPERTY = "sling.jcrinstall.folder.name.regexp";
    
    public static final String DATA_FILE = "service.properties";
    public static final String DATA_LAST_FOLDER_REGEXP = "folder.regexp";
    
    /** Scan delay for watched folders */
    protected long scanDelayMsec = 1000L;
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /** Upon activation, find folders to watch under our roots, and observe those
     *  roots to detect new folders to watch.
     */
    protected void activate(ComponentContext context) throws Exception {
        componentContext = context;

        // Context can be null in automated tests, to make them simpler
        if(context != null) {
            context.getBundleContext().addFrameworkListener(this);
        }
        
        // Read config
        if(context != null) {
            final Object prop = context.getProperties().get(BUNDLES_START_LEVEL__PROPERTY);
            if(prop != null && prop instanceof Number) {
                installedBundlesStartLevel = ((Number)prop).intValue();
                log.info("Start level for installed bundles set to {} by {} property", installedBundlesStartLevel, BUNDLES_START_LEVEL__PROPERTY);
            } else {
                installedBundlesStartLevel = DEFAULT_BUNDLES_START_LEVEL;
                log.info("Start level for installed bundles set to default value {}", installedBundlesStartLevel);
            }
        }

        // Check start levels
        if(context != null) {
            final int myLevel = startLevel.getBundleStartLevel(context.getBundleContext().getBundle());
            if(installedBundlesStartLevel < myLevel) {
                // Running at a lower start level than the bundles that we install
                // allows us to stop them if the repository goes away (SLING-747)
                log.warn(
                        "The configured start level for bundles installed by jcrinstall ({})"
                        + " should be higher than the jcrinstall start level ({})",
                        installedBundlesStartLevel, myLevel
                );
            }
        }
        
        // Call startup() if we already have a repository, else that will be called
        // by the bind method
        if(repository != null) {
            log.debug("activate()");
            startup();
        } else {
            log.debug("activate() - Repository not available, cannot install bundles yet");
        }
        
        activated = true;
    }
    	
    /** Called at activation time, or when repository becomes available again
     *  after going away. */
    protected void startup() throws Exception {
        log.debug("startup()");
        
    	// TODO make this more configurable (in sync with ResourceOverrideRulesImpl)
    	final String [] roots = DEFAULT_ROOTS;
        final String [] main = { "/libs/" };
        final String [] override = { "/apps/" };
    	
        roRules = new ResourceOverrideRulesImpl(main, override);
    	osgiController.setResourceOverrideRules(roRules);

    	/** NodeConverters setup
         *	Using services and a whiteboard pattern for these would be nice,
         * 	but that could be problematic at startup due to async loading
         */
    	converters.add(new FileNodeConverter(installedBundlesStartLevel));
    	converters.add(new ConfigNodeConverter());
    	
    	String folderNameRegexp = getPropertyValue(componentContext, FOLDER_NAME_REGEXP_PROPERTY);
    	if(folderNameRegexp == null) {
    	    folderNameRegexp = DEFAULT_FOLDER_NAME_REGEXP;
            log.info("Using default folder name regexp '{}'", DEFAULT_FOLDER_NAME_REGEXP);
    	} else {
            log.info("Using folder name regexp '{}' from context property '{}'", folderNameRegexp, FOLDER_NAME_REGEXP_PROPERTY);
    	}
        folderNameFilter = new RegexpFilter(folderNameRegexp);
        serviceDataFile = getServiceDataFile(componentContext);
        
        // Listen for any new WatchedFolders created after activation
        session = repository.loginAdministrative(repository.getDefaultWorkspace());
        final int eventTypes = Event.NODE_ADDED;
        final boolean isDeep = true;
        final boolean noLocal = true;
        for (String path : roots) {
            final WatchedFolderCreationListener w = new WatchedFolderCreationListener(folderNameFilter);
            listeners.add(w);
            session.getWorkspace().getObservationManager().addEventListener(w, eventTypes, path,
                    isDeep, null, null, noLocal);
        }
        
        // Find folders to watch
        for(String root : roots) {
            folders.addAll(findWatchedFolders(root));
        }
        
        // Handle initial uninstalls and installs
        observationCycleActive = true;
        final int myStartLevel = startLevel.getStartLevel();
        handleInitialUninstalls();
        runOneCycle();
        
        // Restore start level if we brought it down due to the repository going away
        if(startLevelToSetAtStartup > 0 && startLevelToSetAtStartup != myStartLevel) {
            log.info("startup(): resetting start level to {}", startLevelToSetAtStartup);
            startLevel.setStartLevel(startLevelToSetAtStartup);
            startLevelToSetAtStartup = 0;
        }
        
        // start queue processing
        final Thread t = new Thread(this, getClass().getSimpleName() + "_" + System.currentTimeMillis());
        t.setDaemon(true);
        t.start();
    }
    
    protected File getServiceDataFile(ComponentContext context) {
        return context.getBundleContext().getDataFile(DATA_FILE);
    }
    
    /** Get a property value from the component context or bundle context */
    protected String getPropertyValue(ComponentContext ctx, String name) {
        String result = (String)ctx.getProperties().get(name);
        if(result == null) {
            result = ctx.getBundleContext().getProperty(name);
        }
        return result;
    }
    
    protected void deactivate(ComponentContext context) {
        log.debug("deactivate()");
        shutdown();
        if(context != null) {
            context.getBundleContext().removeFrameworkListener(this);
        }
        activated = false;
        componentContext = null;
        startLevelToSetAtStartup = 0;
    }
    
    /** Called at deactivation time, or when repository stops being available */
    protected void shutdown() {
        log.debug("shutdown()");
        
        observationCycleActive = false;
        
    	for(WatchedFolder f : folders) {
    		f.cleanup();
    	}
    	
    	if(session != null) {
	    	for(WatchedFolderCreationListener w : listeners) {
	    		try {
		        	session.getWorkspace().getObservationManager().removeEventListener(w);
	    		} catch(RepositoryException re) {
	    			log.warn("RepositoryException in removeEventListener call", re);
	    		}
	    	}
	    	
	    	session.logout();
	    	session = null;
    	}
    	
    	listeners.clear();
        folders.clear();
        
        if(componentContext != null) {
            final int currentStartLevel = startLevel.getStartLevel();
            final int myStartLevel = startLevel.getBundleStartLevel(componentContext.getBundleContext().getBundle());
            if(currentStartLevel > myStartLevel) {
                log.info("shutdown(): changing start level from {} to the jcrinstall start level of {}", currentStartLevel, myStartLevel);
                startLevel.setStartLevel(myStartLevel);
            }
        }
    }
    
    /** Add WatchedFolders that have been discovered by our WatchedFolderCreationListeners, if any */
    void addNewWatchedFolders() throws RepositoryException {
    	for(WatchedFolderCreationListener w : listeners) {
    		final Set<String> paths = w.getAndClearPaths();
    		if(paths != null) {
    			for(String path : paths) {
    				folders.add(new WatchedFolder(repository, path, osgiController, filenameFilter, scanDelayMsec, roRules));
    			}
    		}
    	}
    }

    /** Find all folders to watch under rootPath 
     * @throws RepositoryException */
    Set<WatchedFolder> findWatchedFolders(String rootPath) throws RepositoryException {
        final Set<WatchedFolder> result = new HashSet<WatchedFolder>();
        Session s = null;

        try {
            s = repository.loginAdministrative(repository.getDefaultWorkspace());
            if (!s.getRootNode().hasNode(relPath(rootPath))) {
                log.info("Bundles root node {} not found, ignored", rootPath);
            } else {
                log.debug("Bundles root node {} found, looking for bundle folders inside it", rootPath);
                final Node n = s.getRootNode().getNode(relPath(rootPath));
                findWatchedFolders(n, result);
            }
        } finally {
            if (s != null) {
                s.logout();
            }
        }
        
        return result;
    }
    
    /**
     * Add n to setToUpdate if it is a bundle folder, and recurse into its children
     * to do the same.
     */
    void findWatchedFolders(Node n, Set<WatchedFolder> setToUpdate) throws RepositoryException 
    {
        if (folderNameFilter.accept(n.getPath())) {
            setToUpdate.add(new WatchedFolder(repository, n.getPath(), osgiController, filenameFilter, scanDelayMsec, roRules));
        }
        final NodeIterator it = n.getNodes();
        while (it.hasNext()) {
            findWatchedFolders(it.nextNode(), setToUpdate);
        }
    }
    
    /**
     * Return the relative path for supplied path
     */
    static String relPath(String path) {
        if (path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }
    
    /** Uninstall resources as needed when starting up */ 
    void handleInitialUninstalls() throws Exception {
        // If regexp has changed, uninstall resources left in folders 
        // that don't match the new regexp
        // TODO this happens right after activate() is called on this service,
        // might conflict with ongoing SCR activities?
        final Properties props = propertiesUtil.loadProperties(serviceDataFile);
        final String oldRegexp = props.getProperty(DATA_LAST_FOLDER_REGEXP);
        if(oldRegexp != null && !oldRegexp.equals(folderNameFilter.getRegexp())) {
            log.info("Folder name regexp has changed, uninstalling non-applicable resources ( {} -> {} )", 
                    oldRegexp, folderNameFilter.getRegexp());
            for(String uri : osgiController.getInstalledUris()) {
                try {
                    if(session.itemExists(uri)) {
                        final Item i = session.getItem(uri);
                        if(i.isNode()) {
                            final Node n = (Node)i;
                            final Node parent = n.getParent();
                            if(!folderNameFilter.accept(parent.getPath())) {
                                log.info("Uninstalling resource {}, folder name not accepted by current filter", uri);
                                osgiController.scheduleUninstall(uri);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Exception during 'uninstall all'", e);
                }
            }
        }
        props.setProperty(DATA_LAST_FOLDER_REGEXP, folderNameFilter.getRegexp());
        propertiesUtil.saveProperties(props, serviceDataFile);
        
        // Check if any deletions happened while this
        // service was inactive: create a fake WatchFolder 
        // on / and use it to check for any deletions, even 
        // if the corresponding WatchFolders are gone
        try {
            final WatchedFolder rootWf = new WatchedFolder(repository, "/", osgiController, filenameFilter, 0L, null);
            rootWf.checkDeletions(osgiController.getInstalledUris());
        } catch(Exception e) {
            log.warn("Exception in root WatchFolder.checkDeletions call", e);
        }
        
        // Let the OSGi controller execute the uninstalls
        osgiController.executeScheduledOperations();
    }
    
    /**
     * Scan WatchFolders once their timer expires
     */
    public void run() {
        log.info("{} thread {} starts", getClass().getSimpleName(), Thread.currentThread().getName());
        
        // We could use the scheduler service but that makes things harder to test
        boolean scanning = false;
        boolean oldScanning = !scanning;
        
        while (observationCycleActive) {
            try {
                final int currentLevel = startLevel.getStartLevel(); 
                scanning = currentLevel >= installedBundlesStartLevel;
                if(scanning != oldScanning) {
                    if(scanning) {
                        log.info("Scanning enabled, current start level ({}) equals or above {} (start level of installed bundles)", 
                                currentLevel, installedBundlesStartLevel);
                    } else {
                        log.info("Scanning disabled, current start level ({}) is below {} (start level of installed bundles)", 
                                currentLevel, installedBundlesStartLevel);
                    }
                    oldScanning = scanning;
                }
                
                if(scanning) {
                    runOneCycle();
                }
            } catch (IllegalArgumentException ie) {
                log.warn("IllegalArgumentException  in " + getClass().getSimpleName(), ie);
            } catch (RepositoryException re) {
                log.warn("RepositoryException in " + getClass().getSimpleName(), re);
            } catch (Exception e) {
                log.error("Unhandled Exception in runOneCycle()", e);
            } finally {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ignore) {
                    // ignore
                }
            }
        }

        log.info("{} thread {} ends", getClass().getSimpleName(), Thread.currentThread().getName());
    }
    
    /** Let our WatchedFolders run their scanning cycles */ 
    void runOneCycle() throws Exception {
    	
    	// Add any new watched folders, and scan those who need it 
        addNewWatchedFolders();
    	for(WatchedFolder wf : folders) {
    	    if(!observationCycleActive) {
    	        break;
    	    }
    		wf.scanIfNeeded(converters);
        }
    	
    	// And then let the OsgiController install/remove
    	// resources that we detected
    	osgiController.executeScheduledOperations();
    }

    /** Called when a SlingRepository becomes available, either at activation time
     *  or later, if the repository had disappeared since activated.
     */
    protected void bindSlingRepository(SlingRepository r) {
        repository = r;
        if(activated) {
            log.debug("bindSlingRepository()");
            try {
                startup();
            } catch(Exception e) {
                log.error("Exception in bindSlingRepository/startup", e);
            }
        } else {
            log.debug("bindSlingRepository() but not activated yet, startup() not called");
        }
    }

    /** Called when a SlingRepository becomes unavailable, either at deactivation time
     *  or because the repository became unavailable. 
     */
    protected void unbindSlingRepository(SlingRepository r) {
        // Store current start level: shutdown() will bring it down and we want
        // to go back to it if repository comes back
        startLevelToSetAtStartup = startLevel.getStartLevel();
        log.debug("unbindSlingRepository() called at start level {}", startLevelToSetAtStartup);
        shutdown();
        repository = null;
    }

    public void frameworkEvent(FrameworkEvent e) {
        if(e.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
            log.info("FrameworkEvent.STARTLEVEL_CHANGED, start level={}", startLevel.getStartLevel());
        }
    }  
    
    protected Set<WatchedFolder> getWatchedFolders() {
        return folders;
    }

    /** A POST can be used to deactivate/reactivate this, simulating a disappearing SlingRepository.
     *  Used for integration testing.
     */
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) 
    throws ServletException, IOException {
        final String enable = request.getParameter(POST_ENABLE_PARAM);
        if(enable != null) {
            if(Boolean.parseBoolean(enable)) {
                log.info("Processing POST with {}=true, attempting to bind SlingRepository", POST_ENABLE_PARAM);
                if(repository != null) {
                    response.sendError(HttpServletResponse.SC_CONFLICT, "Already enabled");
                    return;
                }
                if(componentContext == null) {
                    response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, "No ComponentContext, cannot enable service");
                    return;
                }
                final ServiceReference ref = componentContext.getBundleContext().getServiceReference(SlingRepository.class.getName());
                if(ref == null) {
                    response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, "No SlingRepository ServiceReference available");
                    return;
                }
                
                final SlingRepository r = (SlingRepository)componentContext.getBundleContext().getService(ref);
                bindSlingRepository(r);
                
            } else {
                log.info("Processing POST with {}=false, attempting to unbind SlingRepository", POST_ENABLE_PARAM);
                if(repository == null) {
                    response.sendError(HttpServletResponse.SC_CONFLICT, "Not currently enabled, cannot disable");
                    return;
                }
                unbindSlingRepository(repository);
            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Use '" + POST_ENABLE_PARAM + "' parameter to enable/disable the RepositoryObserver");
            return;
        }
        
        doGet(request, response);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
    throws ServletException, IOException 
    {
        final String status = repository != null ? "enabled" : "disabled";
        response.setContentType("text/plain");
        response.getWriter().write(getClass().getSimpleName() + " is " + status);
    }
}
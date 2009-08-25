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
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.apache.sling.runmode.RunMode;
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
 *  @scr.service
 *  @scr.property
 *      name="service.description"
 *      value="Sling Jcrinstall Service"
 *  @scr.property
 *      name="service.vendor"
 *      value="The Apache Software Foundation"
 */
public class JcrInstaller implements Runnable {
	private static final long serialVersionUID = 1L;
	public static final String URL_SCHEME = "jcrinstall";

	private final Logger log = LoggerFactory.getLogger(getClass());
    
    /**	This class watches the repository for installable resources  
     * @scr.reference 
     */
    private SlingRepository repository;
    
    /** Additional installation folders are activated based
     *  on the current RunMode. For example, /libs/foo/install.dev
     *  if the current run mode is "dev".
     *  @scr.reference
     */
    private RunMode runMode;

    /**	The OsgiInstaller installs resources in the OSGi framework. 
     * 	@scr.reference 
     */
    private OsgiInstaller installer;
    
    /**	This class looks for installable resources under the search
     * 	paths of the Sling ResourceResolver (by default: /libs and /apps)
     * 	@scr.reference
     */
    private JcrResourceResolverFactory resourceResolverFactory;
    
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

    public static final int DEFAULT_FOLDER_MAX_DEPTH = 4;
    private int maxWatchedFolderDepth;

    /** Filter for folder names */
    private FolderNameFilter folderNameFilter;
    
    /** List of watched folders */
    private List<WatchedFolder> watchedFolders;
    
    /** Session shared by all WatchedFolder */
    private Session session;
    
    /** Count cycles of our run() method, used in testing */
    private int cyclesCount;
    
    /** Used to stop background thread when deactivated */
    private int deactivationCounter = 1;
    
    /** Convert Nodes to InstallableResources */
    static interface NodeConverter {
    	InstallableResource convertNode(String urlScheme, Node n) throws Exception;
    }
    
    /** Our NodeConverters*/
    private final Collection <NodeConverter> converters = new ArrayList<NodeConverter>();
    
    /** Detect newly created folders that we must watch */
    private final List<WatchedFolderCreationListener> listeners = new LinkedList<WatchedFolderCreationListener>();
    
    protected void activate(ComponentContext context) throws Exception {
    	
    	session = repository.loginAdministrative(repository.getDefaultWorkspace());
    	
    	// Setup converters
    	converters.add(new FileNodeConverter());
    	converters.add(new ConfigNodeConverter());
    	
    	// Get root paths for installable resources, from ResourceResolver
    	final ResourceResolver rr = resourceResolverFactory.getResourceResolver(session);
    	final String [] roots = rr.getSearchPath();
    	for(int i=0; i < roots.length; i++) {
    		if(!roots[i].startsWith("/")) {
    			roots[i] = "/" + roots[i];
     		}
    		log.info("Using root folder {} (provided by ResourceResolver)", roots[i]);
    	}
    	
    	// Configurable max depth, system property (via bundle context) overrides default value
    	Object obj = getPropertyValue(context, PROP_INSTALL_FOLDER_MAX_DEPTH);
    	if(obj != null) {
    		// depending on where it's coming from, obj might be a string or integer
    		maxWatchedFolderDepth = Integer.valueOf(String.valueOf(obj)).intValue();
            log.info("Using configured ({}) folder name max depth '{}'", PROP_INSTALL_FOLDER_MAX_DEPTH, maxWatchedFolderDepth);
    	} else {
            maxWatchedFolderDepth = DEFAULT_FOLDER_MAX_DEPTH;
            log.info("Using default folder max depth {}, not provided by {}", maxWatchedFolderDepth, PROP_INSTALL_FOLDER_MAX_DEPTH);
    	}
        
    	// Configurable folder regexp, system property overrides default value
    	String folderNameRegexp = (String)getPropertyValue(context, FOLDER_NAME_REGEXP_PROPERTY);
    	if(folderNameRegexp != null) {
            log.info("Using configured ({}) folder name regexp {}", FOLDER_NAME_REGEXP_PROPERTY, folderNameRegexp);
    	} else {
    	    folderNameRegexp = DEFAULT_FOLDER_NAME_REGEXP;
            log.info("Using default folder name regexp '{}', not provided by {}", folderNameRegexp, FOLDER_NAME_REGEXP_PROPERTY);
    	}
    	
    	// Setup folder filtering and watching
        folderNameFilter = new FolderNameFilter(roots, folderNameRegexp, runMode);
        for (String path : roots) {
            listeners.add(new WatchedFolderCreationListener(session, folderNameFilter, path));
        }
        
    	// Find paths to watch and create WatchedFolders to manage them
    	watchedFolders = new LinkedList<WatchedFolder>();
    	for(String root : roots) {
    		findPathsToWatch(root, watchedFolders);
    	}
    	
    	// Scan watchedFolders and register resources with installer
    	final List<InstallableResource> resources = new LinkedList<InstallableResource>();
    	for(WatchedFolder f : watchedFolders) {
    		final Collection<InstallableResource> c = f.scan();
    		log.debug("Startup: {} provides resources {}", f, c);
    		resources.addAll(c);
    	}
    	
    	log.info("Registering {} resources with OSGi installer", resources.size());
    	installer.registerResources(resources, URL_SCHEME);
    	
    	final Thread t = new Thread(this, getClass().getSimpleName() + "." + deactivationCounter);
    	t.setDaemon(true);
    	t.start();
    }
    
    protected void deactivate(ComponentContext context) {
        try {
            deactivationCounter++;
            listeners.clear();
            folderNameFilter = null;
            watchedFolders = null;
            converters.clear();
            if(session != null) {
                for(WatchedFolderCreationListener wfc : listeners) {
                    wfc.cleanup(session);
                }
                session.logout();
                session = null;
            }
        } catch(Exception e) {
            log.warn("Exception in deactivate()", e);
        }
    }
    
    /** Get a property value from the component context or bundle context */
    protected Object getPropertyValue(ComponentContext ctx, String name) {
        Object result = ctx.getProperties().get(name);
        if(result == null) {
            result = ctx.getBundleContext().getProperty(name);
        }
        return result;
    }

    /** Find the paths to watch under rootPath, according to our folderNameFilter,
     * 	and add them to result */
    void findPathsToWatch(String rootPath, List<WatchedFolder> result) throws RepositoryException {
        Session s = null;

        try {
            s = repository.loginAdministrative(repository.getDefaultWorkspace());
            if (!s.getRootNode().hasNode(relPath(rootPath))) {
                log.info("Bundles root node {} not found, ignored", rootPath);
            } else {
                log.debug("Bundles root node {} found, looking for bundle folders inside it", rootPath);
                final Node n = s.getRootNode().getNode(relPath(rootPath));
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
    void findPathsUnderNode(Node n, List<WatchedFolder> result) throws RepositoryException
    {
        final String path = n.getPath();
        final int priority = folderNameFilter.getPriority(path); 
        if (priority > 0) {
            result.add(new WatchedFolder(session, path, priority, URL_SCHEME, converters));
        }
        final int depth = path.split("/").length;
        if(depth > maxWatchedFolderDepth) {
            log.debug("Not recursing into {} due to maxWatchedFolderDepth={}", path, maxWatchedFolderDepth);
            return;
        } else {
            final NodeIterator it = n.getNodes();
            while (it.hasNext()) {
                findPathsUnderNode(it.nextNode(), result);
            }
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
    
    /** Add new folders to watch if any have been detected
     *  @return true if any WatchedFolders have been removed 
     */ 
    private boolean addAndDeleteFolders() throws RepositoryException {
        for(WatchedFolderCreationListener wfc : listeners) {
            final Set<String> newPaths = wfc.getAndClearPaths();
            if(newPaths != null && newPaths.size() > 0) {
                log.info("Detected {} new folder(s to watch", newPaths.size());
                for(String path : newPaths) {
                    watchedFolders.add(
                            new WatchedFolder(session, path, folderNameFilter.getPriority(path), URL_SCHEME, converters));
                }
            }
        }
        
        boolean deleted = false;
        final List<WatchedFolder> toRemove = new ArrayList<WatchedFolder>();
        for(WatchedFolder wf : watchedFolders) {
            if(!session.itemExists(wf.getPath())) {
                deleted = true;
                log.info("Deleting {}, path does not exist anymore", wf);
                wf.cleanup();
                toRemove.add(wf);
            }
        }
        for(WatchedFolder wf : toRemove) {
            watchedFolders.remove(wf);
        }
        
        return deleted;
    }
    
    /** Run periodic scans of our watched folders, and watch for folders creations/deletions */
    public void run() {
        log.info("Background thread {} starting", Thread.currentThread().getName());
        final int savedCounter = deactivationCounter;
        while(savedCounter == deactivationCounter) {
            try {
                // TODO rendezvous with installer if any folder has been deleted
                addAndDeleteFolders();
                cyclesCount++;

                // TODO wait for events from our listeners, and/or WatchedFolder scan time
                try {
                    Thread.sleep(500L);
                } catch(InterruptedException ignore) {
                }
                
            } catch(Exception e) {
                log.warn("Exception in run()", e);
                try {
                    Thread.sleep(1000L);
                } catch(InterruptedException ignore) {
                }
            }
        }
        log.info("Background thread {} stopping", Thread.currentThread().getName());
    }

}
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
package org.apache.sling.osgi.installer.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.apache.sling.osgi.installer.impl.config.ConfigTaskCreator;
import org.apache.sling.osgi.installer.impl.tasks.BundleTaskCreator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/** Worker thread where all OSGi tasks are executed.
 *  Runs cycles where the list of RegisteredResources is examined,
 *  OsgiTasks are created accordingly and executed.
 *
 *  A separate list of RegisteredResources is kept for resources
 *  that are updated or removed during a cycle, and merged with
 *  the main list at the end of the cycle.
 */
public class OsgiInstallerThread
    extends Thread
    implements BundleListener, FrameworkListener,
               OsgiInstaller, OsgiInstallerContext {

    private final BundleContext ctx;
    private final List<RegisteredResource> newResources = new LinkedList<RegisteredResource>();
    private final SortedSet<OsgiInstallerTask> tasks = new TreeSet<OsgiInstallerTask>();
    private final SortedSet<OsgiInstallerTask> tasksForNextCycle = new TreeSet<OsgiInstallerTask>();
    private final List<SortedSet<RegisteredResource>> newResourcesSets = new ArrayList<SortedSet<RegisteredResource>>();
    private final Set<String> newResourcesSchemes = new HashSet<String>();
    private final Set<String> urlsToRemove = new HashSet<String>();
    private boolean active = true;
    private boolean retriesScheduled;

    /** Group our RegisteredResource by OSGi entity */
    private final HashMap<String, SortedSet<RegisteredResource>> registeredResources;
    private final PersistentResourceList persistentList;

    private final BundleTaskCreator bundleTaskCreator;
    private final ConfigTaskCreator configTaskCreator;

    OsgiInstallerThread(final BundleContext ctx) {
        this.ctx = ctx;
        // listen to framework and bundle events
        this.ctx.addFrameworkListener(this);
        this.ctx.addBundleListener(this);
        this.configTaskCreator = new ConfigTaskCreator(ctx);
        this.bundleTaskCreator = new BundleTaskCreator(ctx);
        setName(getClass().getSimpleName());
        final File f = ctx.getDataFile("RegisteredResourceList.ser");
        persistentList = new PersistentResourceList(f);
        registeredResources = persistentList.getData();
    }

    void deactivate() {
        this.active = false;
        this.configTaskCreator.deactivate();
        this.bundleTaskCreator.deactivate();
        ctx.removeBundleListener(this);
        ctx.removeFrameworkListener(this);
        synchronized (newResources) {
            newResources.notify();
        }
    }

    @Override
    public void run() {
        while (active) {
            try {
            	mergeNewResources();
            	computeTasks();

            	if(tasks.isEmpty() && !retriesScheduled) {
            	    // No tasks to execute - wait until new resources are
            	    // registered
            	    cleanupInstallableResources();
            	    Logger.logDebug("No tasks to process, going idle");

            	    synchronized (newResources) {
            	        try {
            	            newResources.wait();
            	        } catch (InterruptedException ignore) {}
                    }
            	    Logger.logDebug("Notified of new resources, back to work");
            	    continue;
            	}

            	retriesScheduled = false;
                if(executeTasks() > 0) {
                    Logger.logDebug("Tasks have been executed, saving persistentList");
                    persistentList.save();
                }

                // Some integration tests depend on this delay, make sure to
                // rerun/adapt them if changing this value
                Thread.sleep(250);
                cleanupInstallableResources();
            } catch(Exception e) {
                Logger.logWarn(e.toString(), e);
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException ignored) {
                }
            }
        }
        Logger.logInfo("Deactivated, exiting");
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#addTaskToCurrentCycle(org.apache.sling.osgi.installer.impl.OsgiInstallerTask)
     */
    public void addTaskToCurrentCycle(OsgiInstallerTask t) {
        Logger.logDebug("adding task to current cycle:" + t);
        synchronized (tasks) {
            tasks.add(t);
        }
    }

    private void checkScheme(final String scheme) {
        if ( scheme == null || scheme.length() == 0 ) {
            throw new IllegalArgumentException("Scheme required");
        }
        if ( scheme.indexOf(':') != -1 ) {
            throw new IllegalArgumentException("Scheme must not contain a colon");
        }
    }

    /**
     * @see org.apache.sling.osgi.installer.OsgiInstaller#removeResource(java.lang.String, java.lang.String)
     */
    public void removeResource(final String scheme, final String id) {
        checkScheme(scheme);
        final String url = scheme + ':' + id;
		// Will mark all resources which have r's URL as uninstallable
        Logger.logDebug("Adding URL " + url + " to urlsToRemove");

        synchronized (newResources) {
            urlsToRemove.add(url);
            newResources.notify();
        }
    }

    /**
     * @see org.apache.sling.osgi.installer.OsgiInstaller#addResource(java.lang.String, org.apache.sling.osgi.installer.InstallableResource)
     */
    public void addResource(final String scheme, final InstallableResource r) {
        checkScheme(scheme);
        RegisteredResource rr = null;
        try {
            rr = new RegisteredResourceImpl(ctx, r, scheme);
        } catch(IOException ioe) {
            Logger.logWarn("Cannot create RegisteredResource (resource will be ignored):" + r, ioe);
            return;
        }

        synchronized (newResources) {
            Logger.logDebug("Adding new resource " + r);
            newResources.add(rr);
            newResources.notify();
        }
    }

    /**
     * @see org.apache.sling.osgi.installer.OsgiInstaller#registerResources(java.lang.String, java.util.Collection)
     */
    public void registerResources(final String scheme, final Collection<InstallableResource> data) {
        checkScheme(scheme);
        final SortedSet<RegisteredResource> toAdd = new TreeSet<RegisteredResource>();
        for(InstallableResource r : data) {
            RegisteredResource rr =  null;
            try {
                rr = new RegisteredResourceImpl(ctx, r, scheme);
            } catch(IOException ioe) {
                Logger.logWarn("Cannot create RegisteredResource (resource will be ignored):" + r, ioe);
                continue;
            }

            Logger.logDebug("Adding new resource " + r);
            toAdd.add(rr);
        }

        synchronized (newResources) {
            if(!toAdd.isEmpty()) {
            	newResourcesSets.add(toAdd);
            }
            // Need to manage schemes separately: in case toAdd is empty we
            // want to mark all such resources as non-installable
            Logger.logDebug("Adding to newResourcesSchemes: " + scheme);
            newResourcesSchemes.add(scheme);
            newResources.notify();
        }
    }

    private void mergeNewResources() {
        synchronized (newResources) {
            // If we have sets of new resources, each of them represents the complete list
            // of available resources for a given scheme. So, before adding them mark
            // all resources with the same scheme in newResources, and existing
            // registeredResources, as not installable
        	for(String scheme : newResourcesSchemes) {
        	    Logger.logDebug("Processing set of new resources with scheme " + scheme);
                for(RegisteredResource r : newResources) {
                    if(r.getScheme().equals(scheme)) {
                        r.setInstallable(false);
                        Logger.logDebug("New resource set to non-installable: " + r);
                    }
                 }
                for(SortedSet<RegisteredResource> ss : registeredResources.values()) {
                    for(RegisteredResource r : ss) {
                        if(r.getScheme().equals(scheme)) {
                            r.setInstallable(false);
                            Logger.logDebug("Existing resource set to non-installable: " + r);
                        }
                    }
                }
        	}
            for(SortedSet<RegisteredResource> s : newResourcesSets) {
                newResources.addAll(s);
                Logger.logDebug("Added set of " + s.size() + " new resources with scheme "
                            + s.first().getScheme() + ": " + s);
            }
            newResourcesSets.clear();
            newResourcesSchemes.clear();

            for(RegisteredResource r : newResources) {
                SortedSet<RegisteredResource> t = registeredResources.get(r.getEntityId());
                if(t == null) {
                    t = new TreeSet<RegisteredResource>();
                    registeredResources.put(r.getEntityId(), t);
                }

                // If an object with same sort key is already present, replace with the
                // new one which might have different attributes
                if(t.contains(r)) {
                	for(RegisteredResource rr : t) {
                		if(rr.compareTo(r) == 0) {
                		    Logger.logDebug("Cleanup obsolete " + rr);
                			rr.cleanup();
                		}
                	}
                    t.remove(r);
                }
                t.add(r);
            }
            newResources.clear();

            // Mark resources for removal according to urlsToRemove
            if(!urlsToRemove.isEmpty()) {
                for(SortedSet<RegisteredResource> group : registeredResources.values()) {
                	for(RegisteredResource r : group) {
                		if(urlsToRemove.contains(r.getURL())) {
                		    Logger.logDebug("Marking " + r + " uninistallable, URL is included in urlsToRemove");
                			r.setInstallable(false);
                		}
                	}
                }
            }
            urlsToRemove.clear();
        }
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#addTaskToNextCycle(org.apache.sling.osgi.installer.impl.OsgiInstallerTask)
     */
    public void addTaskToNextCycle(OsgiInstallerTask t) {
        Logger.logDebug("adding task to next cycle:" + t);
        synchronized (tasksForNextCycle) {
            tasksForNextCycle.add(t);
        }
    }

    /** Compute OSGi tasks based on our resources, and add to supplied list of tasks */
    void computeTasks() throws Exception {
        // Add tasks that were scheduled for next cycle
        synchronized (tasksForNextCycle) {
            for(OsgiInstallerTask t : tasksForNextCycle) {
                tasks.add(t);
            }
            tasksForNextCycle.clear();
        }

        // Walk the list of entities, and create appropriate OSGi tasks for each group
        // TODO do nothing for a group that's "stable" - i.e. one where no tasks were
        // created in the last cycle??
        for(SortedSet<RegisteredResource> group : registeredResources.values()) {
            if (group.isEmpty()) {
                continue;
            }
            final String rt = group.first().getType();
            if ( InstallableResource.TYPE_BUNDLE.equals(rt) ) {
                bundleTaskCreator.createTasks(this, group, tasks);
            } else if ( InstallableResource.TYPE_CONFIG.equals(rt) ) {
                configTaskCreator.createTasks(this, group, tasks);
            }
        }
    }

    private int executeTasks() {
        int counter = 0;
        while(!tasks.isEmpty()) {
            OsgiInstallerTask t = null;
            synchronized (tasks) {
                t = tasks.first();
                tasks.remove(t);
            }
            t.execute(this);
            counter++;
        }
        return counter;
    }

    private void cleanupInstallableResources() throws IOException {
        // Cleanup resources that are not marked installable,
        // they have been processed by now
        int resourceCount = 0;
        final List<RegisteredResource> toDelete = new ArrayList<RegisteredResource>();
        final List<String> groupKeysToRemove = new ArrayList<String>();
        for(SortedSet<RegisteredResource> group : registeredResources.values()) {
            toDelete.clear();
            String key = null;
            for(RegisteredResource r : group) {
                key = r.getEntityId();
                resourceCount++;
                if(!r.isInstallable()) {
                    toDelete.add(r);
                }
            }
            for(RegisteredResource r : toDelete) {
                group.remove(r);
                r.cleanup();
                Logger.logDebug("Removing RegisteredResource from list, not installable and has been processed: " + r);
            }
            if(group.isEmpty() && key != null) {
                groupKeysToRemove.add(key);
            }
        }

        for(String key : groupKeysToRemove) {
            registeredResources.remove(key);
        }

        // List of resources might have changed
        persistentList.save();
    }

    /** If we have any tasks waiting to be retried, schedule their execution */
    private void scheduleRetries() {
    	final int toRetry = tasksForNextCycle.size();
    	if(toRetry > 0) {
    	    Logger.logDebug(toRetry + " tasks scheduled for retrying");
            synchronized (newResources) {
                newResources.notify();
                retriesScheduled = true;
            }
    	}
    }

    /**
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    public void bundleChanged(BundleEvent e) {
        synchronized (LOCK) {
            eventsCount++;
        }
    	final int t = e.getType();
    	if(t == BundleEvent.INSTALLED || t == BundleEvent.RESOLVED || t == BundleEvent.STARTED || t == BundleEvent.UPDATED) {
    	    Logger.logDebug("Received BundleEvent that might allow installed bundles to start, scheduling retries if any");
    		scheduleRetries();
    	}
    }

    private static volatile long eventsCount;

    private static final Object LOCK = new Object();

    /** Used for tasks that wait for a framework or bundle event before retrying their operations */
    public static long getTotalEventsCount() {
        synchronized (LOCK) {
            return eventsCount;
        }
    }

    /**
     * @see org.osgi.framework.FrameworkListener#frameworkEvent(org.osgi.framework.FrameworkEvent)
     */
    public void frameworkEvent(final FrameworkEvent event) {
        synchronized (LOCK) {
            eventsCount++;
        }
    }
}
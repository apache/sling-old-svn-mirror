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
import org.apache.sling.osgi.installer.OsgiInstallerStatistics;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/** Worker thread where all OSGi tasks are executed.
 *  Runs cycles where the list of RegisteredResources is examined,
 *  OsgiTasks are created accordingly and executed.
 *
 *  A separate list of RegisteredResources is kept for resources
 *  that are updated or removed during a cycle, and merged with
 *  the main list at the end of the cycle.
 */
class OsgiInstallerThread extends Thread implements BundleListener {

    private final OsgiInstallerContext ctx;
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

    private final BundleTaskCreator bundleTaskCreator = new BundleTaskCreator();
    private final ConfigTaskCreator configTaskCreator = new ConfigTaskCreator();

    OsgiInstallerThread(OsgiInstallerContext ctx) {
        setName(getClass().getSimpleName());
        this.ctx = ctx;
        final File f = ctx.getBundleContext().getDataFile("RegisteredResourceList.ser");
        persistentList = new PersistentResourceList(f);
        registeredResources = persistentList.getData();
    }

    void deactivate() {
        ctx.getBundleContext().removeBundleListener(this);
        active = false;
        synchronized (newResources) {
            newResources.notify();
        }
    }

    @Override
    public void run() {
        ctx.getBundleContext().addBundleListener(this);

        while(active) {
            try {
            	mergeNewResources();
            	computeTasks();

            	if(tasks.isEmpty() && !retriesScheduled) {
            	    // No tasks to execute - wait until new resources are
            	    // registered
            	    cleanupInstallableResources();
            	    Logger.logDebug("No tasks to process, going idle");

            	    ctx.setCounter(OsgiInstallerStatistics.WORKER_THREAD_IS_IDLE_COUNTER, 1);
                    ctx.incrementCounter(OsgiInstallerStatistics.WORKER_THREAD_BECOMES_IDLE_COUNTER);
            	    synchronized (newResources) {
                        newResources.wait();
                    }
            	    Logger.logDebug("Notified of new resources, back to work");
                    ctx.setCounter(OsgiInstallerStatistics.WORKER_THREAD_IS_IDLE_COUNTER, 0);
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

    void addTaskToCurrentCycle(OsgiInstallerTask t) {
        Logger.logDebug("adding task to current cycle:" + t);
        synchronized (tasks) {
            tasks.add(t);
        }
    }

    /** Register a resource for removal, or ignore if we don't have that URL */
    void removeResource(String url) {
		// Will mark all resources which have r's URL as uninstallable
        Logger.logDebug("Adding URL " + url + " to urlsToRemove");

        synchronized (newResources) {
            urlsToRemove.add(url);
            newResources.notify();
        }
    }

    /** Register a single new resource, will be processed on the next cycle */
    void addNewResource(final InstallableResource r) {
        RegisteredResource rr = null;
        try {
            rr = new RegisteredResourceImpl(ctx, r);
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

    /** Register a number of new resources, and mark others having the same scheme as not installable.
     *  Used with {@link OsgiInstaller.registerResources}
     */
    void addNewResources(Collection<InstallableResource> data, String urlScheme, BundleContext bundleContext) {
        // Check scheme, do nothing if at least one of them is wrong
        final SortedSet<RegisteredResource> toAdd = new TreeSet<RegisteredResource>(new RegisteredResourceComparator());
        for(InstallableResource r : data) {
            RegisteredResource rr =  null;
            try {
                rr = new RegisteredResourceImpl(ctx, r);
            } catch(IOException ioe) {
                Logger.logWarn("Cannot create RegisteredResource (resource will be ignored):" + r, ioe);
                continue;
            }

            if(!rr.getUrlScheme().equals(urlScheme)) {
                throw new IllegalArgumentException(
                        "URL of all supplied InstallableResource must start with supplied scheme"
                        + ", scheme is not '" + urlScheme + "' for URL " + r.getUrl());
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
            Logger.logDebug("Adding to newResourcesSchemes: " + urlScheme);
            newResourcesSchemes.add(urlScheme);
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
                    if(r.getUrlScheme().equals(scheme)) {
                        r.setInstallable(false);
                        Logger.logDebug("New resource set to non-installable: " + r);
                    }
                 }
                for(SortedSet<RegisteredResource> ss : registeredResources.values()) {
                    for(RegisteredResource r : ss) {
                        if(r.getUrlScheme().equals(scheme)) {
                            r.setInstallable(false);
                            Logger.logDebug("Existing resource set to non-installable: " + r);
                        }
                    }
                }
        	}
            for(SortedSet<RegisteredResource> s : newResourcesSets) {
                newResources.addAll(s);
                Logger.logDebug("Added set of " + s.size() + " new resources with scheme "
                            + s.first().getUrlScheme() + ": " + s);
            }
            newResourcesSets.clear();
            newResourcesSchemes.clear();

            for(RegisteredResource r : newResources) {
                SortedSet<RegisteredResource> t = registeredResources.get(r.getEntityId());
                if(t == null) {
                    t = createRegisteredResourcesEntry();
                    registeredResources.put(r.getEntityId(), t);
                }

                // If an object with same sort key is already present, replace with the
                // new one which might have different attributes
                if(t.contains(r)) {
                	for(RegisteredResource rr : t) {
                		if(t.comparator().compare(rr, r) == 0) {
                		    Logger.logDebug("Cleanup obsolete " + rr);
                			rr.cleanup(ctx);
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
                		if(urlsToRemove.contains(r.getUrl())) {
                		    Logger.logDebug("Marking " + r + " uninistallable, URL is included in urlsToRemove");
                			r.setInstallable(false);
                		}
                	}
                }
            }
            urlsToRemove.clear();
        }
    }

    void addTaskToNextCycle(OsgiInstallerTask t) {
        synchronized (tasksForNextCycle) {
            tasksForNextCycle.add(t);
        }
    }

    /** Factored out to use the exact same structure in tests */
    static SortedSet<RegisteredResource> createRegisteredResourcesEntry() {
        return new TreeSet<RegisteredResource>(new RegisteredResourceComparator());
    }


    /** Compute OSGi tasks based on our resources, and add to supplied list of tasks */
    void computeTasks() throws Exception {
        // Add tasks that were scheduled for next cycle and are executable now
        final List<OsgiInstallerTask> toKeep = new ArrayList<OsgiInstallerTask>();
        synchronized (tasksForNextCycle) {
            for(OsgiInstallerTask t : tasksForNextCycle) {
                if(t.isExecutable(ctx)) {
                    tasks.add(t);
                } else {
                    toKeep.add(t);
                }
            }
            tasksForNextCycle.clear();
            tasksForNextCycle.addAll(toKeep);
        }

        // Walk the list of entities, and create appropriate OSGi tasks for each group
        // TODO do nothing for a group that's "stable" - i.e. one where no tasks were
        // created in the last cycle??
        for(SortedSet<RegisteredResource> group : registeredResources.values()) {
            if (group.isEmpty()) {
                continue;
            }
            final String rt = group.first().getResourceType();
            if ( InstallableResource.TYPE_BUNDLE.equals(rt) ) {
                bundleTaskCreator.createTasks(ctx, group, tasks);
            } else if ( InstallableResource.TYPE_CONFIG.equals(rt) ) {
                configTaskCreator.createTasks(ctx, group, tasks);
            }
        }
    }

    private int executeTasks() throws Exception {
        int counter = 0;
        while(!tasks.isEmpty()) {
            OsgiInstallerTask t = null;
            synchronized (tasks) {
                t = tasks.first();
            }
            try {
                t.execute(ctx);
                removeTask(t);
                counter++;
            } catch(Exception e) {
            	if(!t.canRetry(ctx)) {
            	    Logger.logInfo("Task cannot be retried, removing from list:" + t);
                    removeTask(t);
            	}
            	throw e;
            }
        }
        return counter;
    }

    private void removeTask(OsgiInstallerTask t) {
        synchronized (tasks) {
            tasks.remove(t);
        }
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
                r.cleanup(ctx);
                Logger.logDebug("Removing RegisteredResource from list, not installable and has been processed: " + r);
            }
            if(group.isEmpty() && key != null) {
                groupKeysToRemove.add(key);
            }
        }

        for(String key : groupKeysToRemove) {
            registeredResources.remove(key);
        }

        ctx.setCounter(OsgiInstallerStatistics.REGISTERED_RESOURCES_COUNTER, resourceCount);
        ctx.setCounter(OsgiInstallerStatistics.REGISTERED_GROUPS_COUNTER, registeredResources.size());
        ctx.incrementCounter(OsgiInstallerStatistics.INSTALLER_CYCLES_COUNTER);

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

    public void bundleChanged(BundleEvent e) {
    	final int t = e.getType();
    	if(t == BundleEvent.INSTALLED || t == BundleEvent.RESOLVED || t == BundleEvent.STARTED || t == BundleEvent.UPDATED) {
    	    Logger.logDebug("Received BundleEvent that might allow installed bundles to start, scheduling retries if any");
    		scheduleRetries();
    	}
    }
}
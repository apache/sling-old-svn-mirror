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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/** Worker thread where all OSGi tasks are executed.
 *  Runs cycles where the list of RegisteredResources is examined,
 *  OsgiTasks are created accordingly and executed.
 *  
 *  A separate list of RegisteredResources is kept for resources
 *  that are updated or removed during a cycle, and merged with
 *  the main list at the end of the cycle.
 */
class OsgiInstallerThread extends Thread {
    
    private final OsgiInstallerContext ctx;
    private final List<RegisteredResource> newResources = new LinkedList<RegisteredResource>();
    private final SortedSet<OsgiInstallerTask> tasks = new TreeSet<OsgiInstallerTask>();
    private final SortedSet<OsgiInstallerTask> tasksForNextCycle = new TreeSet<OsgiInstallerTask>();
    private final List<SortedSet<RegisteredResource>> newResourcesSets = new ArrayList<SortedSet<RegisteredResource>>();
    
    /** Group our RegisteredResource by OSGi entity */ 
    private Map<String, SortedSet<RegisteredResource>>registeredResources = 
        new HashMap<String, SortedSet<RegisteredResource>>();
    
    private final BundleTaskCreator bundleTaskCreator = new BundleTaskCreator();
    
    static interface TaskCreator {
    	/** Add the required OsgiInstallerTasks to the tasks collection, so that the resources reach
    	 * 	their desired states.
    	 *  @param ctx used to find out which bundles and configs are currently active
    	 * 	@param resources ordered set of RegisteredResource which all have the same entityId
    	 * 	@param tasks lists of tasks, to which we'll add the ones computed by this method
    	 */
    	void createTasks(OsgiInstallerContext ctx, SortedSet<RegisteredResource> resources, SortedSet<OsgiInstallerTask> tasks);
    }
    
    OsgiInstallerThread(OsgiInstallerContext ctx) {
        setName(getClass().getSimpleName());
        this.ctx = ctx;
    }

    @Override
    public void run() {
        while(true) {
            // TODO do nothing if nothing to process!
            try {
            	mergeNewResources();
            	computeTasks();
                executeTasks();
                Thread.sleep(250);
                cycleDone();
            } catch(Exception e) {
                if(ctx.getLogService() != null) {
                    ctx.getLogService().log(LogService.LOG_WARNING, e.toString(), e);
                }
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException ignored) {
                }
            }
        }
    }
    
    void addTaskToCurrentCycle(OsgiInstallerTask t) {
        if(ctx.getLogService() != null) {
            ctx.getLogService().log(LogService.LOG_DEBUG, "adding task to current cycle:" + t);
        }
        synchronized (tasks) {
            tasks.add(t);
        }
    }
    
    /** Register a single new resource, will be processed on the next cycle */
    void addNewResource(RegisteredResource r) {
        synchronized (newResources) {
            newResources.add(r);
        }
    }
    
    /** Register a number of new resources, and mark others having the same scheme as not installable.
     *  Used with {@link OsgiInstaller.registerResources}
     */
    void addNewResources(Collection<InstallableResource> data, String urlScheme, BundleContext bundleContext) throws IOException {
        // Check scheme, do nothing if at least one of them is wrong
        final SortedSet<RegisteredResource> toAdd = new TreeSet<RegisteredResource>(new RegisteredResourceComparator());
        for(InstallableResource r : data) {
            final RegisteredResource rr = new RegisteredResourceImpl(bundleContext, r);
            if(!rr.getUrlScheme().equals(urlScheme)) {
                throw new IllegalArgumentException(
                        "URL of all supplied InstallableResource must start with supplied scheme"
                        + ", scheme is not '" + urlScheme + "' for URL " + r.getUrl());
            }
            toAdd.add(rr);
        }
        
        if(!toAdd.isEmpty()) {
            synchronized (newResources) {
                newResourcesSets.add(toAdd);
            }
        }
    }
    
    private void mergeNewResources() {
        synchronized (newResources) {
            // If we have sets of new resources, each of them represents the complete list
            // of available resources for a given scheme. So, before adding them mark
            // all resources with the same scheme in newResources, and existing
            // registeredResources, as not installable
            for(SortedSet<RegisteredResource> s : newResourcesSets) {
                final String scheme = s.first().getUrlScheme();
                debug("Processing set of new resources with scheme " + scheme);
                for(RegisteredResource r : newResources) {
                    if(r.getUrlScheme().equals(scheme)) {
                        r.setInstallable(false);
                        debug("New resource set to non-installable: " + r); 
                    }
                 }
                for(SortedSet<RegisteredResource> ss : registeredResources.values()) {
                    for(RegisteredResource r : ss) {
                        if(r.getUrlScheme().equals(scheme)) {
                            r.setInstallable(false);
                            debug("Existing resource set to non-installable: " + r); 
                        }
                    }
                }
                newResources.addAll(s);
                debug("Added set of " + s.size() + " new resources with scheme " + scheme);
            }
            newResourcesSets.clear();
            
            for(RegisteredResource r : newResources) {
                SortedSet<RegisteredResource> t = registeredResources.get(r.getEntityId());
                if(t == null) {
                    t = createRegisteredResourcesEntry();
                    registeredResources.put(r.getEntityId(), t);
                }
                
                // If an object with same sort key is already present, replace with the
                // new one which might have different attributes
                if(t.contains(r)) {
                    t.remove(r);
                }
                t.add(r);
            }
            newResources.clear();
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
            if(group.first().getResourceType().equals(RegisteredResource.ResourceType.BUNDLE)) {
                bundleTaskCreator.createTasks(ctx, group, tasks);
            } else {
                throw new IllegalArgumentException("No TaskCreator for resource type "+ group.first().getResourceType());
            } 
        }
    }
    
    private void executeTasks() throws Exception {
        while(!tasks.isEmpty()) {
            OsgiInstallerTask t = null;
            synchronized (tasks) {
                t = tasks.first();
            }
            t.execute(ctx);
            synchronized (tasks) {
                tasks.remove(t);
            }
        }
    }
    
    protected void cycleDone() {
    }
    
    private void debug(String str) {
        if(ctx.getLogService() != null) {
            ctx.getLogService().log(LogService.LOG_DEBUG, str);
        }
    }
}
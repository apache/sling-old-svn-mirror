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
package org.apache.sling.installer.core.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.RegisteredResourceGroup;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker thread where all OSGi tasks are executed.
 *  Runs cycles where the list of RegisteredResources is examined,
 *  OsgiTasks are created accordingly and executed.
 *
 *  A separate list of RegisteredResources is kept for resources
 *  that are updated or removed during a cycle, and merged with
 *  the main list at the end of the cycle.
 */
public class OsgiInstallerImpl
    extends Thread
    implements BundleListener, FrameworkListener, OsgiInstaller, ServiceTrackerCustomizer {

    /** The logger */
    private final Logger logger =  LoggerFactory.getLogger(this.getClass());

    /** The audit logger */
    private final Logger auditLogger =  LoggerFactory.getLogger("org.apache.sling.audit.osgi.installer");

    /** The bundle context. */
    private final BundleContext ctx;

    /** New clients are joining through this map. */
    private final Map<String, List<RegisteredResource>> newResourcesSchemes = new HashMap<String, List<RegisteredResource>>();

    /** New resources added by clients. */
    private final List<RegisteredResource> newResources = new LinkedList<RegisteredResource>();

    /** Removed resources from clients. */
    private final Set<String> urlsToRemove = new HashSet<String>();

    /** Tasks to be scheduled in the next iteration. */
    private final SortedSet<InstallTask> tasksForNextCycle = new TreeSet<InstallTask>();

    /** Are we still activate? */
    private volatile boolean active = true;

    /** The persistent resource list. */
    private PersistentResourceList persistentList;

    private final FileUtil fileUtil;

    /** A tracker for the factories. */
    private ServiceTracker factoryTracker;

    /** A tracker for the transformers. */
    private ServiceTracker transformerTracker;

    /** Constructor */
    public OsgiInstallerImpl(final BundleContext ctx) {
        this.ctx = ctx;
        this.fileUtil = new FileUtil(ctx);
    }

    /**
     * Deactivate
     */
    public void deactivate() {
        this.active = false;

        // Stop service trackers.
        this.factoryTracker.close();
        this.transformerTracker.close();

        // remove as listener
        ctx.removeBundleListener(this);
        ctx.removeFrameworkListener(this);

        // wake up sleeping thread
        synchronized (newResources) {
            newResources.notify();
        }
        this.logger.debug("Waiting for installer thread to stop");
        try {
            this.join();
        } catch (InterruptedException e) {
            // we simply ignore this
        }

        this.logger.info("Apache Sling OSGi Installer Service stopped.");
    }

    /**
     * Initialize the installer
     */
    private void init() {
        // start service trackers
        this.factoryTracker = new ServiceTracker(ctx, InstallTaskFactory.class.getName(), this);
        this.factoryTracker.open();
        this.transformerTracker = new ServiceTracker(ctx, ResourceTransformer.class.getName(), this);
        this.transformerTracker.open();

        // listen to framework and bundle events
        this.ctx.addFrameworkListener(this);
        this.ctx.addBundleListener(this);
        setName(getClass().getSimpleName());
        final File f = this.fileUtil.getDataFile("RegisteredResourceList.ser");
        this.persistentList = new PersistentResourceList(f);
        this.logger.info("Apache Sling OSGi Installer Service started.");
    }

    @Override
    public void run() {
        this.init();
        while (active) {
            this.mergeNewResources();

            // invoke transformers
            this.transformResources();

            // execute tasks
            final SortedSet<InstallTask> tasks = this.computeTasks();
            final boolean tasksCreated = !tasks.isEmpty();
            this.executeTasks(tasks);

            // clean up and save
            this.cleanupInstallableResources();

            // if we don't have any tasks, we go to sleep
            if (!tasksCreated) {
                synchronized (newResources) {
                    // before we go to sleep, check if new resources arrived in the meantime
                    if ( !this.hasNewResources()) {
                        // No tasks to execute - wait until new resources are
                        // registered
                        logger.debug("No tasks to process, going idle");
                        try {
                            newResources.wait();
                        } catch (InterruptedException ignore) {}
                        logger.debug("Notified of new resources, back to work");
                    }
                }
            } else {
                // Some integration tests depend on this delay, make sure to
                // rerun/adapt them if changing this value
                try {
                    Thread.sleep(250);
                } catch (final InterruptedException ignore) {}
            }
        }
    }

    /**
     * Check the scheme
     * @throws IllegalArgumentException
     */
    private void checkScheme(final String scheme) {
        if ( scheme == null || scheme.length() == 0 ) {
            throw new IllegalArgumentException("Scheme required");
        }
        if ( scheme.indexOf(':') != -1 ) {
            throw new IllegalArgumentException("Scheme must not contain a colon");
        }
    }

    /**
     * Create registered resources for all installable resources.
     */
    private List<RegisteredResource> createResources(final String scheme,
                                                     final InstallableResource[] resources) {
        checkScheme(scheme);
        List<RegisteredResource> createdResources = null;
        if ( resources != null && resources.length > 0 ) {
            createdResources = new ArrayList<RegisteredResource>();
            for(final InstallableResource r : resources ) {
                try {
                    final RegisteredResource rr = RegisteredResourceImpl.create(ctx, r, scheme, this.fileUtil);
                    createdResources.add(rr);
                    logger.debug("Registering new resource: {}", rr);
                } catch (final IOException ioe) {
                    logger.warn("Cannot create RegisteredResource (resource will be ignored):" + r, ioe);
                }
            }
        }
        return createdResources;
    }

    /**
     * Try to close all input streams.
     * This is just a sanity check for input streams which might not have been closed
     * as either processing threw an exception or the resource type is not supported.
     */
    private void closeInputStreams(final InstallableResource[] resources) {
        if ( resources != null ) {
            for(final InstallableResource r : resources ) {
                final InputStream is = r.getInputStream();
                if ( is != null ) {
                    try {
                        is.close();
                    } catch (final IOException ignore) {
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * @see org.apache.sling.installer.api.OsgiInstaller#updateResources(java.lang.String, org.apache.sling.installer.api.InstallableResource[], java.lang.String[])
     */
    public void updateResources(final String scheme,
                                final InstallableResource[] resources,
                                final String[] ids) {
        try {
            final List<RegisteredResource> updatedResources = this.createResources(scheme, resources);

            synchronized (newResources) {
                boolean doNotify = false;
                if ( updatedResources != null && updatedResources.size() > 0 ) {
                    newResources.addAll(updatedResources);
                    doNotify = true;
                }
                if ( ids != null && ids.length > 0 ) {
                    for(final String id : ids) {
                        final String url = scheme + ':' + id;
                        // Will mark all resources which have r's URL as uninstallable
                        logger.debug("Adding URL {} to urlsToRemove", url);

                        urlsToRemove.add(url);
                    }
                    doNotify = true;
                }
                if ( doNotify ) {
                    newResources.notify();
                }
            }
        } finally {
            // we simply close all input streams now
            this.closeInputStreams(resources);
        }
    }

    /**
     * @see org.apache.sling.installer.api.OsgiInstaller#registerResources(java.lang.String, org.apache.sling.installer.api.InstallableResource[])
     */
    public void registerResources(final String scheme, final InstallableResource[] resources) {
        try {
            List<RegisteredResource> registeredResources = this.createResources(scheme, resources);
            if ( registeredResources == null ) {
                // make sure we have a list, this makes processing later on easier
                registeredResources = Collections.emptyList();
            }
            synchronized (newResources) {
                logger.debug("Registered new resource scheme: {}", scheme);
                newResourcesSchemes.put(scheme, registeredResources);
                newResources.notify();
            }
        } finally {
            // we simply close all input streams now
            this.closeInputStreams(resources);
        }
    }

    /**
     * Checks if new resources are available.
     * This method should only be invoked from within a synchronized (newResources) block!
     */
    private boolean hasNewResources() {
        return !this.newResources.isEmpty() || !this.newResourcesSchemes.isEmpty() || !this.urlsToRemove.isEmpty();
    }

    /**
     * This is the heart of the installer - it processes new resources and merges them
     * with existing resources.
     * The second part consists of detecting the resources to be processsed.
     */
    private void mergeNewResources() {
        synchronized (newResources) {
            final boolean changed = this.hasNewResources();

            if ( changed ) {
                // check for new resource providers (schemes)
                // if we have new providers we have to sync them with existing resources
                for(final Map.Entry<String, List<RegisteredResource>> entry : this.newResourcesSchemes.entrySet()) {
                    logger.debug("Processing set of new resources with scheme {}", entry.getKey());

                    // set all previously found resources that are not available anymore to uninstall
                    // if they have been installed - remove resources with a different state
                    for(final String entityId : this.persistentList.getEntityIds()) {
                        final EntityResourceList group = this.persistentList.getEntityResourceList(entityId);

                        final List<RegisteredResource> toRemove = new ArrayList<RegisteredResource>();
                        boolean first = true;
                        for(final RegisteredResource r : group.getResources()) {
                            if ( r.getScheme().equals(entry.getKey()) ) {
                                logger.debug("Checking {}", r);
                                // search if we have a new entry with the same url
                                boolean found = false;
                                final Iterator<RegisteredResource> m = entry.getValue().iterator();
                                while ( !found && m.hasNext() ) {
                                    final RegisteredResource testResource = m.next();
                                    found = testResource.getURL().equals(r.getURL());
                                }
                                if ( !found) {
                                    logger.debug("Resource {} seems to be removed.", r);
                                    if ( first && (r.getState() == RegisteredResource.State.INSTALLED
                                               ||  r.getState() == RegisteredResource.State.INSTALL) ) {
                                         r.setState(RegisteredResource.State.UNINSTALL);
                                    } else {
                                        toRemove.add(r);
                                    }
                                }
                            }
                            first = false;
                        }
                        for(final RegisteredResource rr : toRemove) {
                            this.persistentList.remove(rr);
                        }
                    }
                    logger.debug("Added set of {} new resources with scheme {} : {}",
                            new Object[] {entry.getValue().size(), entry.getKey(), entry.getValue()});
                    newResources.addAll(entry.getValue());
                }

                newResourcesSchemes.clear();

                for(RegisteredResource r : newResources) {
                    this.persistentList.addOrUpdate(r);
                }
                newResources.clear();

                // Mark resources for removal according to urlsToRemove
                if (!urlsToRemove.isEmpty()) {
                    for(final String url : urlsToRemove ) {
                        this.persistentList.remove(url);
                    }
                }
                urlsToRemove.clear();

                printResources("Merged");
                // persist list
                this.persistentList.save();
            }
        }
    }

    private void printResources(String hint) {
        if ( !logger.isDebugEnabled() ) {
            return;
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(hint);
        sb.append(" Resources={\n");
        for(final String id : this.persistentList.getEntityIds() ) {
            sb.append("- ");
            sb.append(id);
            sb.append(" : [");
            boolean first = true;
            for(final RegisteredResource rr : this.persistentList.getEntityResourceList(id).getResources()) {
                if ( !first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(rr);
            }
            sb.append("]\n");
        }
        sb.append("}\n");
        logger.debug(sb.toString());
    }

    /**
     * Compute OSGi tasks based on our resources, and add to supplied list of tasks.
     */
    private SortedSet<InstallTask> computeTasks() {
        final SortedSet<InstallTask> tasks = new TreeSet<InstallTask>();

        // Add tasks that were scheduled for next cycle
        synchronized (tasksForNextCycle) {
            tasks.addAll(tasksForNextCycle);
            tasksForNextCycle.clear();
        }

        // Walk the list of entities, and create appropriate OSGi tasks for each group
        final Object[] services = this.factoryTracker.getServices();
        if ( services != null && services.length > 0 ) {
            for(final String entityId : this.persistentList.getEntityIds()) {
                final EntityResourceList group = this.persistentList.getEntityResourceList(entityId);
                // Check the first resource in each group
                final RegisteredResource toActivate = group.getActiveResource();
                if ( toActivate != null ) {
                    final InstallTask task = getTask(services, group);
                    if ( task != null ) {
                        tasks.add(task);
                    }

                }
            }
        }
        return tasks;
    }

    /**
     * Get the task for the resource.
     */
    private InstallTask getTask(final Object[] services,
            final RegisteredResourceGroup rrg) {
        InstallTask result = null;

        for(int i=0; i<services.length; i++) {
            if ( services[i] instanceof InstallTaskFactory ) {
                final InstallTaskFactory factory = (InstallTaskFactory)services[i];
                result = factory.createTask(rrg);
                if ( result != null ) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Execute all tasks
     */
    private void executeTasks(final SortedSet<InstallTask> tasks) {
        if ( !tasks.isEmpty() ) {

            final InstallationContext ctx = new InstallationContext() {

                public void addTaskToNextCycle(final InstallTask t) {
                    logger.debug("adding task to next cycle: {}", t);
                    synchronized (tasksForNextCycle) {
                        tasksForNextCycle.add(t);
                    }
                }

                public void addTaskToCurrentCycle(final InstallTask t) {
                    logger.debug("adding task to current cycle: {}", t);
                    synchronized ( tasks ) {
                        tasks.add(t);
                    }
                }

                public void log(String message, Object... args) {
                    auditLogger.info(message, args);
                }
            };
            while (this.active && !tasks.isEmpty()) {
                InstallTask t = null;
                synchronized (tasks) {
                    t = tasks.first();
                    tasks.remove(t);
                }
                logger.debug("Executing task: {}", t);
                t.execute(ctx);
            }
            persistentList.save();
        }
    }

    /**
     * Clean up and compact
     */
    private void cleanupInstallableResources() {
        if ( this.persistentList.compact() ) {
            persistentList.save();
        }
        printResources("Compacted");
    }

    /**
     * Invoke the transformers on the resources.
     */
    private void transformResources() {
        // Walk the list of resources and invoke all transformers
        final Object[] services = this.transformerTracker.getServices();
        if ( services != null && services.length > 0 ) {
            boolean changed = false;
            for(final String entityId : this.persistentList.getEntityIds()) {
                final EntityResourceList group = this.persistentList.getEntityResourceList(entityId);
                // Check the first resource in each group
                final RegisteredResource toActivate = group.getActiveResource();
                if ( toActivate != null && toActivate.getState() == RegisteredResource.State.INSTALL ) {
                    for(int i=0; i<services.length; i++) {
                        if ( services[i] instanceof ResourceTransformer ) {
                            final ResourceTransformer transformer = (ResourceTransformer)services[i];
                            final TransformationResult tr = transformer.transform(toActivate);
                            if ( tr != null ) {
                                if ( tr.getResourceType() != null ) {
                                    this.persistentList.remove(toActivate);
                                }
                                if ( ((RegisteredResourceImpl)toActivate).update(tr) ) {
                                    if ( tr.getResourceType() != null ) {
                                        this.persistentList.addOrUpdate(toActivate);
                                    }
                                } else {
                                    // ignore this resource from now on
                                    toActivate.setState(RegisteredResource.State.IGNORED);
                                }
                                changed = true;
                            }
                        }
                    }
                }
            }
            if ( changed ) {
                this.persistentList.save();
                printResources("Transformed");
            }
        }
    }

    /** If we have any tasks waiting to be retried, schedule their execution
    private void scheduleRetries() {
        final int toRetry;
        synchronized ( tasksForNextCycle ) {
            toRetry = tasksForNextCycle.size();
        }
        if (toRetry > 0) {
            logger.debug("{} tasks scheduled for retrying", toRetry);
            synchronized (newResources) {
                newResources.notify();
            }
        }
    }
    */

    /**
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    public void bundleChanged(BundleEvent e) {
        synchronized (LOCK) {
            eventsCount++;
        }
        final int t = e.getType();
        if(t == BundleEvent.INSTALLED || t == BundleEvent.RESOLVED || t == BundleEvent.STARTED || t == BundleEvent.UPDATED) {
            logger.debug("Received BundleEvent that might allow installed bundles to start, scheduling retries if any");
            // TODO - for now we always reschedule regardless if we have retries
            // If the config task factory is only registered when config admin is available we can relax this again.
            synchronized (newResources) {
                newResources.notify();
            }
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

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void removedService(ServiceReference reference, Object service) {
        ctx.ungetService(reference);
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void modifiedService(ServiceReference reference, Object service) {
        // do nothing
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public Object addingService(ServiceReference reference) {
        // new factory has been added, wake up main thread
        synchronized (newResources) {
            newResources.notify();
        }
        return ctx.getService(reference);
    }
}
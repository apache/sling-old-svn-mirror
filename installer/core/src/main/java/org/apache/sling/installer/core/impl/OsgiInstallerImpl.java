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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.CheckForNull;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.ResourceChangeListener;
import org.apache.sling.installer.api.UpdateHandler;
import org.apache.sling.installer.api.UpdateResult;
import org.apache.sling.installer.api.info.InfoProvider;
import org.apache.sling.installer.api.info.InstallationState;
import org.apache.sling.installer.api.info.Resource;
import org.apache.sling.installer.api.info.ResourceGroup;
import org.apache.sling.installer.api.tasks.ChangeStateTask;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.RetryHandler;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.apache.sling.installer.core.impl.tasks.BundleUpdateTask;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.startlevel.StartLevel;
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
implements OsgiInstaller, ResourceChangeListener, RetryHandler, InfoProvider, Runnable {

    /**
     * The name of the framework property defining handling of bundle updates
     */
    private static final String PROP_START_LEVEL_HANDLING = "sling.installer.switchstartlevel";

    /**
     * The name of the framework property setting required services
     */
    private static final String PROP_REQUIRED_SERVICES = "sling.installer.requiredservices";

    /** The logger */
    private final Logger logger =  LoggerFactory.getLogger(this.getClass());

    /** The audit logger */
    private final Logger auditLogger =  LoggerFactory.getLogger("org.apache.sling.audit.osgi.installer");

    /** The bundle context. */
    private final BundleContext ctx;

    /** New clients are joining through this map. */
    private final Map<String, List<InternalResource>> newResourcesSchemes = new HashMap<>();

    /** New resources added by clients. */
    private final List<InternalResource> newResources = new LinkedList<>();

    /** Removed resources from clients. */
    private final Set<String> urlsToRemove = new HashSet<>();

    /** Update infos to process. */
    private final List<UpdateInfo> updateInfos = new ArrayList<>();

    /** Are the required services satisfied? */
    private volatile boolean satisfied = false;

    /** Are we still activate? */
    private volatile boolean active = true;

    /** Are we still running? */
    private volatile Thread backgroundThread;

    /** Flag indicating that a retry event occured during tasks executions. */
    private volatile boolean retryDuringTaskExecution = false;

    /** The persistent resource list. */
    private PersistentResourceList persistentList;

    /** A tracker for the factories. */
    private SortingServiceTracker<InstallTaskFactory> factoryTracker;

    /** A tracker for the transformers. */
    private SortingServiceTracker<ResourceTransformer> transformerTracker;

    /** A tracker for update handlers. */
    private SortingServiceTracker<UpdateHandler> updateHandlerTracker;

    /** New resources lock. */
    private final Object resourcesLock = new Object();

    private final InstallListener listener;
    private final AtomicLong backgroundTaskCounter = new AtomicLong();

    /** Switch start level on bundle update? */
    private final boolean switchStartLevel;

    /**
     *  Constructor
     *
     *  Most of the initialization is defered to the background thread
     */
    public OsgiInstallerImpl(final BundleContext ctx) {
        this.ctx = ctx;
        // Initialize file util
        new FileDataStore(ctx);
        final File f = FileDataStore.SHARED.getDataFile("RegisteredResourceList.ser");
        this.listener = new InstallListener(ctx, logger);
        this.persistentList = new PersistentResourceList(f, listener);
        this.switchStartLevel = PropertiesUtil.toBoolean(ctx.getProperty(PROP_START_LEVEL_HANDLING), false);
    }

    /**
     * Deactivate
     */
    public void deactivate() {
        // wake up sleeping thread
        synchronized (this.resourcesLock) {
            logger.debug("Deactivating and notifying resourcesLock");
            this.active = false;
            this.resourcesLock.notify();
        }

        // Stop service trackers.
        if ( this.factoryTracker != null ) {
            this.factoryTracker.close();
        }
        if ( this.transformerTracker != null ) {
            this.transformerTracker.close();
        }
        if ( this.updateHandlerTracker != null ) {
            this.updateHandlerTracker.close();
        }

        this.listener.dispose();

        if ( this.backgroundThread != null ) {
            if ( logger.isDebugEnabled() ) {
                final Thread t = this.backgroundThread;
                if ( t != null ) {
                    logger.debug("Waiting for main background thread {} to stop", t.getName());
                }
            }

            while ( this.backgroundThread != null ) {
                // use a local variable to avoid NPEs
                final Thread t = backgroundThread;
                if ( t != null ) {
                    try {
                        t.join(50L);
                    } catch (final InterruptedException e) {
                        // we simply ignore this
                    }
                }
            }
            logger.debug("Done waiting for background thread");
        }

        // remove file util
        FileDataStore.SHARED = null;

        this.logger.info("Apache Sling OSGi Installer Service stopped.");
    }

    /**
     * Start this component.
     */
    public void start() {
        this.startBackgroundThread();
    }

    /**
     * Initialize the installer
     */
    private void init() {
        // start service trackers
        this.factoryTracker = new SortingServiceTracker<>(ctx, InstallTaskFactory.class.getName(), this);
        this.transformerTracker = new SortingServiceTracker<>(ctx, ResourceTransformer.class.getName(), this);
        this.updateHandlerTracker = new SortingServiceTracker<>(ctx, UpdateHandler.class.getName(), null);
        this.factoryTracker.open();
        this.transformerTracker.open();
        this.updateHandlerTracker.open();

        this.logger.info("Apache Sling OSGi Installer Service started.");
        this.checkSatisfied();
    }

    /**
     * Start the background thread.
     */
    private void startBackgroundThread() {
        this.backgroundThread = new Thread(this);
        this.backgroundThread.setName(getClass().getSimpleName());
        this.backgroundThread.setDaemon(true);
        this.backgroundThread.start();
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        logger.debug("Main background thread starts");
        try {
            this.init();

            while (this.active) {
                this.listener.start();

                processUpdateInfos();

                // merge potential new resources
                this.mergeNewlyRegisteredResources();

                synchronized ( this.resourcesLock ) {
                    if ( !this.satisfied ) {
                        logger.debug("Required services are not available yet.");
                        try {
                            logger.debug("wait() on resourcesLock");
                            this.resourcesLock.wait();
                        } catch (final InterruptedException ignore) {}
                        continue;
                    }
                    this.retryDuringTaskExecution = false;
                }

                // invoke transformers
                this.transformResources();

                // Compute tasks
                final SortedSet<InstallTask> tasks = this.computeTasks();

                // execute tasks and see if we have to stop processing
                final ACTION action = this.executeTasks(tasks);
                if ( action == ACTION.SLEEP ) {
                    synchronized ( this.resourcesLock ) {
                        // before we go to sleep, check if new resources arrived in the meantime
                        if ( !this.hasNewResources() && this.active && !this.retryDuringTaskExecution) {
                            // No tasks to execute - wait until new resources are
                            // registered
                            logger.debug("No more tasks to process, suspending listener and going idle");
                            this.listener.suspend();

                            try {
                                logger.debug("wait() on resourcesLock");
                                this.resourcesLock.wait();
                            } catch (final InterruptedException ignore) {}

                            if ( active ) {
                                logger.debug("Done wait()ing on resourcesLock, restarting listener");
                                this.listener.start();
                            } else {
                                logger.debug("Done wait()ing on resourcesLock, but active={}, listener won't be restarted now", active);
                            }
                        }
                    }
                } else if ( action == ACTION.SHUTDOWN ) {
                    // stop processing
                    logger.debug("Action is SHUTDOWN, going inactive");
                    active = false;
                }

            }
            this.listener.suspend();
        } catch ( final Exception fatal) {
            logger.error("An unexpected error occured in the installer task. Installer is stopped now!", fatal);
        } finally {
            this.backgroundThread = null;
        }
        logger.debug("Main background thread ends");
    }

    /**
     * Wake up the run cycle.
     */
    private void wakeUp() {
        logger.debug("wakeUp called");
        this.listener.start();
        synchronized (this.resourcesLock) {
            this.resourcesLock.notify();
        }
    }

    /**
     * Checks if new resources are available.
     * This method should only be invoked from within a synchronized (newResources) block!
     */
    private boolean hasNewResources() {
        return !this.newResources.isEmpty()
            || !this.newResourcesSchemes.isEmpty()
            || !this.urlsToRemove.isEmpty()
            || !this.updateInfos.isEmpty();
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
     * Create new installable resources for all installable resources.
     * The new versions has a set resource type.
     */
    private List<InternalResource> createResources(final String scheme,
            final InstallableResource[] resources) {
        checkScheme(scheme);
        List<InternalResource> createdResources = null;
        if ( resources != null && resources.length > 0 ) {
            createdResources = new ArrayList<>();
            for(final InstallableResource r : resources ) {
                try {
                    final InternalResource rr = InternalResource.create(scheme, r);
                    createdResources.add(rr);
                    logger.debug("Registering new resource: {}", rr);
                } catch (final IOException ioe) {
                    logger.warn("Cannot create InternalResource (resource will be ignored):" + r, ioe);
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
    @Override
    public void updateResources(final String scheme,
            final InstallableResource[] resources,
            final String[] ids) {
        this.listener.start();
        try {
            final List<InternalResource> updatedResources = createResources(scheme, resources);

            synchronized ( this.resourcesLock ) {
                if ( updatedResources != null && updatedResources.size() > 0 ) {
                    this.newResources.addAll(updatedResources);
                    final Set<String> newUrls = new HashSet<>();
                    for(final InternalResource rsrc : updatedResources) {
                        newUrls.add(rsrc.getURL());
                    }
                    // now remove this from urlsToRemove
                    final Iterator<String> urlIter = this.urlsToRemove.iterator();
                    while ( urlIter.hasNext() && !newUrls.isEmpty() ) {
                        final String url = urlIter.next();
                        if ( newUrls.remove(url) ) {
                            urlIter.remove();
                        }
                    }
                }
                if ( ids != null && ids.length > 0 ) {
                    final Set<String> removedUrls = new HashSet<>();
                    for(final String id : ids) {
                        final String url = scheme + ':' + id;
                        // Will mark all resources which have r's URL as uninstallable
                        this.urlsToRemove.add(url);
                        removedUrls.add(url);
                    }
                    // now update newResources
                    final Iterator<InternalResource> rsrcIter = this.newResources.iterator();
                    while ( rsrcIter.hasNext() && !removedUrls.isEmpty() ) {
                        final InternalResource rsrc = rsrcIter.next();
                        if ( removedUrls.remove(rsrc.getURL()) ) {
                            if ( rsrc.getPrivateCopyOfFile() != null ) {
                                rsrc.getPrivateCopyOfFile().delete();
                            }
                            rsrcIter.remove();
                        }
                    }
                }
            }
            this.wakeUp();
        } finally {
            // we simply close all input streams now
            this.closeInputStreams(resources);
        }
    }

    /**
     * @see org.apache.sling.installer.api.OsgiInstaller#registerResources(java.lang.String, org.apache.sling.installer.api.InstallableResource[])
     */
    @Override
    public void registerResources(final String scheme, final InstallableResource[] resources) {
        this.listener.start();
        try {
            List<InternalResource> incomingResources = this.createResources(scheme, resources);
            if ( incomingResources == null ) {
                // create empty list to make processing easier
                incomingResources = new ArrayList<>();
            }
            logger.debug("Registered new resource scheme: {}", scheme);
            synchronized (this.resourcesLock) {
                this.newResourcesSchemes.put(scheme, incomingResources);

                // Update new/removed resources
                final String prefix = scheme + ':';

                // newResources are the ones that arrived (via updateResources IIUC)
                // since the last call to this method. Here we remove all newResources
                // that match our prefix, as the incoming ones replace them
                final Iterator<InternalResource> rsrcIter = this.newResources.iterator();
                while ( rsrcIter.hasNext() ) {
                    final InternalResource rsrc = rsrcIter.next();
                    if ( rsrc.getURL().startsWith(prefix) ) {
                        prepareToRemove(rsrc, incomingResources);
                        rsrcIter.remove();
                    }
                }

                // removed urls
                final Iterator<String> urlIter = this.urlsToRemove.iterator();
                while ( urlIter.hasNext() ) {
                    final String url = urlIter.next();
                    if ( url.startsWith(prefix) ) {
                        urlIter.remove();
                    }
                }
            }
            this.wakeUp();
        } finally {
            // we simply close all input streams now
            this.closeInputStreams(resources);
        }
    }

    /** When a resource from "incoming" is about to replace "existing", we might need to transfer their private
     *  data file, or delete it if it's not needed anymore.
     */
    private void prepareToRemove(InternalResource existing, Collection<InternalResource> incoming) {
        if(existing.getPrivateCopyOfFile() != null) {
            for(final InternalResource r : incoming) {
                if(r.getURL().equals(existing.getURL())) {
                    // We have a resource r in "incoming" that's the same as "existing"
                    if(r.getPrivateCopyOfFile() == null) {
                        // New one has not data file, use the existing one
                        logger.debug("{} has no private data file, using the one from {}", r.getURL(), existing.getURL());
                        r.setPrivateCopyOfFile(existing.getPrivateCopyOfFile());
                        existing.setPrivateCopyOfFile(null);
                    } else if(r.getPrivateCopyOfFile().equals(existing.getPrivateCopyOfFile())) {
                        logger.debug("{} has same private data file as existing resource, keeping it", r.getURL());
                        existing.setPrivateCopyOfFile(null);
                    }
                    break;
                }
            }

            if(existing.getPrivateCopyOfFile() != null) {
                logger.debug("Private data file not needed anymore, deleting it: {}", existing.getURL());
                existing.getPrivateCopyOfFile().delete();
            }
        }
    }

    private void mergeNewlyRegisteredResources() {
        synchronized ( this.resourcesLock ) {
            for(final Map.Entry<String, List<InternalResource>> entry : this.newResourcesSchemes.entrySet()) {
                final String scheme = entry.getKey();
                final List<InternalResource> registeredResources = entry.getValue();

                logger.debug("Processing set of new resources with scheme {}", scheme);

                // set all previously found resources that are not available anymore to uninstall
                // if they have been installed - remove resources with a different state
                for(final String entityId : this.persistentList.getEntityIds()) {
                    final EntityResourceList group = this.persistentList.getEntityResourceList(entityId);

                    final List<TaskResource> toRemove = new ArrayList<>();
                    boolean first = true;
                    for(final TaskResource r : group.listResources()) {
                        if ( r.getScheme().equals(scheme) ) {
                            logger.debug("Checking {}", r);
                            // search if we have a new entry with the same url
                            boolean found = false;
                            if ( registeredResources != null ) {
                                final Iterator<InternalResource> m = registeredResources.iterator();
                                while ( !found && m.hasNext() ) {
                                    final InternalResource testResource = m.next();
                                    found = testResource.getURL().equals(r.getURL());
                                }
                            }
                            if ( !found) {
                                logger.debug("Resource {} seems to be removed.", r);
                                if ( first && (r.getState() == ResourceState.INSTALLED
                                        ||  r.getState() == ResourceState.INSTALL) ) {
                                    ((RegisteredResourceImpl)r).setState(ResourceState.UNINSTALL, null);
                                } else {
                                    toRemove.add(r);
                                }
                            }
                        }
                        first = false;
                    }
                    for(final TaskResource rr : toRemove) {
                        this.persistentList.remove(rr.getURL());
                    }
                }
                if ( registeredResources != null ) {
                    this.newResources.addAll(registeredResources);
                }
            }
            this.newResourcesSchemes.clear();
            this.mergeNewResources();

            printResources("Merged");
            // persist list
            this.persistentList.save();
        }
    }

    /**
     * Process new resources and deleted resources and
     * merge them with existing resources.
     */
    private void mergeNewResources() {
        // if we have new resources we have to sync them
        if ( newResources.size() > 0 ) {
            logger.debug("Added set of {} new resources: {}",
                    new Object[] {newResources.size(), newResources});

            for(final InternalResource r : newResources) {
                this.persistentList.addOrUpdate(r);
            }
            newResources.clear();
        }
        // Mark resources for removal according to urlsToRemove
        if (!urlsToRemove.isEmpty()) {
            logger.debug("Removing set of {} resources: {}",
                    new Object[] {urlsToRemove.size(), urlsToRemove});
            for(final String url : urlsToRemove ) {
                this.persistentList.remove(url);
            }
            urlsToRemove.clear();
        }
    }

    private void printResources(String hint) {
        if ( !logger.isDebugEnabled() ) {
            return;
        }
        int counter = 0;
        final StringBuilder sb = new StringBuilder();
        sb.append(hint);
        sb.append(" Resources={\n");
        for(final String id : this.persistentList.getEntityIds() ) {
            sb.append("- ").append(hint).append(" RegisteredResource ");
            sb.append(id);
            sb.append("\n    RegisteredResource.info=[");
            String sep = "";
            for(final RegisteredResource rr : this.persistentList.getEntityResourceList(id).listResources()) {
                sb.append(sep);
                sep=", ";
                sb.append(rr);
                counter++;
            }
            sb.append("]\n");
        }
        sb.append("} (").append(hint).append("): ").append(counter).append(" RegisteredResources\n");
        logger.debug(sb.toString());
    }

    /**
     * Compute OSGi tasks based on our resources, and add to supplied list of tasks.
     */
    private SortedSet<InstallTask> computeTasks() {
        final SortedSet<InstallTask> tasks = new TreeSet<>();

        // Walk the list of entities, and create appropriate OSGi tasks for each group
        final List<InstallTaskFactory> services = this.factoryTracker.getSortedServices();
        if ( services.size() > 0 ) {
            for(final String entityId : this.persistentList.getEntityIds()) {
                final EntityResourceList group = this.persistentList.getEntityResourceList(entityId);
                // Check the first resource in each group
                final TaskResource toActivate = group.getActiveResource();
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
    private InstallTask getTask(List<InstallTaskFactory> services,
            final TaskResourceGroup rrg) {
        InstallTask result = null;

        for(final InstallTaskFactory factory : services) {
            if ( factory != null ) {
                try {
                    result = factory.createTask(rrg);
                } catch ( final Exception fatal ) {
                    logger.error("An exception occured while creating a task for " + rrg.getActiveResource()+ ". Resource will be ignored.", fatal);
                    result = new ChangeStateTask(rrg, ResourceState.IGNORED);
                }
                if ( result != null ) {
                    break;
                }
            }
        }
        return result;
    }

    private enum ACTION {
        SLEEP,
        SHUTDOWN,
        CYCLE
    };

    /**
     * Execute all tasks
     * @param tasks The tasks to executed.
     * @return The action to perform after the execution.
     */
    private ACTION executeTasks(final SortedSet<InstallTask> tasks) {
        if (this.switchStartLevel && this.hasBundleUpdateTask(tasks)) {
            // StartLevel service is always available
            final ServiceReference ref = ctx.getServiceReference(StartLevel.class.getName());
            final StartLevel startLevel = (StartLevel) ctx.getService(ref);
            try {
                final int targetStartLevel = this.getLowestStartLevel(tasks, startLevel);
                final int currentStartLevel = startLevel.getStartLevel();
                if (targetStartLevel < currentStartLevel) {
                    auditLogger.info("Switching to start level {}", targetStartLevel);
                    try {
                        startLevel.setStartLevel(targetStartLevel);
                        // now we have to wait until the start level is reached
                        while (startLevel.getStartLevel() > targetStartLevel) {
                            try {
                                Thread.sleep(300);
                            } catch (final InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }

                        }

                        return doExecuteTasks(tasks);

                    } finally {
                        // restore old start level in any case
                        startLevel.setStartLevel(currentStartLevel);
                        auditLogger.info("Switching back to start level {} after performing the required " +
                                "installation tasks", currentStartLevel);

                    }
                }

            } finally {
                ctx.ungetService(ref);
            }
        }
        return doExecuteTasks(tasks);
    }

    /**
     * Get the lowest start level for the update operation
     */
    private int getLowestStartLevel(final SortedSet<InstallTask> tasks, final StartLevel startLevel) {
        final int currentStartLevel = startLevel.getStartLevel();
        int startLevelToTarget = currentStartLevel;
        for(final InstallTask task : tasks) {
            if (task instanceof BundleUpdateTask){
                final Bundle b = ((BundleUpdateTask) task).getBundle();
                if (b != null) {
                    try {
                        final int bundleStartLevel = startLevel.getBundleStartLevel(b) - 1;
                        if (bundleStartLevel < startLevelToTarget){
                            startLevelToTarget = bundleStartLevel;
                        }
                    } catch ( final IllegalArgumentException iae) {
                        // ignore - bundle is uninstalled
                    }
                }
            }
        }
        // check installer start level
        final int ownStartLevel = startLevel.getBundleStartLevel(ctx.getBundle());
        if ( ownStartLevel > startLevelToTarget ) {
            // we don't want to disable ourselves
            startLevelToTarget = ownStartLevel;
        }
        return startLevelToTarget;
    }

    /**
     * Check if a bundle update task will be executed.
     */
    private boolean hasBundleUpdateTask(final SortedSet<InstallTask> tasks) {
        boolean result = false;
        for(final InstallTask task : tasks){
            if ( task.isAsynchronousTask() ) {
                result = false; // async task is executed immediately#
                break;
            } else if (task instanceof BundleUpdateTask){
                result = true;
            }
        }
        return result;
    }

    /**
     * Execute all tasks
     * @param tasks The tasks to executed.
     * @return The action to perform after the execution.
     */
    private ACTION doExecuteTasks(final SortedSet<InstallTask> tasks) {
        if ( !tasks.isEmpty() ) {

            final InstallationContext ctx = new InstallationContext() {

                @SuppressWarnings("deprecation")
                @Override
                public void addTaskToNextCycle(final InstallTask t) {
                    logger.warn("Deprecated method addTaskToNextCycle was called. Task will be executed in this cycle instead: {}", t);
                    synchronized ( tasks ) {
                        tasks.add(t);
                    }
                }

                @Override
                public void addTaskToCurrentCycle(final InstallTask t) {
                    logger.debug("Adding {}task to current cycle: {}", t.isAsynchronousTask() ? "async " : "", t);
                    synchronized ( tasks ) {
                        tasks.add(t);
                    }
                }

                @SuppressWarnings("deprecation")
                @Override
                public void addAsyncTask(final InstallTask t) {
                    if ( t.isAsynchronousTask() ) {
                        logger.warn("Deprecated method addAsyncTask was called: {}", t);
                        this.addTaskToCurrentCycle(t);
                    } else {
                        logger.warn("Deprecated method addAsyncTask is called with non async task(!): {}", t);
                        this.addTaskToCurrentCycle(new AsyncWrapperInstallTask(t));
                    }
                }

                @Override
                public void log(final String message, final Object... args) {
                    auditLogger.info(message, args);
                }

                @Override
                public void asyncTaskFailed(final InstallTask t) {
                    // persist all changes and retry restart
                    // remove attribute
                    logger.debug("asyncTaskFailed: {}", t);
                    if ( t.getResource() != null ) {
                        t.getResource().setAttribute(InstallTask.ASYNC_ATTR_NAME, null);
                    }
                    persistentList.save();
                    synchronized ( resourcesLock ) {
                        if ( !active ) {
                            logger.debug("Restarting background thread from asyncTaskFailed");
                            active = true;
                            startBackgroundThread();
                        } else {
                            logger.debug("active={}, no need to restart background thread", active);
                        }
                    }
                }
            };
            while (this.active && !tasks.isEmpty()) {
                InstallTask task = null;
                synchronized (tasks) {
                    task = tasks.first();
                    tasks.remove(task);
                }
                // async tasks are executed "immediately"
                if ( task.isAsynchronousTask() ) {
                    logger.debug("Executing async task: {}", task);
                    // set attribute
                    final Integer oldValue;
                    if ( task.getResource() != null ) {
                        oldValue = (Integer)task.getResource().getAttribute(InstallTask.ASYNC_ATTR_NAME);
                        final Integer newValue;
                        if ( oldValue == null ) {
                            newValue = 1;
                        } else {
                            newValue = oldValue + 1;
                        }
                        task.getResource().setAttribute(InstallTask.ASYNC_ATTR_NAME, newValue);
                    } else {
                        oldValue = null;
                    }
                    // save new state
                    this.cleanupInstallableResources();
                    final InstallTask aSyncTask = task;
                    final String threadName = "BackgroundTaskThread" + backgroundTaskCounter.incrementAndGet();
                    final Thread t = new Thread(threadName) {

                        @Override
                        public void run() {
                            logger.debug("Starting background thread {} to execute {}",
                                    Thread.currentThread().getName(),
                                    aSyncTask);
                            try {
                                Thread.sleep(2000L);
                            } catch (final InterruptedException ie) {
                                // ignore
                            }
                            // reset attribute
                            if ( aSyncTask.getResource() != null ) {
                                aSyncTask.getResource().setAttribute(InstallTask.ASYNC_ATTR_NAME, oldValue);
                            }
                            aSyncTask.execute(ctx);
                            logger.debug("Background thread {} ends",  Thread.currentThread().getName());
                        }
                    };
                    t.start();
                    return ACTION.SHUTDOWN;
                }
                try {
                    logger.debug("Executing task: {}", task);
                    task.execute(ctx);
                } catch (final Throwable t) {
                    logger.error("Uncaught exception during task execution!", t);
                }
            }
            // save new state
            final boolean newCycle = this.cleanupInstallableResources();
            if ( newCycle ) {
                return ACTION.CYCLE;
            }

        }
        return ACTION.SLEEP;
    }

    /**
     * Clean up and compact.
     * @return <code>true</code> if another cycle should be started.
     */
    private boolean cleanupInstallableResources() {
        synchronized ( this.resourcesLock ) {
            final boolean result = this.persistentList.compact();
            this.persistentList.save();
            printResources("Compacted");
            logger.debug("cleanupInstallableResources returns {}", result);
            return result;
        }
    }

    /**
     * Invoke the transformers on the resources.
     */
    private void transformResources() {
        boolean changed = false;
        final List<ServiceReference> serviceRefs = this.transformerTracker.getSortedServiceReferences();

        if ( serviceRefs.size() > 0 ) {
            synchronized ( this.resourcesLock ) {
                // Walk the list of unknown resources and invoke all transformers
                int index = 0;
                final List<RegisteredResource> unknownList = this.persistentList.getUntransformedResources();

                while ( index < unknownList.size() ) {
                    final RegisteredResource resource = unknownList.get(index);
                    for(final ServiceReference reference : serviceRefs) {
                        final Long id = (Long)reference.getProperty(Constants.SERVICE_ID);
                        // check if this transformer has already been invoked for the resource
                        final String transformers = (String)((RegisteredResourceImpl)resource).getAttribute(ResourceTransformer.class.getName());
                        if ( id == null ||
                                (transformers != null && transformers.contains(":" + id + ':'))) {
                            continue;
                        }
                        final ResourceTransformer transformer = (ResourceTransformer) this.transformerTracker.getService(reference);
                        if ( transformer != null ) {
                            try {
                                final TransformationResult[] result = transformer.transform(resource);
                                final String newTransformers = (transformers == null ? ":" + id + ':' : transformers + id + ':');
                                ((RegisteredResourceImpl)resource).setAttribute(ResourceTransformer.class.getName(), newTransformers);
                                if ( logger.isDebugEnabled() ) {
                                    logger.debug("Invoked transformer {} on {} : {}",
                                            new Object[] {transformer, resource, Arrays.toString(result)});
                                }
                                if ( result != null && result.length > 0 ) {
                                    this.persistentList.transform(resource, result);
                                    changed = true;
                                    index--;
                                    break;
                                }
                            } catch (final Throwable t) {
                                logger.error("Uncaught exception during resource transformation!", t);
                            }
                        }
                    }
                    index++;
                }
            }
            if ( changed ) {
                this.persistentList.save();
                printResources("Transformed");
            }
        }
    }

    private void checkSatisfied() {
        synchronized ( this.resourcesLock ) {
            if ( !this.satisfied ) {
                this.satisfied = true;
                if ( this.ctx.getProperty(PROP_REQUIRED_SERVICES) != null ) {
                    final String[] reqs = this.ctx.getProperty(PROP_REQUIRED_SERVICES).split(",");
                    this.satisfied = true;
                    for(final String val : reqs) {
                        if ( val.startsWith("resourcetransformer:") ) {
                            final String name = val.substring(20);

                            this.satisfied = this.transformerTracker.check(ResourceTransformer.NAME, name);

                        } else if ( val.startsWith("installtaskfactory:") ) {
                            final String name = val.substring(19);

                            this.satisfied = this.factoryTracker.check(InstallTaskFactory.NAME, name);

                        } else {
                            logger.warn("Invalid requirements for installer: {}", val);
                        }
                    }
                }
            }
        }
    }

    /**
     * @see org.apache.sling.installer.api.tasks.RetryHandler#scheduleRetry()
     */
    @Override
    public void scheduleRetry() {
        logger.debug("scheduleRetry called");
        this.listener.start();
        synchronized ( this.resourcesLock ) {
            this.retryDuringTaskExecution = true;
            this.checkSatisfied();
        }
        this.wakeUp();
    }

    private static final class UpdateInfo {
        public ResourceData data;
        public Dictionary<String, Object> dict;
        public String resourceType;
        public String entityId;
        public Map<String, Object> attributes;
    }

    /**
     * Store the changes in an internal queue, the queue is processed in {@link #processUpdateInfos()}.
     * @see org.apache.sling.installer.api.ResourceChangeListener#resourceAddedOrUpdated(java.lang.String, java.lang.String, java.io.InputStream, java.util.Dictionary, Map)
     */
    @Override
    public void resourceAddedOrUpdated(final String resourceType,
            final String entityId,
            final InputStream is,
            final Dictionary<String, Object> dict,
            final Map<String, Object> attributes) {
        try {
            final UpdateInfo ui = new UpdateInfo();
            ui.data = ResourceData.create(is, dict);
            ui.resourceType = resourceType;
            ui.dict = dict;
            ui.entityId = entityId;
            ui.attributes = attributes;

            synchronized ( this.resourcesLock ) {
                updateInfos.add(ui);
                this.wakeUp();
            }
        } catch (final IOException ioe) {
            logger.error("Unable to handle resource add or update of " + resourceType + ':' + entityId, ioe);
        } finally {
            // always close the input stream!
            if ( is != null ) {
                try {
                    is.close();
                } catch (final IOException ignore) {
                    // ignore
                }
            }
        }
    }

    /**
     * Store the changes in an internal queue, the queue is processed in {@link #processUpdateInfos()}.
     * @see org.apache.sling.installer.api.ResourceChangeListener#resourceRemoved(java.lang.String, java.lang.String)
     */
    @Override
    public void resourceRemoved(final String resourceType, String resourceId) {
        final UpdateInfo ui = new UpdateInfo();
        ui.resourceType = resourceType;
        ui.entityId = resourceId;

        synchronized ( this.resourcesLock ) {
            updateInfos.add(ui);
            this.wakeUp();
        }
    }

    /**
     * Process the internal queue of updates
     * @see org.apache.sling.installer.api.ResourceChangeListener#resourceAddedOrUpdated(java.lang.String, java.lang.String, java.io.InputStream, java.util.Dictionary, Map)
     * @see org.apache.sling.installer.api.ResourceChangeListener#resourceRemoved(java.lang.String, java.lang.String)
     */
    private void processUpdateInfos() {
        final List<UpdateInfo> infos = new ArrayList<>();
        synchronized ( resourcesLock ) {
            infos.addAll(updateInfos);
            updateInfos.clear();
        }
        for(final UpdateInfo info : infos) {
            if ( info.data != null ) {
                this.internalResourceAddedOrUpdated(info.resourceType, info.entityId, info.data, info.dict, info.attributes);
            } else {
                this.internalResourceRemoved(info.resourceType, info.entityId);
            }
        }
    }

    private boolean handleExternalUpdateWithoutWriteBack(final EntityResourceList erl) {
        final TaskResource tr = erl.getFirstResource();

        // if this is an update but the change should not be persisted, we have to change
        // the state to IGNORED
        // or to UNINSTALLED if state is UNINSTALL
        if ( tr.getState() == ResourceState.UNINSTALLED || tr.getState() == ResourceState.IGNORED ) {
            // ignore
            return false;
        } else if ( tr.getState() == ResourceState.UNINSTALL ) {
            erl.setFinishState(ResourceState.UNINSTALLED, null);
            return true;
        } else {
            erl.setForceFinishState(ResourceState.IGNORED, null);
            return true;
        }
    }
    /**
     * Handle external addition or update of a resource
     * @see org.apache.sling.installer.api.ResourceChangeListener#resourceAddedOrUpdated(java.lang.String, java.lang.String, java.io.InputStream, java.util.Dictionary, Map)
     */
    private void internalResourceAddedOrUpdated(final String resourceType,
            final String entityId,
            final ResourceData data,
            final Dictionary<String, Object> dict,
            final Map<String, Object> attributes) {
        final String key = resourceType + ':' + entityId;
        final boolean persistChange = (attributes != null ? PropertiesUtil.toBoolean(attributes.get(ResourceChangeListener.RESOURCE_PERSIST), true) : true);
        try {
            boolean compactAndSave = false;
            boolean done = false;

            synchronized ( this.resourcesLock ) {
                final EntityResourceList erl = this.persistentList.getEntityResourceList(key);
                logger.debug("Added or updated {} : {}", key, erl);

                // we first check for update
                if ( erl != null && erl.getFirstResource() != null ) {
                    // check digest for dictionaries
                    final TaskResource tr = erl.getFirstResource();
                    if ( dict != null ) {
                        final String digest = FileDataStore.computeDigest(dict);
                        if ( tr.getDigest().equals(digest) ) {
                            if ( tr.getState() == ResourceState.INSTALLED  ) {
                                logger.debug("Resource did not change {}", key);
                            } else if ( tr.getState() == ResourceState.INSTALL
                                || tr.getState() == ResourceState.IGNORED ) {
                                erl.setForceFinishState(ResourceState.INSTALLED, null);
                                compactAndSave = true;
                            }
                            done = true;
                        }
                    }

                    final UpdateHandler handler;
                    if ( !done && persistChange ) {
                        handler = this.findHandler(tr.getScheme());
                        if ( handler == null ) {
                            logger.debug("No handler found to handle update of resource with scheme {}", tr.getScheme());
                        }
                    } else {
                        handler = null;
                    }

                    if ( !done && handler == null ) {
                        compactAndSave = this.handleExternalUpdateWithoutWriteBack(erl);
                        done = true;
                    }

                    if ( !done ) {
                        final InputStream localIS = data.getInputStream();
                        try {
                            final UpdateResult result = (localIS == null ? handler.handleUpdate(resourceType, entityId, tr.getURL(), data.getDictionary(), attributes)
                                    : handler.handleUpdate(resourceType, entityId, tr.getURL(), localIS, attributes));
                            if ( result != null ) {
                                if ( !result.getURL().equals(tr.getURL()) && !result.getResourceIsMoved() ) {
                                    // resource has been added!
                                    final InternalResource internalResource = new InternalResource(result.getScheme(),
                                            result.getResourceId(),
                                            null,
                                            data.getDictionary(),
                                            (data.getDictionary() != null ? InstallableResource.TYPE_PROPERTIES : InstallableResource.TYPE_FILE),
                                            data.getDigest(result.getURL(), result.getDigest()),
                                            result.getPriority(),
                                            data.getDataFile(),
                                            null);
                                    final RegisteredResource rr = this.persistentList.addOrUpdate(internalResource);
                                    final TransformationResult transRes = new TransformationResult();
                                    // use the old entity id
                                    final int pos = erl.getResourceId().indexOf(':');
                                    transRes.setId(erl.getResourceId().substring(pos + 1));
                                    transRes.setResourceType(resourceType);
                                    if ( attributes != null ) {
                                        transRes.setAttributes(attributes);
                                    }
                                    this.persistentList.transform(rr, new TransformationResult[] {
                                            transRes
                                    });
                                    final EntityResourceList newGroup = this.persistentList.getEntityResourceList(key);
                                    newGroup.setFinishState(ResourceState.INSTALLED, null);
                                    newGroup.compact();
                                } else {
                                    // resource has been updated or moved
                                    ((RegisteredResourceImpl)tr).update(
                                            data.getDataFile(), data.getDictionary(),
                                            data.getDigest(result.getURL(), result.getDigest()),
                                            result.getPriority(),
                                            result.getURL());
                                    erl.setForceFinishState(ResourceState.INSTALLED, null);
                                }
                                compactAndSave = true;
                            } else {
                                // handler does not persist
                                compactAndSave = this.handleExternalUpdateWithoutWriteBack(erl);
                            }
                        } finally {
                            if ( localIS != null ) {
                                // always close the input stream!
                                try {  localIS.close(); } catch (final IOException ignore) {
                                    // ignore
                                }
                            }
                        }
                        done = true;
                    }
                }

                if ( !done ) {
                    // create
                    final List<UpdateHandler> handlerList = this.updateHandlerTracker.getSortedServices();
                    for(final UpdateHandler handler : handlerList) {
                        final InputStream localIS = data.getInputStream();
                        try {
                            final UpdateResult result = (localIS == null ? handler.handleUpdate(resourceType, entityId, null, data.getDictionary(), attributes)
                                    : handler.handleUpdate(resourceType, entityId, null, localIS, attributes));
                            if ( result != null ) {
                                final InternalResource internalResource = new InternalResource(result.getScheme(),
                                        result.getResourceId(),
                                        null,
                                        data.getDictionary(),
                                        (data.getDictionary() != null ? InstallableResource.TYPE_PROPERTIES : InstallableResource.TYPE_FILE),
                                        data.getDigest(result.getURL(), result.getDigest()),
                                        result.getPriority(),
                                        data.getDataFile(),
                                        null);
                                final RegisteredResource rr = this.persistentList.addOrUpdate(internalResource);
                                final TransformationResult transRes = new TransformationResult();
                                transRes.setId(entityId);
                                transRes.setResourceType(resourceType);
                                if ( attributes != null ) {
                                    transRes.setAttributes(attributes);
                                }
                                this.persistentList.transform(rr, new TransformationResult[] {
                                        transRes
                                });
                                final EntityResourceList newGroup = this.persistentList.getEntityResourceList(key);
                                newGroup.setFinishState(ResourceState.INSTALLED);
                                newGroup.compact();
                                compactAndSave = true;
                                done = true;
                                break;
                            }
                        } finally {
                            if ( localIS != null ) {
                                // always close the input stream!
                                try {
                                    localIS.close();
                                } catch (final IOException ignore) {
                                    // ignore
                                }
                            }
                        }
                    }
                    if ( !done ) {
                        logger.debug("No handler found to handle creation of resource {}", key);
                    }
                }
                if ( compactAndSave ) {
                    if ( erl != null ) {
                        erl.compact();
                    }
                    this.persistentList.save();
                }
            }
        } catch (final IOException ioe) {
            logger.error("Unable to handle resource add or update of " + key, ioe);
        }
    }

    /**
     * Handle external removal a resource
     * @see org.apache.sling.installer.api.ResourceChangeListener#resourceRemoved(java.lang.String, java.lang.String)
     */
    private void internalResourceRemoved(final String resourceType, final String entityId) {

        String key = resourceType + ':' + entityId;
        synchronized ( this.resourcesLock ) {
            final EntityResourceList erl = this.persistentList.getEntityResourceList(key);
            logger.debug("Removed {} : {}", key, erl);
            // if this is not registered at all, we can simply ignore this
            if ( erl != null ) {
                final String resourceId = erl.getResourceId();
                key = resourceType + ':' + resourceId;
                final TaskResource tr = erl.getFirstResource();
                if ( tr != null ) {
                    if ( tr.getState() == ResourceState.IGNORED ) {
                        // if it has been ignored before, we activate it now again!
                        // but only if it is not a template
                        if ( tr.getDictionary() == null
                             || tr.getDictionary().get(InstallableResource.RESOURCE_IS_TEMPLATE) == null ) {
                            ((RegisteredResourceImpl)tr).setState(ResourceState.INSTALL, null);
                            this.persistentList.save();
                        }
                    } else if ( tr.getState() == ResourceState.UNINSTALLED ) {
                        // it has already been removed - nothing do to
                    } else {
                        final UpdateHandler handler = findHandler(tr.getScheme());
                        if ( handler == null ) {
                            // set to ignored
                            String message = MessageFormat.format("No handler found to handle resource with scheme {0}",
                                    tr.getScheme());
                            logger.debug(message);
                            ((RegisteredResourceImpl) tr).setState(ResourceState.IGNORED, message);
                        } else {
                            // we don't need to check the result, we just check if a result is returned
                            if ( handler.handleRemoval(resourceType, resourceId, tr.getURL()) != null ) {
                                erl.setForceFinishState(ResourceState.UNINSTALLED, null);
                                erl.compact();
                            } else {
                                // set to ignored
                                String message = MessageFormat
                                        .format("No handler found to handle removal of resource with scheme {0}", tr.getScheme());
                                logger.debug(message);
                                ((RegisteredResourceImpl) tr).setState(ResourceState.IGNORED, message);
                            }
                        }
                        this.persistentList.save();
                    }
                }
            }
        }
    }

    /**
     * Search a handler for the scheme.
     */
    private UpdateHandler findHandler(final String scheme) {
        final List<ServiceReference> references = this.updateHandlerTracker.getSortedServiceReferences();
        for(final ServiceReference ref : references) {
            final String[] supportedSchemes = PropertiesUtil.toStringArray(ref.getProperty(UpdateHandler.PROPERTY_SCHEMES));
            if ( supportedSchemes != null ) {
                for(final String support : supportedSchemes ) {
                    if ( scheme.equals(support) ) {
                        return (UpdateHandler) this.updateHandlerTracker.getService(ref);
                    }
                }
            }
        }
        return null;
    }

    /**
     * @see org.apache.sling.installer.api.info.InfoProvider#getInstallationState()
     */
    @Override
    public InstallationState getInstallationState() {
        synchronized ( this.resourcesLock ) {
            final InstallationState state = new InstallationState() {

                private final List<ResourceGroup> activeResources = new ArrayList<>();
                private final List<ResourceGroup> installedResources = new ArrayList<>();
                private final List<RegisteredResource> untransformedResources = new ArrayList<>();

                @Override
                public List<ResourceGroup> getActiveResources() {
                    return activeResources;
                }

                @Override
                public List<ResourceGroup> getInstalledResources() {
                    return installedResources;
                }

                @Override
                public List<RegisteredResource> getUntransformedResources() {
                    return untransformedResources;
                }

                @Override
                public String toString() {
                    return "InstallationState[active resources: " + this.activeResources +
                            ", installed resources: " + this.installedResources +
                            ", untransformed resources: " + this.untransformedResources + "]";
                }
            };

            for(final String entityId : this.persistentList.getEntityIds()) {
                if ( !this.persistentList.isSpecialEntityId(entityId) ) {
                    final EntityResourceList group = this.persistentList.getEntityResourceList(entityId);

                    final String alias = group.getAlias();
                    final List<Resource> resources = new ArrayList<>();
                    boolean first = true;
                    boolean isActive = false;
                    for(final TaskResource tr : group.getResources()) {
                        final ResourceState resourceState = tr.getState();
                        if ( first ) {
                            if ( resourceState == ResourceState.INSTALL || resourceState == ResourceState.UNINSTALL ) {
                                isActive = true;
                            }
                            first = false;
                        }
                        resources.add(new Resource() {

                            @Override
                            public String getScheme() {
                                return tr.getScheme();
                            }

                            @Override
                            public String getURL() {
                                return tr.getURL();
                            }

                            @Override
                            public String getType() {
                                return tr.getType();
                            }

                            @Override
                            public InputStream getInputStream() throws IOException {
                                return tr.getInputStream();
                            }

                            @Override
                            public Dictionary<String, Object> getDictionary() {
                                return tr.getDictionary();
                            }

                            @Override
                            public String getDigest() {
                                return tr.getDigest();
                            }

                            @Override
                            public int getPriority() {
                                return tr.getPriority();
                            }

                            @Override
                            public String getEntityId() {
                                return tr.getEntityId();
                            }

                            @Override
                            public ResourceState getState() {
                                return resourceState;
                            }

                            @Override
                            public Version getVersion() {
                                return tr.getVersion();
                            }

                            @Override
                            public long getLastChange() {
                                return ((RegisteredResourceImpl)tr).getLastChange();
                            }

                            @Override
                            public Object getAttribute(final String key) {
                                return tr.getAttribute(key);
                            }

                            @Override
                            @CheckForNull
                            public String getError() {
                                return tr.getError();
                            }

                            @Override
                            public String toString() {
                                return "resource[entityId=" + getEntityId() +
                                        ", scheme=" + getScheme() +
                                        ", url=" + getURL() +
                                        ", type=" + getType() +
                                        ", error=" + getError() +
                                        ", state=" + getState() +
                                        ", version=" + getVersion() +
                                        ", lastChange=" + getLastChange() +
                                        ", priority=" + getPriority() +
                                        ", digest=" + getDigest() +
                                        "]";
                            }
                        });
                    }
                    final ResourceGroup rg = new ResourceGroup() {

                        @Override
                        public List<Resource> getResources() {
                            return resources;
                        }

                        @Override
                        public String getAlias() {
                            return alias;
                        }

                        @Override
                        public String toString() {
                            return "group[" + resources + "]";
                        }
                    };
                    if ( isActive ) {
                        state.getActiveResources().add(rg);
                    } else {
                        state.getInstalledResources().add(rg);
                    }
                }
            }

            Collections.sort(state.getActiveResources(), COMPARATOR);
            Collections.sort(state.getInstalledResources(), COMPARATOR);

            state.getUntransformedResources().addAll(this.persistentList.getUntransformedResources());

            return state;
        }
    }

    private static final Comparator<ResourceGroup> COMPARATOR = new Comparator<ResourceGroup>() {

        @Override
        public int compare(ResourceGroup o1, ResourceGroup o2) {
            RegisteredResource r1 = null;
            RegisteredResource r2 = null;
            if ( o1.getResources().size() > 0 ) {
                r1 = o1.getResources().iterator().next();
            }
            if ( o2.getResources().size() > 0 ) {
                r2 = o2.getResources().iterator().next();
            }
            int result;
            if ( r1 == null && r2 == null ) {
                result = 0;
            } else if ( r1 == null ) {
                result = -1;
            } else if ( r2 == null ) {
                result = 1;
            } else {
                result = r1.getType().compareTo(r2.getType());
                if ( result == 0 ) {
                    result = r1.getEntityId().compareTo(r2.getEntityId());
                }
            }
            return result;
        }

    };
}
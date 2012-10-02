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
import java.util.Arrays;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
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

    /** The logger */
    private final Logger logger =  LoggerFactory.getLogger(this.getClass());

    /** The audit logger */
    private final Logger auditLogger =  LoggerFactory.getLogger("org.apache.sling.audit.osgi.installer");

    /** The bundle context. */
    private final BundleContext ctx;

    /** New clients are joining through this map. */
    private final Map<String, List<InternalResource>> newResourcesSchemes = new HashMap<String, List<InternalResource>>();

    /** New resources added by clients. */
    private final List<InternalResource> newResources = new LinkedList<InternalResource>();

    /** Removed resources from clients. */
    private final Set<String> urlsToRemove = new HashSet<String>();

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
                final Thread t = this.backgroundThread;
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
        this.factoryTracker = new SortingServiceTracker<InstallTaskFactory>(ctx, InstallTaskFactory.class.getName(), this);
        this.factoryTracker.open();
        this.transformerTracker = new SortingServiceTracker<ResourceTransformer>(ctx, ResourceTransformer.class.getName(), this);
        this.transformerTracker.open();
        this.updateHandlerTracker = new SortingServiceTracker<UpdateHandler>(ctx, UpdateHandler.class.getName(), null);
        this.updateHandlerTracker.open();

        this.logger.info("Apache Sling OSGi Installer Service started.");
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
    public void run() {
        logger.debug("Main background thread starts");
        try {
            this.init();

            while (this.active) {
                this.logger.debug("Starting new installer cycle");
                this.listener.start();

                // merge potential new resources
                this.mergeNewlyRegisteredResources();

                // invoke transformers
                this.transformResources();

                // Compute tasks
                final SortedSet<InstallTask> tasks = this.computeTasks();
                // execute tasks and see if we have to stop processing
                synchronized ( this.resourcesLock ) {
                    this.retryDuringTaskExecution = false;
                }
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
                    this.active = false;
                }

            }
            this.listener.suspend();
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
        return !this.newResources.isEmpty() || !this.newResourcesSchemes.isEmpty() || !this.urlsToRemove.isEmpty();
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
            createdResources = new ArrayList<InternalResource>();
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
    public void updateResources(final String scheme,
            final InstallableResource[] resources,
            final String[] ids) {
        this.listener.start();
        try {
            final List<InternalResource> updatedResources = this.createResources(scheme, resources);

            synchronized ( this.resourcesLock ) {
                if ( updatedResources != null && updatedResources.size() > 0 ) {
                    this.newResources.addAll(updatedResources);
                    final Set<String> newUrls = new HashSet<String>();
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
                    final Set<String> removedUrls = new HashSet<String>();
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
    public void registerResources(final String scheme, final InstallableResource[] resources) {
        this.listener.start();
        try {
            List<InternalResource> registeredResources = this.createResources(scheme, resources);
            if ( registeredResources == null ) {
                // create empty list to make processing easier
                registeredResources = new ArrayList<InternalResource>();
            }
            logger.debug("Registered new resource scheme: {}", scheme);
            synchronized (this.resourcesLock) {
                this.newResourcesSchemes.put(scheme, registeredResources);

                // now update resources and removed resources and remove all for this scheme!
                final String prefix = scheme + ':';
                // added resources

                final Iterator<InternalResource> rsrcIter = this.newResources.iterator();
                while ( rsrcIter.hasNext() ) {
                    final InternalResource rsrc = rsrcIter.next();
                    if ( rsrc.getURL().startsWith(prefix) ) {
                        // check if we got the same resource
                        if ( rsrc.getPrivateCopyOfFile() != null ) {
                            boolean found = false;
                            for(final InternalResource newRsrc : registeredResources) {
                                if ( newRsrc.getURL().equals(rsrc.getURL()) && newRsrc.getPrivateCopyOfFile() == null ) {
                                    found = true;
                                    newRsrc.setPrivateCopyOfFile(rsrc.getPrivateCopyOfFile());
                                    break;
                                }
                            }
                            if ( !found ) {
                                rsrc.getPrivateCopyOfFile().delete();
                            }
                        }
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

                    final List<TaskResource> toRemove = new ArrayList<TaskResource>();
                    boolean first = true;
                    for(final TaskResource r : group.getResources()) {
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
                                    ((RegisteredResourceImpl)r).setState(ResourceState.UNINSTALL);
                                } else {
                                    toRemove.add(r);
                                }
                            }
                        }
                        first = false;
                    }
                    for(final TaskResource rr : toRemove) {
                        this.persistentList.remove(rr);
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
            for(final RegisteredResource rr : this.persistentList.getEntityResourceList(id).getResources()) {
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
        final SortedSet<InstallTask> tasks = new TreeSet<InstallTask>();

        // Walk the list of entities, and create appropriate OSGi tasks for each group
        final List<InstallTaskFactory> services = this.factoryTracker.getSortedServices();
        if ( services.size() > 0 ) {
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
    private InstallTask getTask(List<InstallTaskFactory> services,
            final TaskResourceGroup rrg) {
        InstallTask result = null;

        for(final InstallTaskFactory factory : services) {
            if ( factory != null ) {
                result = factory.createTask(rrg);
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
     */
    private ACTION executeTasks(final SortedSet<InstallTask> tasks) {
        if ( !tasks.isEmpty() ) {

            final InstallationContext ctx = new InstallationContext() {

                public void addTaskToNextCycle(final InstallTask t) {
                    logger.warn("Deprecated method addTaskToNextCycle was called. Task will be executed in this cycle instead: {}", t);
                    synchronized ( tasks ) {
                        tasks.add(t);
                    }
                }

                public void addTaskToCurrentCycle(final InstallTask t) {
                    logger.debug("Adding {}task to current cycle: {}", t.isAsynchronousTask() ? "async " : "", t);
                    synchronized ( tasks ) {
                        tasks.add(t);
                    }
                }

                public void addAsyncTask(final InstallTask t) {
                    if ( t.isAsynchronousTask() ) {
                        logger.warn("Deprecated method addAsyncTask was called: {}", t);
                        this.addTaskToCurrentCycle(t);
                    } else {
                        logger.warn("Deprecated method addAsyncTask is called with non async task(!): {}", t);
                        this.addTaskToCurrentCycle(new AsyncWrapperInstallTask(t));
                    }
                }

                public void log(final String message, final Object... args) {
                    auditLogger.info(message, args);
                }

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
        final boolean result = this.persistentList.compact();
        this.persistentList.save();
        printResources("Compacted");
        logger.debug("cleanupInstallableResources returns {}", result);
        return result;
    }

    /**
     * Invoke the transformers on the resources.
     */
    private void transformResources() {
        boolean changed = false;
        final List<ServiceReference> serviceRefs = this.transformerTracker.getSortedServiceReferences();

        if ( serviceRefs.size() > 0 ) {
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

    /**
     * @see org.apache.sling.installer.api.tasks.RetryHandler#scheduleRetry()
     */
    public void scheduleRetry() {
        logger.debug("scheduleRetry called");
        this.listener.start();
        synchronized ( this.resourcesLock ) {
            this.retryDuringTaskExecution = true;
        }
        this.wakeUp();
    }

    /**
     * @see org.apache.sling.installer.api.ResourceChangeListener#resourceAddedOrUpdated(java.lang.String, java.lang.String, java.io.InputStream, java.util.Dictionary, Map)
     */
    public void resourceAddedOrUpdated(final String resourceType,
            final String entityId,
            final InputStream is,
            final Dictionary<String, Object> dict,
            final Map<String, Object> attributes) {
        final String key = resourceType + ':' + entityId;
        try {
            final ResourceData data = ResourceData.create(is, dict);
            synchronized ( this.resourcesLock ) {
                final EntityResourceList erl = this.persistentList.getEntityResourceList(key);
                logger.debug("Added or updated {} : {}", key, erl);

                // we first check for update
                boolean updated = false;
                if ( erl != null && erl.getFirstResource() != null ) {
                    // check digest for dictionaries
                    final TaskResource tr = erl.getFirstResource();
                    if ( dict != null ) {
                        final String digest = FileDataStore.computeDigest(dict);
                        if ( tr.getState() == ResourceState.INSTALLED && tr.getDigest().equals(digest) ) {
                            logger.debug("Resource did not change {}", key);
                            return;
                        }
                    }
                    final UpdateHandler handler = this.findHandler(tr.getScheme());
                    if ( handler == null ) {
                        logger.debug("No handler found to handle update of resource with scheme {}", tr.getScheme());
                    } else {
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
                                    newGroup.setFinishState(ResourceState.INSTALLED);
                                    newGroup.compact();
                                } else {
                                    // resource has been updated or moved
                                    ((RegisteredResourceImpl)tr).update(
                                            data.getDataFile(), data.getDictionary(),
                                            data.getDigest(result.getURL(), result.getDigest()),
                                            result.getPriority(),
                                            result.getURL());
                                    // We first set the state of the resource to install to make setFinishState work in all cases
                                    ((RegisteredResourceImpl)tr).setState(ResourceState.INSTALL);
                                    erl.setFinishState(ResourceState.INSTALLED);
                                    erl.compact();
                                }
                                updated = true;
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

                }

                boolean created = false;
                if ( !updated ) {
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
                                created = true;
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
                    if ( !created ) {
                        logger.debug("No handler found to handle creation of resource {}", key);
                    }
                }
                if ( updated || created ) {
                    this.persistentList.save();
                    this.scheduleRetry();
                }

            }
        } catch (final IOException ioe) {
            logger.error("Unable to handle resource add or update of " + key, ioe);
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
     * @see org.apache.sling.installer.api.ResourceChangeListener#resourceRemoved(java.lang.String, java.lang.String)
     */
    public void resourceRemoved(final String resourceType, String resourceId) {
        String key = resourceType + ':' + resourceId;
        synchronized ( this.resourcesLock ) {
            final EntityResourceList erl = this.persistentList.getEntityResourceList(key);
            logger.debug("Removed {} : {}", key, erl);
            // if this is not registered at all, we can simply ignore this
            if ( erl != null ) {
                resourceId = erl.getResourceId();
                key = resourceType + ':' + resourceId;
                final TaskResource tr = erl.getFirstResource();
                if ( tr != null ) {
                    if ( tr.getState() == ResourceState.IGNORED ) {
                        // if it has been ignored before, we activate it now again!
                        ((RegisteredResourceImpl)tr).setState(ResourceState.INSTALL);
                        this.persistentList.save();
                        this.scheduleRetry();
                    } else if ( tr.getState() == ResourceState.UNINSTALLED ) {
                        // it has already been removed - nothing do to
                    } else {
                        final UpdateHandler handler = this.findHandler(tr.getScheme());
                        if ( handler == null ) {
                            // set to ignored
                            logger.debug("No handler found to handle remove of resource with scheme {}", tr.getScheme());
                            ((RegisteredResourceImpl)tr).setState(ResourceState.IGNORED);
                        } else {
                            // we don't need to check the result, we just check if a result is returned
                            if ( handler.handleRemoval(resourceType, resourceId, tr.getURL()) != null ) {
                                // We first set the state of the resource to uninstall to make setFinishState work in all cases
                                ((RegisteredResourceImpl)tr).setState(ResourceState.UNINSTALL);
                                erl.setFinishState(ResourceState.UNINSTALLED);
                                erl.compact();
                            } else {
                                // set to ignored
                                logger.debug("No handler found to handle remove of resource with scheme {}", tr.getScheme());
                                ((RegisteredResourceImpl)tr).setState(ResourceState.IGNORED);
                            }
                        }
                        this.persistentList.save();
                        this.scheduleRetry();
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
    public InstallationState getInstallationState() {
        synchronized ( this.resourcesLock ) {
            final InstallationState state = new InstallationState() {

                private final List<ResourceGroup> activeResources = new ArrayList<ResourceGroup>();
                private final List<ResourceGroup> installedResources = new ArrayList<ResourceGroup>();
                private final List<RegisteredResource> untransformedResources = new ArrayList<RegisteredResource>();

                public List<ResourceGroup> getActiveResources() {
                    return activeResources;
                }

                public List<ResourceGroup> getInstalledResources() {
                    return installedResources;
                }

                public List<RegisteredResource> getUntransformedResources() {
                    return untransformedResources;
                }

            };

            for(final String entityId : this.persistentList.getEntityIds()) {
                if ( !this.persistentList.isSpecialEntityId(entityId) ) {
                    final EntityResourceList group = this.persistentList.getEntityResourceList(entityId);

                    final String alias = group.getAlias();
                    final List<Resource> resources = new ArrayList<Resource>();
                    for(final TaskResource tr : group.getResources()) {
                        resources.add(new Resource() {

                            public String getScheme() {
                                return tr.getScheme();
                            }

                            public String getURL() {
                                return tr.getURL();
                            }

                            public String getType() {
                                return tr.getType();
                            }

                            public InputStream getInputStream() throws IOException {
                                return tr.getInputStream();
                            }

                            public Dictionary<String, Object> getDictionary() {
                                return tr.getDictionary();
                            }

                            public String getDigest() {
                                return tr.getDigest();
                            }

                            public int getPriority() {
                                return tr.getPriority();
                            }

                            public String getEntityId() {
                                return tr.getEntityId();
                            }

                            public ResourceState getState() {
                                return tr.getState();
                            }

                            public Version getVersion() {
                                return tr.getVersion();
                            }

                            public long getLastChange() {
                                return ((RegisteredResourceImpl)tr).getLastChange();
                            }

                            public Object getAttribute(final String key) {
                                return tr.getAttribute(key);
                            }
                        });
                    }
                    final ResourceGroup rg = new ResourceGroup() {

                        public List<Resource> getResources() {
                            return resources;
                        }

                        public String getAlias() {
                            return alias;
                        }
                    };
                    if ( group.getActiveResource() != null ) {
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
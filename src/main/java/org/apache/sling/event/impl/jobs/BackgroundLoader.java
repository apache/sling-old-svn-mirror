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
package org.apache.sling.event.impl.jobs;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Task for loading stored jobs from the resource tree.
 *
 * This component starts a background thread.
 * The thread is only active when a stable topology view is available.
 * Whenever the component gets activated, it loads all jobs from the
 * resource tree. New incoming jobs are handled via a queue.
 */
public class BackgroundLoader implements Runnable {

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The job manager configuration. */
    private final JobManagerConfiguration configuration;

    /** Resource resolver factory. */
    private final ResourceResolverFactory resourceResolverFactory;

    /** Is this still active? */
    private final AtomicBoolean active = new AtomicBoolean(false);

    /** Is this currently running? */
    private volatile boolean running = false;

    /** Job Manager implementation. */
    private final JobManagerImpl jobManager;

    /** Lock object for loading */
    private final Object loadLock = new Object();

    /** Lock object for stopping */
    private final Object stopLock = new Object();

    /** Unloaded jobs. */
    private final Set<String> unloadedJobs = new HashSet<String>();

    /** A local queue for handling new jobs. */
    private final BlockingQueue<Object> actionQueue = new LinkedBlockingQueue<Object>();

    /** Boolean to detect the initial start. */
    private boolean firstRun = true ;

    /**
     * Create and activate the loader.
     */
    public BackgroundLoader(final JobManagerImpl jobManagerImpl,
            final JobManagerConfiguration configuration2,
            final ResourceResolverFactory resourceResolverFactory2) {
        this.resourceResolverFactory = resourceResolverFactory2;
        this.configuration = configuration2;
        this.jobManager = jobManagerImpl;
        this.active.set(true);
        // start background thread
        final Thread loaderThread = new Thread(this, "Apache Sling Job Background Loader");
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    /**
     * Deactivate the loader.
     */
    public void deactivate() {
        this.active.set(false);
        // make sure to stop background thread
        synchronized ( this.loadLock ) {
            this.running = false;
            this.loadLock.notify();
        }
        this.stop();
        synchronized ( this.stopLock ) {
            this.stopLock.notify();
        }
    }

    /**
     * Helper method which just logs the exception in debug mode.
     * @param e
     */
    private void ignoreException(final Exception e) {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Ignored exception " + e.getMessage(), e);
        }
    }

    /**
     * Start the background loader process.
     */
    public void start() {
        synchronized ( this.loadLock ) {
            this.running = true;
            // make sure to clear out old information
            this.actionQueue.clear();
            this.unloadedJobs.clear();

            this.loadLock.notify();
        }
    }

    private static final String END_TOKEN = "*";

    /**
     * Stop the background loader process.
     */
    public void stop() {
        synchronized ( this.loadLock ) {
            this.running = false;
        }
        // stop action queue
        try {
            this.actionQueue.put(END_TOKEN);
        } catch (final InterruptedException e) {
            this.ignoreException(e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Restart if the instance is currently running.
     */
    public void restart() {
        if ( this.isRunning() ) {
            this.stop();
            this.start();
        }
    }

    @Override
    public void run() {
        while ( this.active.get() ) {
            final long startTime;
            // we have to wait to get started
            synchronized ( this.loadLock ) {
                while ( this.active.get() && !this.running ) {
                    try {
                        this.loadLock.wait();
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        this.active.set(false);
                    }
                }
                startTime = System.currentTimeMillis();
            }

            // give the system some time to start
            if ( this.isRunning() ) {
                synchronized ( this.stopLock ) {
                    try {
                        this.stopLock.wait(1000 * this.configuration.getBackgroundLoadDelay());
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        this.active.set(false);
                    }
                }
            }

            // load jobs from the resource tree
            if ( this.isRunning() ) {
                this.loadJobsInTheBackground(startTime);
            }
            // if we're still running we can clear the first run flag
            if ( this.isRunning() ) {
                this.firstRun = false;
            }
            // and finally process the action queue
            while ( this.isRunning() ) {
                Object nextPathOrJob = null;
                try {
                    nextPathOrJob = this.actionQueue.take();
                } catch (final InterruptedException e) {
                    this.ignoreException(e);
                    Thread.currentThread().interrupt();
                    this.active.set(false);
                }
                if ( nextPathOrJob instanceof JobImpl ) {
                    this.jobManager.process((JobImpl)nextPathOrJob);
                } else if ( nextPathOrJob instanceof String ) {
                    final String path = (String)nextPathOrJob;
                    if ( !END_TOKEN.equals(path) && this.isRunning() ) {
                        ResourceResolver resolver = null;
                        try {
                            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
                            final Resource resource = resolver.getResource(path);
                            if ( resource == null ) {
                                // this should actually never happen, just a sanity check (see SLING-2971)
                                logger.warn("No job resource found for path {}. Potential job will not be processed.", path);
                            } else {
                                if (ResourceHelper.RESOURCE_TYPE_JOB.equals(resource.getResourceType()) ) {
                                    this.logger.debug("Reading local job from {}", path);
                                    final JobImpl job = this.jobManager.readJob(resource);
                                    if ( job != null ) {
                                        if ( job.hasReadErrors() ) {
                                            synchronized ( this.unloadedJobs ) {
                                                this.unloadedJobs.add(path);
                                            }
                                        } else {
                                            this.jobManager.process(job);
                                        }
                                    }
                                }
                            }
                        } catch ( final LoginException le ) {
                            // administrative login should always work
                            this.ignoreException(le);
                        } finally {
                            if ( resolver != null ) {
                                resolver.close();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Load all active jobs from the resource tree.
     */
    private void loadJobsInTheBackground(final long startTime) {
        logger.debug("Starting background loading...");

        ResourceResolver resolver = null;
        long count = 0;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
            final Calendar startDate = Calendar.getInstance();
            startDate.setTimeInMillis(startTime);

            final StringBuilder buf = new StringBuilder(64);

            buf.append("//element(*,");
            buf.append(ResourceHelper.RESOURCE_TYPE_JOB);
            buf.append(")[@");
            buf.append(ISO9075.encode(Job.PROPERTY_JOB_TARGET_INSTANCE));
            buf.append(" = '");
            buf.append(Environment.APPLICATION_ID);
            buf.append("' and @");
            buf.append(ISO9075.encode(Job.PROPERTY_JOB_CREATED));
            buf.append(" < xs:dateTime('");
            buf.append(ISO8601.format(startDate));
            buf.append("')");
            buf.append("] order by @");
            buf.append(ISO9075.encode(Job.PROPERTY_JOB_CREATED));
            buf.append(" ascending");
            if ( this.isRunning() ) {
                final Iterator<Resource> result = resolver.findResources(buf.toString(), "xpath");

                while ( this.isRunning() && result.hasNext() ) {
                    final Resource jobResource = result.next();
                    if ( this.loadJobInTheBackground(jobResource) ) {
                        count++;
                    }
                }
            }
        } catch (final QuerySyntaxException qse) {
            this.ignoreException(qse);
        } catch (final LoginException le) {
            this.ignoreException(le);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }

        logger.debug("Finished background loading of {} jobs.", count);
    }

    /**
     * Load a single job from the resource tree.
     */
    private boolean loadJobInTheBackground(final Resource jobResource) {
        // sanity check for the path
        if ( this.configuration.isLocalJob(jobResource.getPath()) ) {
            final JobImpl job = this.jobManager.readJob(jobResource);
            if ( job != null ) {
                // check if the job is currently running
                if ( this.firstRun  || job.getProcessingStarted() == null ) {
                    // reset started time and increase retry count
                    if ( job.getProcessingStarted() != null && this.isRunning() ) {
                        job.retry();
                        try {
                            final ModifiableValueMap mvm = jobResource.adaptTo(ModifiableValueMap.class);
                            mvm.remove(Job.PROPERTY_JOB_STARTED_TIME);
                            mvm.put(Job.PROPERTY_JOB_RETRY_COUNT, job.getRetryCount());
                            jobResource.getResourceResolver().commit();
                        } catch ( final PersistenceException ignore) {
                            this.ignoreException(ignore);
                        }
                    }

                    if ( job.hasReadErrors() ) {
                        synchronized ( this.unloadedJobs ) {
                            this.unloadedJobs.add(job.getResourcePath());
                        }
                    } else {
                        if ( this.isRunning() ) {
                            this.jobManager.process(job);
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the component is currently running and active
     */
    private boolean isRunning() {
        return this.active.get() && this.running;
    }

    /**
     * Try to reload unloaded jobs - this method is invoked if bundles have been added etc.
     */
    public void tryToReloadUnloadedJobs() {
        // bundle event started or updated
        final Set<String> copyUnloadedJobs = new HashSet<String>();
        synchronized ( this.unloadedJobs ) {
            copyUnloadedJobs.addAll(this.unloadedJobs);
            this.unloadedJobs.clear();
        }
        if ( copyUnloadedJobs.size() > 0 ) {
            final Runnable t = new Runnable() {

                @Override
                public void run() {
                    final Iterator<String> iter = copyUnloadedJobs.iterator();
                    while ( iter.hasNext() ) {
                        synchronized ( loadLock ) {
                            if ( isRunning() ) {
                                try {
                                    actionQueue.put(iter.next());
                                } catch (final InterruptedException e) {
                                    ignoreException(e);
                                    Thread.currentThread().interrupt();
                                    running = false;
                                }
                            }
                        }
                    }
                }

            };
            Environment.THREAD_POOL.execute(t);
        }
    }

    /**
     * Add a path to the load job queue if the instance is running.
     */
    public void loadJob(final String path) {
        synchronized ( loadLock ) {
            if ( isRunning() ) {
                try {
                    this.actionQueue.put(path);
                } catch (final InterruptedException e) {
                    this.ignoreException(e);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Add a job to the load job queue if the instance is running.
     */
    public void addJob(final JobImpl job) {
        synchronized ( loadLock ) {
            if ( isRunning() ) {
                try {
                    this.actionQueue.put(job);
                } catch (final InterruptedException e) {
                    this.ignoreException(e);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}

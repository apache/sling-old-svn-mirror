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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

    /** Use search or traverse? */
    private boolean useSearch;

    /**
     * Create and activate the loader.
     */
    public BackgroundLoader(final JobManagerImpl jobManagerImpl,
            final JobManagerConfiguration configuration2,
            final ResourceResolverFactory resourceResolverFactory2) {
        this.useSearch = JobManagerConfiguration.DEFAULT_BACKGROUND_LOAD_SEARCH;
        this.resourceResolverFactory = resourceResolverFactory2;
        this.configuration = configuration2;
        this.jobManager = jobManagerImpl;
        this.active.set(true);
        logger.debug("Activating Sling Job Background Loader");
        // start background thread
        final Thread loaderThread = new Thread(this, "Apache Sling Job Background Loader");
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    /**
     * Deactivate the loader.
     */
    public void deactivate() {
        logger.debug("Deactivating Sling Job Background Loader");
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
            logger.debug("Starting Sling Job Background Loader");
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
            logger.debug("Stopping Sling Job Background Loader");
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
            logger.debug("Restarting Sling Job Background Loader");
            this.stop();
            this.start();
        }
    }

    @Override
    public void run() {
        logger.debug("Started Sling Job Background Thread");
        try {
            while ( this.active.get() ) {
                final long startTime;
                // we have to wait to get started
                synchronized ( this.loadLock ) {
                    while ( this.active.get() && !this.running ) {
                        logger.debug("Sling Job Background Thread is waiting to be started");
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
                    logger.debug("Sling Job Background Thread is waiting for system to be ready");
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
                    logger.debug("Sling Job Background Thread starts loading jobs");
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
                                        final JobImpl job = Utility.readJob(logger, resource);
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
        } catch ( final Throwable t) {
            // make sure we at least log each unexpected exiting
            logger.error("Unexpected error in background loader thread." + t.getMessage(), t);
        }
        logger.debug("Stopped Sling Job Background Thread");
    }

    /**
     * Load all active jobs from the resource tree.
     */
    private void loadJobsInTheBackground(final long startTime) {
        logger.debug("Starting background loading...");

        long count = 0;

        if ( this.useSearch ) {
            logger.debug("Using search for background loading...");
            ResourceResolver resolver = null;
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
            } catch (final UnsupportedOperationException t ) {
                // this is thrown by Oak if the search is taking "too long"
                this.logger.error("Unexpected unsupported operation exception. This is most probably because of Apache Jackrabbit Oak " +
                                  "complaining about to long running query. Switching to traversal now.");
                this.useSearch = false;
            } finally {
                if ( resolver != null ) {
                    resolver.close();
                }
            }
        }
        if ( !useSearch ) {
            logger.debug("Using traversal for background loading...");
            ResourceResolver resolver = null;
            try {
                resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);

                final Resource baseResource = resolver.getResource(this.configuration.getLocalJobsPath());

                final Comparator<Resource> resourceComparator = new Comparator<Resource>() {

                    @Override
                    public int compare(final Resource o1, final Resource o2) {
                        final int value1 = Integer.valueOf(o1.getName());
                        final int value2 = Integer.valueOf(o2.getName());
                        if ( value1 < value2 ) {
                            return -1;
                        } else if ( value1 > value2 ) {
                            return 1;
                        }
                        return 0;
                    }
                };

                // sanity check - should never be null
                if ( baseResource != null ) {
                    final Calendar now = Calendar.getInstance();
                    now.setTimeInMillis(startTime);

                    final Iterator<Resource> topicIter = baseResource.listChildren();
                    while ( this.isRunning() && topicIter.hasNext() ) {
                        final Resource topicResource = topicIter.next();
                        logger.debug("Processing topic {}", topicResource.getName());

                        // now years
                        final List<Resource> years = new ArrayList<Resource>();
                        final Iterator<Resource> yearIter = topicResource.listChildren();
                        while ( this.isRunning() && yearIter.hasNext() ) {
                            final Resource yearResource = yearIter.next();
                            years.add(yearResource);
                            logger.debug("Found year {}", yearResource.getName());
                        }
                        Collections.sort(years, resourceComparator);

                        for(final Resource yearResource: years) {
                            final int year = Integer.valueOf(yearResource.getName());
                            if ( year > now.get(Calendar.YEAR) ) {
                                logger.debug("Skipping year {}", year);
                                continue;
                            }
                            logger.debug("Processing year {}", year);

                            // now months
                            final List<Resource> months = new ArrayList<Resource>();
                            final Iterator<Resource> monthIter = yearResource.listChildren();
                            while ( this.isRunning() && monthIter.hasNext() ) {
                                final Resource monthResource = monthIter.next();
                                months.add(monthResource);
                                logger.debug("Found month {}",  monthResource.getName());
                            }
                            Collections.sort(months, resourceComparator);

                            for(final Resource monthResource: months) {
                                final int month = Integer.valueOf(monthResource.getName());
                                if ( year == now.get(Calendar.YEAR) && (month > now.get(Calendar.MONTH) + 1)) {
                                    logger.debug("Skipping month {}", month);
                                    continue;
                                }
                                logger.debug("Processing month {}", month);

                                // now days
                                final List<Resource> days = new ArrayList<Resource>();
                                final Iterator<Resource> dayIter = monthResource.listChildren();
                                while ( this.isRunning() && dayIter.hasNext() ) {
                                    final Resource dayResource = dayIter.next();
                                    days.add(dayResource);
                                    logger.debug("Found day {}",  dayResource.getName());
                                }
                                Collections.sort(days, resourceComparator);

                                for(final Resource dayResource: days) {
                                    final int day = Integer.valueOf(dayResource.getName());

                                    if ( year == now.get(Calendar.YEAR)
                                         && month == now.get(Calendar.MONTH) + 1
                                         && day > now.get(Calendar.DAY_OF_MONTH) ) {
                                        logger.debug("Skipping day {}", day);
                                        continue;
                                    }
                                    logger.debug("Processing day {}", day);

                                    // now hours
                                    final List<Resource> hours = new ArrayList<Resource>();
                                    final Iterator<Resource> hourIter = dayResource.listChildren();
                                    while ( this.isRunning() && hourIter.hasNext() ) {
                                        final Resource hourResource = hourIter.next();
                                        hours.add(hourResource);
                                        logger.debug("Found hour {}",  hourResource.getName());
                                    }
                                    Collections.sort(hours, resourceComparator);

                                    for(final Resource hourResource: hours) {
                                        final int hour = Integer.valueOf(hourResource.getName());

                                        if ( year == now.get(Calendar.YEAR)
                                             && month == now.get(Calendar.MONTH) + 1
                                             && day == now.get(Calendar.DAY_OF_MONTH)
                                             && hour > now.get(Calendar.HOUR_OF_DAY) ) {
                                            logger.debug("Skipping hour {}", hour);
                                            continue;
                                        }
                                        logger.debug("Processing hour {}", hour);

                                        // now minutes
                                        final List<Resource> minutes = new ArrayList<Resource>();
                                        final Iterator<Resource> minuteIter = hourResource.listChildren();
                                        while ( this.isRunning() && minuteIter.hasNext() ) {
                                            final Resource minuteResource = minuteIter.next();
                                            minutes.add(minuteResource);
                                            logger.debug("Found minute {}",  minuteResource.getName());
                                        }
                                        Collections.sort(minutes, resourceComparator);

                                        for(final Resource minuteResource: minutes) {
                                            final int minute = Integer.valueOf(minuteResource.getName());

                                            if ( year == now.get(Calendar.YEAR)
                                                 && month == now.get(Calendar.MONTH) + 1
                                                 && day == now.get(Calendar.DAY_OF_MONTH)
                                                 && hour == now.get(Calendar.HOUR_OF_DAY)
                                                 && minute > now.get(Calendar.MINUTE) ) {
                                                logger.debug("Skipping minute {}", minute);
                                                continue;
                                            }
                                            logger.debug("Processing minute {}", minute);

                                            // now jobs
                                            final List<JobImpl> jobs = new ArrayList<JobImpl>();
                                            final Iterator<Resource> jobIter = minuteResource.listChildren();
                                            while ( this.isRunning() && jobIter.hasNext() ) {
                                                final Resource jobResource = jobIter.next();

                                                final JobImpl job = Utility.readJob(logger, jobResource);
                                                if ( job != null && job.getCreated().compareTo(now) <= 0 ) {
                                                    logger.debug("Found job {}", jobResource.getName());
                                                    jobs.add(job);
                                                } else {
                                                    logger.debug("Skipping job {}", jobResource.getName());
                                                }
                                            }

                                            Collections.sort(jobs, new Comparator<Job>() {

                                                @Override
                                                public int compare(final Job o1, final Job o2) {
                                                    return o1.getCreated().compareTo(o2.getCreated());
                                                }
                                            });

                                            for(final JobImpl job : jobs) {
                                                final Resource jobResource = resolver.getResource(job.getResourcePath());
                                                if ( jobResource != null &&  this.isRunning() && this.loadJobInTheBackground(jobResource) ) {
                                                    count++;
                                                }
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (final LoginException le) {
                this.ignoreException(le);
            } finally {
                if ( resolver != null ) {
                    resolver.close();
                }
            }
        }

        logger.info("Finished background loading of {} jobs.", count);
    }

    /**
     * Load a single job from the resource tree.
     */
    private boolean loadJobInTheBackground(final Resource jobResource) {
        // sanity check for the path
        if ( this.configuration.isLocalJob(jobResource.getPath()) ) {
            final JobImpl job = Utility.readJob(logger, jobResource);
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

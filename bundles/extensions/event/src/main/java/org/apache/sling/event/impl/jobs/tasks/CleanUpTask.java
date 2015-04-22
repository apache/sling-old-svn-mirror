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
package org.apache.sling.event.impl.jobs.tasks;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.impl.jobs.Utility;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.config.TopologyCapabilities;
import org.apache.sling.event.impl.support.BatchResourceRemover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintenance task...
 *
 * In the default configuration, this task runs every minute
 */
public class CleanUpTask {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Job manager configuration. */
    private final JobManagerConfiguration configuration;

    /** We count the scheduler runs. */
    private volatile long schedulerRuns;

    /**
     * Constructor
     */
    public CleanUpTask(final JobManagerConfiguration config) {
        this.configuration = config;
    }

    /**
     * One maintenance run
     */
    public void run() {
        this.schedulerRuns++;
        logger.debug("Job manager maintenance: Starting #{}", this.schedulerRuns);

        final TopologyCapabilities topologyCapabilities = configuration.getTopologyCapabilities();
        if ( topologyCapabilities != null ) {
            // Clean up
            final String cleanUpUnassignedPath;;
            if ( topologyCapabilities.isLeader() ) {
                cleanUpUnassignedPath = this.configuration.getUnassignedJobsPath();
            } else {
                cleanUpUnassignedPath = null;
            }

            if ( schedulerRuns % 60 == 0 ) { // full clean up is done every hour
                this.fullEmptyFolderCleanup(topologyCapabilities, this.configuration.getLocalJobsPath());
                if ( cleanUpUnassignedPath != null ) {
                    this.fullEmptyFolderCleanup(topologyCapabilities, cleanUpUnassignedPath);
                }
            } else if ( schedulerRuns % 5 == 0 ) { // simple clean up every 5 minutes
                this.simpleEmptyFolderCleanup(topologyCapabilities, this.configuration.getLocalJobsPath());
                if ( cleanUpUnassignedPath != null ) {
                    this.simpleEmptyFolderCleanup(topologyCapabilities, cleanUpUnassignedPath);
                }
            }
        }

        // lock cleanup is done every minute
        this.lockCleanup(topologyCapabilities);
        logger.debug("Job manager maintenance: Finished #{}", this.schedulerRuns);
    }

    /**
     * Clean up the locks
     * All locks older than two minutes are removed
     */
    private void lockCleanup(final TopologyCapabilities caps) {
        if ( caps != null && caps.isLeader() ) {
            this.logger.debug("Cleaning up job resource tree: removing obsolete locks");
            final List<Resource> candidates = new ArrayList<Resource>();
            final ResourceResolver resolver = this.configuration.createResourceResolver();
            if ( resolver != null ) {
                try {
                    final Resource parentResource = resolver.getResource(this.configuration.getLocksPath());
                    if ( parentResource != null ) {
                        final Calendar startDate = Calendar.getInstance();
                        startDate.add(Calendar.MINUTE, -2);

                        this.lockCleanup(caps, candidates, parentResource, startDate);
                        final BatchResourceRemover remover = new BatchResourceRemover();
                        boolean batchRemove = true;
                        for(final Resource lockResource : candidates) {
                            if ( caps.isActive() ) {
                                try {
                                    if ( batchRemove ) {
                                        remover.delete(lockResource);
                                    } else {
                                        resolver.delete(lockResource);
                                        resolver.commit();
                                    }
                                } catch ( final PersistenceException pe) {
                                    batchRemove = false;
                                    this.ignoreException(pe);
                                    resolver.refresh();
                                }
                            } else {
                                break;
                            }
                        }
                        try {
                            resolver.commit();
                        } catch ( final PersistenceException pe) {
                            this.ignoreException(pe);
                            resolver.refresh();
                        }
                    }
                } finally {
                    resolver.close();
                }
            }
        }
    }

    /**
     * Recursive lock cleanup
     */
    private void lockCleanup(final TopologyCapabilities caps,
            final List<Resource> candidates,
            final Resource parentResource,
            final Calendar startDate) {
        for(final Resource childResource : parentResource.getChildren()) {
            if ( caps.isActive() ) {
                final ValueMap vm = ResourceUtil.getValueMap(childResource);
                final Calendar created = vm.get(Utility.PROPERTY_LOCK_CREATED, Calendar.class);
                if ( created != null ) {
                    // lock resource
                    if ( created.before(startDate) ) {
                        candidates.add(childResource);
                    }
                } else {
                    lockCleanup(caps, candidates, childResource, startDate);
                }
            } else {
                break;
            }
        }
    }

    /**
     * Simple empty folder removes empty folders for the last ten minutes
     * starting five minutes ago.
     * If folder for minute 59 is removed, we check the hour folder as well.
     */
    private void simpleEmptyFolderCleanup(final TopologyCapabilities caps, final String basePath) {
        this.logger.debug("Cleaning up job resource tree: looking for empty folders");
        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            final Calendar cleanUpDate = Calendar.getInstance();
            // go back five minutes
            cleanUpDate.add(Calendar.MINUTE, -5);

            final Resource baseResource = resolver.getResource(basePath);
            // sanity check - should never be null
            if ( baseResource != null ) {
                final Iterator<Resource> topicIter = baseResource.listChildren();
                while ( caps.isActive() && topicIter.hasNext() ) {
                    final Resource topicResource = topicIter.next();

                    for(int i = 0; i < 10; i++) {
                        if ( caps.isActive() ) {
                            final StringBuilder sb = new StringBuilder(topicResource.getPath());
                            sb.append('/');
                            sb.append(cleanUpDate.get(Calendar.YEAR));
                            sb.append('/');
                            sb.append(cleanUpDate.get(Calendar.MONTH) + 1);
                            sb.append('/');
                            sb.append(cleanUpDate.get(Calendar.DAY_OF_MONTH));
                            sb.append('/');
                            sb.append(cleanUpDate.get(Calendar.HOUR_OF_DAY));
                            sb.append('/');
                            sb.append(cleanUpDate.get(Calendar.MINUTE));
                            final String path = sb.toString();

                            final Resource dateResource = resolver.getResource(path);
                            if ( dateResource != null && !dateResource.listChildren().hasNext() ) {
                                resolver.delete(dateResource);
                                resolver.commit();
                            }
                            // check hour folder
                            if ( path.endsWith("59") ) {
                                final String hourPath = path.substring(0, path.length() - 3);
                                final Resource hourResource = resolver.getResource(hourPath);
                                if ( hourResource != null && !hourResource.listChildren().hasNext() ) {
                                    resolver.delete(hourResource);
                                    resolver.commit();
                                }
                            }
                            // go back another minute in time
                            cleanUpDate.add(Calendar.MINUTE, -1);
                        }
                    }
                }
            }

        } catch (final PersistenceException pe) {
            // in the case of an error, we just log this as a warning
            this.logger.warn("Exception during job resource tree cleanup.", pe);
        } finally {
            resolver.close();
        }
    }

    /**
     * Full cleanup - this scans all directories!
     */
    private void fullEmptyFolderCleanup(final TopologyCapabilities caps, final String basePath) {
        this.logger.debug("Cleaning up job resource tree: removing ALL empty folders");
        final ResourceResolver resolver = this.configuration.createResourceResolver();
        if ( resolver == null ) {
            return;
        }
        try {
            final Resource baseResource = resolver.getResource(basePath);
            // sanity check - should never be null
            if ( baseResource != null ) {
                final Calendar now = Calendar.getInstance();

                final Iterator<Resource> topicIter = baseResource.listChildren();
                while ( caps.isActive() && topicIter.hasNext() ) {
                    final Resource topicResource = topicIter.next();

                    // now years
                    final Iterator<Resource> yearIter = topicResource.listChildren();
                    while ( caps.isActive() && yearIter.hasNext() ) {
                        final Resource yearResource = yearIter.next();
                        final int year = Integer.valueOf(yearResource.getName());
                        final boolean oldYear = year < now.get(Calendar.YEAR);

                        // months
                        final Iterator<Resource> monthIter = yearResource.listChildren();
                        while ( caps.isActive() && monthIter.hasNext() ) {
                            final Resource monthResource = monthIter.next();
                            final int month = Integer.valueOf(monthResource.getName());
                            final boolean oldMonth = oldYear || month < (now.get(Calendar.MONTH) + 1);

                            // days
                            final Iterator<Resource> dayIter = monthResource.listChildren();
                            while ( caps.isActive() && dayIter.hasNext() ) {
                                final Resource dayResource = dayIter.next();
                                final int day = Integer.valueOf(dayResource.getName());
                                final boolean oldDay = oldMonth || day < now.get(Calendar.DAY_OF_MONTH);

                                // hours
                                final Iterator<Resource> hourIter = dayResource.listChildren();
                                while ( caps.isActive() && hourIter.hasNext() ) {
                                    final Resource hourResource = hourIter.next();
                                    final int hour = Integer.valueOf(hourResource.getName());
                                    final boolean oldHour = (oldDay && (oldMonth || now.get(Calendar.HOUR_OF_DAY) > 0)) || hour < (now.get(Calendar.HOUR_OF_DAY) -1);

                                    // we only remove minutes if the hour is old
                                    if ( oldHour ) {
                                        final Iterator<Resource> minuteIter = hourResource.listChildren();
                                        while ( caps.isActive() && minuteIter.hasNext() ) {
                                            final Resource minuteResource = minuteIter.next();

                                            // check if we can delete the minute
                                            if ( !minuteResource.listChildren().hasNext() ) {
                                                resolver.delete(minuteResource);
                                                resolver.commit();
                                            }
                                        }
                                    }

                                    // check if we can delete the hour
                                    if ( caps.isActive() && oldHour && !hourResource.listChildren().hasNext()) {
                                        resolver.delete(hourResource);
                                        resolver.commit();
                                    }
                                }
                                // check if we can delete the day
                                if ( caps.isActive() && oldDay && !dayResource.listChildren().hasNext()) {
                                    resolver.delete(dayResource);
                                    resolver.commit();
                                }
                            }

                            // check if we can delete the month
                            if ( caps.isActive() && oldMonth && !monthResource.listChildren().hasNext() ) {
                                resolver.delete(monthResource);
                                resolver.commit();
                            }
                        }

                        // check if we can delete the year
                        if ( caps.isActive() && oldYear && !yearResource.listChildren().hasNext() ) {
                            resolver.delete(yearResource);
                            resolver.commit();
                        }
                    }
                }
            }

        } catch (final PersistenceException pe) {
            // in the case of an error, we just log this as a warning
            this.logger.warn("Exception during job resource tree cleanup.", pe);
        } finally {
            resolver.close();
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
}

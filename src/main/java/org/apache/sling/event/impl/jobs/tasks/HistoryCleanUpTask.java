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

import java.util.Calendar;
import java.util.Iterator;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.impl.jobs.JobManagerImpl;
import org.apache.sling.event.impl.support.BatchResourceRemover;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task to clean up the history
 */
@Component
@Service(value = JobExecutor.class)
@Property(name = JobExecutor.PROPERTY_TOPICS, value = "org/apache/sling/event/impl/jobs/tasks/HistoryCleanUpTask")
public class HistoryCleanUpTask implements JobExecutor {

    private static final String PROPERTY_AGE = "age";

    private static final int DEFAULT_AGE = 60 * 24 * 2; // older than two days

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private JobManager jobManager;

    @Override
    public JobExecutionResult process(final Job job, final JobExecutionContext context) {
        int age = job.getProperty(PROPERTY_AGE, DEFAULT_AGE);
        if ( age < 1 ) {
            age = DEFAULT_AGE;
        }
        final Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, -age);

        context.log("Cleaning up job history. Removing all jobs older than {0}", now);
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);

            this.cleanup(now, resolver, context, ((JobManagerImpl)jobManager).getConfiguration().getStoredCancelledJobsPath());
            this.cleanup(now, resolver, context, ((JobManagerImpl)jobManager).getConfiguration().getStoredSuccessfulJobsPath());


        } catch (final PersistenceException pe) {
            // in the case of an error, we just log this as a warning
            this.logger.warn("Exception during job resource tree cleanup.", pe);
        } catch (final LoginException ignore) {
            this.ignoreException(ignore);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
        return context.result().succeeded();
    }

    private void cleanup(final Calendar now,
            final ResourceResolver resolver, final JobExecutionContext context, final String basePath)
    throws PersistenceException {
        final Resource baseResource = resolver.getResource(basePath);
        // sanity check - should never be null
        if ( baseResource != null ) {
            final Iterator<Resource> topicIter = baseResource.listChildren();
            while ( !context.isStopped() && topicIter.hasNext() ) {
                final Resource topicResource = topicIter.next();

                // now years
                final Iterator<Resource> yearIter = topicResource.listChildren();
                while ( !context.isStopped() && yearIter.hasNext() ) {
                    final Resource yearResource = yearIter.next();
                    final int year = Integer.valueOf(yearResource.getName());
                    final boolean oldYear = year < now.get(Calendar.YEAR);

                    // months
                    final Iterator<Resource> monthIter = yearResource.listChildren();
                    while ( !context.isStopped() && monthIter.hasNext() ) {
                        final Resource monthResource = monthIter.next();
                        final int month = Integer.valueOf(monthResource.getName());
                        final boolean oldMonth = oldYear || month < (now.get(Calendar.MONTH) + 1);

                        // days
                        final Iterator<Resource> dayIter = monthResource.listChildren();
                        while ( !context.isStopped() && dayIter.hasNext() ) {
                            final Resource dayResource = dayIter.next();
                            final int day = Integer.valueOf(dayResource.getName());
                            final boolean oldDay = oldMonth || day < now.get(Calendar.DAY_OF_MONTH);

                            // hours
                            final Iterator<Resource> hourIter = dayResource.listChildren();
                            while ( !context.isStopped() && hourIter.hasNext() ) {
                                final Resource hourResource = hourIter.next();
                                final int hour = Integer.valueOf(hourResource.getName());
                                final boolean oldHour = oldDay || hour < now.get(Calendar.HOUR_OF_DAY);

                                // minutes
                                final Iterator<Resource> minuteIter = hourResource.listChildren();
                                while ( !context.isStopped() && minuteIter.hasNext() ) {
                                    final Resource minuteResource = minuteIter.next();

                                    // check if we can delete the minute
                                    final int minute = Integer.valueOf(minuteResource.getName());
                                    final boolean oldMinute = oldHour || minute <= now.get(Calendar.MINUTE);
                                    if ( oldMinute ) {
                                        BatchResourceRemover remover = new BatchResourceRemover();
                                        remover.delete(minuteResource);
                                        resolver.commit();
                                    }
                                }

                                // check if we can delete the hour
                                if ( !context.isStopped() && oldHour && !hourResource.listChildren().hasNext()) {
                                    resolver.delete(hourResource);
                                    resolver.commit();
                                }
                            }
                            // check if we can delete the day
                            if ( !context.isStopped() && oldDay && !dayResource.listChildren().hasNext()) {
                                resolver.delete(dayResource);
                                resolver.commit();
                            }
                        }

                        // check if we can delete the month
                        if ( !context.isStopped() && oldMonth && !monthResource.listChildren().hasNext() ) {
                            resolver.delete(monthResource);
                            resolver.commit();
                        }
                    }

                    // check if we can delete the year
                    if ( !context.isStopped() && oldYear && !yearResource.listChildren().hasNext() ) {
                        resolver.delete(yearResource);
                        resolver.commit();
                    }
                }
            }
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

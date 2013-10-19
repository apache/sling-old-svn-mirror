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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.JobManagerImpl;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task to clean up the history,
 * A clean up task can be configured with three properties:
 * - age : only jobs older than this amount of minutes are removed (default is two days)
 * - topic : only jobs with this topic are removed (default is no topic, meaning all jobs are removed)
 *           The value should either be a string or an array of string
 * - state : only jobs in this state are removed (default is no state, meaning all jobs are removed)
 *           The value should either be a string or an array of string. Allowed values are:
 *           SUCCEEDED, STOPPED, GIVEN_UP, ERROR, DROPPED
 */
@Component
@Service(value = JobExecutor.class)
@Property(name = JobExecutor.PROPERTY_TOPICS, value = "org/apache/sling/event/impl/jobs/tasks/HistoryCleanUpTask")
public class HistoryCleanUpTask implements JobExecutor {

    private static final String PROPERTY_AGE = "age";

    private static final String PROPERTY_TOPIC = "topic";

    private static final String PROPERTY_STATE = "state";

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
        final Calendar removeDate = Calendar.getInstance();
        removeDate.add(Calendar.MINUTE, -age);

        final String[] topics = job.getProperty(PROPERTY_TOPIC, String[].class);
        final String[] states = job.getProperty(PROPERTY_STATE, String[].class);
        final String logTopics = (topics == null ? "ALL" : Arrays.toString(topics));
        final String logStates = (states == null ? "ALL" : Arrays.toString(states));
        context.log("Cleaning up job history. Removing all jobs older than {0}, with topics {1} and states {2}",
                removeDate, logTopics, logStates);

        final List<String> stateList;
        if ( states != null ) {
            stateList = new ArrayList<String>();
            for(final String s : states) {
                stateList.add(s);
            }
        } else {
            stateList = null;
        }
        ResourceResolver resolver = null;
        try {
            resolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);

            if ( stateList == null || stateList.contains(Job.JobState.SUCCEEDED.name()) ) {
                this.cleanup(removeDate, resolver, context, ((JobManagerImpl)jobManager).getConfiguration().getStoredSuccessfulJobsPath(), topics, null);
            }
            if ( stateList == null || stateList.contains(Job.JobState.DROPPED.name())
                 || stateList.contains(Job.JobState.ERROR.name())
                 || stateList.contains(Job.JobState.GIVEN_UP.name())
                 || stateList.contains(Job.JobState.STOPPED.name())) {
                this.cleanup(removeDate, resolver, context, ((JobManagerImpl)jobManager).getConfiguration().getStoredCancelledJobsPath(), topics, stateList);
            }

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

    private void cleanup(final Calendar removeDate,
            final ResourceResolver resolver,
            final JobExecutionContext context,
            final String basePath,
            final String[] topics,
            final List<String> stateList)
    throws PersistenceException {
        final Resource baseResource = resolver.getResource(basePath);
        // sanity check - should never be null
        if ( baseResource != null ) {
            final Iterator<Resource> topicIter = baseResource.listChildren();
            while ( !context.isStopped() && topicIter.hasNext() ) {
                final Resource topicResource = topicIter.next();

                // check topic
                boolean found = topics == null;
                int index = 0;
                while ( !found && index < topics.length ) {
                    if ( topicResource.getName().equals(topics[index]) ) {
                        found = true;
                    }
                    index++;
                }
                if ( !found ) {
                    continue;
                }

                // now years
                final Iterator<Resource> yearIter = topicResource.listChildren();
                while ( !context.isStopped() && yearIter.hasNext() ) {
                    final Resource yearResource = yearIter.next();
                    final int year = Integer.valueOf(yearResource.getName());
                    final boolean oldYear = year < removeDate.get(Calendar.YEAR);

                    // months
                    final Iterator<Resource> monthIter = yearResource.listChildren();
                    while ( !context.isStopped() && monthIter.hasNext() ) {
                        final Resource monthResource = monthIter.next();
                        final int month = Integer.valueOf(monthResource.getName());
                        final boolean oldMonth = oldYear || month < (removeDate.get(Calendar.MONTH) + 1);

                        // days
                        final Iterator<Resource> dayIter = monthResource.listChildren();
                        while ( !context.isStopped() && dayIter.hasNext() ) {
                            final Resource dayResource = dayIter.next();
                            final int day = Integer.valueOf(dayResource.getName());
                            final boolean oldDay = oldMonth || day < removeDate.get(Calendar.DAY_OF_MONTH);

                            // hours
                            final Iterator<Resource> hourIter = dayResource.listChildren();
                            while ( !context.isStopped() && hourIter.hasNext() ) {
                                final Resource hourResource = hourIter.next();
                                final int hour = Integer.valueOf(hourResource.getName());
                                final boolean oldHour = oldDay || hour < removeDate.get(Calendar.HOUR_OF_DAY);

                                // minutes
                                final Iterator<Resource> minuteIter = hourResource.listChildren();
                                while ( !context.isStopped() && minuteIter.hasNext() ) {
                                    final Resource minuteResource = minuteIter.next();

                                    // check if we can delete the minute
                                    final int minute = Integer.valueOf(minuteResource.getName());
                                    final boolean oldMinute = oldHour || minute <= removeDate.get(Calendar.MINUTE);
                                    if ( oldMinute ) {
                                        final Iterator<Resource> jobIter = minuteResource.listChildren();
                                        while ( !context.isStopped() && jobIter.hasNext() ) {
                                            final Resource jobResource = jobIter.next();
                                            boolean remove = stateList == null;
                                            if ( !remove ) {
                                                final ValueMap vm = ResourceUtil.getValueMap(jobResource);
                                                final String state = vm.get(JobImpl.PROPERTY_FINISHED_STATE, String.class);
                                                if ( state != null && stateList.contains(state) ) {
                                                    remove = true;
                                                }
                                            }
                                            if ( remove ) {
                                                resolver.delete(jobResource);
                                                resolver.commit();
                                            }
                                        }
                                    }
                                    // check if we can delete the minute
                                    if ( !context.isStopped() && oldMinute && !minuteResource.listChildren().hasNext()) {
                                        resolver.delete(minuteResource);
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

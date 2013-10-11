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
package org.apache.sling.event.impl.jobs.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.event.impl.jobs.JobManagerImpl;
import org.apache.sling.event.impl.jobs.Utility;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.impl.support.ScheduleInfoImpl;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduleInfo;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component handling scheduled job OSGi configurations
 */
@Component
@Service(value=TopologyEventListener.class)
@Reference(name="config", cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
           policy=ReferencePolicy.DYNAMIC, referenceInterface=ScheduledJobConfiguration.class)
public class ScheduledJobConfigurationManager implements TopologyEventListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AtomicBoolean isLeader = new AtomicBoolean(false);

    private final Map<String, List<Schedule>> configs = new HashMap<String, List<Schedule>>();

    @Reference
    private JobManager jobManager;

    @Override
    public void handleTopologyEvent(final TopologyEvent event) {
        if ( event.getType() == Type.TOPOLOGY_CHANGED || event.getType() == Type.TOPOLOGY_INIT ) {
            synchronized ( this.configs ) {
                final boolean wasLeader = this.isLeader.get();
                this.isLeader.set(event.getNewView().getLocalInstance().isLeader());
                if ( !wasLeader && this.isLeader.get() ) {
                    this.updateSchedules();
                }
            }
        }
    }

    /**
     * Bind a new configuration
     */
    protected void bindConfig(final ScheduledJobConfiguration config, final Map<String, Object> properties) {
        // create schedule
        final Schedule schedule = new Schedule(config, properties);

        if ( schedule.errors.size() > 0 ) {
            logger.warn("Ignoring job schedule configuration: {}. Reason(s): {}", schedule,
                    schedule.errors);
            return;
        }

        synchronized ( configs ) {
            List<Schedule> schedules = this.configs.get(schedule.scheduleName);
            if ( schedules == null ) {
                schedules = new ArrayList<ScheduledJobConfigurationManager.Schedule>();
                this.configs.put(schedule.scheduleName, schedules);
            }
            schedules.add(schedule);
            Collections.sort(schedules);
            if ( this.isLeader.get() && schedules.get(0) == schedule ) {
                if ( schedules.size() > 1 ) {
                    this.stop(schedule.scheduleName);
                }
            }
        }
    }

    /**
     * Unbind a configuration
     */
    protected void unbindConfig(final ServiceReference ref) {
        final String scheduleName = PropertiesUtil.toString(ref.getProperty(ResourceHelper.PROPERTY_SCHEDULE_NAME), null);
        if ( scheduleName != null && scheduleName.length() > 0 ) {
            final long serviceId = (Long)ref.getProperty(Constants.SERVICE_ID);
            synchronized ( configs ) {
                List<Schedule> schedules = this.configs.get(scheduleName);
                if ( schedules != null ) {
                    boolean isFirst = true;
                    boolean update = false;
                    final Iterator<Schedule> i = schedules.iterator();
                    while ( i.hasNext() ) {
                        final Schedule current = i.next();
                        if ( current.serviceId == serviceId ) {
                            if ( isFirst ) {
                                update = true;
                            }
                            i.remove();
                            break;
                        }
                        isFirst = false;
                    }
                    if ( schedules.size() == 0 ) {
                        this.configs.remove(scheduleName);
                    }
                    if ( update && this.isLeader.get() ) {
                        this.stop(scheduleName);
                        if ( schedules.size() > 0 ) {
                            this.start(schedules.get(0));
                        }
                    }
                }
            }
        }
    }

    private void start(final Schedule s) {
        final List<String> errors = new ArrayList<String>();
        ((JobManagerImpl)jobManager).addScheduledJob(s.jobTopic, null, s.jobProperties, s.scheduleName, s.suspended, s.scheduleInfos, errors);
        if ( errors.size() > 0 ) {
            logger.error("Unable to schedule job from configuration: {} : {}", errors, s);
        }
    }

    private void stop(final String scheduleName) {
        final ScheduledJobInfo info = this.jobManager.getScheduledJob(scheduleName);
        if ( info != null ) {
            info.unschedule();
        }
    }

    private void updateSchedules() {
        for(final List<Schedule> schedules : this.configs.values() ) {
            final Schedule s = schedules.get(0);
            this.start(s);
        }
    }

    private static final class Schedule implements Comparable<Schedule> {

        public final int ranking;
        public final long serviceId;
        public final ScheduleInfo.ScheduleType scheduleType;
        public final String jobTopic;
        public final String scheduleName;
        public final boolean suspended;
        public final Map<String, Object> jobProperties;
        public final List<ScheduleInfoImpl> scheduleInfos = new ArrayList<ScheduleInfoImpl>();

        public final List<String> errors = new ArrayList<String>();

        public Schedule(final ScheduledJobConfiguration config, final Map<String, Object> properties) {
            final Object sr = properties.get(Constants.SERVICE_RANKING);
            if ( sr == null || !(sr instanceof Integer)) {
                this.ranking = 0;
            } else {
                this.ranking = (Integer)sr;
            }
            this.serviceId = (Long)properties.get(Constants.SERVICE_ID);
            final Map<String, Object> configProperties = config.getConfiguration();
            // type
            final String scheduleTypeString = PropertiesUtil.toString(configProperties.get(ResourceHelper.PROPERTY_SCHEDULE_INFO_TYPE), null);
            ScheduleInfo.ScheduleType sType = null;
            if ( scheduleTypeString != null ) {
                try {
                    sType = ScheduleInfo.ScheduleType.valueOf(scheduleTypeString);
                } catch ( final IllegalArgumentException iae) {
                    // ignore
                }
            }
            this.scheduleType = sType;
            if ( this.scheduleType == null ) {
                this.errors.add("No valid schedule type set: " + scheduleTypeString);
            } else {
                // Schedule info
                final String[] scheduleConfigs = PropertiesUtil.toStringArray(configProperties.get(ResourceHelper.PROPERTY_SCHEDULE_INFO), null);
                if ( scheduleConfigs != null ) {
                    for(final String s : scheduleConfigs) {
                        final ScheduleInfoImpl info = ScheduleInfoImpl.deserialize(this.scheduleType, s.trim());
                        if ( info != null ) {
                            this.scheduleInfos.add(info);
                        }
                    }
                }
                if ( this.scheduleInfos.size() == 0 || this.scheduleInfos.size() < scheduleConfigs.length ) {
                    this.errors.add("Either no schedules or invalid schedules found: " + Arrays.toString(scheduleConfigs));
                }
            }
            this.jobTopic = PropertiesUtil.toString(configProperties.get(ResourceHelper.PROPERTY_JOB_TOPIC), null);
            this.scheduleName = PropertiesUtil.toString(configProperties.get(ResourceHelper.PROPERTY_SCHEDULE_NAME), null);
            this.suspended = PropertiesUtil.toBoolean(configProperties.get(ResourceHelper.PROPERTY_SCHEDULE_SUSPENDED), false);
            this.jobProperties = new HashMap<String, Object>(configProperties);
            final Iterator<String> nameIter = this.jobProperties.keySet().iterator();
            while ( nameIter.hasNext() ) {
                final String name = nameIter.next();
                if ( name.startsWith("service.")
                     || name.startsWith("component.")
                     || name.equals(ResourceHelper.PROPERTY_SCHEDULE_NAME)
                     || name.equals(ResourceHelper.PROPERTY_SCHEDULE_INFO)
                     || name.equals(ResourceHelper.PROPERTY_SCHEDULE_INFO_TYPE)
                     || name.equals(ResourceHelper.PROPERTY_SCHEDULE_SUSPENDED)
                     || name.equals(ResourceHelper.PROPERTY_JOB_TOPIC) ) {
                    nameIter.remove();
                }
            }

            if ( this.scheduleName == null || this.scheduleName.trim().length() == 0 ) {
                this.errors.add("Schedule name missign.");
                return;
            }
            final String errorMessage = Utility.checkJob(this.jobTopic, this.jobProperties);
            if ( errorMessage != null ) {
                this.errors.add(errorMessage);
            }
            for(final ScheduleInfoImpl info : this.scheduleInfos) {
                info.check(this.errors);
            }
        }

        @Override
        public String toString() {
            return "Schedule [ranking=" + ranking + ", serviceId=" + serviceId
                    + ", scheduleType=" + scheduleType + ", jobTopic="
                    + jobTopic + ", scheduleName=" + scheduleName
                    + ", suspended=" + suspended + ", jobProperties="
                    + jobProperties + ", scheduleInfos=" + scheduleInfos + ", errors=" + errors
                    + "]";
        }

        @Override
        public int compareTo(final Schedule o) {
            if ( this.ranking < o.ranking ) {
                return 1;
            } else if (this.ranking > o.ranking ) {
                return -1;
            }
            // If ranks are equal, then sort by service id in descending order.
            return (this.serviceId < o.serviceId) ? -1 : 1;
        }

        @Override
        public boolean equals(final Object obj) {
            if ( obj instanceof Schedule ) {
                return ((Schedule)obj).serviceId == this.serviceId;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.scheduleName.hashCode();
        }
    }
}

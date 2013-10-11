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

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.osgi.framework.Constants;

@Component(name="org.apache.sling.event.jobs.ScheduledJobConfiguration",
        configurationFactory=true,policy=ConfigurationPolicy.REQUIRE,
        metatype=true, label="Apache Sling Scheduled Job",
        description="Configuration for a scheduled job.")
@Service(value={ScheduledJobConfiguration.class})
@Properties({
    @Property(name=ResourceHelper.PROPERTY_SCHEDULE_NAME, label="Schedule Name", description="Unique schedule name", value=""),
    @Property(name=ResourceHelper.PROPERTY_JOB_TOPIC, label="Job Topic", description="The topic of the scheduled job.", value=""),
    @Property(name=ResourceHelper.PROPERTY_SCHEDULE_INFO_TYPE, label="Schedule Type", description="Define the schedule frequency.",
        value="WEEKLY",
        options={@PropertyOption(name="WEEKLY",value="Weekly"),
                 @PropertyOption(name="DAILY",value="Daily"),
                 @PropertyOption(name="HOURLY",value="Hourly")}),
    @Property(name=ResourceHelper.PROPERTY_SCHEDULE_INFO, unbounded=PropertyUnbounded.ARRAY, value="",
            label="Schedules", description="This value depends on the type. For a weekly schedule three numbers"
                    + " separated by a colon must be used for each schedule, the first number specifying the day (1-7)"
                    + " the second the hour (0-23) and the third the minute (0-59). For a daily schedule two numbers"
                    + " separated by a colon for hour and minute must be specified and for hourly a single number"
                    + " (0-59) for the minute is required."),
    @Property(name=ResourceHelper.PROPERTY_SCHEDULE_SUSPENDED, boolValue=false,
              label="Suspended", description="If this flag is set, the schedule is currently suspended (inactive)"),
    @Property(name=Constants.SERVICE_RANKING, intValue=0, propertyPrivate=false,
              label="Ranking", description="Configuration ranking - if there is more than one configuration"
                    +" with the same name, the one with the highest ranking is used.")
})
public class ScheduledJobConfiguration {


    private Map<String, Object> configuration;

    @Activate
    protected void activate(final Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    @Deactivate
    protected void deactivate() {
        this.configuration = null;
    }

    public Map<String, Object> getConfiguration() {
        return this.configuration;
    }
}

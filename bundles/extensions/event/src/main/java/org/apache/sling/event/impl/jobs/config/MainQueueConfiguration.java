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

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is the configuration for the main queue.
 *
 */
@Component(label="Apache Sling Job Default Queue",
           description="The configuration of the default job queue.",
           name="org.apache.sling.event.impl.jobs.DefaultJobManager",
           metatype=true)
@Service(value=MainQueueConfiguration.class)
@Properties({
    @Property(name=ConfigurationConstants.PROP_PRIORITY,
              value=ConfigurationConstants.DEFAULT_PRIORITY,
              options={@PropertyOption(name="NORM",value="Norm"),
                       @PropertyOption(name="MIN",value="Min"),
                       @PropertyOption(name="MAX",value="Max")},
              label="Priority",
              description="The priority for the threads used by this queue. Default is norm."),
    @Property(name=ConfigurationConstants.PROP_RETRIES,
            intValue=ConfigurationConstants.DEFAULT_RETRIES,
            label="Maximum Retries",
            description="The maximum number of times a failed job slated "
                      + "for retries is actually retried. If a job has been retried this number of "
                      + "times and still fails, it is not rescheduled and assumed to have failed. The "
                      + "default value is 10."),
    @Property(name=ConfigurationConstants.PROP_RETRY_DELAY,
            longValue=ConfigurationConstants.DEFAULT_RETRY_DELAY,
            label="Retry Delay",
            description="The number of milliseconds to sleep between two "
                      + "consecutive retries of a job which failed and was set to be retried. The "
                      + "default value is 2 seconds. This value is only relevant if there is a single "
                      + "failed job in the queue. If there are multiple failed jobs, each job is "
                      + "retried in turn without an intervening delay."),
    @Property(name=ConfigurationConstants.PROP_MAX_PARALLEL,
            intValue=ConfigurationConstants.DEFAULT_MAX_PARALLEL,
            label="Maximum Parallel Jobs",
            description="The maximum number of parallel jobs started for this queue. "
                      + "A value of -1 is substituted with the number of available processors."),
})
public class MainQueueConfiguration {

    public static final String MAIN_QUEUE_NAME = "<main queue>";

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private InternalQueueConfiguration mainConfiguration;

    /**
     * Activate this component.
     * @param props Configuration properties
     */
    @Activate
    protected void activate(final Map<String, Object> props) {
        this.update(props);
    }

    /**
     * Configure this component.
     * @param props Configuration properties
     */
    @Modified
    protected void update(final Map<String, Object> props) {
        // create a new dictionary with the missing info and do some sanity puts
        final Map<String, Object> queueProps = new HashMap<String, Object>(props);
        queueProps.put(ConfigurationConstants.PROP_TOPICS, "*");
        queueProps.put(ConfigurationConstants.PROP_NAME, MAIN_QUEUE_NAME);
        queueProps.put(ConfigurationConstants.PROP_TYPE, InternalQueueConfiguration.Type.UNORDERED);

        // check max parallel - this should never be lower than 2!
        final int maxParallel = PropertiesUtil.toInteger(queueProps.get(ConfigurationConstants.PROP_MAX_PARALLEL),
                ConfigurationConstants.DEFAULT_MAX_PARALLEL);
        if ( maxParallel < 2 ) {
            this.logger.debug("Ignoring invalid setting of {} for {}. Setting to minimum value: 2",
                    maxParallel, ConfigurationConstants.PROP_MAX_PARALLEL);
            queueProps.put(ConfigurationConstants.PROP_MAX_PARALLEL, 2);
        }
        this.mainConfiguration = InternalQueueConfiguration.fromConfiguration(queueProps);
    }

    /**
     * Return the main queue configuration object.
     * @return The main queue configuration object.
     */
    public InternalQueueConfiguration getMainConfiguration() {
        return this.mainConfiguration;
    }
}

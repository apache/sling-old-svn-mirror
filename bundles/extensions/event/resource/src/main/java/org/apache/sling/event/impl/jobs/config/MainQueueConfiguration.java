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

import java.lang.annotation.Annotation;
import java.util.Collections;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is the configuration for the main queue.
 *
 */
@Component(name="org.apache.sling.event.impl.jobs.DefaultJobManager",
           service=MainQueueConfiguration.class,
           property={
                   Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
           })
@Designate(ocd=MainQueueConfiguration.Config.class)
public class MainQueueConfiguration {

    @ObjectClassDefinition(name = "Apache Sling Job Default Queue",
           description="The configuration of the default job queue.")
    public @interface Config {

        @AttributeDefinition(
                name="Priority",
                description="The priority for the threads used by this queue. Default is norm.",
                options = {
                        @Option(label="Norm",value="NORM"),
                        @Option(label="Min",value="MIN"),
                        @Option(label="Max",value="MAX")
                })
        String queue_priority() default ConfigurationConstants.DEFAULT_PRIORITY;

        @AttributeDefinition(name="Maximum Retries",
              description="The maximum number of times a failed job slated "
                        + "for retries is actually retried. If a job has been retried this number of "
                        + "times and still fails, it is not rescheduled and assumed to have failed. The "
                        + "default value is 10.")
        int queue_retries() default ConfigurationConstants.DEFAULT_RETRIES;

        @AttributeDefinition(name="Retry Delay",
              description="The number of milliseconds to sleep between two "
                        + "consecutive retries of a job which failed and was set to be retried. The "
                        + "default value is 2 seconds. This value is only relevant if there is a single "
                        + "failed job in the queue. If there are multiple failed jobs, each job is "
                        + "retried in turn without an intervening delay.")
        long queue_retrydelay() default ConfigurationConstants.DEFAULT_RETRY_DELAY;

        @AttributeDefinition(name="Maximum Parallel Jobs",
              description="The maximum number of parallel jobs started for this queue. "
                        + "A value of -1 is substituted with the number of available processors.")
        int queue_maxparallel() default ConfigurationConstants.DEFAULT_MAX_PARALLEL;
    }

    public static final String MAIN_QUEUE_NAME = "<main queue>";

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private InternalQueueConfiguration mainConfiguration;

    /**
     * Activate this component.
     * @param config Configuration properties
     */
    @Activate
    protected void activate(final Config config) {
        this.update(config);
    }

    /**
     * Configure this component.
     * @param config Configuration properties
     */
    @Modified
    protected void update(final Config config) {
        logger.debug("properties for queue {}: {}", MAIN_QUEUE_NAME, config);
        this.mainConfiguration = InternalQueueConfiguration.fromConfiguration(
                Collections.singletonMap(Constants.SERVICE_PID, (Object)"org.apache.sling.event.impl.jobs.DefaultJobManager"),
                new InternalQueueConfiguration.Config() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return InternalQueueConfiguration.Config.class;
            }

            @Override
            public int service_ranking() {
                return 0;
            }

            @Override
            public String queue_type() {
                return InternalQueueConfiguration.Type.UNORDERED.name();
            }

            @Override
            public String[] queue_topics() {
                return new String[] {"*"};
            }

            @Override
            public int queue_threadPoolSize() {
                return 0;
            }

            @Override
            public long queue_retrydelay() {
                return config.queue_retrydelay();
            }

            @Override
            public int queue_retries() {
                return config.queue_retries();
            }

            @Override
            public String queue_priority() {
                return config.queue_priority();
            }

            @Override
            public boolean queue_preferRunOnCreationInstance() {
                return false;
            }

            @Override
            public String queue_name() {
                return MAIN_QUEUE_NAME;
            }

            @Override
            public double queue_maxparallel() {
                return config.queue_maxparallel();
            }

            @Override
            public boolean queue_keepJobs() {
                return false;
            }

            @Override
            public String webconsole_configurationFactory_nameHint() {
                return "Queue: {" + ConfigurationConstants.PROP_NAME + "}";
            }
        });
    }

    /**
     * Return the main queue configuration object.
     * @return The main queue configuration object.
     */
    public InternalQueueConfiguration getMainConfiguration() {
        return this.mainConfiguration;
    }
}

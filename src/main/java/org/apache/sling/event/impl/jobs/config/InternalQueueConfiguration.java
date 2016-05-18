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

import java.util.Arrays;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.impl.support.TopicMatcher;
import org.apache.sling.event.impl.support.TopicMatcherHelper;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype=true,
           name="org.apache.sling.event.jobs.QueueConfiguration",
           label="Apache Sling Job Queue Configuration",
           description="The configuration of a job processing queue.",
           configurationFactory=true, policy=ConfigurationPolicy.REQUIRE)
@Service(value={InternalQueueConfiguration.class})
@Properties({
    @Property(name=ConfigurationConstants.PROP_NAME,
              label="Name",
              description="The name of the queue. If matching is used the token {0} can be used to substitute the real value."),
    @Property(name=ConfigurationConstants.PROP_TOPICS,
              unbounded=PropertyUnbounded.ARRAY,
              label="Topics",
              description="This value is required and lists the topics processed by "
                        + "this queue. The value is a list of strings. If a string ends with a dot, "
                        + "all topics in exactly this package match. If the string ends with a star, "
                        + "all topics in this package and all subpackages match. If the string neither "
                        + "ends with a dot nor with a star, this is assumed to define an exact topic."),
    @Property(name=ConfigurationConstants.PROP_TYPE,
              value=ConfigurationConstants.DEFAULT_TYPE,
              options={@PropertyOption(name="UNORDERED",value="Parallel"),
                       @PropertyOption(name="ORDERED",value="Ordered"),
                       @PropertyOption(name="TOPIC_ROUND_ROBIN",value="Topic Round Robin")},
              label="Type",
              description="The queue type."),
    @Property(name=ConfigurationConstants.PROP_MAX_PARALLEL,
              doubleValue=ConfigurationConstants.DEFAULT_MAX_PARALLEL,
              label="Maximum Parallel Jobs",
              description="The maximum number of parallel jobs started for this queue. "
                        + "A value of -1 is substituted with the number of available processors. "
                        + "Positive integer values specify number of processors to use.  Can be greater than number of processors. "
                        + "A decimal number between 0.0 and 1.0 is treated as a fraction of available processors. "
                        + "For example 0.5 means half of the available processors."),
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
    @Property(name=ConfigurationConstants.PROP_PRIORITY,
              value=ConfigurationConstants.DEFAULT_PRIORITY,
              options={@PropertyOption(name="NORM",value="Norm"),
                       @PropertyOption(name="MIN",value="Min"),
                       @PropertyOption(name="MAX",value="Max")},
              label="Priority",
              description="The priority for the threads used by this queue. Default is norm."),
    @Property(name=ConfigurationConstants.PROP_KEEP_JOBS,
              boolValue=ConfigurationConstants.DEFAULT_KEEP_JOBS,
              label="Keep History",
              description="If this option is enabled, successful finished jobs are kept "
                        + "to provide a complete history."),
    @Property(name=ConfigurationConstants.PROP_PREFER_RUN_ON_CREATION_INSTANCE,
              boolValue=ConfigurationConstants.DEFAULT_PREFER_RUN_ON_CREATION_INSTANCE,
              label="Prefer Creation Instance",
              description="If this option is enabled, the jobs are tried to "
                        + "be run on the instance where the job was created."),
    @Property(name=ConfigurationConstants.PROP_THREAD_POOL_SIZE,
              intValue=ConfigurationConstants.DEFAULT_THREAD_POOL_SIZE,
              label="Thread Pool Size",
              description="Optional configuration value for a thread pool to be used by "
                        + "this queue. If this is value has a positive number of threads configuration, this queue uses "
                        + "an own thread pool with the configured number of threads."),
    @Property(name=Constants.SERVICE_RANKING,
              intValue=0,
              propertyPrivate=false,
              label="Ranking",
              description="Integer value defining the ranking of this queue configuration. "
                        + "If more than one queue matches a job topic, the one with the highest ranking is used."),
    @Property(name="webconsole.configurationFactory.nameHint", value="Queue: {" + ConfigurationConstants.PROP_NAME + "}")
})
public class InternalQueueConfiguration
    implements QueueConfiguration, Comparable<InternalQueueConfiguration> {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The name of the queue. */
    private String name;

    /** The queue type. */
    private Type type;

    /** Number of retries. */
    private int retries;

    /** Retry delay. */
    private long retryDelay;

    /** Thread priority. */
    private ThreadPriority priority;

    /** The maximum number of parallel processes (for non ordered queues) */
    private int maxParallelProcesses;

    /** The ordering. */
    private int serviceRanking;

    /** The matchers for topics. */
    private TopicMatcher[] matchers;

    /** The configured topics. */
    private String[] topics;

    /** Keep jobs. */
    private boolean keepJobs;

    /** Valid flag. */
    private boolean valid = false;

    /** Optional thread pool size. */
    private int ownThreadPoolSize;

    /** Prefer creation instance. */
    private boolean preferCreationInstance;

    private String pid;

    /**
     * Create a new configuration from a config
     */
    public static InternalQueueConfiguration fromConfiguration(final Map<String, Object> params) {
        final InternalQueueConfiguration c = new InternalQueueConfiguration();
        c.activate(params);
        return c;
    }

    public InternalQueueConfiguration() {
        // nothing to do, see activate
    }

    /**
     * Create a new queue configuration
     */
    @Activate
    protected void activate(final Map<String, Object> params) {
        this.name = PropertiesUtil.toString(params.get(ConfigurationConstants.PROP_NAME), null);
        try {
            this.priority = ThreadPriority.valueOf(PropertiesUtil.toString(params.get(ConfigurationConstants.PROP_PRIORITY), ConfigurationConstants.DEFAULT_PRIORITY));
        } catch ( final IllegalArgumentException iae) {
            logger.warn("Invalid value for queue priority. Using default instead of : {}", params.get(ConfigurationConstants.PROP_PRIORITY));
            this.priority = ThreadPriority.valueOf(ConfigurationConstants.DEFAULT_PRIORITY);
        }
        try {
            this.type = Type.valueOf(PropertiesUtil.toString(params.get(ConfigurationConstants.PROP_TYPE), ConfigurationConstants.DEFAULT_TYPE));
        } catch ( final IllegalArgumentException iae) {
            logger.error("Invalid value for queue type configuration: {}", params.get(ConfigurationConstants.PROP_TYPE));
            this.type = null;
        }
        this.retries = PropertiesUtil.toInteger(params.get(ConfigurationConstants.PROP_RETRIES), ConfigurationConstants.DEFAULT_RETRIES);
        this.retryDelay = PropertiesUtil.toLong(params.get(ConfigurationConstants.PROP_RETRY_DELAY), ConfigurationConstants.DEFAULT_RETRY_DELAY);

        // Float values are treated as percentage.  int values are treated as number of cores, -1 == all available
        // Note: the value is based on the core count at startup.  It will not change dynamically if core count changes.
        int cores = ConfigurationConstants.NUMBER_OF_PROCESSORS;
        final double inMaxParallel = PropertiesUtil.toDouble(params.get(ConfigurationConstants.PROP_MAX_PARALLEL),
                ConfigurationConstants.DEFAULT_MAX_PARALLEL);
        logger.debug("Max parallel for queue {} is {}", this.name, inMaxParallel);
        if ((inMaxParallel == Math.floor(inMaxParallel)) && !Double.isInfinite(inMaxParallel)) {
            // integral type
            if ((int) inMaxParallel == 0) {
                logger.warn("Max threads property for {} set to zero.", this.name);
            }
            this.maxParallelProcesses = (inMaxParallel <= -1 ? cores : (int) inMaxParallel);
        } else {
            // percentage (rounded)
            if ((inMaxParallel > 0.0) && (inMaxParallel < 1.0)) {
                this.maxParallelProcesses = (int) Math.round(cores * inMaxParallel);
            } else {
                logger.warn("Invalid queue max parallel value for queue {}. Using {}", this.name, cores);
                this.maxParallelProcesses =  cores;
            }
        }
        logger.debug("Thread pool size for {} was set to {}", this.name, this.maxParallelProcesses);

        // ignore parallel setting for ordered queues
        if ( this.type == Type.ORDERED ) {
            this.maxParallelProcesses = 1;
        }
        final String[] topicsParam = PropertiesUtil.toStringArray(params.get(ConfigurationConstants.PROP_TOPICS));
        this.matchers = TopicMatcherHelper.buildMatchers(topicsParam);
        if ( this.matchers == null ) {
            this.topics = null;
        } else {
            this.topics = topicsParam;
        }
        this.keepJobs = PropertiesUtil.toBoolean(params.get(ConfigurationConstants.PROP_KEEP_JOBS), ConfigurationConstants.DEFAULT_KEEP_JOBS);
        this.serviceRanking = PropertiesUtil.toInteger(params.get(Constants.SERVICE_RANKING), 0);
        this.ownThreadPoolSize = PropertiesUtil.toInteger(params.get(ConfigurationConstants.PROP_THREAD_POOL_SIZE), ConfigurationConstants.DEFAULT_THREAD_POOL_SIZE);
        this.preferCreationInstance = PropertiesUtil.toBoolean(params.get(ConfigurationConstants.PROP_PREFER_RUN_ON_CREATION_INSTANCE), ConfigurationConstants.DEFAULT_PREFER_RUN_ON_CREATION_INSTANCE);
        this.pid = (String)params.get(Constants.SERVICE_PID);
        this.valid = this.checkIsValid();
    }

    /**
     * Check if this configuration is valid,
     * If it is invalid, it is ignored.
     */
    private boolean checkIsValid() {
        if ( type == null ) {
            return false;
        }
        boolean hasMatchers = false;
        if ( this.matchers != null ) {
            for(final TopicMatcher m : this.matchers ) {
                if ( m != null ) {
                    hasMatchers = true;
                    break;
                }
            }
        }
        if ( !hasMatchers ) {
            return false;
        }
        if ( name == null || name.length() == 0 ) {
            return false;
        }
        if ( retries < -1 ) {
            return false;
        }
        if ( maxParallelProcesses < 1 ) {
            return false;
        }
        return true;
    }

    public boolean isValid() {
        return this.valid;
    }

    /**
     * Check if the queue processes the event.
     * @param topic The topic of the event
     * @return The queue name or <code>null</code>
     */
    public String match(final String topic) {
        if ( this.matchers != null ) {
            for(final TopicMatcher m : this.matchers ) {
                if ( m != null ) {
                    final String rep = m.match(topic);
                    if ( rep != null ) {
                        return this.name.replace("{0}", rep);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Return the name of the queue.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getRetryDelayInMs()
     */
    @Override
    public long getRetryDelayInMs() {
        return this.retryDelay;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getMaxRetries()
     */
    @Override
    public int getMaxRetries() {
        return this.retries;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getType()
     */
    @Override
    public Type getType() {
        return this.type;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getMaxParallel()
     */
    @Override
    public int getMaxParallel() {
        return this.maxParallelProcesses;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getTopics()
     */
    @Override
    public String[] getTopics() {
        return this.topics;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getRanking()
     */
    @Override
    public int getRanking() {
        return this.serviceRanking;
    }

    public String getPid() {
        return this.pid;
    }

    @Override
    public boolean isKeepJobs() {
        return this.keepJobs;
    }

    @Override
    public int getOwnThreadPoolSize() {
        return this.ownThreadPoolSize;
    }

    @Override
    public boolean isPreferRunOnCreationInstance() {
        return this.preferCreationInstance;
    }

    @Override
    public String toString() {
        return "Queue-Configuration(" + this.hashCode() + ") : {" +
            "name=" + this.name +
            ", type=" + this.type +
            ", topics=" + (this.matchers == null ? "[]" : Arrays.toString(this.matchers)) +
            ", maxParallelProcesses=" + this.maxParallelProcesses +
            ", retries=" + this.retries +
            ", retryDelayInMs=" + this.retryDelay +
            ", keepJobs=" + this.keepJobs +
            ", preferRunOnCreationInstance=" + this.preferCreationInstance +
            ", ownThreadPoolSize=" + this.ownThreadPoolSize +
            ", serviceRanking=" + this.serviceRanking +
            ", pid=" + this.pid +
            ", isValid=" + this.isValid() + "}";
    }

    @Override
    public int compareTo(final InternalQueueConfiguration other) {
        if ( this.serviceRanking < other.serviceRanking ) {
            return 1;
        } else if ( this.serviceRanking > other.serviceRanking ) {
            return -1;
        }
        return 0;
    }

    @Override
    public ThreadPriority getThreadPriority() {
        return this.priority;
    }
}

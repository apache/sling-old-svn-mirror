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
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.osgi.framework.Constants;

@Component(metatype=true,name="org.apache.sling.event.jobs.QueueConfiguration",
        label="%queue.name", description="%queue.description",
        configurationFactory=true,policy=ConfigurationPolicy.REQUIRE)
@Service(value={InternalQueueConfiguration.class})
@Properties({
    @Property(name=ConfigurationConstants.PROP_NAME),
    @Property(name=ConfigurationConstants.PROP_TYPE,
            value=ConfigurationConstants.DEFAULT_TYPE,
            options={@PropertyOption(name="UNORDERED",value="Parallel"),
                     @PropertyOption(name="ORDERED",value="Ordered"),
                     @PropertyOption(name="TOPIC_ROUND_ROBIN",value="Topic Round Robin"),
                     @PropertyOption(name="IGNORE",value="Ignore"),
                     @PropertyOption(name="DROP",value="Drop")}),
    @Property(name=ConfigurationConstants.PROP_TOPICS,
            unbounded=PropertyUnbounded.ARRAY),
    @Property(name=ConfigurationConstants.PROP_MAX_PARALLEL,
            intValue=ConfigurationConstants.DEFAULT_MAX_PARALLEL),
    @Property(name=ConfigurationConstants.PROP_RETRIES,
            intValue=ConfigurationConstants.DEFAULT_RETRIES),
    @Property(name=ConfigurationConstants.PROP_RETRY_DELAY,
            longValue=ConfigurationConstants.DEFAULT_RETRY_DELAY),
    @Property(name=ConfigurationConstants.PROP_PRIORITY,
            value=ConfigurationConstants.DEFAULT_PRIORITY,
            options={@PropertyOption(name="NORM",value="Norm"),
                     @PropertyOption(name="MIN",value="Min"),
                     @PropertyOption(name="MAX",value="Max")}),
    @Property(name=ConfigurationConstants.PROP_KEEP_JOBS,
              boolValue=ConfigurationConstants.DEFAULT_KEEP_JOBS),
    @Property(name=ConfigurationConstants.PROP_PREFER_RUN_ON_CREATION_INSTANCE,
              boolValue=ConfigurationConstants.DEFAULT_PREFER_RUN_ON_CREATION_INSTANCE),
    @Property(name=ConfigurationConstants.PROP_THREAD_POOL_SIZE,
              intValue=ConfigurationConstants.DEFAULT_THREAD_POOL_SIZE),
    @Property(name=Constants.SERVICE_RANKING, intValue=0, propertyPrivate=false,
              label="%queue.ranking.name", description="%queue.ranking.description")
})
public class InternalQueueConfiguration
    implements QueueConfiguration, Comparable<InternalQueueConfiguration> {

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
        this.priority = ThreadPriority.valueOf(PropertiesUtil.toString(params.get(ConfigurationConstants.PROP_PRIORITY), ConfigurationConstants.DEFAULT_PRIORITY));
        this.type = Type.valueOf(PropertiesUtil.toString(params.get(ConfigurationConstants.PROP_TYPE), ConfigurationConstants.DEFAULT_TYPE));
        this.retries = PropertiesUtil.toInteger(params.get(ConfigurationConstants.PROP_RETRIES), ConfigurationConstants.DEFAULT_RETRIES);
        this.retryDelay = PropertiesUtil.toLong(params.get(ConfigurationConstants.PROP_RETRY_DELAY), ConfigurationConstants.DEFAULT_RETRY_DELAY);
        final int maxParallel = PropertiesUtil.toInteger(params.get(ConfigurationConstants.PROP_MAX_PARALLEL), ConfigurationConstants.DEFAULT_MAX_PARALLEL);
        this.maxParallelProcesses = (maxParallel == -1 ? ConfigurationConstants.NUMBER_OF_PROCESSORS : maxParallel);
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
        if ( type == Type.UNORDERED || type == Type.TOPIC_ROUND_ROBIN ) {
            if ( maxParallelProcesses < 1 ) {
                return false;
            }
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
     * @see org.apache.sling.event.jobs.QueueConfiguration#getPriority()
     */
    @Override
    public JobUtil.JobPriority getPriority() {
        return JobUtil.JobPriority.valueOf(this.priority.name());
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getMaxParallel()
     */
    @Override
    public int getMaxParallel() {
        return this.maxParallelProcesses;
    }

    @Override
    @Deprecated
    public boolean isLocalQueue() {
        return false;
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
    @Deprecated
    public String[] getApplicationIds() {
        return null;
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

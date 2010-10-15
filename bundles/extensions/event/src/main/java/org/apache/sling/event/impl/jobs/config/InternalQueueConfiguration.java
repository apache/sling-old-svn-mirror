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
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.jobs.JobEvent;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.osgi.framework.Constants;
import org.osgi.service.event.Event;

@Component(metatype=true,name="org.apache.sling.event.jobs.QueueConfiguration",
        label="%queue.name", description="%queue.description",
        configurationFactory=true,policy=ConfigurationPolicy.REQUIRE)
@Service(value=InternalQueueConfiguration.class)
@Properties({
    @Property(name=ConfigurationConstants.PROP_NAME),
    @Property(name=ConfigurationConstants.PROP_TYPE,
            value=ConfigurationConstants.DEFAULT_TYPE,
            options={@PropertyOption(name="UNORDERED",value="Parallel"),
                     @PropertyOption(name="ORDERED",value="Ordered"),
                     @PropertyOption(name="TOPIC_ROUND_ROBIN",value="Topic Round Robin"),
                     @PropertyOption(name="IGNORE",value="Ignore")}),
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
    @Property(name=ConfigurationConstants.PROP_RUN_LOCAL,
            boolValue=ConfigurationConstants.DEFAULT_RUN_LOCAL),
    @Property(name=ConfigurationConstants.PROP_APP_IDS,
            unbounded=PropertyUnbounded.ARRAY)
})
public class InternalQueueConfiguration
    implements QueueConfiguration {

    /** The name of the queue. */
    private String name;

    /** The queue type. */
    private Type type;

    /** Number of retries. */
    private int retries;

    /** Retry delay. */
    private long retryDelay;

    /** Local queue? */
    private boolean runLocal;

    /** Thread priority. */
    private JobUtil.JobPriority priority;

    /** The maximum number of parallel processes (for non ordered queues) */
    private int maxParallelProcesses;

    /** Optional application ids where this queue is running on. */
    private String[] applicationIds;

    /** The ordering. */
    private int serviceRanking;

    /** The matchers for topics. */
    private Matcher[] matchers;

    /** The configured topics. */
    private String[] topics;

    /** Valid flag. */
    private boolean valid = false;

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
        this.name = OsgiUtil.toString(params.get(ConfigurationConstants.PROP_NAME), null);
        this.priority = JobUtil.JobPriority.valueOf(OsgiUtil.toString(params.get(ConfigurationConstants.PROP_PRIORITY), ConfigurationConstants.DEFAULT_PRIORITY));
        this.type = Type.valueOf(OsgiUtil.toString(params.get(ConfigurationConstants.PROP_TYPE), ConfigurationConstants.DEFAULT_TYPE));
        this.runLocal = OsgiUtil.toBoolean(params.get(ConfigurationConstants.PROP_RUN_LOCAL), ConfigurationConstants.DEFAULT_RUN_LOCAL);
        this.retries = OsgiUtil.toInteger(params.get(ConfigurationConstants.PROP_RETRIES), ConfigurationConstants.DEFAULT_RETRIES);
        this.retryDelay = OsgiUtil.toLong(params.get(ConfigurationConstants.PROP_RETRY_DELAY), ConfigurationConstants.DEFAULT_RETRY_DELAY);
        final int maxParallel = OsgiUtil.toInteger(params.get(ConfigurationConstants.PROP_MAX_PARALLEL), ConfigurationConstants.DEFAULT_MAX_PARALLEL);
        this.maxParallelProcesses = (maxParallel == -1 ? ConfigurationConstants.NUMBER_OF_PROCESSORS : maxParallel);
        final String appIds[] = OsgiUtil.toStringArray(params.get(ConfigurationConstants.PROP_APP_IDS));
        if ( appIds == null
             || appIds.length == 0
             || (appIds.length == 1 && (appIds[0] == null || appIds[0].length() == 0)) ) {
            this.applicationIds = null;
        } else {
            this.applicationIds = appIds;
        }
        final String[] topicsParam = OsgiUtil.toStringArray(params.get(ConfigurationConstants.PROP_TOPICS));
        if ( topicsParam == null
             || topicsParam.length == 0
             || (topicsParam.length == 1 && (topicsParam[0] == null || topicsParam[0].length() == 0))) {
            matchers = null;
            this.topics = null;
        } else {
            final Matcher[] newMatchers = new Matcher[topicsParam.length];
            for(int i=0; i < topicsParam.length; i++) {
                String value = topicsParam[i];
                if ( value != null ) {
                    value = value.trim();
                }
                if ( value != null && value.length() > 0 ) {
                    if ( value.endsWith(".") ) {
                        newMatchers[i] = new PackageMatcher(value);
                    } else if ( value.endsWith("*") ) {
                        newMatchers[i] = new SubPackageMatcher(value);
                    } else {
                        newMatchers[i] = new ClassMatcher(value);
                    }
                }
            }
            matchers = newMatchers;
            this.topics = topicsParam;
        }
        this.serviceRanking = OsgiUtil.toInteger(params.get(Constants.SERVICE_RANKING), 0);
        this.pid = (String)params.get(Constants.SERVICE_PID);
        this.valid = this.checkIsValid();
    }

    public InternalQueueConfiguration(final Event jobEvent) {
        this.name = (String)jobEvent.getProperty(JobUtil.PROPERTY_JOB_QUEUE_NAME);
        if ( jobEvent.getProperty(JobUtil.PROPERTY_JOB_QUEUE_ORDERED) != null ) {
            this.type = Type.ORDERED;
            this.maxParallelProcesses = 1;
        } else {
            this.type = Type.UNORDERED;
            int maxPar = ConfigurationConstants.DEFAULT_MAX_PARALLEL;
            final Object value = jobEvent.getProperty(JobUtil.PROPERTY_JOB_PARALLEL);
            if ( value != null ) {
                if ( value instanceof Boolean ) {
                    final boolean result = ((Boolean)value).booleanValue();
                    if ( !result ) {
                        maxPar = 1;
                    }
                } else if ( value instanceof Number ) {
                    final int result = ((Number)value).intValue();
                    if ( result > 1 ) {
                        maxPar = result;
                    } else {
                        maxPar = 1;
                    }
                } else {
                    final String strValue = value.toString();
                    if ( "no".equalsIgnoreCase(strValue) || "false".equalsIgnoreCase(strValue) ) {
                        maxPar = 1;
                    } else {
                        // check if this is a number
                        try {
                            final int result = Integer.valueOf(strValue).intValue();
                            if ( result > 1 ) {
                                maxPar = result;
                            } else {
                                maxPar = 1;
                            }
                        } catch (NumberFormatException ne) {
                            // we ignore this
                        }
                    }
                }
            }
            if ( maxPar == -1 ) {
                maxPar = ConfigurationConstants.NUMBER_OF_PROCESSORS;
            }
            this.maxParallelProcesses = maxPar;
        }
        this.priority = JobUtil.JobPriority.valueOf(ConfigurationConstants.DEFAULT_PRIORITY);
        this.runLocal = false;
        this.retries = ConfigurationConstants.DEFAULT_RETRIES;
        this.retryDelay = ConfigurationConstants.DEFAULT_RETRY_DELAY;
        this.serviceRanking = 0;
        this.applicationIds = null;
        this.matchers = null;
        this.topics = new String[] {"<Custom:" + jobEvent.getProperty(JobUtil.PROPERTY_JOB_TOPIC) + ">"};
        this.valid = true;
    }

    /**
     * Check if this configuration is valid,
     * If it is invalid, it is ignored.
     */
    private boolean checkIsValid() {
        boolean hasMatchers = false;
        if ( this.matchers != null ) {
            for(final Matcher m : this.matchers ) {
                if ( m != null ) {
                    hasMatchers = true;
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
     * @param event The event
     */
    public boolean match(final JobEvent event) {
        final String topic = (String)event.event.getProperty(JobUtil.PROPERTY_JOB_TOPIC);
        if ( this.matchers != null ) {
            for(final Matcher m : this.matchers ) {
                if ( m != null ) {
                    final String rep = m.match(topic);
                    if ( rep != null ) {
                        event.queueName = this.name.replace("{0}", rep);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Return the name of the queue.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Checks if the event should be skipped.
     * This can happen if
     * - the queue is of type ignore
     * - the queue is bound to some application id
     * - the event is a local event generated with a different application id
     */
    public boolean isSkipped(final JobEvent event) {
        if ( this.type == Type.IGNORE ) {
            return true;
        }
        if ( this.applicationIds != null ) {
            boolean found = false;
            for(final String id : this.applicationIds) {
                if ( Environment.APPLICATION_ID.equals(id) ) {
                    found = true;
                }
            }
            if ( !found ) {
                return true;
            }
        }
        if ( this.runLocal
             && !event.event.getProperty(EventUtil.PROPERTY_APPLICATION).equals(Environment.APPLICATION_ID) ) {
            return true;
        }
        return false;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getRetryDelayInMs()
     */
    public long getRetryDelayInMs() {
        return this.retryDelay;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getMaxRetries()
     */
    public int getMaxRetries() {
        return this.retries;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getType()
     */
    public Type getType() {
        return this.type;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getPriority()
     */
    public JobUtil.JobPriority getPriority() {
        return this.priority;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getMaxParallel()
     */
    public int getMaxParallel() {
        return this.maxParallelProcesses;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#isLocalQueue()
     */
    public boolean isLocalQueue() {
        return this.runLocal;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getApplicationIds()
     */
    public String[] getApplicationIds() {
        return this.applicationIds;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getTopics()
     */
    public String[] getTopics() {
        return this.topics;
    }

    /**
     * @see org.apache.sling.event.jobs.QueueConfiguration#getRanking()
     */
    public int getRanking() {
        return this.serviceRanking;
    }

    public String getPid() {
        return this.pid;
    }

    @Override
    public String toString() {
        return "Queue-Configuration(" + this.hashCode() + ") : {" +
            "name=" + this.name +
            ", type=" + this.type +
            ", topics=" + (this.matchers == null ? "[]" : Arrays.toString(this.matchers)) +
            ", maxParallelProcesses=" + this.maxParallelProcesses +
            ", retries=" + this.retries +
            ", retryDelayInMs= " + this.retryDelay +
            ", applicationIds= " + (this.applicationIds == null ? "[]" : Arrays.toString(this.applicationIds)) +
            ", serviceRanking=" + this.serviceRanking +
            ", pid=" + this.pid +
            ", isValid=" + this.isValid() + "}";
    }

    /**
     * Internal interface for topic matching
     */
    private static interface Matcher {
        /** Check if the topic matches and return the variable part - null if not matching. */
        String match(String topic);
    }

    /** Package matcher - the topic must be in the same package. */
    private static final class PackageMatcher implements Matcher {

        private final String packageName;

        public PackageMatcher(final String name) {
            // remove last char and maybe a trailing slash
            int lastPos = name.length() - 1;
            if ( lastPos > 0 && name.charAt(lastPos - 1) == '/' ) {
                lastPos--;
            }
            this.packageName = name.substring(0, lastPos);
        }

        /**
         * @see org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration.Matcher#match(java.lang.String)
         */
        public String match(final String topic) {
            final int pos = topic.lastIndexOf('/');
            return pos > -1 && topic.substring(0, pos).equals(packageName) ? topic.substring(pos + 1) : null;
        }
    }

    /** Sub package matcher - the topic must be in the same package or a sub package. */
    private static final class SubPackageMatcher implements Matcher {
        private final String packageName;

        public SubPackageMatcher(final String name) {
            // remove last char and maybe a trailing slash
            int lastPos = name.length() - 1;
            if ( lastPos > 0 && name.charAt(lastPos - 1) == '/' ) {
                this.packageName = name.substring(0, lastPos);
            } else {
                this.packageName = name.substring(0, lastPos) + '/';
            }
        }

        /**
         * @see org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration.Matcher#match(java.lang.String)
         */
        public String match(final String topic) {
            final int pos = topic.lastIndexOf('/');
            return pos > -1 && topic.substring(0, pos + 1).startsWith(this.packageName) ? topic.substring(this.packageName.length()) : null;
        }
    }

    /** The topic must match exactly. */
    private static final class ClassMatcher implements Matcher {
        private final String className;

        public ClassMatcher(final String name) {
            this.className = name;
        }

        /**
         * @see org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration.Matcher#match(java.lang.String)
         */
        public String match(String topic) {
            return this.className.equals(topic) ? "" : null;
        }
    }

}

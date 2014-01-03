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
package org.apache.sling.event.impl.jobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.discovery.PropertyProvider;
import org.apache.sling.event.impl.support.TopicMatcher;
import org.apache.sling.event.impl.support.TopicMatcherHelper;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.event.jobs.consumer.JobConsumer.JobResult;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.LoggerFactory;

/**
 * This component manages/keeps track of all job consumer services.
 */
@Component(label="%job.consumermanager.name",
           description="%job.consumermanager.description",
           metatype=true)
@Service(value=JobConsumerManager.class)
@References({
    @Reference(referenceInterface=JobConsumer.class,
            cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy=ReferencePolicy.DYNAMIC),
    @Reference(referenceInterface=JobExecutor.class,
            cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy=ReferencePolicy.DYNAMIC)
})
@Property(name="org.apache.sling.installer.configuration.persist", boolValue=false,
          label="Distribute config",
          description="If this is disabled, the configuration is not persisted on save in the cluster and is "
                    + "only used on the current instance. This option should always be disabled!")
public class JobConsumerManager {

    @Property(unbounded=PropertyUnbounded.ARRAY, value = "*")
    private static final String PROPERTY_WHITELIST = "job.consumermanager.whitelist";

    @Property(unbounded=PropertyUnbounded.ARRAY)
    private static final String PROPERTY_BLACKLIST = "job.consumermanager.blacklist";

    /** The map with the consumers, keyed by topic, sorted by service ranking. */
    private final Map<String, List<ConsumerInfo>> topicToConsumerMap = new HashMap<String, List<ConsumerInfo>>();

    /** Marker if this instance supports bridged events. */
    private boolean supportsBridgedEvents;

    /** ServiceRegistration for propagation. */
    private ServiceRegistration propagationService;

    private String topics;

    private TopicMatcher[] whitelistMatchers;

    private TopicMatcher[] blacklistMatchers;

    private volatile long changeCount;

    private BundleContext bundleContext;

    private final Map<String, Object[]> listenerMap = new HashMap<String, Object[]>();

    private Dictionary<String, Object> getRegistrationProperties() {
        final Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
        serviceProps.put(PropertyProvider.PROPERTY_PROPERTIES, TopologyCapabilities.PROPERTY_TOPICS);
        // we add a changing property to the service registration
        // to make sure a modification event is really sent
        synchronized ( this ) {
            serviceProps.put("changeCount", this.changeCount++);
        }
        return serviceProps;
    }

    @Activate
    protected void activate(final BundleContext bc, final Map<String, Object> props) {
        this.bundleContext = bc;
        this.modified(bc, props);
    }

    @Modified
    protected void modified(final BundleContext bc, final Map<String, Object> props) {
        final boolean wasEnabled = this.propagationService != null;
        this.whitelistMatchers = TopicMatcherHelper.buildMatchers(PropertiesUtil.toStringArray(props.get(PROPERTY_WHITELIST)));
        this.blacklistMatchers = TopicMatcherHelper.buildMatchers(PropertiesUtil.toStringArray(props.get(PROPERTY_BLACKLIST)));

        final boolean enable = this.whitelistMatchers != null && this.blacklistMatchers != TopicMatcherHelper.MATCH_ALL;
        if ( wasEnabled != enable ) {
            synchronized ( this.topicToConsumerMap ) {
                this.calculateTopics(enable);
            }
            if ( enable ) {
                LoggerFactory.getLogger(this.getClass()).info("Registering property provider with: {}", this.topics);
                this.propagationService = bc.registerService(PropertyProvider.class.getName(),
                        new PropertyProvider() {

                            @Override
                            public String getProperty(final String name) {
                                if ( TopologyCapabilities.PROPERTY_TOPICS.equals(name) ) {
                                    return topics;
                                }
                                return null;
                            }
                        }, this.getRegistrationProperties());
            } else {
                LoggerFactory.getLogger(this.getClass()).info("Unregistering property provider with");
                this.propagationService.unregister();
                this.propagationService = null;
            }
        } else if ( enable ) {
            // update properties
            synchronized ( this.topicToConsumerMap ) {
                this.calculateTopics(true);
            }
            LoggerFactory.getLogger(this.getClass()).info("Updating property provider with: {}", this.topics);
            this.propagationService.setProperties(this.getRegistrationProperties());
        }
    }

    @Deactivate
    protected void deactivate() {
        if ( this.propagationService != null ) {
            this.propagationService.unregister();
            this.propagationService = null;
        }
        this.bundleContext = null;
        synchronized ( this.topicToConsumerMap ) {
            this.topicToConsumerMap.clear();
            this.listenerMap.clear();
        }
    }

    /**
     * Get the executor for the topic.
     * @param topic The job topic
     * @return A consumer or <code>null</code>
     */
    public JobExecutor getExecutor(final String topic) {
        synchronized ( this.topicToConsumerMap ) {
            final List<ConsumerInfo> consumers = this.topicToConsumerMap.get(topic);
            if ( consumers != null ) {
                return consumers.get(0).getExecutor(this.bundleContext);
            }
            final int pos = topic.lastIndexOf('/');
            if ( pos > 0 ) {
                final String category = topic.substring(0, pos + 1).concat("*");
                final List<ConsumerInfo> categoryConsumers = this.topicToConsumerMap.get(category);
                if ( categoryConsumers != null ) {
                    return categoryConsumers.get(0).getExecutor(this.bundleContext);
                }
            }
        }
        return null;
    }

    public void registerListener(final String key, final JobExecutor consumer, final JobExecutionContext handler) {
        synchronized ( this.topicToConsumerMap ) {
            this.listenerMap.put(key, new Object[] {consumer, handler});
        }
    }

    public void unregisterListener(final String key) {
        synchronized ( this.topicToConsumerMap ) {
            this.listenerMap.remove(key);
        }
    }

    /**
     * Return the topics information of this instance.
     */
    public String getTopics() {
        return this.topics;
    }

    /**
     * Does this instance supports bridged events?
     */
    public boolean supportsBridgedEvents() {
        return supportsBridgedEvents;
    }

    /**
     * Bind a new consumer
     * @param serviceReference The service reference to the consumer.
     */
    protected void bindJobConsumer(final ServiceReference serviceReference) {
        this.bindService(serviceReference, true);
    }

    /**
     * Unbind a consumer
     * @param serviceReference The service reference to the consumer.
     */
    protected void unbindJobConsumer(final ServiceReference serviceReference) {
        this.unbindService(serviceReference, true);
    }

    /**
     * Bind a new executor
     * @param serviceReference The service reference to the executor.
     */
    protected void bindJobExecutor(final ServiceReference serviceReference) {
        this.bindService(serviceReference, false);
    }

    /**
     * Unbind a executor
     * @param serviceReference The service reference to the executor.
     */
    protected void unbindJobExecutor(final ServiceReference serviceReference) {
        this.unbindService(serviceReference, false);
    }

    /**
     * Bind a consumer or executor
     * @param serviceReference The service reference to the consumer or executor.
     * @param isConsumer Indicating whether this is a JobConsumer or JobExecutor
     */
    private void bindService(final ServiceReference serviceReference, final boolean isConsumer) {
        final String[] topics = PropertiesUtil.toStringArray(serviceReference.getProperty(JobConsumer.PROPERTY_TOPICS));
        if ( topics != null && topics.length > 0 ) {
            final ConsumerInfo info = new ConsumerInfo(serviceReference, isConsumer);
            boolean changed = false;
            synchronized ( this.topicToConsumerMap ) {
                for(final String t : topics) {
                    if ( t != null ) {
                        final String topic = t.trim();
                        if ( topic.length() > 0 ) {
                            List<ConsumerInfo> consumers = this.topicToConsumerMap.get(topic);
                            if ( consumers == null ) {
                                consumers = new ArrayList<JobConsumerManager.ConsumerInfo>();
                                this.topicToConsumerMap.put(topic, consumers);
                                changed = true;
                            }
                            consumers.add(info);
                            Collections.sort(consumers);
                        }
                    }
                }
                this.supportsBridgedEvents = this.topicToConsumerMap.containsKey("/");
                if ( changed ) {
                    this.calculateTopics(this.propagationService != null);
                }
            }
            if ( changed && this.propagationService != null ) {
                LoggerFactory.getLogger(this.getClass()).info("Updating property provider with: {}", this.topics);
                this.propagationService.setProperties(this.getRegistrationProperties());
            }
        }
    }

    /**
     * Unbind a consumer or executor
     * @param serviceReference The service reference to the consumer or executor.
     * @param isConsumer Indicating whether this is a JobConsumer or JobExecutor
     */
    private void unbindService(final ServiceReference serviceReference, final boolean isConsumer) {
        final String[] topics = PropertiesUtil.toStringArray(serviceReference.getProperty(JobConsumer.PROPERTY_TOPICS));
        if ( topics != null && topics.length > 0 ) {
            final ConsumerInfo info = new ConsumerInfo(serviceReference, isConsumer);
            boolean changed = false;
            synchronized ( this.topicToConsumerMap ) {
                for(final String t : topics) {
                    if ( t != null ) {
                        final String topic = t.trim();
                        if ( topic.length() > 0 ) {
                            final List<ConsumerInfo> consumers = this.topicToConsumerMap.get(topic);
                            if ( consumers != null ) { // sanity check
                                for(final ConsumerInfo oldConsumer : consumers) {
                                    if ( oldConsumer.equals(info) && oldConsumer.executor != null ) {
                                        // notify listener
                                        for(final Object[] listenerObjects : this.listenerMap.values()) {
                                            if ( listenerObjects[0] == oldConsumer.executor ) {
                                                final JobExecutionContext context = (JobExecutionContext)listenerObjects[1];
                                                context.asyncProcessingFinished(context.result().failed());
                                                break;
                                            }
                                        }
                                    }
                                }
                                consumers.remove(info);
                                if ( consumers.size() == 0 ) {
                                    this.topicToConsumerMap.remove(topic);
                                    changed = true;
                                }
                            }
                        }
                    }
                }
                this.supportsBridgedEvents = this.topicToConsumerMap.containsKey("/");
                if ( changed ) {
                    this.calculateTopics(this.propagationService != null);
                }
            }
            if ( changed && this.propagationService != null ) {
                LoggerFactory.getLogger(this.getClass()).info("Updating property provider with: {}", this.topics);
                this.propagationService.setProperties(this.getRegistrationProperties());
            }
        }
    }

    private boolean match(final String topic, final TopicMatcher[] matchers) {
        for(final TopicMatcher m : matchers) {
            if ( m.match(topic) != null ) {
                return true;
            }
        }
        return false;
    }

    private void calculateTopics(final boolean enabled) {
        if ( enabled ) {
            // create a sorted list - this ensures that the property value
            // is always the same for the same topics.
            final List<String> topicList = new ArrayList<String>();
            for(final String topic : this.topicToConsumerMap.keySet() ) {
                // check whitelist
                if ( this.match(topic, this.whitelistMatchers) ) {
                    // and blacklist
                    if ( this.blacklistMatchers == null || !this.match(topic, this.blacklistMatchers) ) {
                        topicList.add(topic);
                    }
                }
            }
            Collections.sort(topicList);

            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            for(final String topic : topicList ) {
                if ( first ) {
                    first = false;
                } else {
                    sb.append(',');
                }
                sb.append(topic);
            }
            this.topics = sb.toString();
        } else {
            this.topics = null;
        }
    }

    /**
     * Internal class caching some consumer infos like service id and ranking.
     */
    private final static class ConsumerInfo implements Comparable<ConsumerInfo> {

        public final ServiceReference serviceReference;
        private final boolean isConsumer;
        public JobExecutor executor;
        public final int ranking;
        public final long serviceId;

        public ConsumerInfo(final ServiceReference serviceReference, final boolean isConsumer) {
            this.serviceReference = serviceReference;
            this.isConsumer = isConsumer;
            final Object sr = serviceReference.getProperty(Constants.SERVICE_RANKING);
            if ( sr == null || !(sr instanceof Integer)) {
                this.ranking = 0;
            } else {
                this.ranking = (Integer)sr;
            }
            this.serviceId = (Long)serviceReference.getProperty(Constants.SERVICE_ID);
        }

        @Override
        public int compareTo(final ConsumerInfo o) {
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
            if ( obj instanceof ConsumerInfo ) {
                return ((ConsumerInfo)obj).serviceId == this.serviceId;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return serviceReference.hashCode();
        }

        public JobExecutor getExecutor(final BundleContext bundleContext) {
            if ( executor == null ) {
                if ( this.isConsumer ) {
                    executor = new JobConsumerWrapper((JobConsumer) bundleContext.getService(this.serviceReference));
                } else {
                    executor = (JobExecutor) bundleContext.getService(this.serviceReference);
                }
            }
            return executor;
        }
    }

    private final static class JobConsumerWrapper implements JobExecutor {

        private final JobConsumer consumer;

        public JobConsumerWrapper(final JobConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public JobExecutionResult process(final Job job, final JobExecutionContext context) {
            final JobConsumer.AsyncHandler asyncHandler =
                    new JobConsumer.AsyncHandler() {

                        final Object asyncLock = new Object();
                        final AtomicBoolean asyncDone = new AtomicBoolean(false);

                        private void check(final JobExecutionResult result) {
                            synchronized ( asyncLock ) {
                                if ( !asyncDone.get() ) {
                                    asyncDone.set(true);
                                    context.asyncProcessingFinished(result);
                                } else {
                                    throw new IllegalStateException("Job is already marked as processed");
                                }
                            }
                        }

                        @Override
                        public void ok() {
                            this.check(context.result().succeeded());
                        }

                        @Override
                        public void failed() {
                            this.check(context.result().failed());
                        }

                        @Override
                        public void cancel() {
                            this.check(context.result().cancelled());
                        }
                    };
            ((JobImpl)job).setProperty(JobConsumer.PROPERTY_JOB_ASYNC_HANDLER, asyncHandler);
            final JobConsumer.JobResult result = this.consumer.process(job);
            if ( result == JobResult.ASYNC ) {
                return null;
            } else if ( result == JobResult.FAILED) {
                return context.result().failed();
            } else if ( result == JobResult.OK) {
                return context.result().succeeded();
            }
            return context.result().cancelled();
        }
    }
}

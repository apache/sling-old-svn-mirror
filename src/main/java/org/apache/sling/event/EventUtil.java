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
package org.apache.sling.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.event.EventUtil.JobStatusNotifier.NotifierContext;
import org.apache.sling.event.impl.AbstractRepositoryEventHandler;
import org.apache.sling.event.impl.JobEventHandler;
import org.osgi.service.event.Event;
import org.slf4j.LoggerFactory;

/**
 * The <code>EventUtil</code> class is an utility class for
 * clustered environments.
 */
public abstract class EventUtil {

    /** This event property indicates, if the event should be distributed in the cluster (default false). */
    public static final String PROPERTY_DISTRIBUTE = "event.distribute";

    /** This event property specifies the application node. */
    public static final String PROPERTY_APPLICATION = "event.application";

    /**
     * Job Handling
     */

    /** The job topic property. */
    public static final String PROPERTY_JOB_TOPIC = "event.job.topic";

    /** The property for the unique event id. Value is of type String. */
    public static final String PROPERTY_JOB_ID = "event.job.id";

    /** The property to set if a job can be run parallel to any other job. */
    public static final String PROPERTY_JOB_PARALLEL = "event.job.parallel";

    /** The property to set if a job should only be run on the same app it has been created. */
    public static final String PROPERTY_JOB_RUN_LOCAL = "event.job.run.local";

    /** The property to track the retry count for jobs. Value is of type Integer. */
    public static final String PROPERTY_JOB_RETRY_COUNT = "event.job.retrycount";

    /** The property to for setting the maximum number of retries. Value is of type Integer. */
    public static final String PROPERTY_JOB_RETRIES = "event.job.retries";

    /** The property to set a retry delay. Value is of type Long and specifies milliseconds. */
    public static final String PROPERTY_JOB_RETRY_DELAY = "event.job.retrydelay";

    /** The property to set to put the jobs into a separate job queue. This property
     * spcifies the name of the job queue. If the job queue does not exists yet
     * a new queue is created.
     * If a job queue is used, the jobs are never executed in parallel from this queue!
     */
    public static final String PROPERTY_JOB_QUEUE_NAME = "event.job.queuename";

    /** If this property is set with any value, the queue processes the jobs in the same
     * order as they have arrived.
     * This property has only an effect if {@link #PROPERTY_JOB_QUEUE_NAME} is specified.
     */
    public static final String PROPERTY_JOB_QUEUE_ORDERED = "event.job.queueordered";

    /** The topic for jobs. */
    public static final String TOPIC_JOB = "org/apache/sling/event/job";

    /**
     * Timed Events
     */

    /** The topic for timed events. */
    public static final String TOPIC_TIMED_EVENT = "org/apache/sling/event/timed";

    /** The real topic of the event. */
    public static final String PROPERTY_TIMED_EVENT_TOPIC = "event.topic.timed";

    /** The property for the unique event id. */
    public static final String PROPERTY_TIMED_EVENT_ID = "event.timed.id";

    /** The scheduler expression for the timed event. */
    public static final String PROPERTY_TIMED_EVENT_SCHEDULE = "event.timed.scheduler";

    /** The period for the timed event. */
    public static final String PROPERTY_TIMED_EVENT_PERIOD = "event.timed.period";

    /** The date for the timed event. */
    public static final String PROPERTY_TIMED_EVENT_DATE = "event.timed.date";

    /**
     * Utility Methods
     */

    /**
     * Create a distributable event.
     * A distributable event is distributed across the cluster.
     * @param topic
     * @param properties
     * @return An OSGi event.
     */
    public static Event createDistributableEvent(String topic,
                                                 Dictionary<String, Object> properties) {
        final Dictionary<String, Object> newProperties;
        // create a new dictionary
        newProperties = new Hashtable<String, Object>();
        if ( properties != null ) {
            final Enumeration<String> e = properties.keys();
            while ( e.hasMoreElements() ) {
                final String key = e.nextElement();
                newProperties.put(key, properties.get(key));
            }
        }
        // for now the value has no meaning, so we just put an empty string in it.
        newProperties.put(PROPERTY_DISTRIBUTE, "");
        return new Event(topic, newProperties);
    }

    /**
     * Should this event be distributed in the cluster?
     * @param event
     * @return <code>true</code> if the event should be distributed.
     */
    public static boolean shouldDistribute(Event event) {
        return event.getProperty(PROPERTY_DISTRIBUTE) != null;
    }

    /**
     * Is this a local event?
     * @param event
     * @return <code>true</code> if this is a local event
     */
    public static boolean isLocal(Event event) {
        final String appId = getApplicationId(event);
        return appId == null || appId.equals(AbstractRepositoryEventHandler.APPLICATION_ID);
    }

    /**
     * Return the application id the event was created at.
     * @param event
     * @return The application id or null if the event has been created locally.
     */
    public static String getApplicationId(Event event) {
        return (String)event.getProperty(PROPERTY_APPLICATION);
    }

    /**
     * Is this a job event?
     * This method checks if the event contains the {@link #PROPERTY_JOB_TOPIC}
     * property.
     * @param event The event to check.
     * @return <code>true></code> if this is a job event.
     */
    public static boolean isJobEvent(Event event) {
        return event.getProperty(PROPERTY_JOB_TOPIC) != null;
    }
    /**
     * Notify a finished job.
     */
    public static void finishedJob(Event job) {
        // check if this is a job event
        if ( !isJobEvent(job) ) {
            return;
        }
        final JobStatusNotifier.NotifierContext ctx = (NotifierContext) job.getProperty(JobStatusNotifier.CONTEXT_PROPERTY_NAME);
        if ( ctx == null ) {
            throw new NullPointerException("JobStatusNotifier context is not available in event properties.");
        }
        ctx.notifier.finishedJob(job, ctx.eventNodePath, false);
    }

    /**
     * Notify a failed job.
     * @return <code>true</code> if the job has been rescheduled, <code>false</code> otherwise.
     */
    public static boolean rescheduleJob(Event job) {
        // check if this is a job event
        if ( !isJobEvent(job) ) {
            return false;
        }
        final JobStatusNotifier.NotifierContext ctx = (NotifierContext) job.getProperty(JobStatusNotifier.CONTEXT_PROPERTY_NAME);
        if ( ctx == null ) {
            throw new NullPointerException("JobStatusNotifier context is not available in event properties.");
        }
        return ctx.notifier.finishedJob(job, ctx.eventNodePath, true);
    }

    /**
     * Process a job in the background and notify its success.
     */
    public static void processJob(final Event job, final JobProcessor processor) {
        final Runnable task = new Runnable() {

            /**
             * @see java.lang.Runnable#run()
             */
            public void run() {
                boolean result = false;
                try {
                    result = processor.process(job);
                } catch (Throwable t) {
                    LoggerFactory.getLogger(EventUtil.class).error("Unhandled error occured in job processor " + t.getMessage(), t);
                    // we don't reschedule if an exception occurs
                    result = true;
                } finally {
                    if ( result ) {
                        EventUtil.finishedJob(job);
                    } else {
                        EventUtil.rescheduleJob(job);
                    }
                }
            }

        };
        // check if the job handler thread pool is available
        if ( JobEventHandler.JOB_THREAD_POOL != null ) {
            JobEventHandler.JOB_THREAD_POOL.execute(task);
        } else {
            // if we don't have a thread pool, we create the thread directly
            // (this should never happen for jobs, but is a safe fallback and
            // allows to call this method for other background processing.
            new Thread(task).start();
        }
    }

    /**
     * This is a private interface which is only public for import reasons.
     */
    public static interface JobStatusNotifier {

        String CONTEXT_PROPERTY_NAME = JobStatusNotifier.class.getName();

        public static final class NotifierContext {
            public final JobStatusNotifier notifier;
            public final String eventNodePath;

            public NotifierContext(JobStatusNotifier n, String path) {
                this.notifier = n;
                this.eventNodePath = path;
            }
        }

        /**
         * Notify that the job is finished.
         * If the job is not rescheduled, a return value of <code>false</code> indicates an error
         * during the processing. If the job should be rescheduled, <code>true</code> indicates
         * that the job could be rescheduled. If an error occurs or the number of retries is
         * exceeded, <code>false</code> will be returned.
         * @param job The job.
         * @param eventNodePath The storage node in the repository.
         * @param reschedule Should the event be rescheduled?
         * @return <code>true</code> if everything went fine, <code>false</code> otherwise.
         */
        boolean finishedJob(Event job, String eventNodePath, boolean reschedule);
    }

    /**
     * Add all java properties as properties to the node.
     * If the name and the value of a map entry can easily converted into
     * a repository property, it is directly added. All other java
     * properties are stored in one binary property.
     *
     * @param node The node where all properties are added to
     * @param properties The map of properties.
     * @param ignoreProps optional list of property which should be ignored
     * @param binPropertyName The name of the binary property.
     * @throws RepositoryException
     */
    public static void addProperties(final Node node,
                                     final Map<String, Object> properties,
                                     final String[] ignoreProps,
                                     final String binPropertyName)
    throws RepositoryException {
        addProperties(node, new EventPropertiesMap(properties), ignoreProps, binPropertyName);
    }

    /**
     * Add all java properties as properties to the node.
     * If the name and the value of a map entry can easily converted into
     * a repository property, it is directly added. All other java
     * properties are stored in one binary property.
     *
     * @param node The node where all properties are added to
     * @param properties The map of properties.
     * @param ignoreProps optional list of property which should be ignored
     * @param binPropertyName The name of the binary property.
     * @throws RepositoryException
     */
    public static void addProperties(final Node node,
                                     final EventPropertiesMap properties,
                                     final String[] ignoreProps,
                                     final String binPropertyName)
    throws RepositoryException {
        if ( properties != null ) {
            final List<String> ignorePropList = (ignoreProps == null ? null : Arrays.asList(ignoreProps));
            // check which props we can write directly and
            // which we need to write as a binary blob
            final List<String> propsAsBlob = new ArrayList<String>();

            final Iterator<Map.Entry<String, Object>> i = properties.entrySet().iterator();
            while ( i.hasNext() ) {
                final Map.Entry<String, Object> current = i.next();

                if (ignorePropList == null || !ignorePropList.contains(current.getKey()) ) {
                    // sanity check
                    if ( current.getValue() != null ) {
                        if ( !setProperty(current.getKey(), current.getValue(), node) ) {
                            propsAsBlob.add(current.getKey());
                        }
                    }
                }
            }
            // write the remaining properties as a blob
            if ( propsAsBlob.size() > 0 ) {
                try {
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    final ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeInt(propsAsBlob.size());
                    for(final String propName : propsAsBlob) {
                        oos.writeObject(propName);
                        oos.writeObject(properties.get(propName));
                    }
                    oos.close();
                    node.setProperty(binPropertyName, new ByteArrayInputStream(baos.toByteArray()));
                } catch (IOException ioe) {
                    throw new RepositoryException("Unable to serialize properties.", ioe);
                }
            }
        }
    }

    /**
     * Read properties from a repository node and create a property map.
     * @throws RepositoryException
     * @throws ClassNotFoundException
     */
    public static EventPropertiesMap readProperties(final Node node,
                                                    final String binPropertyName,
                                                    final String[] ignorePrefixes)
    throws RepositoryException, ClassNotFoundException {
        final Map<String, Object> properties = new HashMap<String, Object>();

        // check the properties blob
        if ( node.hasProperty(binPropertyName) ) {
            try {
                final ObjectInputStream ois = new ObjectInputStream(node.getProperty(binPropertyName).getStream());
                int length = ois.readInt();
                for(int i=0;i<length;i++) {
                    final String key = (String)ois.readObject();
                    final Object value = ois.readObject();
                    properties.put(key, value);
                }
            } catch (IOException ioe) {
                throw new RepositoryException("Unable to deserialize event properties.", ioe);
            }
        }
        // now all properties that have been set directly
        final PropertyIterator pI = node.getProperties();
        while ( pI.hasNext() ) {
            final Property p = pI.nextProperty();
            boolean ignore = p.getName().startsWith("jcr:");
            if ( !ignore && ignorePrefixes != null ) {
                int index = 0;
                while ( !ignore && index < ignorePrefixes.length ) {
                    ignore = p.getName().startsWith(ignorePrefixes[index]);
                    index++;
                }
            }
            if ( !ignore ) {
                final String name = ISO9075.decode(p.getName());
                final Value value = p.getValue();
                final Object o;
                switch (value.getType()) {
                    case PropertyType.BOOLEAN:
                        o = value.getBoolean(); break;
                    case PropertyType.DATE:
                        o = value.getDate(); break;
                    case PropertyType.DOUBLE:
                        o = value.getDouble(); break;
                    case PropertyType.LONG:
                        o = value.getLong(); break;
                    case PropertyType.STRING:
                        o = value.getString(); break;
                    default: // this should never happen - we convert to a string...
                        o = value.getString();
                }
                properties.put(name, o);
            }
        }
        return new EventPropertiesMap(properties);
    }

    /**
     * Return the converted repository property name
     * @param name The java object property name
     * @return The converted name or null if not possible.
     */
    public static String getNodePropertyName(final String name) {
        // if name contains a colon, we can't set it as a property
        if ( name.indexOf(':') != -1 ) {
            return null;
        }
        return ISO9075.encode(name);
    }

    /**
     * Return the converted repository property value
     * @param valueFactory The value factory
     * @param eventValue The event value
     * @return The converted value or null if not possible
     */
    public static Value getNodePropertyValue(final ValueFactory valueFactory, final Object eventValue) {
        final Value val;
        if (eventValue.getClass().isAssignableFrom(Calendar.class)) {
            val = valueFactory.createValue((Calendar)eventValue);
        } else if (eventValue.getClass().isAssignableFrom(Long.class)) {
            val = valueFactory.createValue((Long)eventValue);
        } else if (eventValue.getClass().isAssignableFrom(Double.class)) {
            val = valueFactory.createValue(((Double)eventValue).doubleValue());
        } else if (eventValue.getClass().isAssignableFrom(Boolean.class)) {
            val = valueFactory.createValue((Boolean) eventValue);
        } else if (eventValue instanceof String) {
            val = valueFactory.createValue((String)eventValue);
        } else {
            val = null;
        }
        return val;
    }

    /**
     * Try to set the java property as a property of the node.
     * @param name
     * @param value
     * @param node
     * @return
     * @throws RepositoryException
     */
    private static boolean setProperty(String name, Object value, Node node)
    throws RepositoryException {
        final String propName = getNodePropertyName(name);
        if ( propName == null ) {
            return false;
        }
        final ValueFactory fac = node.getSession().getValueFactory();
        final Value val = getNodePropertyValue(fac, value);
        if ( val != null ) {
            node.setProperty(propName, val);
            return true;
        }
        return false;
    }
}

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
package org.apache.sling.event.impl.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.config.MainQueueConfiguration;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.ScheduleInfo;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.event.EventConstants;

public abstract class ResourceHelper {

    public static final String RESOURCE_TYPE_FOLDER = "sling:Folder";

    public static final String RESOURCE_TYPE_JOB = "slingevent:Job";

    /** We use the same resource type as for timed events. */
    public static final String RESOURCE_TYPE_SCHEDULED_JOB = "slingevent:TimedEvent";

    public static final String BUNDLE_EVENT_UPDATED = "org/osgi/framework/BundleEvent/UPDATED";

    public static final String BUNDLE_EVENT_STARTED = "org/osgi/framework/BundleEvent/STARTED";

    public static final String PROPERTY_SCHEDULE_NAME = "slingevent:scheduleName";
    public static final String PROPERTY_SCHEDULE_INFO = "slingevent:scheduleInfo";
    public static final String PROPERTY_SCHEDULE_INFO_TYPE = "slingevent:scheduleInfoType";
    public static final String PROPERTY_SCHEDULE_SUSPENDED = "slingevent:scheduleSuspended";

    public static final String PROPERTY_JOB_ID = "slingevent:eventId";
    public static final String PROPERTY_JOB_TOPIC = "event.job.topic";
    public static final String PROPERTY_DISTRIBUTE = "event.distribute";
    public static final String PROPERTY_APPLICATION = "event.application";

    /** List of ignored properties to write to the repository. */
    private static final String[] IGNORE_PROPERTIES = new String[] {
        ResourceHelper.PROPERTY_DISTRIBUTE,
        ResourceHelper.PROPERTY_APPLICATION,
        EventConstants.EVENT_TOPIC,
        ResourceHelper.PROPERTY_JOB_ID,
        JobImpl.PROPERTY_DELAY_OVERRIDE,
        JobConsumer.PROPERTY_JOB_ASYNC_HANDLER,
        Job.PROPERTY_JOB_PROGRESS_LOG,
        Job.PROPERTY_JOB_PROGRESS_ETA,
        Job.PROPERTY_JOB_PROGRESS_STEP,
        Job.PROPERTY_JOB_PROGRESS_STEPS,
        Job.PROPERTY_FINISHED_DATE,
        JobImpl.PROPERTY_FINISHED_STATE,
        Job.PROPERTY_RESULT_MESSAGE,
        PROPERTY_SCHEDULE_INFO,
        PROPERTY_SCHEDULE_NAME,
        PROPERTY_SCHEDULE_INFO_TYPE,
        PROPERTY_SCHEDULE_SUSPENDED
    };

    /**
     * Check if this property should be ignored
     */
    public static boolean ignoreProperty(final String name) {
        for(final String prop : IGNORE_PROPERTIES) {
            if ( prop.equals(name) ) {
                return true;
            }
        }
        return false;
    }

    /** Allowed characters for a node name */
    private static final BitSet ALLOWED_CHARS;

    /** Replacement characters for unallowed characters in a node name */
    private static final char REPLACEMENT_CHAR = '_';

    // Prepare the ALLOWED_CHARS bitset with bits indicating the unicode
    // character index of allowed characters. We deliberately only support
    // a subset of the actually allowed set of characters for nodes ...
    static {
        final String allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz0123456789_,.-+#!?$%&()=";
        final BitSet allowedSet = new BitSet();
        for (int i = 0; i < allowed.length(); i++) {
            allowedSet.set(allowed.charAt(i));
        }
        ALLOWED_CHARS = allowedSet;
    }

    /**
     * Filter the queue name for not allowed characters and replace them
     * - with the exception of the main queue, which will not be filtered
     * @param queueName the suggested queue name
     * @return the filtered queue name
     */
    public static String filterQueueName(final String queueName) {
        if ( queueName.equals(MainQueueConfiguration.MAIN_QUEUE_NAME) ) {
            return queueName;
        } else {
            return ResourceHelper.filterName(queueName);
        }
    }

    /**
     * Filter the node name for not allowed characters and replace them.
     * @param resourceName The suggested resource name.
     * @return The filtered node name.
     */
    public static String filterName(final String resourceName) {
        if ( resourceName == null ) {
            return null;
        }
        final StringBuilder sb = new StringBuilder(resourceName.length());
        char lastAdded = 0;

        for(int i=0; i < resourceName.length(); i++) {
            final char c = resourceName.charAt(i);
            char toAdd = c;

            if (!ALLOWED_CHARS.get(c)) {
                if (lastAdded == REPLACEMENT_CHAR) {
                    // do not add several _ in a row
                    continue;
                }
                toAdd = REPLACEMENT_CHAR;

            } else if(i == 0 && Character.isDigit(c)) {
                sb.append(REPLACEMENT_CHAR);
            }

            sb.append(toAdd);
            lastAdded = toAdd;
        }

        if (sb.length()==0) {
            sb.append(REPLACEMENT_CHAR);
        }

        return sb.toString();
    }

    public static final String PROPERTY_MARKER_READ_ERROR_LIST = ResourceHelper.class.getName() + "/ReadErrorList";

    public static Map<String, Object> cloneValueMap(final ValueMap vm) throws InstantiationException {
        List<Exception> hasReadError = null;
        try {
            final Map<String, Object> result = new HashMap<String, Object>(vm);
            for(final Map.Entry<String, Object> entry : result.entrySet()) {
                if ( entry.getKey().equals(PROPERTY_SCHEDULE_INFO) ) {
                    final String[] infoArray = vm.get(entry.getKey(), String[].class);
                    if ( infoArray == null || infoArray.length == 0 ) {
                        if ( hasReadError == null ) {
                            hasReadError = new ArrayList<Exception>();
                        }
                        hasReadError.add(new Exception("Unable to deserialize property '" + entry.getKey() + "' : " + entry.getValue()));
                    } else {
                        final List<ScheduleInfo> infos = new ArrayList<ScheduleInfo>();
                        for(final String i : infoArray) {
                            final ScheduleInfoImpl info = ScheduleInfoImpl.deserialize(i);
                            if ( info != null ) {
                                infos.add(info);
                            }
                        }
                        if ( infos.size() < infoArray.length ) {
                            if ( hasReadError == null ) {
                                hasReadError = new ArrayList<Exception>();
                            }
                            hasReadError.add(new Exception("Unable to deserialize property '" + entry.getKey() + "' : " + Arrays.toString(infoArray)));
                        } else {
                            entry.setValue(infos);
                        }
                    }
                }
                if ( entry.getValue() instanceof InputStream ) {
                    final Object value = vm.get(entry.getKey(), Serializable.class);
                    if ( value != null ) {
                        entry.setValue(value);
                    } else {
                        if ( hasReadError == null ) {
                            hasReadError = new ArrayList<Exception>();
                        }
                        // let's find out which class might be missing
                        ObjectInputStream ois = null;
                        try {
                            ois = new ObjectInputStream((InputStream)entry.getValue());
                            ois.readObject();

                            hasReadError.add(new Exception("Unable to deserialize property '" + entry.getKey() + "'"));
                        } catch (final ClassNotFoundException cnfe) {
                             hasReadError.add(new Exception("Unable to deserialize property '" + entry.getKey() + "'", cnfe));
                        } catch (final IOException ioe) {
                            hasReadError.add(new RuntimeException("Unable to deserialize property '" + entry.getKey() + "'", ioe));
                        } finally {
                            if ( ois != null ) {
                                try {
                                    ois.close();
                                } catch (IOException ignore) {
                                    // ignore
                                }
                            }
                        }
                    }
                }
            }
            if ( hasReadError != null ) {
                result.put(PROPERTY_MARKER_READ_ERROR_LIST, hasReadError);
            }
            return result;
        } catch ( final IllegalArgumentException iae) {
            // the JCR implementation might throw an IAE if something goes wrong
            throw (InstantiationException)new InstantiationException(iae.getMessage()).initCause(iae);
        }
    }

    public static ValueMap getValueMap(final Resource resource) throws InstantiationException {
        final ValueMap vm = ResourceUtil.getValueMap(resource);
        // trigger full loading
        try {
            vm.size();
        } catch ( final IllegalArgumentException iae) {
            // the JCR implementation might throw an IAE if something goes wrong
            throw (InstantiationException)new InstantiationException(iae.getMessage()).initCause(iae);
        }
        return vm;
    }

    public static void getOrCreateBasePath(final ResourceResolver resolver,
            final String path)
    throws PersistenceException {
       getOrCreateResource(resolver,
                        path,
                        ResourceHelper.RESOURCE_TYPE_FOLDER,
                        ResourceHelper.RESOURCE_TYPE_FOLDER,
                        true);
    }

    public static Resource getOrCreateResource(final ResourceResolver resolver,
            final String path, final Map<String, Object> props)
    throws PersistenceException {
       return getOrCreateResource(resolver,
                        path,
                        props,
                        ResourceHelper.RESOURCE_TYPE_FOLDER,
                        true);
    }

    /**
     * Creates or gets the resource at the given path.
     * This is a copy of Sling's API ResourceUtil method to avoid a dependency on the latest
     * Sling API version! We can remove this once we update to Sling API > 2.8
     * @param resolver The resource resolver to use for creation
     * @param path     The full path to be created
     * @param resourceType The optional resource type of the final resource to create
     * @param intermediateResourceType THe optional resource type of all intermediate resources
     * @param autoCommit If set to true, a commit is performed after each resource creation.
     */
    private static Resource getOrCreateResource(
                            final ResourceResolver resolver,
                            final String path,
                            final String resourceType,
                            final String intermediateResourceType,
                            final boolean autoCommit)
    throws PersistenceException {
        final Map<String, Object> props;
        if ( resourceType == null ) {
            props = null;
        } else {
            props = Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)resourceType);
        }
        return getOrCreateResource(resolver, path, props, intermediateResourceType, autoCommit);
    }

    /**
     * Creates or gets the resource at the given path.
     * If an exception occurs, it retries the operation up to five times if autoCommit is enabled.
     * In this case, {@link ResourceResolver#revert()} is called on the resolver before the
     * creation is retried.
     * This is a copy of Sling's API ResourceUtil method to avoid a dependency on the latest
     * Sling API version! We can remove this once we update to Sling API > 2.8
     *
     * @param resolver The resource resolver to use for creation
     * @param path     The full path to be created
     * @param resourceProperties The optional resource properties of the final resource to create
     * @param intermediateResourceType THe optional resource type of all intermediate resources
     * @param autoCommit If set to true, a commit is performed after each resource creation.
     */
    private static Resource getOrCreateResource(
            final ResourceResolver resolver,
            final String path,
            final Map<String, Object> resourceProperties,
            final String intermediateResourceType,
            final boolean autoCommit)
    throws PersistenceException {
        PersistenceException mostRecentPE = null;
        for(int i=0;i<5;i++) {
            try {
                return getOrCreateResourceInternal(resolver,
                        path,
                        resourceProperties,
                        intermediateResourceType,
                        autoCommit);
            } catch ( final PersistenceException pe ) {
                if ( autoCommit ) {
                    // in case of exception, revert to last clean state and retry
                    resolver.revert();
                    resolver.refresh();
                    mostRecentPE = pe;
                } else {
                    throw pe;
                }
            }
        }
        throw mostRecentPE;
    }

    /**
     * Creates or gets the resource at the given path.
     * This is a copy of Sling's API ResourceUtil method to avoid a dependency on the latest
     * Sling API version! We can remove this once we update to Sling API > 2.8
     *
     * @param resolver The resource resolver to use for creation
     * @param path     The full path to be created
     * @param resourceProperties The optional resource properties of the final resource to create
     * @param intermediateResourceType THe optional resource type of all intermediate resources
     * @param autoCommit If set to true, a commit is performed after each resource creation.
     */
    private static Resource getOrCreateResourceInternal(
            final ResourceResolver resolver,
            final String path,
            final Map<String, Object> resourceProperties,
            final String intermediateResourceType,
            final boolean autoCommit)
    throws PersistenceException {
        Resource rsrc = resolver.getResource(path);
        if ( rsrc == null ) {
            final int lastPos = path.lastIndexOf('/');
            final String name = path.substring(lastPos + 1);

            final Resource parentResource;
            if ( lastPos == 0 ) {
                parentResource = resolver.getResource("/");
            } else {
                final String parentPath = path.substring(0, lastPos);
                parentResource = getOrCreateResource(resolver,
                        parentPath,
                        intermediateResourceType,
                        intermediateResourceType,
                        autoCommit);
            }
            if ( autoCommit ) {
                resolver.refresh();
            }
            try {
                int retry = 5;
                while ( retry > 0 && rsrc == null ) {
                    rsrc = resolver.create(parentResource, name, resourceProperties);
                    // check for SNS
                    if ( !name.equals(rsrc.getName()) ) {
                        resolver.refresh();
                        resolver.delete(rsrc);
                        rsrc = resolver.getResource(parentResource, name);
                    }
                    retry--;
                }
                if ( rsrc == null ) {
                    throw new PersistenceException("Unable to create resource.");
                }
            } catch ( final PersistenceException pe ) {
                // this could be thrown because someone else tried to create this
                // node concurrently
                resolver.refresh();
                rsrc = resolver.getResource(parentResource, name);
                if ( rsrc == null ) {
                    throw pe;
                }
            }
            if ( autoCommit ) {
                try {
                    resolver.commit();
                    resolver.refresh();
                    rsrc = resolver.getResource(parentResource, name);
                } catch ( final PersistenceException pe ) {
                    // try again - maybe someone else did create the resource in the meantime
                    // or we ran into Jackrabbit's stale item exception in a clustered environment
                    resolver.revert();
                    resolver.refresh();
                    rsrc = resolver.getResource(parentResource, name);
                    if ( rsrc == null ) {
                        rsrc = resolver.create(parentResource, name, resourceProperties);
                        resolver.commit();
                    }
                }
            }
        }
        return rsrc;
    }
}
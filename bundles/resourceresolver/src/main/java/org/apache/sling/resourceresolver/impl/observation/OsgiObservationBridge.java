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
package org.apache.sling.resourceresolver.impl.observation;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(policy = ConfigurationPolicy.REQUIRE, metatype = true,
        label="Apache Sling OSGi Observation Bridge", description="Legacy bridge which converts resource change events to OSGi events")
@Service(ResourceChangeListener.class)
@Properties({ @Property(name = ResourceChangeListener.CHANGES, value = { "ADDED", "CHANGED", "REMOVED" }),
        @Property(name = ResourceChangeListener.PATHS, value = "/") })
public class OsgiObservationBridge implements ResourceChangeListener, ExternalResourceChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(OsgiObservationBridge.class);

    @Reference
    private EventAdmin eventAdmin;

    @Reference
    private ResourceResolverFactory resolverFactory;

    private ResourceResolver resolver;

    private BlockingQueue<ResourceChange> changesQueue;

    private EventSendingJob job;

    @SuppressWarnings("deprecation")
    protected void activate() throws LoginException {
        resolver = resolverFactory.getAdministrativeResourceResolver(null);
        changesQueue = new LinkedBlockingQueue<ResourceChange>();
        job = new EventSendingJob(changesQueue);
        Executors.newSingleThreadExecutor().submit(job);
    }

    protected void deactivate() {
        changesQueue.clear();
        job.stop();
        resolver.close();
    }

    @Override
    public void onChange(List<ResourceChange> changes) {
        changesQueue.addAll(changes);
    }

    @SuppressWarnings("deprecation")
    private void sendOsgiEvent(ResourceChange change) {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        String topic;
        switch (change.getType()) {
        case ADDED:
            topic = SlingConstants.TOPIC_RESOURCE_ADDED;
            break;

        case CHANGED:
            topic = SlingConstants.TOPIC_RESOURCE_CHANGED;
            break;

        case REMOVED:
            topic = SlingConstants.TOPIC_RESOURCE_REMOVED;
            break;

        default:
            return;
        }

        props.put(SlingConstants.PROPERTY_PATH, change.getPath());
        if (change.getUserId() != null) {
            props.put(SlingConstants.PROPERTY_USERID, change.getUserId());
        }
        if (change.getAddedPropertyNames() != null ) {
            props.put(SlingConstants.PROPERTY_ADDED_ATTRIBUTES, change.getAddedPropertyNames().toArray(new String[change.getAddedPropertyNames().size()]));
        }
        if (change.getChangedPropertyNames() != null) {
            props.put(SlingConstants.PROPERTY_CHANGED_ATTRIBUTES, change.getChangedPropertyNames().toArray(new String[change.getChangedPropertyNames().size()]));
        }
        if ( change.getRemovedPropertyNames() != null ) {
            props.put(SlingConstants.PROPERTY_REMOVED_ATTRIBUTES, change.getRemovedPropertyNames().toArray(new String[change.getRemovedPropertyNames().size()]));
        }
        if (change.getType() != ChangeType.REMOVED) {
            Resource resource = resolver.getResource(change.getPath());
            if (resource == null) {
                resolver.refresh();
                resource = resolver.getResource(change.getPath());
            }
            if (resource != null) {
                if (resource.getResourceType() != null) {
                    props.put(SlingConstants.PROPERTY_RESOURCE_TYPE, resource.getResourceType());
                }
                if (resource.getResourceSuperType() != null) {
                    props.put(SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE, resource.getResourceSuperType());
                }
            }
        }
        if (change.isExternal()) {
            props.put("event.application", "unknown");
        }

        final Event event = new Event(topic, props);
        eventAdmin.sendEvent(event);
    }

    private class EventSendingJob implements Runnable {

        private final BlockingQueue<ResourceChange> changes;

        private volatile boolean stop;

        public EventSendingJob(BlockingQueue<ResourceChange> changes) {
            this.changes = changes;
        }

        @Override
        public void run() {
            while (!stop) {
                ResourceChange change = null;
                try {
                    change = changes.poll(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    logger.warn("Interrupted the OSGi runnable", e);
                }
                if (change == null) {
                    continue;
                }
                try {
                    sendOsgiEvent(change);
                } catch (Exception e) {
                    logger.error("processOsgiEventQueue: Unexpected problem processing resource change {}", change, e);
                }
            }
        }

        public void stop() {
            stop = true;
        }
    }

}

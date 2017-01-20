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

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ResourceChangeListener.class,
configurationPolicy = ConfigurationPolicy.IGNORE,
property = {
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
        ResourceChangeListener.PATHS + "=/",
        ResourceChangeListener.CHANGES + "=ADDED",
        ResourceChangeListener.CHANGES + "=CHANGED",
        ResourceChangeListener.CHANGES + "=REMOVED"
})
public class OsgiObservationBridge implements ResourceChangeListener, ExternalResourceChangeListener {

    private final Logger logger = LoggerFactory.getLogger(OsgiObservationBridge.class);

    @Reference
    private EventAdmin eventAdmin;

    @Reference
    private ResourceResolverFactory resolverFactory;

    private ResourceResolver resolver;

    private BlockingQueue<ResourceChange> changesQueue;

    private EventSendingJob job;

    protected void activate() throws LoginException {
        resolver = resolverFactory.getServiceResourceResolver(Collections.<String, Object>singletonMap(ResourceResolverFactory.SUBSERVICE, "observation"));
        changesQueue = new LinkedBlockingQueue<ResourceChange>();
        job = new EventSendingJob(changesQueue);
        Executors.newSingleThreadExecutor().submit(job);
    }

    @Reference(name = "handlers",
            cardinality=ReferenceCardinality.AT_LEAST_ONE,
            policy=ReferencePolicy.DYNAMIC,
            service=EventHandler.class,
            target="(|(event.topics=org/apache/sling/api/resource/Resource/*)(event.topics=org/apache/sling/api/resource/ResourceProvider/*))")
    private void bindEventHandler(final EventHandler handler) {
        logger.warn("Found OSGi Event Handler for deprecated resource bridge: {}", handler);
    }

    @SuppressWarnings("unused")
    private void unbindEventHandler(final EventHandler handler) {
        // nothing to do here
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

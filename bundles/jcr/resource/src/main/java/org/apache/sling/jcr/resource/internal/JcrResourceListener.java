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
package org.apache.sling.jcr.resource.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.Constants;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JcrResourceListener</code> listens for JCR observation
 * events and creates resource events which are sent through the
 * OSGi event admin.
 */
@Component(immediate = true)
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Apache Sling JcrResourceListener"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation")

})
public class JcrResourceListener implements EventListener {

    /** Logger */
    private final Logger logger = LoggerFactory.getLogger(JcrResourceListener.class);

    @Reference(policy=ReferencePolicy.DYNAMIC)
    private EventAdmin eventAdmin;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    /** The admin resource resolver. */
    private ResourceResolver resourceResolver;

    /** The session for observation. */
    private Session session;

    /** Everything below this path is observed. */
    private String startPath;

    /** The repository is mounted under this path. */
    private String mountPrefix;

    /** Is the Jackrabbit event class available? */
    private final boolean hasJackrabbitEventClass;

    /**
     * A queue of OSGi Events created by
     * {@link #sendOsgiEvent(String, Event, String, EventAdmin, ChangedAttributes)}
     * waiting for actual dispatching to the OSGi Event Admin in
     * {@link #processOsgiEventQueue()}
     */
    private LinkedBlockingQueue<Dictionary<String, Object>> osgiEventQueue;

    /**
     * Marker event for {@link #processOsgiEventQueue()} to be signaled to
     * terminate processing Events.
     */
    private final Dictionary<String, Object> TERMINATE_PROCESSING = new Hashtable<String, Object>(1);

    public JcrResourceListener() {
        boolean foundClass = false;
        try {
            this.getClass().getClassLoader().loadClass(JackrabbitEvent.class.getName());
            foundClass = true;
        } catch (final Throwable t) {
            // we ignore this
        }
        this.hasJackrabbitEventClass = foundClass;
    }

    @Activate
    protected void activate() throws LoginException {
        this.resourceResolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
        this.startPath = "/";
        this.mountPrefix = null;

        this.osgiEventQueue = new LinkedBlockingQueue<Dictionary<String,Object>>();
        final Thread oeqt = new Thread(new Runnable() {
            public void run() {
                init();
                processOsgiEventQueue();
            }
        }, "JCR Resource Event Queue Processor");
        oeqt.start();

    }

    private void init() {
        // lazy polling
        Session session = null;
        ResourceResolver resolver = this.resourceResolver;
        while ( resolver != null && session == null ) {
            session = this.resourceResolver.adaptTo(Session.class);
            if ( session == null ) {
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException ignore) {
                    // we ignore this
                }
                resolver = this.resourceResolver;
            }
        }
        if ( session != null ) {
            try {
                session.getWorkspace().getObservationManager().addEventListener(this,
                                Event.NODE_ADDED|Event.NODE_REMOVED|Event.PROPERTY_ADDED|Event.PROPERTY_CHANGED|Event.PROPERTY_REMOVED,
                                this.startPath, true, null, null, false);
                this.session = session;
            } catch (final RepositoryException re) {
                logger.error("Unable to register event listener.", re);
                this.deactivate();
            }
        }
    }

    /**
     * Dispose this listener.
     */
    @Deactivate
    protected void deactivate() {
        // unregister from observations
        if ( this.session != null ) {
            try {
                this.session.getWorkspace().getObservationManager().removeEventListener(this);
            } catch (RepositoryException e) {
                logger.warn("Unable to remove session listener: " + this, e);
            }
        }
        if ( this.resourceResolver != null ) {
            this.resourceResolver.close();
            this.resourceResolver = null;
        }

        // drop any remaining OSGi Events not processed yet
        this.osgiEventQueue.clear();
        this.osgiEventQueue.offer(TERMINATE_PROCESSING);
    }

    /**
     * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
     */
    public void onEvent(final EventIterator events) {
        // if the event admin is currently not available, we just skip this
        final EventAdmin localEA = this.eventAdmin;
        if ( localEA == null ) {
            return;
        }
        final Map<String, Event> addedEvents = new HashMap<String, Event>();
        final Map<String, ChangedAttributes> changedEvents = new HashMap<String, ChangedAttributes>();
        final Map<String, Event> removedEvents = new HashMap<String, Event>();
        while ( events.hasNext() ) {
            final Event event = events.nextEvent();
            try {
                final String eventPath;
                if ( this.mountPrefix != null ) {
                    eventPath = this.mountPrefix + event.getPath();
                } else {
                    eventPath = event.getPath();
                }
                if ( event.getType() == Event.PROPERTY_ADDED
                     || event.getType() == Event.PROPERTY_REMOVED
                     || event.getType() == Event.PROPERTY_CHANGED ) {
                    final int lastSlash = eventPath.lastIndexOf('/');
                    final String nodePath = eventPath.substring(0, lastSlash);
                    final String propName = eventPath.substring(lastSlash + 1);
                    this.updateChangedEvent(changedEvents, nodePath, event, propName);

                } else if ( event.getType() == Event.NODE_ADDED ) {
                    // check if this is a remove/add operation
                    if ( removedEvents.remove(eventPath) != null ) {
                        this.updateChangedEvent(changedEvents, eventPath, event, null);
                    } else {
                        addedEvents.put(eventPath, event);
                    }

                } else if ( event.getType() == Event.NODE_REMOVED) {
                    // remove is the strongest operation, therefore remove all removed
                    // paths from added
                    addedEvents.remove(eventPath);
                    removedEvents.put(eventPath, event);
                }
            } catch (final RepositoryException e) {
                logger.error("Error during modification: {}", e.getMessage());
            }
        }

        for (final Entry<String, Event> e : removedEvents.entrySet()) {
            // Launch an OSGi event
            sendOsgiEvent(e.getKey(), e.getValue(), SlingConstants.TOPIC_RESOURCE_REMOVED,
                changedEvents.remove(e.getKey()));
        }

        for (final Entry<String, Event> e : addedEvents.entrySet()) {
            // Launch an OSGi event.
            sendOsgiEvent(e.getKey(), e.getValue(), SlingConstants.TOPIC_RESOURCE_ADDED,
                changedEvents.remove(e.getKey()));
        }

        // Send the changed events.
        for (final Entry<String, ChangedAttributes> e : changedEvents.entrySet()) {
            // Launch an OSGi event.
            sendOsgiEvent(e.getKey(), e.getValue().firstEvent, SlingConstants.TOPIC_RESOURCE_CHANGED, e.getValue());
        }
    }

    private static final class ChangedAttributes {

        private final Event firstEvent;

        public ChangedAttributes(final Event event) {
            this.firstEvent = event;
        }

        public Set<String> addedAttributes, changedAttributes, removedAttributes;

        public void addEvent(final Event event, final String propName) {
            if ( event.getType() == Event.PROPERTY_ADDED ) {
                if ( removedAttributes != null ) {
                    removedAttributes.remove(propName);
                }
                if ( addedAttributes == null ) {
                    addedAttributes = new HashSet<String>();
                }
                addedAttributes.add(propName);
            } else if ( event.getType() == Event.PROPERTY_REMOVED ) {
                if ( addedAttributes != null ) {
                    addedAttributes.remove(propName);
                }
                if ( removedAttributes == null ) {
                    removedAttributes = new HashSet<String>();
                }
                removedAttributes.add(propName);
            } else if ( event.getType() == Event.PROPERTY_CHANGED ) {
                if ( changedAttributes == null ) {
                    changedAttributes = new HashSet<String>();
                }
                changedAttributes.add(propName);
            }
        }

        public void addProperties(final Dictionary<String, Object> properties) {
            // we're not using the Constants from SlingConstants here to avoid the requirement of the latest
            // SLING API to be available!!
            if ( addedAttributes != null )  {
                properties.put("resourceAddedAttributes", addedAttributes.toArray(new String[addedAttributes.size()]));
            }
            if ( changedAttributes != null )  {
                properties.put("resourceChangedAttributes", changedAttributes.toArray(new String[changedAttributes.size()]));
            }
            if ( removedAttributes != null )  {
                properties.put("resourceRemovedAttributes", removedAttributes.toArray(new String[removedAttributes.size()]));
            }
        }
    }

    private void updateChangedEvent(final Map<String, ChangedAttributes> changedEvents, final String path,
            final Event event, final String propName) {
        ChangedAttributes storedEvent = changedEvents.get(path);
        if ( storedEvent == null ) {
            storedEvent = new ChangedAttributes(event);
            changedEvents.put(path, storedEvent);
        }
        storedEvent.addEvent(event, propName);
    }

    /**
     * Send an OSGi event based on a JCR Observation Event.
     *
     * @param path The path too the node where the event occurred.
     * @param event The JCR observation event.
     * @param topic The topic that should be used for the OSGi event.
     */
    private void sendOsgiEvent(final String path, final Event event, final String topic,
            final ChangedAttributes changedAttributes) {

        final Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(SlingConstants.PROPERTY_USERID, event.getUserID());
        if (this.isExternal(event)) {
            properties.put("event.application", "unknown");
        }
        if (changedAttributes != null) {
            changedAttributes.addProperties(properties);
        }

        // set the path (might have been changed for nt:file content)
        properties.put(SlingConstants.PROPERTY_PATH, path);
        properties.put(EventConstants.EVENT_TOPIC, topic);

        // enqueue event for dispatching
        this.osgiEventQueue.offer(properties);
    }

    /**
     * Called by the Runnable.run method of the JCR Event Queue processor to
     * process the {@link #osgiEventQueue} until the
     * {@link #TERMINATE_PROCESSING} event is received.
     */
    void processOsgiEventQueue() {
        while (true) {
            final Dictionary<String, Object> event;
            try {
                event = this.osgiEventQueue.take();
            } catch (InterruptedException e) {
                // interrupted waiting for the event; keep on waiting
                continue;
            }

            if (event == null || event == TERMINATE_PROCESSING) {
                break;
            }

            try {
                final EventAdmin localEa = this.eventAdmin;
                if (localEa != null) {
                    final String topic = (String) event.remove(EventConstants.EVENT_TOPIC);
                    final String path = (String) event.get(SlingConstants.PROPERTY_PATH);
                    Resource resource = this.resourceResolver.getResource(path);
                    boolean sendEvent = true;
                    if (!SlingConstants.TOPIC_RESOURCE_REMOVED.equals(topic)) {
                        if (resource != null) {
                            // check if this is a JCR backed resource, otherwise it is not visible!
                            final Node node = resource.adaptTo(Node.class);
                            if (node != null) {
                                // check for nt:file nodes
                                if (path.endsWith("/jcr:content")) {
                                    try {
                                        if (node.getParent().isNodeType("nt:file")) {
                                            final Resource parentResource = resource.getParent();
                                            if (parentResource != null) {
                                                resource = parentResource;
                                                event.put(SlingConstants.PROPERTY_PATH, resource.getPath());
                                            }
                                        }
                                    } catch (final RepositoryException re) {
                                        // ignore this
                                    }
                                }

                                final String resourceType = resource.getResourceType();
                                if (resourceType != null) {
                                    event.put(SlingConstants.PROPERTY_RESOURCE_TYPE, resource.getResourceType());
                                }
                                final String resourceSuperType = resource.getResourceSuperType();
                                if (resourceSuperType != null) {
                                    event.put(SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE, resource.getResourceSuperType());
                                }
                            } else {
                                // this is not a jcr backed resource
                                sendEvent = false;
                            }

                        } else {
                            // take a quite silent note of not being able to
                            // resolve the resource
                            logger.debug(
                                "processOsgiEventQueue: Resource at {} not found, which is not expected for an added or modified node",
                                path);
                            sendEvent = false;
                        }
                    } else {
                        // check if the resource is still available - if so the node was not visible!
                        if ( resource != null ) {
                            sendEvent = false;
                        }
                    }

                    if ( sendEvent ) {
                        localEa.sendEvent(new org.osgi.service.event.Event(topic, event));
                    }
                }
            } catch (final Exception e) {
                logger.warn("processOsgiEventQueue: Unexpected problem processing event " + event, e);
            }
        }

        this.osgiEventQueue.clear();
    }

    private boolean isExternal(final Event event) {
        if ( this.hasJackrabbitEventClass && event instanceof JackrabbitEvent) {
            final JackrabbitEvent jEvent = (JackrabbitEvent)event;
            return jEvent.isExternal();
        }
        return false;
    }
}

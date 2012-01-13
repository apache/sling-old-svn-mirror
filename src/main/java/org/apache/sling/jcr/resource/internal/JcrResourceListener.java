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

import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JcrResourceListener</code> listens for JCR observation
 * events and creates resource events which are sent through the
 * OSGi event admin.
 */
public class JcrResourceListener implements EventListener {

    /** Logger */
    private final Logger logger = LoggerFactory.getLogger(JcrResourceListener.class);

    /** The workspace for observation. */
    private final String workspaceName;

    /** The session for observation. */
    private final Session session;

    /** Everything below this path is observed. */
    private final String startPath;

    /** The repository is mounted under this path. */
    private final String mountPrefix;

    /** The resource resolver. */
    private final ResourceResolver resolver;

    /** The event admin tracker. */
    private final ServiceTracker eventAdminTracker;

    /** Is the Jackrabbit event class available? */
    private final boolean hasJackrabbitEventClass;

    /**
     * A queue of OSGi Events created by
     * {@link #sendOsgiEvent(String, Event, String, EventAdmin, ChangedAttributes)}
     * waiting for actual dispatching to the OSGi Event Admin in
     * {@link #processOsgiEventQueue()}
     */
    private final LinkedBlockingQueue<Dictionary<String, Object>> osgiEventQueue;

    /**
     * Marker event for {@link #processOsgiEventQueue()} to be signaled to
     * terminate processing Events.
     */
    private final Dictionary<String, Object> TERMINATE_PROCESSING = new Hashtable<String, Object>(1);

    /**
     * Constructor.
     * @param workspaceName The workspace name to observe
     * @param factory    The resource resolver factory.
     * @param startPath  The observation root path
     * @param mountPrefix The mount path in the repository
     * @param eventAdminTracker The service tracker for the event admin.
     * @throws RepositoryException
     */
    public JcrResourceListener(final String workspaceName,
                               final ResourceResolverFactory factory,
                               final String startPath,
                               final String mountPrefix,
                               final ServiceTracker eventAdminTracker)
    throws LoginException, RepositoryException {
        this.workspaceName = workspaceName;
        final Map<String,Object> authInfo = new HashMap<String,Object>();
        if (workspaceName != null) {
            authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_WORKSPACE,
                workspaceName);
        }
        this.resolver = factory.getAdministrativeResourceResolver(authInfo);
        this.session = resolver.adaptTo(Session.class);
        this.startPath = startPath;
        this.eventAdminTracker = eventAdminTracker;
        this.mountPrefix = (mountPrefix.equals("/") ? null : mountPrefix);

        this.osgiEventQueue = new LinkedBlockingQueue<Dictionary<String,Object>>();
        Thread oeqt = new Thread(new Runnable() {
            public void run() {
                processOsgiEventQueue();
            }
        }, "JCR Resource Event Queue Processor");
        oeqt.start();

        this.session.getWorkspace().getObservationManager().addEventListener(this,
            Event.NODE_ADDED|Event.NODE_REMOVED|Event.PROPERTY_ADDED|Event.PROPERTY_CHANGED|Event.PROPERTY_REMOVED,
            this.startPath, true, null, null, false);
        boolean foundClass = false;
        try {
            this.getClass().getClassLoader().loadClass(JackrabbitEvent.class.getName());
            foundClass = true;
        } catch (final Throwable t) {
            // we ignore this
        }
        this.hasJackrabbitEventClass = foundClass;
    }

    /**
     * Dispose this listener.
     */
    public void dispose() {

        // unregister from observations
        try {
            this.session.getWorkspace().getObservationManager().removeEventListener(this);
        } catch (RepositoryException e) {
            logger.warn("Unable to remove session listener: " + this, e);
        }
        this.resolver.close();

        // drop any remaining OSGi Events not processed yet
        this.osgiEventQueue.clear();
        this.osgiEventQueue.offer(TERMINATE_PROCESSING);
    }

    /**
     * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
     */
    public void onEvent(EventIterator events) {
        // if the event admin is currently not available, we just skip this
        final EventAdmin localEA = (EventAdmin) this.eventAdminTracker.getService();
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
                    if ( !addedEvents.containsKey(nodePath) ) {
                        this.updateChangedEvent(changedEvents, nodePath, event, propName);
                    }
                } else if ( event.getType() == Event.NODE_ADDED ) {
                    // check if this is a remove/add operation
                    if ( removedEvents.remove(eventPath) != null ) {
                        this.updateChangedEvent(changedEvents, eventPath, event, null);
                    } else {
                        changedEvents.remove(eventPath);
                        addedEvents.put(eventPath, event);
                    }

                } else if ( event.getType() == Event.NODE_REMOVED) {
                    // remove is the strongest operation, therefore remove all removed
                    // paths from changed and added
                    addedEvents.remove(eventPath);
                    changedEvents.remove(eventPath);

                    removedEvents.put(eventPath, event);
                }
            } catch (final RepositoryException e) {
                logger.error("Error during modification: {}", e.getMessage());
            }
        }

        for (final Entry<String, Event> e : removedEvents.entrySet()) {
            // Launch an OSGi event
            sendOsgiEvent(e.getKey(), e.getValue(), SlingConstants.TOPIC_RESOURCE_REMOVED, null);
        }

        for (final Entry<String, Event> e : addedEvents.entrySet()) {
            // Launch an OSGi event.
            sendOsgiEvent(e.getKey(), e.getValue(), SlingConstants.TOPIC_RESOURCE_ADDED, null);
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
    private void sendOsgiEvent(String path, final Event event, final String topic,
            final ChangedAttributes changedAttributes) {

        path = createWorkspacePath(path);

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
                final EventAdmin localEa = (EventAdmin) this.eventAdminTracker.getService();
                if (localEa != null) {
                    final String topic = (String) event.remove(EventConstants.EVENT_TOPIC);
                    if (!SlingConstants.TOPIC_RESOURCE_REMOVED.equals(topic)) {
                        final String path = (String) event.get(SlingConstants.PROPERTY_PATH);
                        Resource resource = this.resolver.getResource(path);
                        if (resource != null) {
                            // check for nt:file nodes
                            if (path.endsWith("/jcr:content")) {
                                final Node node = resource.adaptTo(Node.class);
                                if (node != null) {
                                    try {
                                        if (node.getParent().isNodeType("nt:file")) {
                                            @SuppressWarnings("deprecation")
                                            final Resource parentResource = ResourceUtil.getParent(resource);
                                            if (parentResource != null) {
                                                resource = parentResource;
                                                event.put(SlingConstants.PROPERTY_PATH, resource.getPath());
                                            }
                                        }
                                    } catch (RepositoryException re) {
                                        // ignore this
                                    }
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
                            logger.error(
                                "processOsgiEventQueue: Resource at {} not found, which is not expected for an added or modified node",
                                path);
                        }
                    }

                    localEa.postEvent(new org.osgi.service.event.Event(topic, event));
                }
            } catch (Exception e) {
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

    private String createWorkspacePath(final String path) {
        if (workspaceName == null) {
            return path;
        }
        return workspaceName + ":" + path;
    }
}

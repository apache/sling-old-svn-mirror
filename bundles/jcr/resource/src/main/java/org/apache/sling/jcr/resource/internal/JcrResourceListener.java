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
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.event.EventAdmin;
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
        this.session.getWorkspace().getObservationManager().addEventListener(this,
            Event.NODE_ADDED|Event.NODE_REMOVED|Event.PROPERTY_ADDED|Event.PROPERTY_CHANGED|Event.PROPERTY_REMOVED,
            this.startPath, true, null, null, false);
    }

    /**
     * Dispose this listener.
     */
    public void dispose() {
        try {
            this.session.getWorkspace().getObservationManager().removeEventListener(this);
        } catch (RepositoryException e) {
            logger.warn("Unable to remove session listener: " + this, e);
        }
        this.resolver.close();
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
        final Map<String, Event> changedEvents = new HashMap<String, Event>();
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
                    changedEvents.put(eventPath.substring(0, lastSlash), event);
                } else if ( event.getType() == Event.NODE_ADDED ) {
                    // check if this is a remove/add operation
                    if ( removedEvents.containsKey(eventPath) ) {
                        changedEvents.put(eventPath, event);
                    } else {
                        addedEvents.put(eventPath, event);
                    }
                } else if ( event.getType() == Event.NODE_REMOVED) {
                    // check if this is a add/remove operation
                    if ( !addedEvents.containsKey(eventPath) ) {
                        removedEvents.put(eventPath, event);
                    }
                }
            } catch (RepositoryException e) {
                logger.error("Error during modificatiozas" +
                		"n: {}", e.getMessage());
            }
        }

        for (final Entry<String, Event> e : removedEvents.entrySet()) {
            // remove is the strongest operation, therefore remove all removed
            // paths from changed and added
            addedEvents.remove(e.getKey());
            changedEvents.remove(e.getKey());

            // Launch an OSGi event
            final Dictionary<String, String> properties = new Hashtable<String, String>();
            properties.put(SlingConstants.PROPERTY_PATH, createWorkspacePath(e.getKey()));
            properties.put(SlingConstants.PROPERTY_USERID, e.getValue().getUserID());
            localEA.postEvent(new org.osgi.service.event.Event(SlingConstants.TOPIC_RESOURCE_REMOVED, properties));
        }

        // add is stronger than changed
        for (final Entry<String, Event> e : addedEvents.entrySet()) {
            changedEvents.remove(e.getKey());

            // Launch an OSGi event.
            sendOsgiEvent(e.getKey(), e.getValue(), SlingConstants.TOPIC_RESOURCE_ADDED, localEA);
        }

        // Send the changed events.
        for (final Entry<String, Event> e : changedEvents.entrySet()) {
            // Launch an OSGi event.
            sendOsgiEvent(e.getKey(), e.getValue(), SlingConstants.TOPIC_RESOURCE_CHANGED, localEA);
        }
    }

    /**
     * Send an OSGi event based on a JCR Observation Event.
     * @param path The path too the node where the event occurred.
     * @param event The JCR observation event.
     * @param topic The topic that should be used for the OSGi event.
     * @param localEA The OSGi Event Admin that can be used to post events.
     */
    private void sendOsgiEvent(String path, Event event, final String topic, final EventAdmin localEA) {
        path = createWorkspacePath(path);
        Resource resource = this.resolver.getResource(path);
        if ( resource != null ) {
            // check for nt:file nodes
            if ( path.endsWith("/jcr:content") ) {
                final Node node = resource.adaptTo(Node.class);
                if ( node != null ) {
                    try {
                        if (node.getParent().isNodeType("nt:file") ) {
                            final Resource parentResource = ResourceUtil.getParent(resource);
                            if ( parentResource != null ) {
                                resource = parentResource;
                            }
                        }
                    } catch (RepositoryException re) {
                        // ignore this
                    }
                }
            }
            final Dictionary<String, String> properties = new Hashtable<String, String>();
            properties.put(SlingConstants.PROPERTY_PATH, resource.getPath());
            properties.put(SlingConstants.PROPERTY_USERID, event.getUserID());
            final String resourceType = resource.getResourceType();
            if ( resourceType != null ) {
                properties.put(SlingConstants.PROPERTY_RESOURCE_TYPE, resource.getResourceType());
            }
            final String resourceSuperType = resource.getResourceSuperType();
            if ( resourceSuperType != null ) {
                properties.put(SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE, resource.getResourceSuperType());
            }
            localEA.postEvent(new org.osgi.service.event.Event(topic, properties));
        }
    }

    private String createWorkspacePath(final String path) {
        if (workspaceName == null) {
            return path;
        }
        return workspaceName + ":" + path;
    }
}

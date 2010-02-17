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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
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
     * @param repository The repository to observe.
     * @param factory    The resource resolver factory.
     * @param startPath  The observation root path
     * @param mountPrefix The mount path in the repository
     * @param eventAdminTracker The service tracker for the event admin.
     * @throws RepositoryException
     */
    public JcrResourceListener(final SlingRepository repository,
                               final JcrResourceResolverFactory factory,
                               final String startPath,
                               final String mountPrefix,
                               final ServiceTracker eventAdminTracker)
    throws RepositoryException {
        this.session = repository.loginAdministrative(null);
        this.resolver = factory.getResourceResolver(this.session);
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
        this.session.logout();
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
        final Set<String>addedPaths = new HashSet<String>();
        final Set<String>removedPaths = new HashSet<String>();
        final Set<String>changedPaths = new HashSet<String>();
        while ( events.hasNext() ) {
            final Event event = events.nextEvent();
            try {
                Set<String> set = null;
                String nodePath = event.getPath();
                if ( event.getType() == Event.PROPERTY_ADDED
                     || event.getType() == Event.PROPERTY_REMOVED
                     || event.getType() == Event.PROPERTY_CHANGED ) {
                    final int lastSlash = nodePath.lastIndexOf('/');
                    nodePath = nodePath.substring(0, lastSlash);
                    set = changedPaths;
                } else if ( event.getType() == Event.NODE_ADDED ) {
                    set = addedPaths;
                } else if ( event.getType() == Event.NODE_REMOVED) {
                    set = removedPaths;
                }
                if ( set != null ) {
                    if ( this.mountPrefix != null ) {
                        set.add(this.mountPrefix + nodePath);
                    } else {
                        set.add(nodePath);
                    }
                }
            } catch (RepositoryException e) {
                logger.error("Error during modification: {}", e.getMessage());
            }
        }
        // remove is the strongest operation, therefore remove all removed
        // paths from changed and added
        addedPaths.removeAll(removedPaths);
        changedPaths.removeAll(removedPaths);
        // add is stronger than changed
        changedPaths.removeAll(addedPaths);

        // send events for added and changed
        sendEvents(addedPaths, SlingConstants.TOPIC_RESOURCE_ADDED, localEA);
        sendEvents(changedPaths, SlingConstants.TOPIC_RESOURCE_CHANGED, localEA);

        // send events for removed
        for(final String path : removedPaths) {
            final Dictionary<String, String> properties = new Hashtable<String, String>();
            properties.put(SlingConstants.PROPERTY_PATH, path);

            localEA.postEvent(new org.osgi.service.event.Event(SlingConstants.TOPIC_RESOURCE_REMOVED, properties));
        }
    }

    private void sendEvents(final Set<String> paths, final String topic, final EventAdmin localEA) {
        for(final String path : paths) {
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
    }
}

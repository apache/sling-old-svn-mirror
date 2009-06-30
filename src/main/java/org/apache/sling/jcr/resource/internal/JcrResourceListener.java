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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrResourceListener implements EventListener {

    /** Logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(JcrResourceListener.class);

    /** The session for observation. */
    private final Session session;

    /** Everything below this path is observed. */
    private final String startPath;

    /** The repository is mounted under this path. */
    private final String mountPrefix;

    /** The resource resolver. */
    private final ResourceResolver resolver;

    /** The event admin tracker. */
    private ServiceTracker eventAdminTracker;

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

    public void dispose() {
        try {
            this.session.getWorkspace().getObservationManager().removeEventListener(this);
        } catch (RepositoryException e) {
            LOGGER.warn("Unable to remove session listener: " + this, e);
        }
        this.session.logout();
    }

    /**
     * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
     */
    public void onEvent(EventIterator events) {
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
                LOGGER.error("Error during modification: {}", e.getMessage());
            }
        }
        // remove is the strongest oberation, therefore remove all removed
        // paths from changed and added
        addedPaths.removeAll(removedPaths);
        changedPaths.removeAll(removedPaths);
        // add is stronger than changed
        changedPaths.removeAll(addedPaths);

        // send events
        for(final String path : addedPaths) {
            final Resource resource = this.resolver.getResource(path);
            if ( resource != null ) {
                final Dictionary<String, String> properties = new Hashtable<String, String>();
                properties.put(SlingConstants.PROPERTY_PATH, resource.getPath());
                properties.put(SlingConstants.PROPERTY_RESOURCE_TYPE, resource.getResourceType());
                properties.put(SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE, resource.getResourceSuperType());

                localEA.postEvent(new org.osgi.service.event.Event(SlingConstants.TOPIC_RESOURCE_ADDED, properties));
            }
        }
        for(final String path : changedPaths) {
            final Resource resource = this.resolver.getResource(path);
            if ( resource != null ) {
                final Dictionary<String, String> properties = new Hashtable<String, String>();
                properties.put(SlingConstants.PROPERTY_PATH, resource.getPath());
                properties.put(SlingConstants.PROPERTY_RESOURCE_TYPE, resource.getResourceType());
                properties.put(SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE, resource.getResourceSuperType());

                localEA.postEvent(new org.osgi.service.event.Event(SlingConstants.TOPIC_RESOURCE_CHANGED, properties));
            }
        }
        for(final String path : removedPaths) {
            final Dictionary<String, String> properties = new Hashtable<String, String>();
            properties.put(SlingConstants.PROPERTY_PATH, path);

            localEA.postEvent(new org.osgi.service.event.Event(SlingConstants.TOPIC_RESOURCE_REMOVED, properties));
        }
    }
}

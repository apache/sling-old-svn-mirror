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

import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ObserverConfiguration;

/**
 * The <code>JcrResourceListener</code> listens for JCR observation
 * events and creates resource change events which are sent through
 * the {@link ObservationReporter}.
 */
public class JcrResourceListener implements EventListener, Closeable {

    private volatile ObserverConfiguration config;

    private final JcrListenerBaseConfig baseConfig;

    public JcrResourceListener(final JcrListenerBaseConfig listenerConfig,
                    final ObserverConfiguration config)
    throws RepositoryException {
        this.baseConfig = listenerConfig;
        this.config = config;
        this.baseConfig.register(this, config);
    }

    /**
     * Update the observation configuration.
     *
     * @param cfg The updated config
     */
    public void update(final ObserverConfiguration cfg) {
        this.config = cfg;
    }

    /**
     * Get the observation configuration
     * @return The observation configuration
     */
    public ObserverConfiguration getConfig() {
        return this.config;
    }

    /**
     * Dispose this listener.
     */
    @Override
    public void close() throws IOException {
        // unregister from observations
        this.baseConfig.unregister(this);
    }

    /**
     * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
     */
    @Override
    public void onEvent(final EventIterator events) {
        final Map<String, ResourceChange> addedEvents = new HashMap<String, ResourceChange>();
        final Map<String, ResourceChange> changedEvents = new HashMap<String, ResourceChange>();
        final Map<String, ResourceChange> removedEvents = new HashMap<String, ResourceChange>();

        while ( events.hasNext() ) {
            final Event event = events.nextEvent();

            final String identifier;
            final String path;
            try {
                identifier = event.getIdentifier();
                path =  event.getPath();
            } catch (final RepositoryException e) {
                // event.getPath or event.getIdentifier threw an exception
                // there is nothing we can do about it anyway
                continue;
            }

            final String eventPath = (identifier != null && identifier.startsWith("/") ? identifier : path);
            final int type = event.getType();

            if ( type == PROPERTY_ADDED && path.endsWith("/jcr:primaryType") ) {
                final int lastSlash = path.lastIndexOf('/');
                final String rsrcPath = path.substring(0, lastSlash);

                // add is stronger than update
                changedEvents.remove(rsrcPath);
                addedEvents.put(rsrcPath, createResourceChange(event, rsrcPath, ChangeType.ADDED));
            } else if ( type == PROPERTY_ADDED
                     || type == PROPERTY_REMOVED
                     || type == PROPERTY_CHANGED ) {
                final String rsrcPath;
                if ( identifier == null || !identifier.startsWith("/") ) {
                    final int lastSlash = eventPath.lastIndexOf('/');
                    rsrcPath = eventPath.substring(0, lastSlash);
                } else {
                    rsrcPath = eventPath;
                }
                if ( !addedEvents.containsKey(rsrcPath)
                  && !removedEvents.containsKey(rsrcPath)
                  && !changedEvents.containsKey(rsrcPath) ) {

                    changedEvents.put(rsrcPath, createResourceChange(event, rsrcPath, ChangeType.CHANGED));
                }
            } else if ( type == NODE_ADDED ) {
                // add is stronger than update
                changedEvents.remove(eventPath);
                addedEvents.put(eventPath, createResourceChange(event, eventPath, ChangeType.ADDED));
            } else if ( type == NODE_REMOVED) {
                // remove is stronger than add and change
                addedEvents.remove(eventPath);
                changedEvents.remove(eventPath);
                removedEvents.put(eventPath, createResourceChange(event, eventPath, ChangeType.REMOVED));
            }
        }

        final List<ResourceChange> changes = new ArrayList<ResourceChange>();
        changes.addAll(addedEvents.values());
        changes.addAll(removedEvents.values());
        changes.addAll(changedEvents.values());
        this.baseConfig.getReporter().reportChanges(this.config, changes, false);

    }

    private ResourceChange createResourceChange(final Event event,
            final String path,
            final ChangeType changeType) {
        final String fullPath = this.baseConfig.getPathMapper().mapJCRPathToResourcePath(path);
        final boolean isExternal = this.isExternal(event);
        final String userId;
        if (!isExternal) {
            userId = event.getUserID();
        } else {
            userId = null;
        }
        return new JcrResourceChange(changeType, fullPath, isExternal, userId);
    }

    private boolean isExternal(final Event event) {
        if ( event instanceof JackrabbitEvent) {
            final JackrabbitEvent jEvent = (JackrabbitEvent)event;
            return jEvent.isExternal();
        }
        return false;
    }

    @Override
    public String toString() {
        return "JcrResourceListener [" + config + "]";
    }
}

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

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.jackrabbit.api.observation.JackrabbitEventFilter;
import org.apache.jackrabbit.api.observation.JackrabbitObservationManager;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.path.Path;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.internal.helper.jcr.PathMapper;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ObserverConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the base configuration for a JCR listener, shared
 * by all registered {@link JcrResourceListener}s.
 */
public class JcrListenerBaseConfig implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(JcrResourceListener.class);

    private final Session session;

    private final PathMapper pathMapper;

    private final ObservationReporter reporter;

    @SuppressWarnings("deprecation")
    public JcrListenerBaseConfig(
                    final ObservationReporter reporter,
                    final PathMapper pathMapper,
                    final SlingRepository repository)
    throws RepositoryException {
        this.pathMapper = pathMapper;
        this.reporter = reporter;
        this.session = repository.loginAdministrative(repository.getDefaultWorkspace());
    }

    /**
     * Dispose this config
     * Close session.
     */
    @Override
    public void close() throws IOException {
        this.session.logout();
    }

    public void register(final JcrResourceListener listener, final ObserverConfiguration config)
    throws RepositoryException {
        final ObservationManager mgr = this.session.getWorkspace().getObservationManager();
        if ( mgr instanceof JackrabbitObservationManager ) {
            final JackrabbitEventFilter filter = new JackrabbitEventFilter();

            // paths
            final Set<String> paths = config.getPaths().toStringSet();
            final String[] pathArray = new String[paths.size()];
            int i=0;
            // remove global prefix
            boolean hasGlob = false;
            for(final String p : paths) {
                if ( p.startsWith(Path.GLOB_PREFIX )) {
                    hasGlob = true;
                }
                pathArray[i] = (p.startsWith(Path.GLOB_PREFIX) ? p.substring(Path.GLOB_PREFIX.length()) : p);
                i++;
            }
            final EventListener regListener;
            if ( hasGlob ) {
                // TODO we can't use glob patterns directly here
                filter.setAbsPath("/");
                regListener = new EventListener() {

                    @Override
                    public void onEvent(final EventIterator events) {
                        listener.onEvent(new EventIterator() {

                            Event next = seek();

                            private Event seek() {
                                while ( events.hasNext() ) {
                                    final Event e = events.nextEvent();
                                    String path = null;
                                    try {
                                        path = e.getPath();
                                        if ( e.getType() == Event.PROPERTY_ADDED
                                                || e.getType() == Event.PROPERTY_CHANGED
                                                || e.getType() == Event.PROPERTY_REMOVED ) {
                                                  path = ResourceUtil.getParent(path);
                                        }
                                        if ( config.getPaths().matches(path) != null ) {
                                            return e;
                                        }
                                        if ( path.endsWith("/jcr:content") && config.getPaths().matches(path.substring(0, path.length() - 12)) != null ) {
                                            return e;

                                        }
                                    } catch (RepositoryException e1) {
                                        // ignore
                                    }
                                }
                                return null;
                            }

                            @Override
                            public void remove() {
                                // we don't support this -> NOP
                            }

                            @Override
                            public Object next() {
                                return nextEvent();
                            }

                            @Override
                            public boolean hasNext() {
                                return next != null;
                            }

                            @Override
                            public void skip(long skipNum) {
                                // we don't support this -> NOP
                            }

                            @Override
                            public long getSize() {
                                // we don't support this -> 0
                                return 0;
                            }

                            @Override
                            public long getPosition() {
                                // we don't support this -> 0
                                return 0;
                            }

                            @Override
                            public Event nextEvent() {
                                final Event result = next;
                                next = seek();
                                return result;
                            }
                        });
                    }
                };

            } else {
                filter.setAdditionalPaths(pathArray);
                regListener = listener;
            }
            filter.setIsDeep(true);

            // exclude paths
            final Set<String> excludePaths = config.getExcludedPaths().toStringSet();
            if ( !excludePaths.isEmpty() ) {
                filter.setExcludedPaths(excludePaths.toArray(new String[excludePaths.size()]));
            }

            // external
            filter.setNoExternal(config.includeExternal());

            // types
            filter.setEventTypes(this.getTypes(config));

            ((JackrabbitObservationManager)mgr).addEventListener(regListener, filter);
        } else {
            throw new RepositoryException("Observation manager is not a JackrabbitObservationManager");
        }

    }

    private int getTypes(final ObserverConfiguration c) {
        int result = 0;
        for (ChangeType t : c.getChangeTypes()) {
            switch (t) {
            case ADDED:
                result = result | Event.NODE_ADDED;
                break;
            case REMOVED:
                result = result | Event.NODE_REMOVED;
                break;
            case CHANGED:
                result = result | Event.PROPERTY_ADDED;
                result = result | Event.PROPERTY_CHANGED;
                result = result | Event.PROPERTY_REMOVED;
                break;
            default:
                break;
            }
        }
        return result;
    }

    public void unregister(final JcrResourceListener listener) {
        try {
            this.session.getWorkspace().getObservationManager().removeEventListener(listener);
        } catch (RepositoryException e) {
            logger.warn("Unable to remove session listener: " + this, e);
        }
    }

    public Logger getLogger() {
        return this.logger;
    }

    public ObservationReporter getReporter() {
        return this.reporter;
    }

    public PathMapper getPathMapper() {
        return this.pathMapper;
    }

    public Session getSession() {
        return this.session;
    }
}

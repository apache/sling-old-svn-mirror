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

import static org.apache.sling.api.resource.observation.ResourceChangeListener.CHANGES;
import static org.apache.sling.api.resource.observation.ResourceChangeListener.PATHS;
import static org.apache.sling.commons.osgi.PropertiesUtil.toStringArray;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.path.PathSet;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.framework.ServiceReference;

/**
 * Information about a resource change listener.
 */
public class ResourceChangeListenerInfo {

    private static final Set<ChangeType> DEFAULT_CHANGE_RESOURCE_TYPES = EnumSet.of(ChangeType.ADDED, ChangeType.REMOVED, ChangeType.CHANGED);

    private static final Set<ChangeType> DEFAULT_CHANGE_PROVIDER_TYPES = EnumSet.of(ChangeType.PROVIDER_ADDED, ChangeType.PROVIDER_REMOVED);

    private final PathSet paths;

    private final Set<ChangeType> resourceChangeTypes;

    private final Set<ChangeType> providerChangeTypes;

    private final boolean valid;

    private volatile boolean external = false;

    private volatile ResourceChangeListener listener;

    public ResourceChangeListenerInfo(final ServiceReference ref, final String[] searchPaths) {
        boolean configValid = true;
        final Set<String> pathsSet = new HashSet<String>();
        final String paths[] = toStringArray(ref.getProperty(PATHS), null);
        if ( paths != null ) {
            for(final String p : paths) {
                String normalisedPath = ResourceUtil.normalize(p);
                if (!".".equals(p) && normalisedPath.isEmpty()) {
                    configValid = false;
                } else if ( normalisedPath.startsWith("/") ) {
                    pathsSet.add(normalisedPath);
                } else {
                    for(final String sp : searchPaths) {
                        if ( p.equals(".") ) {
                            pathsSet.add(sp);
                        } else {
                            pathsSet.add(ResourceUtil.normalize(sp + normalisedPath));
                        }
                    }
                }
            }
        }
        if ( pathsSet.isEmpty() ) {
            configValid = false;
        } else {
            // check for sub paths
            final Iterator<String> iter = pathsSet.iterator();
            while ( iter.hasNext() ) {
                final String path = iter.next();
                boolean remove = false;
                for(final String p : pathsSet) {
                    if ( p.length() > path.length() && path.startsWith(p + "/") ) {
                        remove = true;
                        break;
                    }
                }
                if ( remove ) {
                    iter.remove();
                }
            }
        }
        this.paths = PathSet.fromStringCollection(pathsSet);
        if (ref.getProperty(CHANGES) != null ) {
            final Set<ChangeType> rts = new HashSet<ChangeType>();
            final Set<ChangeType> pts = new HashSet<ChangeType>();
            for (final String changeName : toStringArray(ref.getProperty(CHANGES))) {
                try {
                    final ChangeType ct = ChangeType.valueOf(changeName);
                    if ( ct.ordinal() < ChangeType.PROVIDER_ADDED.ordinal()) {
                        rts.add(ct);
                    } else {
                        pts.add(ct);
                    }
                } catch ( final IllegalArgumentException iae) {
                    configValid = false;
                }
            }
            if ( rts.isEmpty() ) {
                this.resourceChangeTypes = Collections.emptySet();
            } else if ( rts.size() == 3 ) {
                this.resourceChangeTypes = DEFAULT_CHANGE_RESOURCE_TYPES;
            } else {
                this.resourceChangeTypes = Collections.unmodifiableSet(rts);
            }
            if ( pts.isEmpty() ) {
                this.providerChangeTypes = Collections.emptySet();
            } else if ( pts.size() == 2 ) {
                this.providerChangeTypes = DEFAULT_CHANGE_PROVIDER_TYPES;
            } else {
                this.providerChangeTypes = Collections.unmodifiableSet(pts);
            }
        } else {
            // default is added, changed, removed for resources and
            // added and removed for providers
            this.resourceChangeTypes = DEFAULT_CHANGE_RESOURCE_TYPES;
            this.providerChangeTypes = DEFAULT_CHANGE_PROVIDER_TYPES;
        }

        this.valid = configValid;
    }

    public boolean isValid() {
        return this.valid;
    }

    public Set<ChangeType> getResourceChangeTypes() {
        return this.resourceChangeTypes;
    }

    public Set<ChangeType> getProviderChangeTypes() {
        return this.providerChangeTypes;
    }

    public PathSet getPaths() {
        return this.paths;
    }

    public boolean isExternal() {
        return this.external;
    }

    public ResourceChangeListener getListener() {
        return listener;
    }

    public void setListener(final ResourceChangeListener listener) {
        this.listener = listener;
        this.external = listener instanceof ExternalResourceChangeListener;
    }
}

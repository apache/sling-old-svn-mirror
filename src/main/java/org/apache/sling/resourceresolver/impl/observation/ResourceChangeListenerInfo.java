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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.resourceresolver.impl.providers.tree.PathSet;
import org.osgi.framework.ServiceReference;

public class ResourceChangeListenerInfo {

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
                if ( p.isEmpty() ) {
                    configValid = false;
                } else if ( p.startsWith("/") ) {
                    pathsSet.add(p);
                } else {
                    for(final String sp : searchPaths) {
                        if ( p.equals(".") ) {
                            pathsSet.add(sp);
                        } else {
                            pathsSet.add(sp + p);
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
        this.paths = new PathSet(pathsSet);
        final Set<ChangeType> typesSet = new HashSet<ChangeType>();
        if (ref.getProperty(CHANGES) != null ) {
            for (String changeName : toStringArray(ref.getProperty(CHANGES))) {
                try {
                    typesSet.add(ChangeType.valueOf(changeName));
                } catch ( final IllegalArgumentException iae) {
                    configValid = false;
                }
            }
        } else {
            // default is added, changed, removed
            typesSet.add(ChangeType.ADDED);
            typesSet.add(ChangeType.CHANGED);
            typesSet.add(ChangeType.REMOVED);
        }
        final Set<ChangeType> rts = new HashSet<ChangeType>();
        if ( typesSet.contains(ChangeType.ADDED)) {
            rts.add(ChangeType.ADDED);
        }
        if ( typesSet.contains(ChangeType.CHANGED)) {
            rts.add(ChangeType.CHANGED);
        }
        if ( typesSet.contains(ChangeType.REMOVED)) {
            rts.add(ChangeType.REMOVED);
        }
        this.resourceChangeTypes = Collections.unmodifiableSet(rts);

        final Set<ChangeType> pts = new HashSet<ChangeType>();
        if ( typesSet.contains(ChangeType.PROVIDER_ADDED)) {
            pts.add(ChangeType.PROVIDER_ADDED);
        }
        if ( typesSet.contains(ChangeType.PROVIDER_REMOVED)) {
            pts.add(ChangeType.PROVIDER_REMOVED);
        }
        this.providerChangeTypes = Collections.unmodifiableSet(pts);

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

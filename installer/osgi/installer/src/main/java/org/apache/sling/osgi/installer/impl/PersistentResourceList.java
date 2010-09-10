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
package org.apache.sling.osgi.installer.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistent list of RegisteredResource, used by installer to
 * keep track of all registered resources
 */
public class PersistentResourceList {

    /** The logger */
    private final Logger logger =  LoggerFactory.getLogger(this.getClass());

    /**
     * Map of registered resource sets.
     * The key of the map is the entity id of the registered resource.
     * The value is a set containing all registered resources for the
     * same entity. Usually this is just one resource per entity.
     */
    private final Map<String, SortedSet<RegisteredResource>> data;
    private final File dataFile;

    @SuppressWarnings("unchecked")
    public PersistentResourceList(final File dataFile) {
        this.dataFile = dataFile;

        Map<String, SortedSet<RegisteredResource>> restoredData = null;
        if ( dataFile.exists() ) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new FileInputStream(dataFile));
                restoredData = (Map<String, SortedSet<RegisteredResource>>)ois.readObject();
                logger.debug("Restored rsource list: {}", restoredData);
            } catch (final Exception e) {
                logger.warn("Unable to restore data, starting with empty list (" + e.getMessage() + ")", e);
            } finally {
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (final IOException ignore) {
                        // ignore
                    }
                }
            }
        }
        data = restoredData != null ? restoredData : new HashMap<String, SortedSet<RegisteredResource>>();
    }

    /** This method is just for testing. */
    Map<String, SortedSet<RegisteredResource>>  getData() {
        return data;
    }

    public void save() {
        try {
            final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile));
            try {
                oos.writeObject(data);
                logger.debug("Persisted resource list.");
            } finally {
                oos.close();
            }
        } catch (final Exception e) {
            logger.warn("Unable to save persistent list: " + e.getMessage(), e);
        }
    }

    public Collection<String> getEntityIds() {
        return this.data.keySet();
    }

    public void addOrUpdate(final RegisteredResource r) {
        logger.debug("Adding: {}", r);
        SortedSet<RegisteredResource> t = this.data.get(r.getEntityId());
        if (t == null) {
            t = new TreeSet<RegisteredResource>();
            this.data.put(r.getEntityId(), t);
        }

        // If an object with same sort key is already present, replace with the
        // new one which might have different attributes
        boolean first = true;
        for(final RegisteredResource rr : t) {
            if ( rr.getURL().equals(r.getURL()) ) {
                logger.debug("Cleanup obsolete resource: {}", rr);
                rr.cleanup();
                t.remove(rr);
                if ( first && rr.equals(r) ) {
                    r.setState(rr.getState());
                }
                break;
            }
            first = false;
        }
        t.add(r);
    }

    public void remove(final String url) {
        for(final SortedSet<RegisteredResource> group : this.data.values()) {
            final Iterator<RegisteredResource> i = group.iterator();
            boolean first = true;
            while ( i.hasNext() ) {
                final RegisteredResource r = i.next();
                if ( r.getURL().equals(url) ) {
                    if ( first && (r.getState() == RegisteredResource.State.INSTALLED
                            || r.getState() == RegisteredResource.State.INSTALL)) {
                        logger.debug("Marking for uninstalling: {}", r);
                        r.setState(RegisteredResource.State.UNINSTALL);
                    } else {
                        logger.debug("Removing unused: {}", r);
                        i.remove();
                        r.cleanup();
                    }
                }
                first = false;
            }
        }
    }

    public void remove(final RegisteredResource r) {
        final SortedSet<RegisteredResource> group = this.data.get(r.getEntityId());
        if ( group != null ) {
            logger.debug("Removing unused: {}", r);
            group.remove(r);
            r.cleanup();
        }
    }

    public Collection<RegisteredResource> getResources(final String entityId) {
        return this.data.get(entityId);
    }

    public boolean compact() {
        boolean changed = false;
        final Iterator<Map.Entry<String, SortedSet<RegisteredResource>>> i = this.data.entrySet().iterator();
        while ( i.hasNext() ) {
            final Map.Entry<String, SortedSet<RegisteredResource>> entry = i.next();

            final List<RegisteredResource> toDelete = new ArrayList<RegisteredResource>();
            for(final RegisteredResource r : entry.getValue()) {
                if ( r.getState() == RegisteredResource.State.UNINSTALLED ) {
                    toDelete.add(r);
                }
            }
            for(final RegisteredResource r : toDelete) {
                changed = true;
                entry.getValue().remove(r);
                r.cleanup();
                logger.debug("Removing from list, uninstalled: {}", r);
            }

            if ( entry.getValue().isEmpty() ) {
                changed = true;
                i.remove();
            }
        }
        return changed;
    }

}

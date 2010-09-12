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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    private final Map<String, EntityResourceList> data;
    private final File dataFile;

    @SuppressWarnings("unchecked")
    public PersistentResourceList(final File dataFile) {
        this.dataFile = dataFile;

        Map<String, EntityResourceList> restoredData = null;
        if ( dataFile.exists() ) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new FileInputStream(dataFile));
                restoredData = (Map<String, EntityResourceList>)ois.readObject();
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
        data = restoredData != null ? restoredData : new HashMap<String, EntityResourceList>();
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
        EntityResourceList t = this.data.get(r.getEntityId());
        if (t == null) {
            t = new EntityResourceList();
            this.data.put(r.getEntityId(), t);
        }

        t.addOrUpdate(r);
    }

    public void remove(final String url) {
        for(final EntityResourceList group : this.data.values()) {
            group.remove(url);
        }
    }

    public void remove(final RegisteredResource r) {
        final EntityResourceList group = this.data.get(r.getEntityId());
        if ( group != null ) {
            group.remove(r);
        }
    }

    public EntityResourceList getEntityResourceList(final String entityId) {
        return this.data.get(entityId);
    }

    public boolean compact() {
        boolean changed = false;
        final Iterator<Map.Entry<String, EntityResourceList>> i = this.data.entrySet().iterator();
        while ( i.hasNext() ) {
            final Map.Entry<String, EntityResourceList> entry = i.next();

            changed |= entry.getValue().compact();
            if ( entry.getValue().isEmpty() ) {
                changed = true;
                i.remove();
            }
        }
        return changed;
    }

}

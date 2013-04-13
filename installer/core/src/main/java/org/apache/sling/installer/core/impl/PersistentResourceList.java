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
package org.apache.sling.installer.core.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.event.InstallationListener;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistent list of RegisteredResource, used by installer to
 * keep track of all registered resources
 */
public class PersistentResourceList {

    /** Serialization version. */
    private static final int VERSION = 2;

    /** Entity id for restart active bundles. */
    public static final String RESTART_ACTIVE_BUNDLES_TYPE = "org.apache.sling.installer.core.restart.bundles";
    public static final String RESTART_ACTIVE_BUNDLES_ID = "org.apache.sling.installer.core.restart.bundles";
    public static final String RESTART_ACTIVE_BUNDLES_ENTITY_ID = RESTART_ACTIVE_BUNDLES_TYPE + ':' + RESTART_ACTIVE_BUNDLES_ID;

    /** The logger */
    private final Logger logger =  LoggerFactory.getLogger(this.getClass());

    /**
     * Map of registered resource sets.
     * The key of the map is the entity id of the registered resource.
     * The value is a set containing all registered resources for the
     * same entity. Usually this is just one resource per entity.
     */
    private final Map<String, EntityResourceList> data;

    /** The persistence file. */
    private final File dataFile;

    /** All untransformed resources. */
    private final List<RegisteredResource> untransformedResources;

    private final InstallationListener listener;

    @SuppressWarnings("unchecked")
    public PersistentResourceList(final File dataFile, final InstallationListener listener) {
        this.dataFile = dataFile;
        this.listener = listener;

        Map<String, EntityResourceList> restoredData = null;
        List<RegisteredResource> unknownList = null;
        if ( dataFile.exists() ) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(dataFile)));
                final int version = ois.readInt();
                if ( version > 0 && version <= VERSION ) {
                    restoredData = (Map<String, EntityResourceList>)ois.readObject();
                    if ( version == VERSION ) {
                        unknownList = (List<RegisteredResource>)ois.readObject();
                    }
                } else {
                    logger.warn("Unknown version for persistent resource list: {}", version);
                }
                logger.debug("Restored resource list: {}", restoredData);
                logger.debug("Restored unknown resource list: {}", unknownList);
            } catch (final Exception e) {
                logger.warn("Unable to restore data, starting with empty list (" + e.getMessage() + ")", e);
                restoredData = null;
                unknownList = null;
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
        this.untransformedResources = unknownList != null ? unknownList : new ArrayList<RegisteredResource>();

        this.updateCache();

        // update resource ids
        for(final Map.Entry<String, EntityResourceList> entry : this.data.entrySet()) {
            final EntityResourceList erl = entry.getValue();
            erl.setResourceId(entry.getKey());
            erl.setListener(listener);
        }

        // check for special resources
        if ( this.getEntityResourceList(RESTART_ACTIVE_BUNDLES_ENTITY_ID) == null ) {
            final RegisteredResource rr = this.addOrUpdate(new InternalResource("$sling-installer$",
                            RESTART_ACTIVE_BUNDLES_ID,
                            null,
                            new Hashtable<String, Object>(),
                            InstallableResource.TYPE_PROPERTIES, "1",
                            null, null, null));
            final TransformationResult result = new TransformationResult();
            result.setId(RESTART_ACTIVE_BUNDLES_ID);
            result.setResourceType(RESTART_ACTIVE_BUNDLES_TYPE);
            this.transform(rr, new TransformationResult[] {result});
        }
    }

    /**
     * Update the url to digest cache
     */
    private void updateCache() {
        for(final EntityResourceList group : this.data.values()) {
            for(final RegisteredResource rr : group.getResources()) {
                if ( ((RegisteredResourceImpl)rr).hasDataFile() ) {
                    FileDataStore.SHARED.updateDigestCache(rr.getURL(), rr.getDigest());
                }
            }
        }
        for(final RegisteredResource rr : this.untransformedResources ) {
            if ( ((RegisteredResourceImpl)rr).hasDataFile() ) {
                FileDataStore.SHARED.updateDigestCache(rr.getURL(), rr.getDigest());
            }
        }
    }

    /**
     * Persist the current state
     */
    public void save() {
        try {
            final ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(dataFile)));
            try {
                oos.writeInt(VERSION);
                oos.writeObject(data);
                oos.writeObject(untransformedResources);
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

    /**
     * Add or update an installable resource.
     * @param input The installable resource
     */
    public RegisteredResource addOrUpdate(final InternalResource input) {
        // first check untransformed resource if there are resources with the same url and digest
        for(final RegisteredResource rr : this.untransformedResources ) {
            if ( rr.getURL().equals(input.getURL()) && ( rr.getDigest().equals(input.getDigest())) ) {
                // if we found the resource we can return after updating
                ((RegisteredResourceImpl)rr).update(input);
                return rr;
            }
        }
        // installed resources are next
        for(final EntityResourceList group : this.data.values()) {
            for(final RegisteredResource rr : group.getResources()) {
                if ( rr.getURL().equals(input.getURL()) && ( rr.getDigest().equals(input.getDigest()))) {
                    // if we found the resource we can return after updating
                    ((RegisteredResourceImpl)rr).update(input);
                    return rr;
                }
            }
        }
        try {
            final RegisteredResourceImpl registeredResource = RegisteredResourceImpl.create(input);
            this.checkInstallable(registeredResource);
            return registeredResource;
        } catch (final IOException ioe) {
            logger.warn("Ignoring resource. Error during processing of " + input.getURL(), ioe);
            return null;
        }
    }

    /**
     * Check if the provided installable resource is already installable (has a
     * known resource type)
     */
    private void checkInstallable(final RegisteredResourceImpl input) {
        if ( !InstallableResource.TYPE_FILE.equals(input.getType())
             && !InstallableResource.TYPE_PROPERTIES.equals(input.getType()) ) {

            EntityResourceList t = this.data.get(input.getEntityId());
            if (t == null) {
                t = new EntityResourceList(input.getEntityId(), this.listener);
                this.data.put(input.getEntityId(), t);
            }

            t.addOrUpdate(input);
        } else {
            // check if there is an old resource and remove it first
            if ( this.untransformedResources.contains(input) ) {
                this.untransformedResources.remove(input);
            }
            this.untransformedResources.add(input);
        }
    }

    /**
     * Get the list of untransformed resources = resources without resource type
     */
    public List<RegisteredResource> getUntransformedResources() {
        return this.untransformedResources;
    }

    /**
     * Remove a resource by url.
     * Check all resource groups and the list of untransformed resources.
     * @param url The url to remove
     */
    public void remove(final String url) {
        // iterate over all resource groups and remove resources
        // with the given url
        for(final EntityResourceList group : this.data.values()) {
            group.remove(url);
        }
        // iterate over untransformed resources and remove
        // the resource with that url
        final Iterator<RegisteredResource> i = this.untransformedResources.iterator();
        while ( i.hasNext() ) {
            final RegisteredResource rr = i.next();
            if ( rr.getURL().equals(url) ) {
                ((RegisteredResourceImpl)rr).cleanup();
                i.remove();
                break;
            }
        }
    }

    /**
     * Get the resource group for an entity id.
     */
    public EntityResourceList getEntityResourceList(final String entityId) {
        EntityResourceList erl = this.data.get(entityId);
        if ( erl == null ) {
            for(final EntityResourceList group : this.data.values()) {
                if ( entityId.equals(group.getFullAlias()) ) {
                    erl = group;
                    break;
                }
            }
        }
        return erl;
    }

    /**
     * Compact the internal state and remove empty groups.
     * @return <code>true</code> if another cycle should be started.
     */
    public boolean compact() {
        boolean startNewCycle = false;
        final Iterator<Map.Entry<String, EntityResourceList>> i = this.data.entrySet().iterator();
        while ( i.hasNext() ) {
            final Map.Entry<String, EntityResourceList> entry = i.next();

            startNewCycle |= entry.getValue().compact();
            if ( entry.getValue().isEmpty() ) {
                i.remove();
            }
        }
        return startNewCycle;
    }

    /**
     * Transform an unknown resource to a registered one
     */
    public void transform(final RegisteredResource resource,
                          final TransformationResult[] result) {
        // remove resource from unknown list
        this.untransformedResources.remove(resource);
        try {
            for(int i=0; i<result.length; i++) {
                // check the result
                final TransformationResult tr = result[i];
                if ( tr == null ) {
                    logger.warn("Ignoring null result for {}", resource);
                    continue;
                }
                if ( tr.getResourceType() != null && tr.getId() == null) {
                    logger.error("Result for {} contains new resource type {} but no unique id!",
                            resource, tr.getResourceType());
                    continue;
                }
                final RegisteredResourceImpl clone =  (RegisteredResourceImpl)((RegisteredResourceImpl)resource).clone(result[i]);
                this.checkInstallable(clone);
            }
        } catch (final IOException ioe) {
            logger.warn("Ignoring resource. Error during processing of " + resource, ioe);
        }
    }

    /**
     * Check if the id is a special id and should not be included in the info report
     */
    public boolean isSpecialEntityId(final String id) {
        return RESTART_ACTIVE_BUNDLES_ENTITY_ID.equals(id);
    }
}

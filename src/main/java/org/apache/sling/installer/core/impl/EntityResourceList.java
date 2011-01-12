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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistent list of RegisteredResource, used by installer to
 * keep track of all registered resources
 */
public class EntityResourceList implements Serializable, TaskResourceGroup {

    /** Use own serial version ID as we control serialization. */
    private static final long serialVersionUID = 6L;

    /** Serialization version. */
    private static final int VERSION = 1;

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityResourceList.class);

    /** The set of registered resources for this entity. */
    private final SortedSet<TaskResource> resources = new TreeSet<TaskResource>();

    /**
     * Serialize the object
     * - write version id
     * - serialize each entry in the resources list
     * @param out Object output stream
     * @throws IOException
     */
    private void writeObject(final java.io.ObjectOutputStream out)
    throws IOException {
        out.writeInt(VERSION);
        out.writeInt(resources.size());
        for(final RegisteredResource rr : this.resources) {
            out.writeObject(rr);
        }
    }

    /**
     * Deserialize the object
     * - read version id
     * - deserialize each entry in the resources list
     */
    private void readObject(final java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        final int version = in.readInt();
        if ( version != VERSION ) {
            throw new ClassNotFoundException(this.getClass().getName());
        }
        Util.setField(this, "resources", new TreeSet<RegisteredResource>());
        final int size = in.readInt();
        for(int i=0; i < size; i++) {
            final TaskResource rr = (TaskResource)in.readObject();
            this.resources.add(rr);
        }
    }

    /** The resource list is empty if it contains no resources. */
    public boolean isEmpty() {
        return resources.isEmpty();
    }

    /**
     * Return the first resource if it either needs to be installed or uninstalled.
     */
    public TaskResource getActiveResource() {
        if ( !resources.isEmpty() ) {
            final TaskResource r = resources.first();
            if ( r.getState() == ResourceState.INSTALL
              || r.getState() == ResourceState.UNINSTALL ) {
                return r;
            }
        }
        return null;
    }

    /**
     * Set the finish state for the resource.
     * If this resource has been uninstalled, check the next in the list if it needs to
     * be reactivated.
     */
    public void setFinishState(ResourceState state) {
        final TaskResource toActivate = getActiveResource();
        if ( toActivate != null ) {
            if ( toActivate.getState() == ResourceState.UNINSTALL
                 && this.resources.size() > 1 ) {

                // to get the second item in the set we have to use an iterator!
                final Iterator<TaskResource> i = this.resources.iterator();
                i.next(); // skip first
                final TaskResource second = i.next();
                if ( state == ResourceState.UNINSTALLED ) {
                    // first resource got uninstalled, go back to second
                    if (second.getState() == ResourceState.IGNORED || second.getState() == ResourceState.INSTALLED) {
                        LOGGER.debug("Reactivating for next cycle: {}", second);
                        second.setState(ResourceState.INSTALL);
                    }
                } else {
                    // don't install as the first did not get uninstalled
                    if ( second.getState() == ResourceState.INSTALL ) {
                        second.setState(ResourceState.IGNORED);
                    }
                    // and now set resource to uninstalled
                    state = ResourceState.UNINSTALLED;
                }
            }
            toActivate.setState(state);
            if ( state == ResourceState.UNINSTALLED ) {
                this.cleanup(toActivate);
            }
        }
    }

    private void cleanup(final RegisteredResource rr) {
        if ( rr instanceof RegisteredResourceImpl ) {
            ((RegisteredResourceImpl)rr).cleanup();
        }
    }

    public Collection<TaskResource> getResources() {
        return resources;
    }

    public void addOrUpdate(final TaskResource r) {
        LOGGER.debug("Adding new resource: {}", r);
        // If an object with same url is already present, replace with the
        // new one which might have different attributes
        boolean first = true;
        for(final TaskResource rr : resources) {
            if ( rr.getURL().equals(r.getURL()) ) {
                LOGGER.debug("Cleanup obsolete resource: {}", rr);
                this.cleanup(rr);
                resources.remove(rr);
                if ( first && rr.equals(r) ) {
                    r.setState(rr.getState());
                }
                break;
            }
            first = false;
        }
        resources.add(r);
    }

    public void remove(final String url) {
        final Iterator<TaskResource> i = resources.iterator();
        boolean first = true;
        while ( i.hasNext() ) {
            final TaskResource r = i.next();
            if ( r.getURL().equals(url) ) {
                if ( first && (r.getState() == ResourceState.INSTALLED
                        || r.getState() == ResourceState.INSTALL)) {
                    LOGGER.debug("Marking for uninstalling: {}", r);
                    r.setState(ResourceState.UNINSTALL);
                } else {
                    LOGGER.debug("Removing unused: {}", r);
                    i.remove();
                    this.cleanup(r);
                }
            }
            first = false;
        }
    }

    public void remove(final TaskResource r) {
        if ( resources.remove(r) ) {
            LOGGER.debug("Removing unused: {}", r);
            this.cleanup(r);
        }
    }

    /**
     * Compact the resource group by removing uninstalled entries
     */
    public boolean compact() {
        boolean changed = false;
        final List<TaskResource> toDelete = new ArrayList<TaskResource>();
        for(final TaskResource r : resources) {
            if ( r.getState() == ResourceState.UNINSTALLED ) {
                toDelete.add(r);
            }
        }
        for(final RegisteredResource r : toDelete) {
            changed = true;
            resources.remove(r);
            this.cleanup(r);
            LOGGER.debug("Removing uninstalled from list: {}", r);
        }
        return changed;
    }
}

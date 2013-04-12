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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.sling.installer.api.event.InstallationEvent;
import org.apache.sling.installer.api.event.InstallationListener;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
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
    private static final int VERSION = 2;

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityResourceList.class);

    /** The list of registered resources for this entity. */
    private final List<RegisteredResourceImpl> resources = new ArrayList<RegisteredResourceImpl>();

    /** Alias for this id. */
    private String alias;

    /** The resource id of this group. */
    private String resourceId;

    /** The listener. */
    private transient InstallationListener listener;

    public EntityResourceList(final String resourceId, final InstallationListener listener) {
        this.resourceId = resourceId;
        this.listener = listener;
    }
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
        out.writeObject(this.alias);
        out.writeObject(this.resourceId);
    }

    /**
     * Deserialize the object
     * - read version id
     * - deserialize each entry in the resources list
     */
    private void readObject(final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        final int version = in.readInt();
        if ( version < 1 || version > VERSION ) {
            throw new ClassNotFoundException(this.getClass().getName());
        }
        Util.setField(this, "resources", new ArrayList<RegisteredResourceImpl>());
        final int size = in.readInt();
        for(int i=0; i < size; i++) {
            final RegisteredResourceImpl rr = (RegisteredResourceImpl)in.readObject();
            this.resources.add(rr);
        }
        if ( version > 1 ) {
            this.alias = (String)in.readObject();
            this.resourceId = (String)in.readObject();
        }
    }

    /**
     * The resource list is empty if it contains no resources.
     */
    public boolean isEmpty() {
        return resources.isEmpty();
    }

    /**
     * @see org.apache.sling.installer.api.tasks.TaskResourceGroup#getActiveResource()
     */
    public TaskResource getActiveResource() {
        if ( !resources.isEmpty() ) {
            Collections.sort(this.resources);
            final TaskResource r = resources.get(0);
            if ( r.getState() == ResourceState.INSTALL
                    || r.getState() == ResourceState.UNINSTALL ) {
                return r;
            }
        }
        return null;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.TaskResourceGroup#getNextActiveResource()
     */
    public TaskResource getNextActiveResource() {
        if ( this.getActiveResource() != null ) {
            if ( this.resources.size() > 1 ) {
                Collections.sort(this.resources);
                // to get the second item in the set we have to use an iterator!
                final Iterator<RegisteredResourceImpl> i = this.resources.iterator();
                i.next(); // skip first
                return i.next();
            }
        }
        return null;
    }
    /**
     * Return the first resource or null
     */
    public TaskResource getFirstResource() {
        if ( !resources.isEmpty() ) {
            Collections.sort(this.resources);
            return resources.get(0);
        }
        return null;
    }

    /**
     * Get the alias for this group or null
     */
    public String getAlias() {
        return this.alias;
    }

    /**
     * Get the alias for this group or null
     */
    public String getFullAlias() {
        if ( this.alias != null ) {
            final int pos = this.resourceId.indexOf(':');
            return this.resourceId.substring(0, pos + 1) + this.alias;
        }
        return null;
    }

    /**
     * Get the resource id.
     */
    public String getResourceId() {
        return this.resourceId;
    }

    /**
     * Set the resource id
     */
    public void setResourceId(final String id) {
        this.resourceId = id;
    }

    /**
     * Set the listener
     */
    public void setListener(final InstallationListener listener) {
        this.listener = listener;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.TaskResourceGroup#setFinishState(org.apache.sling.installer.api.tasks.ResourceState)
     */
    public void setFinishState(ResourceState state) {
        final TaskResource toActivate = getActiveResource();
        if ( toActivate != null ) {
            if ( toActivate.getState() == ResourceState.UNINSTALL
                    && this.resources.size() > 1 ) {

                final TaskResource second = this.getNextActiveResource();
                if ( state == ResourceState.UNINSTALLED ) {
                    // first resource got uninstalled, go back to second
                    if (second.getState() == ResourceState.IGNORED || second.getState() == ResourceState.INSTALLED) {
                        LOGGER.debug("Reactivating for next cycle: {}", second);
                        ((RegisteredResourceImpl)second).setState(ResourceState.INSTALL);
                    }
                } else {
                    // don't install as the first did not get uninstalled
                    if ( second.getState() == ResourceState.INSTALL ) {
                        ((RegisteredResourceImpl)second).setState(ResourceState.IGNORED);
                    }
                    // and now set resource to uninstalled
                    state = ResourceState.UNINSTALLED;
                }
            } else if ( state == ResourceState.INSTALLED ) {
                // make sure that no other resource has state INSTALLED
                if ( this.resources.size() > 1 ) {
                    // to get the second item in the set we have to use an iterator!
                    final Iterator<RegisteredResourceImpl> i = this.resources.iterator();
                    i.next(); // skip first
                    while ( i.hasNext() ) {
                        final TaskResource rsrc = i.next();
                        if ( rsrc.getState() == ResourceState.INSTALLED ) {
                            ((RegisteredResourceImpl)rsrc).setState(ResourceState.INSTALL);
                        }
                    }
                }

            }
            ((RegisteredResourceImpl)toActivate).setState(state);

            if ( state != ResourceState.INSTALLED ) {
                // make sure to remove all install info attributes if the resource is not
                // installed anymore
                toActivate.setAttribute(TaskResource.ATTR_INSTALL_EXCLUDED, null);
                toActivate.setAttribute(TaskResource.ATTR_INSTALL_INFO, null);
            }
            // remove install info attributes on all other resources in the group
            final Iterator<RegisteredResourceImpl> tri = this.resources.iterator();
            tri.next(); // skip first
            while ( tri.hasNext() ) {
                final TaskResource rsrc = tri.next();
                rsrc.setAttribute(TaskResource.ATTR_INSTALL_EXCLUDED, null);
                rsrc.setAttribute(TaskResource.ATTR_INSTALL_INFO, null);
            }

            this.listener.onEvent(new InstallationEvent() {

                public TYPE getType() {
                    return TYPE.PROCESSED;
                }

                public Object getSource() {
                    return toActivate;
                }
            });
            if ( state == ResourceState.UNINSTALLED ) {
                this.cleanup(toActivate);
            }
        }
    }

    /**
     * @see org.apache.sling.installer.api.tasks.TaskResourceGroup#setFinishState(org.apache.sling.installer.api.tasks.ResourceState, java.lang.String)
     */
    public void setFinishState(final ResourceState state, final String alias) {
        this.alias = alias;
        this.setFinishState(state);
    }

    private void cleanup(final RegisteredResource rr) {
        if ( rr instanceof RegisteredResourceImpl ) {
            ((RegisteredResourceImpl)rr).cleanup();
        }
    }

    public Collection<RegisteredResourceImpl> getResources() {
        Collections.sort(this.resources);
        return resources;
    }

    public void addOrUpdate(final RegisteredResourceImpl r) {
        LOGGER.debug("Adding new resource: {}", r);
        Collections.sort(this.resources);
        // If an object with same url is already present, replace with the
        // new one which might have different attributes
        boolean first = true;
        boolean add = true;
        final Iterator<RegisteredResourceImpl> taskIter = this.resources.iterator();
        while ( taskIter.hasNext() ) {
            final TaskResource rr = taskIter.next();
            if ( rr.getURL().equals(r.getURL()) ) {
                if ( RegisteredResourceImpl.isSameResource((RegisteredResourceImpl)rr, r) ) {
                    if ( !rr.getDigest().equals(r.getDigest()) ) {
                        // same resource but different digest, we need to remove the file
                        LOGGER.debug("Cleanup duplicate resource: {}", r);
                        this.cleanup(r);
                    }
                    // same resource, just ignore the new one
                    add = false;
                } else {
                    if ( first && rr.getState() == ResourceState.INSTALLED) {
                        // it's not the same, but the first one is installed, so uninstall
                        ((RegisteredResourceImpl)rr).setState(ResourceState.UNINSTALL);
                    } else {
                        LOGGER.debug("Cleanup obsolete resource: {}", rr);
                        taskIter.remove();
                        this.cleanup(rr);
                    }
                }
                break;
            }
            first = false;
        }
        if ( add ) {
            resources.add(r);
        }
    }

    public void remove(final String url) {
        Collections.sort(this.resources);
        final Iterator<RegisteredResourceImpl> i = resources.iterator();
        boolean first = true;
        while ( i.hasNext() ) {
            final TaskResource r = i.next();
            if ( r.getURL().equals(url) ) {
                if ( first && (r.getState() == ResourceState.INSTALLED
                        || r.getState() == ResourceState.INSTALL)) {
                    LOGGER.debug("Marking for uninstalling: {}", r);
                    ((RegisteredResourceImpl)r).setState(ResourceState.UNINSTALL);
                } else {
                    LOGGER.debug("Removing unused: {}", r);
                    i.remove();
                    this.cleanup(r);
                }
            }
            first = false;
        }
    }

    /**
     * Compact the resource group by removing uninstalled entries
     * @return <code>true</code> if another cycle should be started.
     */
    public boolean compact() {
        Collections.sort(this.resources);
        boolean startNewCycle = false;
        final List<TaskResource> toDelete = new ArrayList<TaskResource>();
        boolean first = true;
        for(final TaskResource r : resources) {
            if ( r.getState() == ResourceState.UNINSTALLED || (!first && r.getState() == ResourceState.UNINSTALL) ) {
                toDelete.add(r);
            }
            first = false;
        }

        if (!toDelete.isEmpty()) {
            // Avoid resources.remove(r) as the resource might have
            // changed since it was added, which causes it to compare()
            // differently and trip the TreeSet.remove() search.
            final Set<RegisteredResourceImpl> copy = new HashSet<RegisteredResourceImpl>(resources);
            for(final RegisteredResource r : toDelete) {
                copy.remove(r);
                this.cleanup(r);
                LOGGER.debug("Removing uninstalled from list: {}", r);
            }
            resources.clear();
            resources.addAll(copy);
            if ( !this.isEmpty() ) {
                startNewCycle = true;
            }
        }

        return startNewCycle;
    }
}

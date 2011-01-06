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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistent list of RegisteredResource, used by installer to
 * keep track of all registered resources
 */
public class EntityResourceList implements Serializable, RegisteredResourceGroup {

    /** Use own serial version ID as we control serialization. */
    private static final long serialVersionUID = 6L;

    /** Serialization version. */
    private static final int VERSION = 1;

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityResourceList.class);

    /** The set of registered resources for this entity. */
    private final SortedSet<RegisteredResource> resources = new TreeSet<RegisteredResource>();

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
            final RegisteredResource rr = (RegisteredResource)in.readObject();
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
    public RegisteredResource getActiveResource() {
        if ( !resources.isEmpty() ) {
            final RegisteredResource r = resources.first();
            if ( r.getState() == RegisteredResource.State.INSTALL
              || r.getState() == RegisteredResource.State.UNINSTALL ) {
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
    public void setFinishState(RegisteredResource.State state) {
        final RegisteredResource toActivate = getActiveResource();
        if ( toActivate != null ) {
            if ( toActivate.getState() == RegisteredResource.State.UNINSTALL
                 && this.resources.size() > 1 ) {

                // to get the second item in the set we have to use an iterator!
                final Iterator<RegisteredResource> i = this.resources.iterator();
                i.next(); // skip first
                final RegisteredResource second = i.next();
                if ( state == RegisteredResource.State.UNINSTALLED ) {
                    // first resource got uninstalled, go back to second
                    if (second.getState() == RegisteredResource.State.IGNORED || second.getState() == RegisteredResource.State.INSTALLED) {
                        LOGGER.debug("Reactivating for next cycle: {}", second);
                        second.setState(RegisteredResource.State.INSTALL);
                    }
                } else {
                    // don't install as the first did not get uninstalled
                    if ( second.getState() == RegisteredResource.State.INSTALL ) {
                        second.setState(RegisteredResource.State.IGNORED);
                    }
                    // and now set resource to uninstalled
                    state = RegisteredResource.State.UNINSTALLED;
                }
            }
            toActivate.setState(state);
        }
    }

    private void cleanup(final RegisteredResource rr) {
        if ( rr instanceof RegisteredResourceImpl ) {
            ((RegisteredResourceImpl)rr).cleanup();
        }
    }

    public Collection<RegisteredResource> getResources() {
        return resources;
    }

    public void addOrUpdate(final RegisteredResource r) {
        LOGGER.debug("Adding new resource: {}", r);
        // If an object with same url is already present, replace with the
        // new one which might have different attributes
        boolean first = true;
        for(final RegisteredResource rr : resources) {
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
        final Iterator<RegisteredResource> i = resources.iterator();
        boolean first = true;
        while ( i.hasNext() ) {
            final RegisteredResource r = i.next();
            if ( r.getURL().equals(url) ) {
                if ( first && (r.getState() == RegisteredResource.State.INSTALLED
                        || r.getState() == RegisteredResource.State.INSTALL)) {
                    LOGGER.debug("Marking for uninstalling: {}", r);
                    r.setState(RegisteredResource.State.UNINSTALL);
                } else {
                    LOGGER.debug("Removing unused: {}", r);
                    i.remove();
                    this.cleanup(r);
                }
            }
            first = false;
        }
    }

    public void remove(final RegisteredResource r) {
        if ( resources.remove(r) ) {
            LOGGER.debug("Removing unused: {}", r);
            this.cleanup(r);
        }
    }

    public boolean compact() {
        boolean changed = false;
        final List<RegisteredResource> toDelete = new ArrayList<RegisteredResource>();
        for(final RegisteredResource r : resources) {
            if ( r.getState() == RegisteredResource.State.UNINSTALLED ) {
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

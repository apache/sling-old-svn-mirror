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
package org.apache.sling.jcr.ocm.impl;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.jackrabbit.ocm.manager.cache.impl.RequestObjectCacheImpl;

/**
 * The <code>ObservingObjectCache</code> TODO
 */
public class ObservingObjectCache extends RequestObjectCacheImpl implements
        EventListener {

    private boolean unregistered;

    ObservingObjectCache(Session session) {
        try {
            session.getWorkspace().getObservationManager().addEventListener(
                this,
                Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED
                    | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED, "/",
                true, null, null, false);
            this.unregistered = false;
        } catch (RepositoryException re) {
            // TODO: log
            this.unregistered = true;
        }
    }

    public void onEvent(EventIterator eventiterator) {
        super.clear();
    }

    public void clear() {
        if (this.unregistered) {
            super.clear();
        }
    }
}

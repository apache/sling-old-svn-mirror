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
package org.apache.sling.jcr.webdav.impl.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * IOManager service that uses a ServiceTracker to find available IOHandlers.
 */
public class SlingHandlerManager<ManagedType> {

    private final TreeMap<ServiceReference, ManagedType> handlerServices = new TreeMap<ServiceReference, ManagedType>();

    private ComponentContext componentContext;

    private final String referenceName;

    private ManagedType[] handlers;

    protected SlingHandlerManager(final String referenceName) {
        this.referenceName = referenceName;
    }

    public void setComponentContext(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    @SuppressWarnings("unchecked")
    protected ManagedType[] getHandlers(ManagedType[] type) {
        if (this.handlers == null) {

            final Set<Entry<ServiceReference, ManagedType>> entries;
            synchronized (this.handlerServices) {
                entries = this.handlerServices.entrySet();
            }

            final ArrayList<ManagedType> ioHandlers = new ArrayList<ManagedType>(
                entries.size());
            final Map<ServiceReference, ManagedType> updates = new HashMap<ServiceReference, ManagedType>();
            for (Entry<ServiceReference, ManagedType> entry : entries) {
                final ManagedType ioHandler;
                if (entry.getValue() == null) {
                    final ServiceReference key = entry.getKey();
                    // unckecked cast
                    ioHandler = (ManagedType) this.componentContext.locateService(
                        referenceName, key);
                    // since we're inside the entries iterator, we can't update the map
                    // defer updating the map until this loop is finished
                    if (ioHandler != null) {
                        updates.put(key, ioHandler);
                    }
                } else {
                    ioHandler = entry.getValue();
                }
                ioHandlers.add(ioHandler);
            }

            if (!updates.isEmpty()) {
                synchronized (this.handlerServices) {
                    this.handlerServices.putAll(updates);
                }
            }

            // unckecked cast
            this.handlers = ioHandlers.toArray(type);
        }
        return this.handlers;
    }

    protected void bindHandler(final ServiceReference ref) {
        synchronized (this.handlerServices) {
            this.handlerServices.put(ref, null);
            this.handlers = null;
        }
    }

    protected void unbindHandler(final ServiceReference ref) {
        synchronized (this.handlerServices) {
            this.handlerServices.remove(ref);
            this.handlers = null;
        }
    }
}

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
package org.apache.sling.tenant.internal;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.tenant.TenantConstants;

/**
 * <p>
 * The {@code EventTypes} interface provides some symbolic constants
 * for well known constant strings in Sling Tenant bundle.
 * </p>
 *
 * @since 1.1
 */
enum EventTypes {

    /**
     * <p>The topic for the OSGi event which is sent when a tenant has been created.</p>
     * <p>The event contains at least the {@link #PROPERTY_TENANTID}.</p>
     *
     * @since 1.1
     */
    CREATED {
        @Override
        public String getTopic() {
            return TenantConstants.TOPIC_TENANT_CREATED;
        }
        @Override
        public void setProps(Dictionary<String, Object> props, Map<String, Object> oldProps, Map<String, Object> newProps) {
            props.put(TenantConstants.PROPERTIES_ADDED, Collections.unmodifiableMap(newProps));
        }
    },


    /**
     * <p>The topic for the OSGi event which is sent when a tenant has been removed.</p>
     * <p>The event contains at least the {@link #PROPERTY_TENANTID}.</p>
     *
     * @since 1.1
     */
    REMOVED {
        @Override
        public String getTopic() {
            return TenantConstants.TOPIC_TENANT_REMOVED;
        }
        @Override
        public void setProps(Dictionary<String, Object> props, Map<String, Object> oldProps, Map<String, Object> newProps) {
            props.put(TenantConstants.PROPERTIES_REMOVED, Collections.unmodifiableMap(oldProps));
        }
    },

    /**
     * <p>The topic for the OSGi event which is sent when a tenant has been updated.</p>
     * <p>The event contains at least the {@link #PROPERTY_TENANTID}.</p>
     *
     * @since 1.1
     */
    UPDATED {
        @Override
        public String getTopic() {
            return TenantConstants.TOPIC_TENANT_UPDATED;
        }
        @Override
        public void setProps(Dictionary<String, Object> props, Map<String, Object> oldProps, Map<String, Object> newProps) {
            Map<String, Object> added = new HashMap<String, Object>();
            Map<String, Object> updated = new HashMap<String, Object>();
            Map<String, Object> removed = new HashMap<String, Object>();

            // handle new (only in newProps) and updated (in both new and old) entries
            for (String key : newProps.keySet()) {
                if (oldProps.containsKey(key)) {
                    updated.put(key, newProps.get(key));
                } else {
                    added.put(key, newProps.get(key));
                }
            }

            // handle removed (only in oldProps) entries
            for (String key : oldProps.keySet()) {
                if (!newProps.containsKey(key)) {
                    removed.put(key, newProps.get(key));
                }
            }

            props.put(TenantConstants.PROPERTIES_ADDED, added);
            props.put(TenantConstants.PROPERTIES_UPDATED, updated);
            props.put(TenantConstants.PROPERTIES_REMOVED, removed);
        }
    };

    public abstract String getTopic();
    public abstract void setProps(Dictionary<String, Object> props, Map<String, Object> oldProps, Map<String, Object> newProps);
}
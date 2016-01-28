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
package org.apache.sling.jcr.resource.internal;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.sling.api.resource.observation.ResourceChange;

public class JcrResourceChange extends ResourceChange {

    private final String userId;

    private JcrResourceChange(Builder builder) {
        super(builder.changeType, builder.path, builder.isExternal, builder.addedAttributeNames, builder.changedAttributeNames, builder.removedAttributeNames);
        this.userId = builder.userId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("ResourceChange[type=").append(this.getType()).append(", path=").append(this.getPath());
        if (getAddedPropertyNames() != null && !getAddedPropertyNames().isEmpty()) {
            b.append(", added=").append(getAddedPropertyNames());
        }
        if (getChangedPropertyNames() != null && !getChangedPropertyNames().isEmpty()) {
            b.append(", changed=").append(getChangedPropertyNames());
        }
        if (getRemovedPropertyNames() != null && !getRemovedPropertyNames().isEmpty()) {
            b.append(", removed=").append(getRemovedPropertyNames());
        }
        b.append("]");
        return b.toString();
    }

    public static class Builder {

        private String path;

        private ChangeType changeType;

        private boolean isExternal;

        private String userId;

        private Set<String> changedAttributeNames;

        private Set<String> addedAttributeNames;

        private Set<String> removedAttributeNames;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public ChangeType getChangeType() {
            return changeType;
        }

        public void setChangeType(ChangeType changeType) {
            this.changeType = changeType;
        }

        public boolean isExternal() {
            return isExternal;
        }

        public void setExternal(boolean isExternal) {
            this.isExternal = isExternal;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public Set<String> getChangedAttributeNames() {
            return changedAttributeNames;
        }

        public void addChangedAttributeName(String propName) {
            if (changedAttributeNames == null) {
                changedAttributeNames = new LinkedHashSet<String>();
            }
            if (!changedAttributeNames.contains(propName)) {
                changedAttributeNames.add(propName);
            }
        }

        public Set<String> getAddedAttributeNames() {
            return addedAttributeNames;
        }

        public void addAddedAttributeName(String propName) {
            if (addedAttributeNames == null) {
                addedAttributeNames = new LinkedHashSet<String>();
            }
            if (!addedAttributeNames.contains(propName)) {
                addedAttributeNames.add(propName);
            }
        }

        public Set<String> getRemovedAttributeNames() {
            return removedAttributeNames;
        }

        public void addRemovedAttributeName(String propName) {
            if (removedAttributeNames == null) {
                removedAttributeNames = new LinkedHashSet<String>();
            }
            if (!removedAttributeNames.contains(propName)) {
                removedAttributeNames.add(propName);
            }
        }

        public ResourceChange build() {
            return new JcrResourceChange(this);
        }
    }

}
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
package org.apache.sling.installer.api.tasks;

import java.util.Map;


/**
 * Simple general task, setting the state of a registered resource.
 * @since 1.2
 */
public class ChangeStateTask extends InstallTask {

    private static final String ORDER = "00-";

    private final ResourceState state;

    private final String[] removeAttributes;

    private final Map<String, Object> addAttributes;

    /**
     * Change the state of the task
     * @param r The resource group to change.
     * @param s The new state.,
     */
    public ChangeStateTask(final TaskResourceGroup r,
                           final ResourceState s) {
        this(r, s, null, null);
    }

    /**
     * Change the state of the task
     * @param r The resource group to change.
     * @param s The new state.,
     * @param addAttributes    An optional map of attributes to set before the state is changed.
     * @param removeAttributes A optional list of attributes to remove before the state is changed.
     * @since 1.3
     */
    public ChangeStateTask(final TaskResourceGroup r,
                           final ResourceState s,
                           final Map<String, Object> addAttributes,
                           final String[] removeAttributes) {
        super(r);
        this.state = s;
        this.addAttributes = addAttributes;
        this.removeAttributes = removeAttributes;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.InstallTask#execute(org.apache.sling.installer.api.tasks.InstallationContext)
     */
    public void execute(final InstallationContext ctx) {
        final TaskResource resource = this.getResource();
        if ( resource != null ) {
            if ( this.removeAttributes != null ) {
                for(final String name : this.removeAttributes ) {
                    resource.setAttribute(name, null);
                }
            }
            if ( this.addAttributes != null ) {
                for(final Map.Entry<String, Object> entry : this.addAttributes.entrySet()) {
                    resource.setAttribute(entry.getKey(), entry.getValue());
                }
            }
        }
        this.setFinishedState(this.state);
    }

    @Override
    public String getSortKey() {
        return ORDER + getResource().getEntityId();
    }
}

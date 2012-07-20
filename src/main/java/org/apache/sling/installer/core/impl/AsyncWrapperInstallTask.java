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

import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;

/**
 * Wrapper class to add the async flag.
 * This is needed for pre 1.3 api usage
 */
public class AsyncWrapperInstallTask extends InstallTask {

    private final InstallTask delegatee;

    public AsyncWrapperInstallTask(final InstallTask delegatee) {
        super(delegatee.getResourceGroup());
        this.delegatee = delegatee;
    }

    @Override
    public void execute(final InstallationContext ctx) {
        this.delegatee.execute(ctx);
    }

    @Override
    public String getSortKey() {
        return this.delegatee.getSortKey();
    }

    @Override
    public TaskResource getResource() {
        return this.delegatee.getResource();
    }

    @Override
    public TaskResourceGroup getResourceGroup() {
        return this.delegatee.getResourceGroup();
    }

    @Override
    public void setFinishedState(final ResourceState state) {
        this.delegatee.setFinishedState(state);
    }

    @Override
    public void setFinishedState(final ResourceState state, final String alias) {
        this.delegatee.setFinishedState(state, alias);
    }

    @Override
    public String toString() {
        return this.delegatee.toString();
    }

    @Override
    public boolean isAsynchronousTask() {
        return true;
    }
}

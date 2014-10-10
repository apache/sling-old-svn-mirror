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
package org.apache.sling.installer.factories.subsystems.impl;

import org.apache.sling.installer.api.tasks.ChangeStateTask;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.osgi.service.subsystem.Subsystem;

/**
 * This is the subsystem start task.
 */
public class StartSubsystemTask extends InstallTask {

    private static final String INSTALL_ORDER = "55-";

    private final Subsystem subsystem;

    public StartSubsystemTask(final TaskResourceGroup grp, final Subsystem system) {
        super(grp);
        this.subsystem = system;
    }

    @Override
    public void execute(final InstallationContext ctx) {
        final TaskResource tr = this.getResource();
        ctx.log("Starting subsystem from {}", tr);

        this.subsystem.start();
        ctx.addTaskToCurrentCycle(new ChangeStateTask(this.getResourceGroup(), ResourceState.INSTALLED));
        ctx.log("Started subsystem {}", this.subsystem);
    }

    @Override
    public String getSortKey() {
        return INSTALL_ORDER + getResource().getURL();
    }
}

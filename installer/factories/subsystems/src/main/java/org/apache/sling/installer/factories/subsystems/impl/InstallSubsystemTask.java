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

import java.io.IOException;

import org.apache.sling.installer.api.tasks.ChangeStateTask;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.osgi.service.subsystem.Subsystem;

/**
 * This is the subsystem install task.
 */
public class InstallSubsystemTask extends InstallTask {

    private static final String INSTALL_ORDER = "53-";

    private final Subsystem rootSubsystem;

    public InstallSubsystemTask(final TaskResourceGroup grp, final Subsystem rootSubsystem) {
        super(grp);
        this.rootSubsystem = rootSubsystem;
    }

    @Override
    public void execute(final InstallationContext ctx) {
        final TaskResource tr = this.getResource();
        ctx.log("Installing new subsystem from {}", tr);

        try {
            final Subsystem sub = this.rootSubsystem.install(tr.getURL(), tr.getInputStream());
            ctx.addTaskToCurrentCycle(new StartSubsystemTask(this.getResourceGroup(), sub));
            ctx.log("Installed new subsystem {}", sub);
        } catch (final IOException e) {
            ctx.log("Unable to install subsystem {} : {}", tr, e);
            ctx.addTaskToCurrentCycle(new ChangeStateTask(this.getResourceGroup(), ResourceState.IGNORED));
        }
    }

    @Override
    public String getSortKey() {
        return INSTALL_ORDER + getResource().getURL();
    }
}

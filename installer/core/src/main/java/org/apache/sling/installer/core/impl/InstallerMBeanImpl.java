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

import org.apache.sling.installer.api.event.InstallationEvent;
import org.apache.sling.installer.api.event.InstallationListener;
import org.apache.sling.installer.api.info.InfoProvider;
import org.apache.sling.installer.api.jmx.InstallerMBean;

public class InstallerMBeanImpl implements InstallationListener, InstallerMBean {
    private final InfoProvider infoProvider;
    private volatile boolean active;
    private volatile long lastEventTime;

    public InstallerMBeanImpl(InfoProvider infoProvider) {
        this.infoProvider = infoProvider;
    }

    //~---------------------------------------< InstallationListener >

    public void onEvent(InstallationEvent event) {
        switch (event.getType()) {
            case STARTED:
                active = true;
                break;
            case SUSPENDED:
                active = false;
                break;
        }
        lastEventTime = System.currentTimeMillis();
    }

    //~----------------------------------------< InstallerMBean >

    public int getActiveResourceCount() {
        return infoProvider.getInstallationState().getActiveResources().size();
    }

    public int getInstalledResourceCount() {
        return infoProvider.getInstallationState().getInstalledResources().size();
    }

    public boolean isActive() {
        return active;
    }

    public long getSuspendedSince() {
        return active ? -1 : lastEventTime;
    }
}

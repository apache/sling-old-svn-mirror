/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.installer.impl;

import org.apache.sling.installer.api.event.InstallationEvent;
import org.apache.sling.installer.api.event.InstallationListener;
import org.apache.sling.launchpad.api.StartupHandler;

public class LaunchpadListener implements InstallationListener {

    private final StartupHandler startupHandler;

    private volatile boolean started = false;

    public LaunchpadListener(final StartupHandler startupHandler) {
        this.startupHandler = startupHandler;
    }

    /**
     * @see org.apache.sling.installer.api.event.InstallationListener#onEvent(org.apache.sling.installer.api.event.InstallationEvent)
     */
    public void onEvent(final InstallationEvent event) {
        if ( event.getType() == InstallationEvent.TYPE.STARTED ) {
            this.start();
        } else if ( event.getType() == InstallationEvent.TYPE.SUSPENDED ) {
            this.stop();
        }
    }

    /**
     * Suspend the startup handler (if not already done so)
     */
    public void start() {
        if ( !started ) {
            this.startupHandler.waitWithStartup(true);
            started = true;
        }
    }

    /**
     * Make sure the startup handler is not in suspended state
     */
    public void stop() {
        if ( started ) {
            this.startupHandler.waitWithStartup(false);
            started = false;
        }
    }

}
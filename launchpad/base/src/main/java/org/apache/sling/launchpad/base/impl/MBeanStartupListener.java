/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.launchpad.base.impl;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.sling.launchpad.api.StartupListener;
import org.apache.sling.launchpad.api.StartupMode;

/**
 * The startup listener is listening for startup events.
 *
 * It notifies a JMX MBean when the startup is completed and about the
 * progress.
 */
public class MBeanStartupListener
    implements StartupListener {

    /** The launcher mbean name. */
    private static final String NAME_LAUNCHER = "org.apache.sling.launchpad:type=Launcher";

    /** JMX Server. */
    private final MBeanServer jmxServer;

    /** Name of the launcher mbean. */
    private final ObjectName launcherName;

    /** Marker if the mbean is available. */
    private boolean available;

    /**
     * Create a new MBean helper.
     * @throws MalformedObjectNameException if the format of the object name does not correspond to a valid ObjectName.
     */
    public MBeanStartupListener() throws MalformedObjectNameException {
        this.launcherName = new ObjectName(NAME_LAUNCHER);
        this.jmxServer = ManagementFactory.getPlatformMBeanServer();
        // we check for the launcher mbean
        try {
            this.jmxServer.getMBeanInfo(launcherName);
            available = true;
        } catch (final Exception ignore) {
            available = false;
            // ignore
        }
    }

    /**
     * @see org.apache.sling.launchpad.api.StartupListener#inform(org.apache.sling.launchpad.api.StartupMode, boolean)
     */
    @Override
    public void inform(StartupMode mode, boolean finished) {
        // nothing to do
    }

    /**
     * @see org.apache.sling.launchpad.api.StartupListener#startupFinished(org.apache.sling.launchpad.api.StartupMode)
     */
    @Override
    public void startupFinished(StartupMode mode) {
        if ( this.available ) {
            try {
                this.jmxServer.invoke(launcherName, "startupFinished", null, null);
            } catch (final Exception e) {
                // we ignore this
            }
        }
    }

    /**
     * @see org.apache.sling.launchpad.api.StartupListener#startupProgress(float)
     */
    @Override
    public void startupProgress(float ratio) {
        if ( this.available ) {
            try {
                this.jmxServer.invoke(launcherName, "startupProgress", new Object[]{ratio}, new String[]{Float.class.getName()});
            } catch (final Exception e) {
                // we ignore this
            }
        }
    }
}

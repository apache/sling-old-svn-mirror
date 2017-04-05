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

package org.apache.sling.installer.api.jmx;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface InstallerMBean {

    /**
     * Count of Active resource groups. These resource groups are currently
     * in processing.
     * @return Get the active resource count.
     */
    int getActiveResourceCount();

    /**
     * Count of Installed resource groups. These resources groups are processed.
     * @return Get the installed resource count
     */
    int getInstalledResourceCount();

    /**
     * Indicates that whether the installer is currently active
     * @return If the installer is active.
     */
    boolean isActive();

    /**
     * Determines the time since when the installer is in suspended state
     * @return Time since last suspended.
     */
    long getSuspendedSince();
}

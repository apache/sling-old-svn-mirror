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
package org.apache.sling.installer.api.info;

import org.osgi.annotation.versioning.ProviderType;


/**
 * The info provider delivers information about the current state of the
 * OSGi installer
 *
 * @since 1.0
 */
@ProviderType
public interface InfoProvider {

    /**
     * Return the current installation state.
     * The installation state is a snapshot of the state at method call time.
     * @return The installation state
     */
    InstallationState getInstallationState();
}

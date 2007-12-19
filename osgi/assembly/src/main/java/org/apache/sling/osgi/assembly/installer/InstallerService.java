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
package org.apache.sling.osgi.assembly.installer;


/**
 * The <code>InstallerService</code> provides functionality to Project Sling
 * applications to install, update and uninstall bundles in the framework in
 * thread safe and stable manner. All Bundles and Services wishing to install,
 * update and uninstall bundles are strongly advised to go through this service
 * to prevent unexpected behaviour and exceptions.
 */
public interface InstallerService {

    /**
     * Returns an {@link Installer} instance, which may be used to install
     * and/or update bundles in the framework.
     * <p>
     * Each call to this method returns a new installer instance.
     *
     * @return An installer.
     */
    Installer getInstaller();

    BundleRepositoryAdmin getBundleRepositoryAdmin();
}

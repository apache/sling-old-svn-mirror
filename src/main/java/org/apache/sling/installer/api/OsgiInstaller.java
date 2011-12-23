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
package org.apache.sling.installer.api;

/**
 * Service for installing/updating/removing installable resources
 * {@link InstallableResource} in an OSGi framework.
 *
 * The resources are provided by provider components which are
 * clients of this API. For example a file directory provider might
 * scan a directory in the file system and provide the artifacts
 * contained in the configured directory.
 *
 * When such a client starts it should first call
 * {@link #registerResources(String, InstallableResource[])} and
 * inform this service about all available resources. In the case
 * of a file directory provider, this list would contain all
 * files found in the directory (and sub directories). This is
 * the rendezvous point. The OSGi installer service compares this
 * complete list with previous lists it might have and triggers
 * corresponding tasks like installing new artifacts and
 * uninstalling removed artifacts.
 *
 * Once this rendezvous has been done between a client and the
 * OSGi installe, the client calls
 * {@link #updateResources(String, InstallableResource[], String[])}
 * to inform about changes like new resources or removed resources.
 *
 * A single provider should never call these methods in parallel,
 * before calling any method a previous call must have finished!
 *
 * The OSGi installer detects the resources and based on their type
 * and metadata, the installer decides whether to install them,
 * update an already installed artifact or simply ignore them.
 * For example, for bundles the symbolic name and the version is used.
 */
public interface OsgiInstaller {

	/**
	 * Provide the installer with the complete list of installable
	 * resources for a given client.
	 *
	 * Client must call this at startup and/or when the installer
	 * service becomes available. The installer stores the list of
	 * previously registered/added resources, compares with the new
	 * list and removes resources that have disappeared.
	 *
     * Invalid resources are ignored.
	 *
     * @param urlScheme identifies the client.
	 * @param resources the list of available resources
	 */
	void registerResources(String urlScheme, InstallableResource[] resources);

    /**
	 * Inform the installer that resources are available for installation
	 * and/or other resources are no longer available.
	 * This method is called if
	 * - installed resources have been modified
	 * - new resources are available
	 * - installed resources should be uninstalled
	 * Invalid resources are ignored.
     * @param urlScheme identifies the client.
     * @param resource An array of updated/new resources - might be null
     * @param ids An array of identifiers for removed resources - might be null
	 */
	void updateResources(String urlScheme, InstallableResource[] resources,
	        String[] ids);
}
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
 * OSGi Service that installs/updates/removes installable data
 * {@link InstallableResource} in the OSGi framework.
 *
 * The client can register a number of such resources, and the
 * installer decides based on the resource weights, bundle version
 * numbers, etc. which ones are actually installed.
 *
 * An InstallableResource can be a bundle, a configuration, and later
 * we might support deployment packages as well.
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
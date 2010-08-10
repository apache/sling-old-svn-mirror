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
package org.apache.sling.osgi.installer;

import java.util.Collection;

/** OSGi Service that installs/updates/removes InstallableData
 * 	in the OSGi framework.
 *
 *  The client can register a number of such resources, and the
 *  installer decides based on the resource weights, bundle version
 *  numbers, etc. which ones are actually installed.
 *
 *	An InstallableResource can be a bundle, a configuration, and later
 *	we might support deployment packages as well.
 */
public interface OsgiInstaller {

	/** Provide the installer with the complete list of installable
	 * 	resources for a given client.
	 *
	 * 	Client must call this at startup and/or when the installer
	 * 	service becomes available. The installer stores the list of
	 * 	previously registered/added resources, compares with the new
	 * 	list and removes resources that have disappeared.
	 *
     *  Invalid resources are ignored.
	 *
	 * 	@param data the list of available resources
	 * 	@param urlScheme identifies the client. All URLs of the supplied data
	 * 		must use this scheme
	 */
	void registerResources(Collection<InstallableResource> data, String urlScheme);

	/** Inform the installer that a resource is available for installation.
	 * 	also called if the resource has been modified since it was registered.
	 *  Invalid resources are ignored.
	 */
	void addResource(InstallableResource r);

	/** Inform the installer that a resource is no longer available
	 * 	@param r an empty InstallableResource, isEmpty() must return true */
	void removeResource(String url);
}
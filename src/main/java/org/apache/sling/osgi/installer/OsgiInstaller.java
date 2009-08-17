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

import java.io.IOException;
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
	 * 	@param data the list of available resources
	 * 	@param urlScheme identifies the client. All URLs of the supplied data
	 * 		must use this scheme
	 */
	void registerResources(Collection<InstallableResource> data, String urlScheme) throws IOException;
	
	/** Inform the installer that a resource is available for installation.
	 * 	also called if the resource has been modified since it was registered.
	 */
	void addResource(InstallableResource d) throws IOException;
	
	/** Inform the installer that a resource is no longer available */
	void removeResource(InstallableResource d) throws IOException;
	
	/** Return counters used for statistics, console display, testing, etc. */
	long [] getCounters();
	
	/** Counter index: number of OSGi tasks executed */
	int OSGI_TASKS_COUNTER = 0;
	
    /** Counter index: number of installer cycles */
    int INSTALLER_CYCLES_COUNTER = 1;
    
	/** Size of the counters array */
	int COUNTERS_SIZE = 2;
}
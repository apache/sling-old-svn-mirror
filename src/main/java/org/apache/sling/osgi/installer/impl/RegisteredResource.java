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
package org.apache.sling.osgi.installer.impl;

import java.util.Map;

import org.apache.sling.osgi.installer.InstallableResource;

/** A resource that's been registered in the OSGi controller.
 * 	Data can be either an InputStream or a Dictionary, and we store
 *  it locally to avoid holding up to classes or data from our
 *  clients, in case those disappear while we're installing stuff.
 */
public interface RegisteredResource extends InstallableResource {

	String DIGEST_TYPE = "MD5";
    String ENTITY_JAR_PREFIX = "jar:";
	String ENTITY_BUNDLE_PREFIX = "bundle:";
	String ENTITY_CONFIG_PREFIX = "config:";

    /** Attribute key: configuration pid */
    String CONFIG_PID_ATTRIBUTE = "config.pid";


    void cleanup();
	String getURL();

	boolean isInstallable();
	void setInstallable(boolean installable);
	String getUrlScheme();
	long getSerialNumber();

	/** Attributes include the bundle symbolic name, bundle version, etc. */
	Map<String, Object> getAttributes();

	/** Return the identifier of the OSGi "entity" that this resource
     *  represents, for example "bundle:SID" where SID is the bundle's
     *  symbolic ID, or "config:PID" where PID is config's PID.
     */
    String getEntityId();

}

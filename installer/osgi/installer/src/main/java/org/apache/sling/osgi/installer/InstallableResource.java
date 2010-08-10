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

import java.io.InputStream;
import java.util.Dictionary;

/**
 * A piece of data that can be installed by the {@link OsgiInstaller}
 * Currently the OSGi installer supports bundles and configurations.
 *
 * A client can either use the {@link InstallableResourceFactory} to create
 * new data objects or implement this interface.
 */
public interface InstallableResource {

    /**
     * The type for a bundle - in this case {@link #getInputStream} must
     * return an input stream to the bundle. {@link #getDictionary()} might
     * return additional information.
     */
    String TYPE_BUNDLE = "bundle";

    /**
     * The type for a configuration - in this case {@link #getDictionary()}
     * must return a dictionary with the configuration.
     */
    String TYPE_CONFIG = "config";

    /**
     * Return this data's id. It is opaque for the {@link OsgiInstaller}
	 * but should uniquely identify the resource within the namespace of
	 * the used installation mechanism.
	 */
    String getId();

	/**
	 * Return the type of this resource.
	 * @return The resource type.
	 */
    String getType();

	/**
	 * Return an input stream with the data of this resource.
	 * Null if resource contains a configuration instead. Caller is responsible for
	 * closing the stream.
	 * If this resource is of type CONFIG it must not return an input stream and
	 * if this resource is of type BUNDLE it must return an input stream!
	 * @return The input stream or null.
	 */
    InputStream getInputStream();

	/**
	 * Return this resource's dictionary.
	 * Null if resource contains an InputStream instead. If this resource is of
	 * type CONFIG it must return a dictionary and if this resource is of type BUNDLE
	 * it might return a dictionary!
	 * @return The resource's dictionary or null.
	 */
	Dictionary<String, Object> getDictionary();

	/**
	 * Return this resource's digest. Not necessarily an actual md5 or other digest of the
	 * data, can be any string that changes if the data changes.
	 */
    String getDigest();

	/**Return the priority of this resource. Priorities are used to decide which
	 * resource to install when several are registered for the same OSGi entity
	 * (bundle, config, etc.)
	 */
    int getPriority();
}

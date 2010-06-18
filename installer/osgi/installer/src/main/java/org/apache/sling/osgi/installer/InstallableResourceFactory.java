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
 * This is a factory for installable resources. It should be used
 * if the client of the OSGi installer does not detect the resource
 * type itself and just handles this task over to the OSGi installer.
 */
public interface InstallableResourceFactory {

    /** Default resource priority */
    int DEFAULT_PRIORITY = 100;

    /**
     * Create a data object based on an input stream.
     * The data object is derived from the supplied input stream. If no resource type
     * is specified, the type is detected.
     *
     * @param url unique URL of the supplied data, must start with the scheme used
     *     {@link OsgiInstaller#registerResources} call
     * @param is the resource contents
     * @param digest must be supplied by client. Does not need to be an actual digest
     *     of the contents, but must change if the contents change. Having this supplied
     *     by the client avoids having to compute real digests to find out if a resource
     *     has changed, which can be expensive. If no digest is provided a digest
     *     will be calculated
     * @param optional resource type - if the client knows the resource type it should
     *     be specified - if not, the factory detects the type
     * @param optional priority - if not specified {@link #DEFAULT_PRIORITY} will be used.
     */
    InstallableResource create(String url,
                               InputStream is,
                               String digest,
                               InstallableResource.Type type,
                               Integer priority);

    /**
     * Create a data object based on a dictionary.
     * The data object is derived from the supplied dictionary. If no resource type
     * is specified, the type is detected.
     * Currently if a dictionary is provided, the resource is always assumed to be
     * a configuration - specifying a different resource type with a dictionary
     * results in an exception.
     *
     * @param url unique URL of the supplied data, must start with the scheme used
     *     {@link OsgiInstaller#registerResources} call
     * @param d the dictionary
     * @param digest must be supplied by client. Does not need to be an actual digest
     *     of the contents, but must change if the contents change. Having this supplied
     *     by the client avoids having to compute real digests to find out if a resource
     *     has changed, which can be expensive. If no digest is provided a digest
     *     will be calculated
     * @param optional resource type - if the client knows the resource type it should
     *     be specified - if not, the factory detects the type
     * @param optional priority - if not specified {@link #DEFAULT_PRIORITY} will be used.
     */
    InstallableResource create(String url,
                               Dictionary<String, Object> d,
                               String digest,
                               InstallableResource.Type type,
                               Integer priority);
}

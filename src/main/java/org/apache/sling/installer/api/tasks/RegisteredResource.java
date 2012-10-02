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
package org.apache.sling.installer.api.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;

/**
 * A resource that's been registered in the OSGi controller.
 * Data can be either an input stream or a dictionary.
 * Registered resources are processed by a {@link ResourceTransformer}.
 */
public interface RegisteredResource {

    /**
     * Return the scheme from where the artifact is originated.
     */
    String getScheme();

    /**
     * Return this data's URL. The URL is the {@link #getScheme}
     * followed by a colon, followed by a unique identifier of
     * the resource within the providers space..
     */
    String getURL();

    /**
     * Return the type of this resource.
     * @return The resource type.
     */
    String getType();

    /**
     * Return an input stream with the data of this resource.
     * Null if resource contains a configuration instead. Caller is responsible for
     * closing the stream.
     * If this resource is of type PROPERTIES it must not return an input stream and
     * if this resource is of type FILE it must return an input stream!
     * @return The input stream or null.
     */
    InputStream getInputStream() throws IOException;

    /**
     * Return this resource's dictionary.
     * Null if resource contains an InputStream instead. If this resource is of
     * type PROPERTIES it must return a dictionary and if this resource is of type FILE
     * it might return a dictionary!
     * @return The resource's dictionary or null.
     */
    Dictionary<String, Object> getDictionary();

    /**
     * Return this resource's digest. Not necessarily an actual md5 or other digest of the
     * data, can be any string that changes if the data changes.
     */
    String getDigest();

    /**
     * Return the priority of this resource. Priorities are used to decide which
     * resource to install when several are registered for the same OSGi entity
     * (bundle, configuration, etc.)
     */
    int getPriority();

    /**
     * Return the identifier of the OSGi "entity" that this resource
     * represents, for example "bundle:SID" where SID is the bundle's
     * symbolic ID, or "config:PID" where PID is config's PID.
     */
    String getEntityId();
}

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
package org.apache.sling.api.resource;

import java.util.HashMap;


/**
 * The <code>ResourceMetadata</code> interface defines the API for the
 * metadata of a Sling {@link Resource}. Essentially the resource's metadata is
 * just a map of objects indexed by string keys.
 * <p>
 * The actual contents of the meta data map is implementation specific with the
 * exception for the {@link #RESOLUTION_PATH sling.resolutionPath} property
 * which must be provided by all implementations and contain the part of the
 * request URI used to resolve the resource. The type of this property value is
 * defined to be <code>String</code>.
 * <p>
 * Note, that the prefix <em>sling.</em> to key names is reserved for the
 * Sling implementation.
 */
public class ResourceMetadata extends HashMap<String, Object> {

    /**
     * The name of the required property providing the part of the request URI
     * which was used to the resolve the resource to which the meta data
     * instance belongs (value is "sling.resolutionPath").
     */
    public static final String RESOLUTION_PATH = "sling.resolutionPath";

}

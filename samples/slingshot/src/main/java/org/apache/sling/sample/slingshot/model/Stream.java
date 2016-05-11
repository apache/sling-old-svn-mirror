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
package org.apache.sling.sample.slingshot.model;

import org.apache.sling.api.resource.Resource;

public class Stream {

    /** The resource type for a stream. */
    public static final String RESOURCETYPE = "slingshot/Stream";

    private final Resource resource;

    private volatile StreamInfo info;

    public Stream(final Resource resource) {
        this.resource = resource;
    }

    public StreamInfo getInfo() {
        if ( info == null ) {
            info = new StreamInfo(this.resource == null ? null : this.resource.getChild("info"));
        }
        return info;
    }
}

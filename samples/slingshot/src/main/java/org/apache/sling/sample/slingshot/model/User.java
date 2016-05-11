/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.sample.slingshot.model;

import org.apache.sling.api.resource.Resource;

public class User {

    /** The resource type for a user. */
    public static final String RESOURCETYPE = "slingshot/User";

    private final Resource resource;

    private volatile UserInfo info;

    public User(final Resource resource) {
        this.resource = resource;
    }

    public UserInfo getInfo() {
        if ( info == null ) {
            info = new UserInfo(this.resource.getChild("info"));
        }
        return info;
    }
}

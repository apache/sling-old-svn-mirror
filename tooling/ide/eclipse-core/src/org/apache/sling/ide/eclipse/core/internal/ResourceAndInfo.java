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
package org.apache.sling.ide.eclipse.core.internal;

import org.apache.sling.ide.transport.FileInfo;
import org.apache.sling.ide.transport.ResourceProxy;

/**
 * The <tt>ResourceAndInfo</tt> is a simple value class allowing both a ResourceProxy and a FileInfo to be passed
 * together
 *
 */
public class ResourceAndInfo {
    private final ResourceProxy resource;
    private final FileInfo info;
    private final boolean onlyWhenMissing;

    public ResourceAndInfo(ResourceProxy resource, FileInfo info) {
        this(resource, info, false);
    }

    public ResourceAndInfo(ResourceProxy resource, FileInfo info, boolean onlyIfMissing) {
        this.resource = resource;
        this.info = info;
        this.onlyWhenMissing = onlyIfMissing;
    }

    public ResourceProxy getResource() {
        return resource;
    }

    public FileInfo getInfo() {
        return info;
    }

    public boolean isOnlyWhenMissing() {
        return onlyWhenMissing;
    }
}
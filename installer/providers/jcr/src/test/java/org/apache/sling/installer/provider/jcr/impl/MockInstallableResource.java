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
package org.apache.sling.installer.provider.jcr.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Dictionary;

import org.apache.sling.installer.api.InstallableResource;

public class MockInstallableResource extends InstallableResource {

    private static int counter;

    public MockInstallableResource(String uri) {
        this(uri, "", null);
    }

    public MockInstallableResource(String uri, String data, String digest) {
        super(uri, new ByteArrayInputStream(data.getBytes()), null, getNextDigest(digest),
                InstallableResource.TYPE_BUNDLE, InstallableResource.DEFAULT_PRIORITY);
    }

    public MockInstallableResource(String uri, InputStream is, Dictionary<String, Object> d, String digest, String type, Integer priority) {
        super(uri, is, d,
                digest,
                type != null ? type : (is != null ? InstallableResource.TYPE_BUNDLE : InstallableResource.TYPE_CONFIG),
                priority != null ? priority : InstallableResource.DEFAULT_PRIORITY);
    }

    public MockInstallableResource(String uri, InputStream is, String digest, String type, Integer priority) {
        super(uri, is, null, digest, type != null ? type : InstallableResource.TYPE_BUNDLE, priority);
    }

    public MockInstallableResource(String uri, Dictionary<String, Object> d, String digest, String type, Integer priority) {
        super(uri, null, d, digest,
                type != null ? type : InstallableResource.TYPE_CONFIG, priority);
    }

    static String getNextDigest(String digest) {
        if(digest != null) {
            return digest;
        }
        synchronized (MockInstallableResource.class) {
            return String.valueOf(System.currentTimeMillis() + (counter++));
        }
    }
}

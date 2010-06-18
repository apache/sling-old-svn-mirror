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
package org.apache.sling.jcr.jcrinstall.impl;

import java.io.InputStream;
import java.util.Dictionary;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.InstallableResourceFactory;
import org.apache.sling.osgi.installer.InstallableResource.Type;


class MockInstallableResourceFactory implements InstallableResourceFactory {

    public InstallableResource create(String url, Dictionary<String, Object> d,
            String digest, Type type, Integer priority) {
        return new MockInstallableResource(url, d, digest, type, priority);
    }

    public InstallableResource create(String url, InputStream is,
            String digest, Type type, Integer priority) {
        return new MockInstallableResource(url, is, digest, type, priority);
    }
}

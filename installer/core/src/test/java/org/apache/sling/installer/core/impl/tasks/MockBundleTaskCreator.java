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
package org.apache.sling.installer.core.impl.tasks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.installer.core.impl.MockBundleContext;
import org.osgi.framework.Version;

/** BundleTaskCreator that simulates the presence and state of bundles */
class MockBundleTaskCreator extends BundleTaskCreator {

    private final Map<String, BundleInfo> fakeBundleInfo = new HashMap<String, BundleInfo>();

    public MockBundleTaskCreator() throws IOException {
        this.init(new MockBundleContext(), null, null);
    }

    void addBundleInfo(String symbolicName, String version, int state) {
        fakeBundleInfo.put(symbolicName, new BundleInfo(symbolicName, new Version(version), state, 1));
    }

    @Override
    protected BundleInfo getBundleInfo(String symbolicName, String version) {
        BundleInfo info = fakeBundleInfo.get(symbolicName);
        if ( version == null || info.version.compareTo(new Version(version)) == 0 ) {
            return info;
        }
        return null;
    }
}

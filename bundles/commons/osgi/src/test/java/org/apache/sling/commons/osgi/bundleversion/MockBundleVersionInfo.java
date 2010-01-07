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
package org.apache.sling.commons.osgi.bundleversion;

import org.osgi.framework.Version;

class MockBundleVersionInfo implements BundleVersionInfo<String> {

    private final String source;
    private final String symbolicName;
    private final Version version;
    private final long lastModified;
    
    MockBundleVersionInfo() {
        source = null;
        symbolicName = null;
        version = null;
        lastModified = BundleVersionInfo.BND_LAST_MODIFIED_MISSING;
    }
    
    MockBundleVersionInfo(String symbolicName, String version, long lastModified) {
        this.symbolicName = symbolicName;
        this.version = new Version(version);
        this.source = symbolicName + "." + version + "." + lastModified;
        this.lastModified = lastModified;
    }
    
    @Override
    public String toString() {
        return source;
    }
    
    public long getBundleLastModified() {
        return lastModified;
    }

    public String getBundleSymbolicName() {
        return symbolicName;
    }

    public String getSource() {
        return source;
    }

    public Version getVersion() {
        return version;
    }

    public boolean isBundle() {
        return symbolicName != null;
    }

    public boolean isSnapshot() {
        return version.toString().contains(SNAPSHOT_MARKER);
    }
}

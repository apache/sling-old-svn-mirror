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

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/** BundleVersionInfo based on a Bundle object */
public class BundleBundleVersionInfo implements BundleVersionInfo<Bundle> {

    private Bundle source;
    private final long lastModified;
    
    public BundleBundleVersionInfo(Bundle b) {
        source = b;
        
        long lastMod = BND_LAST_MODIFIED_MISSING;
        final String mod = (String)source.getHeaders().get(BND_LAST_MODIFIED);
        if(mod != null) {
            try {
                lastMod = Long.parseLong(mod);
            } catch(NumberFormatException ignore) {
            }
        }
        lastModified = lastMod;
    }
    
    public long getBundleLastModified() {
        return lastModified;
    }

    public String getBundleSymbolicName() {
        return source.getSymbolicName();
    }

    public Bundle getSource() {
        return source;
    }

    public Version getVersion() {
        return (Version)source.getHeaders().get(Constants.BUNDLE_VERSION);
    }

    public boolean isBundle() {
        return true;
    }

    public boolean isSnapshot() {
        return getVersion().toString().contains(SNAPSHOT_MARKER);
    }
}

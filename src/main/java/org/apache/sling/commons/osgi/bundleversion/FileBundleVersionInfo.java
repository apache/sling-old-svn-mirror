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

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/** BundleVersionInfo based on a bundle jar file */
public class FileBundleVersionInfo extends BundleVersionInfo<File> {

    private final String symbolicName;
    private final Version version;
    private final boolean isSnapshot;
    private final long lastModified;
    private final File source;
    
    public FileBundleVersionInfo(File bundle) throws IOException {
        source = bundle;
        final Manifest m = new JarFile(bundle).getManifest();
        if(m == null) {
            symbolicName = null;
            version = null;
            isSnapshot = false;
            lastModified = BND_LAST_MODIFIED_MISSING;
        } else {
            symbolicName = m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
            final String v = m.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
            version = v == null ? null : new Version(v);
            isSnapshot = v != null && v.contains(SNAPSHOT_MARKER);
            final String last = m.getMainAttributes().getValue(BND_LAST_MODIFIED);
            long lastMod = BND_LAST_MODIFIED_MISSING;
            if(last != null) {
                try {
                    lastMod = Long.parseLong(last);
                } catch(NumberFormatException ignore) {
                }
            }
            lastModified = lastMod;
        }
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + source.getAbsolutePath();
    }
    
    public boolean isBundle() {
        return symbolicName != null;
    }
    
    public long getBundleLastModified() {
        return lastModified;
    }

    public String getBundleSymbolicName() {
        return symbolicName;
    }

    public File getSource() {
        return source;
    }

    public Version getVersion() {
        return version;
    }

    public boolean isSnapshot() {
        return isSnapshot;
    }
}

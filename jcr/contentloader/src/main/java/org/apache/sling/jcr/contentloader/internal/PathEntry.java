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
package org.apache.sling.jcr.contentloader.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.commons.osgi.ManifestHeader;
import org.osgi.framework.Bundle;

/**
 * A path entry from the manifest for initial content.
 */
public class PathEntry {

    /** The manifest header to specify initial content to be loaded. */
    public static final String CONTENT_HEADER = "Sling-Initial-Content";

    /** The overwrite flag specifying if content should be overwritten or just initially added. */
    public static final String OVERWRITE_FLAG = "overwrite";

    /** The uninstall flag specifying if content should be uninstalled. */
    public static final String UNINSTALL_FLAG = "uninstall";

    /** The path for the initial content. */
    private final String path;

    /** Should existing content be overwritten? */
    private final boolean overwrite;

    /** Should existing content be uninstalled? */
    private final boolean uninstall;

    public static Iterator<PathEntry> getContentPaths(final Bundle bundle) {
        final List<PathEntry> entries = new ArrayList<PathEntry>();

        final String root = (String) bundle.getHeaders().get(CONTENT_HEADER);
        if (root != null) {
            final ManifestHeader header = ManifestHeader.parse(root);
            for(final ManifestHeader.Entry entry : header.getEntries()) {
                entries.add(new PathEntry(entry));
            }
        }

        if ( entries.size() == 0 ) {
            return null;
        }
        return entries.iterator();
    }

    public PathEntry(ManifestHeader.Entry entry) {
        // check for overwrite and uninstall flag
        final String overwriteValue = entry.getDirectiveValue(OVERWRITE_FLAG);
        final String uninstallValue = entry.getDirectiveValue(UNINSTALL_FLAG);
        boolean overwriteFlag = false;
        if ( overwriteValue != null ) {
            overwriteFlag = Boolean.valueOf(overwriteValue).booleanValue();
        }
        this.path =  entry.getValue();
        this.overwrite = overwriteFlag;
        if ( uninstallValue != null ) {
            this.uninstall = Boolean.valueOf(uninstallValue);
        } else {
            this.uninstall = this.overwrite;
        }
    }

    public String getPath() {
        return this.path;
    }

    public boolean isOverwrite() {
        return this.overwrite;
    }

    public boolean isUninstall() {
        return this.uninstall;
    }
}

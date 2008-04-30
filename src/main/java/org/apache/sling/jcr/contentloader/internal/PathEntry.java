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
import java.util.StringTokenizer;

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
            final StringTokenizer tokener = new StringTokenizer(root, ",");
            while (tokener.hasMoreTokens()) {
                final String path = tokener.nextToken().trim();
                entries.add(new PathEntry(path));
            }
        }

        if ( entries.size() == 0 ) {
            return null;
        }
        return entries.iterator();
    }

    public PathEntry(String path) {
        // check for overwrite flag
        boolean overwriteFlag = false;
        Boolean uninstallFlag = null;
        int flagPos = path.indexOf(";");
        if ( flagPos != -1 ) {
            final StringTokenizer flagTokenizer = new StringTokenizer(path.substring(flagPos+1), ";");
            while ( flagTokenizer.hasMoreTokens() ) {
                final String token = flagTokenizer.nextToken();
                int pos = token.indexOf(":=");
                if ( pos != -1 ) {
                    final String name = token.substring(0, pos);
                    final String value = token.substring(pos+2);
                    if ( name.equals(OVERWRITE_FLAG) ) {
                        overwriteFlag = Boolean.valueOf(value).booleanValue();
                    } else if (name.equals(UNINSTALL_FLAG) ) {
                        uninstallFlag = Boolean.valueOf(value);
                    }
                }
            }
            path = path.substring(0, flagPos);
        }
        this.path = path;
        this.overwrite = overwriteFlag;
        if ( uninstallFlag != null ) {
            this.uninstall = uninstallFlag;
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

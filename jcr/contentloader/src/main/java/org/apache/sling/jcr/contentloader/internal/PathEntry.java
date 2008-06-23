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

    /**
     * The overwrite directive specifying if content should be overwritten or
     * just initially added.
     */
    public static final String OVERWRITE_DIRECTIVE = "overwrite";

    /** The uninstall directive specifying if content should be uninstalled. */
    public static final String UNINSTALL_DIRECTIVE = "uninstall";

    /**
     * The path directive specifying the target node where initial content will
     * be loaded.
     */
    public static final String PATH_DIRECTIVE = "path";

    /**
     * The checkin directive specifying whether versionable nodes should be
     * checked in
     */
    public static final String CHECKIN_DIRECTIVE = "checkin";

    /**
     * The expand directive specifying whether the available {@link ImportProvider}s
     * should be used during content loading. This is a boolean value that
     * defaults to true.
     * @since 2.0.4
     */
    public static final String EXPAND_DIRECTIVE = "expand";

    /** The path for the initial content. */
    private final String path;

    /** Should existing content be overwritten? */
    private final boolean overwrite;

    /** Should existing content be uninstalled? */
    private final boolean uninstall;

    /** Should versionable nodes be checked in? */
    private final boolean checkin;

    /** Should archives be expanded? @since 2.0.4 */
    private final boolean expand;

    /**
     * Target path where initial content will be loaded. If it´s null then
     * target node is the root node
     */
    private final String target;

    public static Iterator<PathEntry> getContentPaths(final Bundle bundle) {
        final List<PathEntry> entries = new ArrayList<PathEntry>();

        final String root = (String) bundle.getHeaders().get(CONTENT_HEADER);
        if (root != null) {
            final ManifestHeader header = ManifestHeader.parse(root);
            for (final ManifestHeader.Entry entry : header.getEntries()) {
                entries.add(new PathEntry(entry));
            }
        }

        if (entries.size() == 0) {
            return null;
        }
        return entries.iterator();
    }

    public PathEntry(ManifestHeader.Entry entry) {
        this.path = entry.getValue();

        // check for directives

        // overwrite directive
        final String overwriteValue = entry.getDirectiveValue(OVERWRITE_DIRECTIVE);
        if (overwriteValue != null) {
            this.overwrite = Boolean.valueOf(overwriteValue);
        } else {
            this.overwrite = false;
        }

        // uninstall directive
        final String uninstallValue = entry.getDirectiveValue(UNINSTALL_DIRECTIVE);
        if (uninstallValue != null) {
            this.uninstall = Boolean.valueOf(uninstallValue);
        } else {
            this.uninstall = this.overwrite;
        }

        // path directive
        final String pathValue = entry.getDirectiveValue(PATH_DIRECTIVE);
        if (pathValue != null) {
            this.target = pathValue;
        } else {
            this.target = null;
        }

        // checkin directive
        final String checkinValue = entry.getDirectiveValue(CHECKIN_DIRECTIVE);
        if (checkinValue != null) {
            this.checkin = Boolean.valueOf(checkinValue);
        } else {
            this.checkin = false;
        }

        // expand directive
        final String expandValue = entry.getDirectiveValue(EXPAND_DIRECTIVE);
        if ( expandValue != null ) {
            this.expand = Boolean.valueOf(expandValue);
        } else {
            this.expand = true;
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

    public boolean isCheckin() {
        return this.checkin;
    }

    public boolean isExpand() {
        return this.expand;
    }

    public String getTarget() {
        return target;
    }
}

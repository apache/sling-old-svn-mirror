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

import org.apache.sling.commons.osgi.ManifestHeader;
import org.apache.sling.jcr.contentloader.ImportOptions;
import org.osgi.framework.Bundle;

/**
 * A path entry from the manifest for initial content.
 */
public class PathEntry extends ImportOptions {

    /** The manifest header to specify initial content to be loaded. */
    public static final String CONTENT_HEADER = "Sling-Initial-Content";

    /**
     * The overwrite directive specifying if content should be overwritten or
     * just initially added.
     */
    public static final String OVERWRITE_DIRECTIVE = "overwrite";

    /**
     * The overwriteProperties directive specifying if content properties 
     * should be overwritten or just initially added.
     */
    public static final String OVERWRITE_PROPERTIES_DIRECTIVE = "overwriteProperties";

    /** The uninstall directive specifying if content should be uninstalled. */
    public static final String UNINSTALL_DIRECTIVE = "uninstall";

    /**
     * The path directive specifying the target node where initial content will
     * be loaded.
     */
    public static final String PATH_DIRECTIVE = "path";

    /**
     * The workspace into which the content will be loaded.
     */
    public static final String WORKSPACE_DIRECTIVE = "workspace";

    /**
     * The checkin directive specifying whether versionable nodes should be
     * checked in
     */
    public static final String CHECKIN_DIRECTIVE = "checkin";

    /**
     * The autoCheckout directive specifying whether versionable nodes should be
     * checked out when necessary
     */
    public static final String AUTOCHECKOUT_DIRECTIVE = "autoCheckout";

    /**
     * The ignore content readers directive specifying whether the available {@link ContentReader}s
     * should be used during content loading. This is a string value that defaults to the empty
     * string..
     * @since 2.0.4
     */
    public static final String IGNORE_CONTENT_READERS_DIRECTIVE = "ignoreImportProviders";

    /** The path for the initial content. */
    private final String path;

    /** Should existing content be overwritten? */
    private final boolean overwrite;

    /** Should existing content properties be overwritten? */
    private final boolean overwriteProperties;

    /** Should existing content be uninstalled? */
    private final boolean uninstall;

    /** Should versionable nodes be checked in? */
    private final boolean checkin;

    /** Should versionable nodes be auto checked out when necessary? */
    private final boolean autoCheckout;
    
    /** Which content readers should be ignored? @since 2.0.4 */
    private final List<String> ignoreContentReaders;

    /**
     * Target path where initial content will be loaded. If it´s null then
     * target node is the root node
     */
    private final String target;

    /** Workspace to import into. */
    private final String workspace;

    private long lastModified;

    public static Iterator<PathEntry> getContentPaths(final Bundle bundle) {
        final List<PathEntry> entries = new ArrayList<PathEntry>();
        String bundleLastModifiedStamp = (String) bundle.getHeaders().get("Bnd-LastModified");
        long bundleLastModified = bundle.getLastModified(); // time last modified inside the container
        if ( bundleLastModifiedStamp != null ) {
            bundleLastModified = Math.min(bundleLastModified, Long.parseLong(bundleLastModifiedStamp));
        }
        final String root = (String) bundle.getHeaders().get(CONTENT_HEADER);
        if (root != null) {
            final ManifestHeader header = ManifestHeader.parse(root);
            for (final ManifestHeader.Entry entry : header.getEntries()) {
                
                entries.add(new PathEntry(entry, bundleLastModified ));
            }
        }

        if (entries.size() == 0) {
            return null;
        }
        return entries.iterator();
    }

    public PathEntry(ManifestHeader.Entry entry, long bundleLastModified) {
        this.path = entry.getValue();
        this.lastModified = bundleLastModified;

        // check for directives

        // overwrite directive
        final String overwriteValue = entry.getDirectiveValue(OVERWRITE_DIRECTIVE);
        if (overwriteValue != null) {
            this.overwrite = Boolean.valueOf(overwriteValue);
        } else {
            this.overwrite = false;
        }

        // overwriteProperties directive
        final String overwritePropertiesValue = entry.getDirectiveValue(OVERWRITE_PROPERTIES_DIRECTIVE);
        if (overwritePropertiesValue != null) {
            this.overwriteProperties = Boolean.valueOf(overwritePropertiesValue);
        } else {
            this.overwriteProperties = false;
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

        // autoCheckout directive
        final String autoCheckoutValue = entry.getDirectiveValue(AUTOCHECKOUT_DIRECTIVE);
        if (autoCheckoutValue != null) {
            this.autoCheckout = Boolean.valueOf(autoCheckoutValue);
        } else {
            this.autoCheckout = true;
        }

        // expand directive
        this.ignoreContentReaders = new ArrayList<String>();
        final String expandValue = entry.getDirectiveValue(IGNORE_CONTENT_READERS_DIRECTIVE);
        if ( expandValue != null && expandValue.length() > 0 ) {
            final StringTokenizer st = new StringTokenizer(expandValue, ",");
            while ( st.hasMoreTokens() ) {
                this.ignoreContentReaders.add(st.nextToken());
            }
        }

        // workspace directive
        final String workspaceValue = entry.getDirectiveValue(WORKSPACE_DIRECTIVE);
        if (pathValue != null) {
            this.workspace = workspaceValue;
        } else {
            this.workspace = null;
        }
    }
    
    public long getLastModified() {
        return lastModified;
    }
    
    public String getPath() {
        return this.path;
    }

    /* (non-Javadoc)
	 * @see org.apache.sling.jcr.contentloader.internal.ImportOptions#isOverwrite()
	 */
    public boolean isOverwrite() {
        return this.overwrite;
    }

    /* (non-Javadoc)
	 * @see org.apache.sling.jcr.contentloader.ImportOptions#isPropertyOverwrite()
	 */
	@Override
	public boolean isPropertyOverwrite() {
		return this.overwriteProperties;
	}

	public boolean isUninstall() {
        return this.uninstall;
    }

    /* (non-Javadoc)
	 * @see org.apache.sling.jcr.contentloader.internal.ImportOptions#isCheckin()
	 */
    public boolean isCheckin() {
        return this.checkin;
    }
    
    /* (non-Javadoc)
	 * @see org.apache.sling.jcr.contentloader.ImportOptions#isAutoCheckout()
	 */
	@Override
	public boolean isAutoCheckout() {
		return this.autoCheckout;
	}

	/* (non-Javadoc)
	 * @see org.apache.sling.jcr.contentloader.internal.ImportOptions#isIgnoredImportProvider(java.lang.String)
	 */
    public boolean isIgnoredImportProvider(String extension) {
        if ( extension.startsWith(".") ) {
            extension = extension.substring(1);
        }
        return this.ignoreContentReaders.contains(extension);
    }

    public String getTarget() {
        return target;
    }

    public String getWorkspace() {
        return workspace;
    }
}

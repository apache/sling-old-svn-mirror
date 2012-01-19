/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.launchpad.base.impl;

import java.io.File;
import java.io.FileFilter;
import java.util.Map;

import org.apache.sling.launchpad.base.shared.SharedConstants;

public class DirectoryUtil {

    /**
     * The possible file extensions for a bundle archive file.
     */
    private static final String[] BUNDLE_EXTENSIONS = { ".jar", ".war" };

    /**
     * The path of startup bundles in the sling home
     */
    public static final String PATH_STARTUP = "startup";

    /**
     * The path of startup bundles in the sling home
     */
    public static final String PATH_CONF = "conf";

    //---------- FileFilter implementations to scan startup folders

    /**
     * Simple directory filter
     */
    public static final FileFilter DIRECTORY_FILTER = new FileFilter() {
        public boolean accept(final File f) {
            return f.isDirectory();
        }
    };

    /**
     * Simple bundle file filter
     */
    public static final FileFilter BUNDLE_FILE_FILTER = new FileFilter() {
        public boolean accept(final File f) {
            return f.isFile() && isBundle(f.getName());
        }
    };

    /**
     * Determine if a path could be a bundle based on its extension.
     *
     * @param path the path to the file
     * @return true if the path could be a bundle
     */
    public static boolean isBundle(final String path) {
        for (String extension : BUNDLE_EXTENSIONS) {
            if (path.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    public static File getHomeDir(final Map<String, String> properties) {
        String home = properties.get(SharedConstants.SLING_LAUNCHPAD);
        if (home == null) {
            home = properties.get(SharedConstants.SLING_HOME);
        }
        return new File(home);
    }

    /**
     * Return the config dir.
     */
    public static File getConfigDir(final Map<String, String> properties) {
        return new File(getHomeDir(properties), PATH_CONF);
    }

    /**
     * Return the startup dir.
     */
    public static File getStartupDir(final Map<String, String> properties) {
        return new File(getHomeDir(properties), PATH_STARTUP);
    }
}

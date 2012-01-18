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

public class DirectoryUtil {

    /**
     * The possible file extensions for a bundle archive file.
     */
    private static final String[] BUNDLE_EXTENSIONS = { ".jar", ".war" };

    //---------- FileFilter implementations to scan startup folders

    /**
     * Simple directory filter
     */
    public static final FileFilter DIRECTORY_FILTER = new FileFilter() {
        public boolean accept(File f) {
            return f.isDirectory();
        }
    };

    /**
     * Simple bundle file filter
     */
    public static final FileFilter BUNDLE_FILE_FILTER = new FileFilter() {
        public boolean accept(File f) {
            return f.isFile() && isBundle(f.getName());
        }
    };

    /**
     * Determine if a path could be a bundle based on its extension.
     *
     * @param path the path to the file
     * @return true if the path could be a bundle
     */
    public static boolean isBundle(String path) {
        for (String extension : BUNDLE_EXTENSIONS) {
            if (path.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}

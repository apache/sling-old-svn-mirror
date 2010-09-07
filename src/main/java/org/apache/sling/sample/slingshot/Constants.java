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
package org.apache.sling.sample.slingshot;

import org.apache.sling.api.resource.Resource;

public abstract class Constants {

    /** This is the resource path for the root of our application. */
    public static final String APP_ROOT = "/slingshot";

    /** This is the resource path for the root album. */
    public static final String ALBUMS_ROOT = APP_ROOT + "/albums";

    /** The resource type of a folder (= album) */
    public static final String RESOURCETYPE_FOLDER = "nt:folder";

    /** The resource type of an extended folder (= album) */
    public static final String RESOURCETYPE_EXT_FOLDER = "sling:Folder";

    /** The resource type for an album. */
    public static final String RESOURCETYPE_ALBUM = "slingshot/Album";

    /** The resource type of a file (= photo) */
    public static final String RESOURCETYPE_FILE = "nt:file";

    /** The resource type for a photo. */
    public static final String RESOURCETYPE_PHOTO = "slingshot/Photo";

    /** The property containing the tags. */
    public static final String PROPERTY_SLINGSHOT_TAGS = "slingshot:tags";

    /** Name of the preview folder */
    public static final String FOLDER_NAME_PREVIEW = "preview";

    /**
     * We only include resource which names do not start with a dot
     * and we also exclude the preview folder.
     */
    public static boolean includeAsAlbum(final Resource resource) {
        final String name = resource.getName();
        if ( name.equals(FOLDER_NAME_PREVIEW) || name.startsWith(".") ) {
            return false;
        }
        return true;
    }

    /**
     * Exclude all files starting with a dot
     */
    public static boolean includeAsMedia(final Resource resource) {
        final String name = resource.getName();
        if ( name.startsWith(".") ) {
            return false;
        }
        return true;
    }
}

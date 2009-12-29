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

public interface Constants {

    /** This is the resource path for the root of our application. */
    String APP_ROOT = "/slingshot";

    /** This is the resource path for the root album. */
    String ALBUMS_ROOT = APP_ROOT + "/albums";

    /** The resource type of a folder (= album) */
    String RESOURCETYPE_FOLDER = "nt:folder";

    /** The resource type of an extended folder (= album) */
    String RESOURCETYPE_EXT_FOLDER = "sling:Folder";

    /** The resource type for an album. */
    String RESOURCETYPE_ALBUM = "slingshot/Album";

    /** The resource type of a file (= photo) */
    String RESOURCETYPE_FILE = "nt:file";

    /** The resource type for a photo. */
    String RESOURCETYPE_PHOTO = "slingshot/Photo";

    /** The property containing the tags. */
    String PROPERTY_SLINGSHOT_TAGS = "slingshot:tags";
}

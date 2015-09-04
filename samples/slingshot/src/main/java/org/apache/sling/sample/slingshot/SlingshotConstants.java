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


public abstract class SlingshotConstants {

    /** This is the resource path for the root of our application. */
    public static final String APP_ROOT_PATH = "/slingshot";

    /** The resource type for a user. */
    public static final String RESOURCETYPE_USER = "slingshot/User";

    /** The resource type for the content. */
    public static final String RESOURCETYPE_CONTENT = "slingshot/Content";

    /** The resource type for a category. */
    public static final String RESOURCETYPE_CATEGORY = "slingshot/Category";

    /** The resource type for an item. */
    public static final String RESOURCETYPE_ITEM = "slingshot/Item";

    public static final String PROPERTY_TITLE = "title";

    public static final String PROPERTY_DESCRIPTION = "description";

    public static final String PROPERTY_LOCATION = "location";

    public static final String PROPERTY_TAGS = "tags";
}

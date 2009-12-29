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

package org.apache.sling.explorer.client;

import com.google.gwt.i18n.client.Constants;


public interface ExplorerConstants extends Constants {

    String CONTENT_ROOT = "/";
    String JSON_TREE_REQUEST_EXTENSION = ".explorer.tree-node.json";

    String JSON_CHILDREN_REQUEST_EXTENSION = ".explorer.children.json";
    String PROPERTY = "property";
    String RESOURCE = "resource";

    String SLING_HOMEPAGE = "http://sling.apache.org";
    String SLING_DOCUMENTATION = SLING_HOMEPAGE + "/documentation";

    /**
     * Descriptions to translate
     * (see the ExplorerConstants.properties & associted files)
     *
     */
    String mainTitle();

    String mainSubTitle();

    String slingHomePage();

    String rootItemDescription();

    String propertiesDescripton();

    String subResourcesDescription();
}

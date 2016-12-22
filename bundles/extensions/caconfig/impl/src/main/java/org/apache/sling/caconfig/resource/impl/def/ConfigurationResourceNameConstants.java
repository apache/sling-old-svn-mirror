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
package org.apache.sling.caconfig.resource.impl.def;

public final class ConfigurationResourceNameConstants {

    private ConfigurationResourceNameConstants() {
        // constants only
    }

    /**
     * Property that points to the configuration path to be used.
     * Additionally each resource having this property marks the beginning of a new context sub-tree.
     */
    public static final String PROPERTY_CONFIG_REF = "sling:configRef";
   
    /**
     * Boolean property that controls whether config resource collections should be merged on inheritance or not.
     * Merging means merging the lists, not the list items (properties of the resources) itself.
     */
    public static final String PROPERTY_CONFIG_COLLECTION_INHERIT = "sling:configCollectionInherit";

}

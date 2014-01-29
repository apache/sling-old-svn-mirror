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
package org.apache.sling.resourcemerger.impl;

/**
 * Constants related to {@link MergedResource}.
 */
public class MergedResourceConstants {

    private MergedResourceConstants() {
        // Make sure it never gets instantiated
    }

    /**
     * Property name containing the list of properties to hide.
     */
    public static final String PN_HIDE_PROPERTIES = "sling:hideProperties";

    /**
     * Property name which has to be set to <code>true</code> to hide the
     * whole resource (and its children) of the current resource.
     */
    public static final String PN_HIDE_RESOURCE = "sling:hideResource";

    /**
     * Property name containing the list of child resources to hide.
     */
    public static final String PN_HIDE_CHILDREN = "sling:hideChildren";

    /**
     * Property name for the reordering option.
     */
    public static final String PN_ORDER_BEFORE = "sling:orderBefore";

}

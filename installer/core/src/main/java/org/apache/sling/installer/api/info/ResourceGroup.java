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
package org.apache.sling.installer.api.info;

import java.util.List;

/**
 * This is a group of resources all pointing to the same artifact,
 * but maybe in different versions or locations.
 * @since 1.0
 */
public interface ResourceGroup {

    /**
     * Get all resources for this group.
     * @return The list of resources.
     */
    List<Resource> getResources();

    /**
     * Get the current alias for this group.
     * @return The alias or <code>null</code>
     */
    String getAlias();
}

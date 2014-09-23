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
package org.apache.sling.resourcemerger.api;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.resourcemerger.impl.MergedResourceConstants;

/**
 * Utility methods for merged resources.
 * @since 1.2
 */
public abstract class ResourceMergerUtil {

    /**
     * Returns <code>true</code> if the provided {@link Resource} is a merged resource.
     * If the resource is <code>null</code>, <code>false</code> is returned.
     * @param resource The resource
     * @return Returns <code>true</code> if the provided {@link Resource} is a merged resource.
     */
    public static boolean isMergedResource(final Resource resource) {
        if (resource == null) {
            return false;
        }

        return Boolean.TRUE.equals(resource.getResourceMetadata().get(MergedResourceConstants.METADATA_FLAG));
    }
}

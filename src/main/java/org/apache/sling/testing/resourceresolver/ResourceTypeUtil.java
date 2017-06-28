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
package org.apache.sling.testing.resourceresolver;

/**
 * Some helper methods for doing comparisons on resource types.
 * This class is private the resource resolver bundle.
 * Consumers should rely on {@link Resource#isResourceType(String)} or {@link ResourceResolver#isResourceType(Resource, String)} instead.
 */
class ResourceTypeUtil {

    /**
     * Returns <code>true</code> if the given resource type are equal.
     * 
     * In case the value of any of the given resource types 
     * starts with one of the resource resolver's search paths
     * it is converted to a relative resource type by stripping off 
     * the resource resolver's search path before doing the comparison.
     *
     * @param resourceType A resource type
     * @param anotherResourceType Another resource type to compare with {@link resourceType}.
     * @return <code>true</code> if the resource type equals the given resource type.
     */
    public static boolean areResourceTypesEqual(String resourceType, String anotherResourceType, String[] searchPath) {
        return relativizeResourceType(resourceType, searchPath).equals(relativizeResourceType(anotherResourceType, searchPath));
    }

    /**
     * Makes the given resource type relative by stripping off any prefix which equals one of the given search paths.
     * In case the given resource type does not start with any of the given search paths it is returned unmodified.
     * @param resourceType the resourceType to relativize.
     * @param searchPath the search paths to strip off from the given resource type.
     * @return the relative resource type
     */
    public static String relativizeResourceType(String resourceType, String[] searchPath) {
        if (resourceType.startsWith("/")) {
            for (String prefix : searchPath) {
                if (resourceType.startsWith(prefix)) {
                    return resourceType.substring(prefix.length());
                }
            }
        }
        return resourceType;
    }

}

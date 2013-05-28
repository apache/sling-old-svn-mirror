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

package org.apache.sling.resource.collection.impl.util;


import org.apache.sling.api.resource.Resource;

public class ResourceCollectionUtil {
	
	 /**
     * Create a unique name for a child of the <code>resource</code>. Generates a unique name and test if child
     * already exists. If name is already existing, iterate until a unique one is found
     *
     * @param resource parent resource
     * @param name the name to check
     * @return a unique label string
     */
    public static String createUniqueChildName(Resource resource, String name) {
        if (resource.getChild(name)!=null) {
            // leaf node already exists, create new unique name
            String leafNodeName;
            int i = 0;
            do {
                leafNodeName = name + String.valueOf(i);
                i++;
            } while (resource.getChild(leafNodeName)!=null);
            return leafNodeName;
        }
        return name;
    }

}

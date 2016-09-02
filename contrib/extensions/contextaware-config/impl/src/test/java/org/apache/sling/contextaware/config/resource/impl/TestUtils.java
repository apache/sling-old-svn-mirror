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
package org.apache.sling.contextaware.config.resource.impl;

import static org.junit.Assert.assertArrayEquals;

import java.util.Collection;
import java.util.Iterator;

import org.apache.sling.api.resource.Resource;

final class TestUtils {
    
    private TestUtils() {
        // static methods only
    }

    public static void assetResourcePaths(String[] expectedPaths, Collection<Resource> actualResources) {
        String[] actualPaths = new String[actualResources.size()];
        int i = 0;
        for (Iterator<Resource> it=actualResources.iterator(); it.hasNext(); i++) {
            actualPaths[i] = it.next().getPath();
        }
        assertArrayEquals(expectedPaths, actualPaths);
    }
    
}

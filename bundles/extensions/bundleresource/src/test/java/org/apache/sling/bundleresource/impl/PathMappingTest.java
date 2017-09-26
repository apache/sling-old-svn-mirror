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
package org.apache.sling.bundleresource.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PathMappingTest {

    @Test public void testSimpleRoot() {
        final PathMapping[] paths = PathMapping.getRoots("/libs/foo");
        assertEquals(1, paths.length);
        assertNull(paths[0].getEntryRoot());
        assertNull(paths[0].getEntryRootPrefix());
        assertEquals("/libs/foo", paths[0].getResourceRoot());
        assertEquals("/libs/foo/", paths[0].getResourceRootPrefix());
        assertNull(paths[0].getJSONPropertiesExtension());
    }

    @Test public void testSimpleRootWithJSON() {
        final PathMapping[] paths = PathMapping.getRoots("/libs/foo;" + PathMapping.DIR_JSON + ":=json");
        assertEquals(1, paths.length);
        assertNull(paths[0].getEntryRoot());
        assertNull(paths[0].getEntryRootPrefix());
        assertEquals("/libs/foo", paths[0].getResourceRoot());
        assertEquals("/libs/foo/", paths[0].getResourceRootPrefix());
        assertEquals(".json", paths[0].getJSONPropertiesExtension());
    }
}

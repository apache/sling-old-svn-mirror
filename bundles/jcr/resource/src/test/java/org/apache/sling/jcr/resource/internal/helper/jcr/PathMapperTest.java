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
package org.apache.sling.jcr.resource.internal.helper.jcr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import junitx.util.PrivateAccessor;

import org.junit.Test;

public class PathMapperTest {

    @Test public void mappingTest() throws Throwable {
        final Map<String, Object> config = new HashMap<String, Object>();
        config.put("path.mapping", new String[] {
                "/libs:/foo",
                "/hidden:.",
                "/deep/node:/deep/resource"
        });
        final PathMapper pm = new PathMapper();
        PrivateAccessor.invoke(pm, "activate", new Class[] {Map.class}, new Object[] {config});

        assertEquals("/", pm.mapResourcePathToJCRPath("/"));
        assertEquals("/", pm.mapJCRPathToResourcePath("/"));

        assertEquals("/unmapped", pm.mapResourcePathToJCRPath("/unmapped"));
        assertEquals("/unmapped", pm.mapJCRPathToResourcePath("/unmapped"));
        assertEquals("/unmapped/a", pm.mapResourcePathToJCRPath("/unmapped/a"));
        assertEquals("/unmapped/a", pm.mapJCRPathToResourcePath("/unmapped/a"));

        assertEquals("/libs", pm.mapResourcePathToJCRPath("/foo"));
        assertEquals("/foo", pm.mapJCRPathToResourcePath("/libs"));
        assertEquals("/foo1", pm.mapResourcePathToJCRPath("/foo1"));
        assertEquals("/libs1", pm.mapJCRPathToResourcePath("/libs1"));
        assertEquals("/libs/a", pm.mapResourcePathToJCRPath("/foo/a"));
        assertEquals("/foo/a", pm.mapJCRPathToResourcePath("/libs/a"));

        assertEquals("/deep/node", pm.mapResourcePathToJCRPath("/deep/resource"));
        assertEquals("/deep/resource", pm.mapJCRPathToResourcePath("/deep/node"));
        assertEquals("/deep/node/a", pm.mapResourcePathToJCRPath("/deep/resource/a"));
        assertEquals("/deep/resource/a", pm.mapJCRPathToResourcePath("/deep/node/a"));

        assertNull(pm.mapResourcePathToJCRPath("/hidden"));
        assertNull(pm.mapResourcePathToJCRPath("/hidden/a"));
        assertEquals("/hidden1", pm.mapResourcePathToJCRPath("/hidden1"));
        assertNull(pm.mapJCRPathToResourcePath("/hidden"));
        assertNull(pm.mapJCRPathToResourcePath("/hidden/a"));
        assertEquals("/hidden1", pm.mapJCRPathToResourcePath("/hidden1"));
    }
}

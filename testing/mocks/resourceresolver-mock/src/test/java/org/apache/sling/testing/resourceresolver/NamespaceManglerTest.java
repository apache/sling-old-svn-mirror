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

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class NamespaceManglerTest {
    
    private Map<String, String> TEST_PATHS = ImmutableMap.<String, String>builder()
            .put("/content/aa/bb/content.png", "/content/aa/bb/content.png")
            .put("/content/aa/bb/jcr:content.png", "/content/aa/bb/_jcr_content.png")
            .put("/content/aa/bb/jcr:content/anotherpath/xyz:abc", "/content/aa/bb/_jcr_content/anotherpath/_xyz_abc")
            .build();

    @Test
    public void testMangleNamespaces() throws Exception {
        for (Map.Entry<String, String> entry : TEST_PATHS.entrySet()) {
            assertEquals(entry.getValue(), NamespaceMangler.mangleNamespaces(entry.getKey()));
        }
    }

    @Test
    public void testUnmangleNamespaces() throws Exception {
        for (Map.Entry<String, String> entry : TEST_PATHS.entrySet()) {
            assertEquals(entry.getKey(), NamespaceMangler.unmangleNamespaces(entry.getValue()));
        }
    }

}

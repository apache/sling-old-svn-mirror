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
package org.apache.sling.distribution.resources.impl.common;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;

public class SimpleReadableResourceTestCase {

    @Test
    public void verifyWrongTypeConversion() {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        String resourcePath = "";
        Map<String, Object> properties = new HashMap<String, Object>();
        Object[] adapters = new Object[]{
            new String[]{ "hello Apache Sling" }
        };
        Resource resource = new SimpleReadableResource(resourceResolver, resourcePath, properties, adapters);

        String[] result = resource.adaptTo(String[].class);
        assertNotNull(result);
        assertTrue(result.length == 1);
        assertEquals( ((String[]) adapters[0])[0], result[0] );
    }

}

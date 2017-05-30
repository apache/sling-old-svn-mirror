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
package org.apache.sling.caconfig.resource.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ContextResourceTest {
    
    @Rule
    public SlingContext context = new SlingContext();
    
    private Resource resource1;
    private Resource resource2;
    
    @Before
    public void setUp() {
        resource1 = context.create().resource("/content/test1");
        resource2 = context.create().resource("/content/test2");
    }

    @Test
    public void testGetter() {
        ContextResource r1 = new ContextResource(resource1, "/conf/test", 20);
        assertEquals("/content/test1", r1.getResource().getPath());
        assertEquals("/conf/test", r1.getConfigRef());
        assertEquals(20, r1.getServiceRanking());
    }

    @Test
    public void testEquals() {
        assertTrue(new ContextResource(resource1, "/conf/test", 0).equals(new ContextResource(resource1, "/conf/test", 10)));
        assertTrue(new ContextResource(resource1, null, 0).equals(new ContextResource(resource1, null, 0)));
    }

    @Test
    public void testNotEquals() {
        assertFalse(new ContextResource(resource1, "/conf/test", 0).equals(new ContextResource(resource2, "/conf/test", 0)));
        assertFalse(new ContextResource(resource1, "/conf/test1", 0).equals(new ContextResource(resource1, "/conf/test2", 0)));
        assertFalse(new ContextResource(resource1, null, 0).equals(new ContextResource(resource1, "/conf/test", 0)));
    }

}

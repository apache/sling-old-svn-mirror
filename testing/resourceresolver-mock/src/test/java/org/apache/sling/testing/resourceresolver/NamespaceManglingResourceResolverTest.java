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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;

public class NamespaceManglingResourceResolverTest {

    private ResourceResolver resolver;
    
    @Before
    public void setUp() throws Exception {
        MockResourceResolverFactoryOptions options = new MockResourceResolverFactoryOptions();
        options.setMangleNamespacePrefixes(true);
        ResourceResolverFactory factory = new MockResourceResolverFactory(options);
        resolver = factory.getResourceResolver(null);
        
        Resource res1 = resolver.create(resolver.getResource("/"), "res1", ValueMap.EMPTY);
        Resource content = resolver.create(res1, "jcr:content", ValueMap.EMPTY);
        resolver.create(content, "res2", ValueMap.EMPTY);
    }
    
    @Test
    public void testMap() {
        assertEquals("/res1/_jcr_content/res2", resolver.map("/res1/jcr:content/res2"));
    }
    
    @Test
    public void testResolve() {
        assertEquals("/res1/jcr:content/res2", resolver.resolve("/res1/_jcr_content/res2").getPath());
    }
    
}

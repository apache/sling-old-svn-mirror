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
package org.apache.sling.servlet.resolver.helper;

import junit.framework.TestCase;

import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.servlet.resolver.mock.MockResource;
import org.apache.sling.servlet.resolver.mock.MockResourceResolver;
import org.apache.sling.servlet.resolver.mock.MockSlingHttpServletRequest;

public abstract class LocationTestBase extends TestCase {

    protected MockResourceResolver resourceResolver;

    protected MockSlingHttpServletRequest request;

    protected MockResource resource;

    protected String resourcePath;

    protected String resourceType;

    protected String resourceTypePath;

    protected String resourceSuperType;

    protected String resourceSuperTypePath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        resourceResolver = new MockResourceResolver();
        resourceResolver.setSearchPath("/apps", "/libs");

        resourceType = "foo:bar";
        resourceTypePath = JcrResourceUtil.resourceTypeToPath(resourceType);

        resourcePath = "/content/page";
        resource = new MockResource(resourceResolver, resourcePath,
            resourceType);
        resourceResolver.addResource(resource);

        request = new MockSlingHttpServletRequest(resourcePath, "print.a4",
            "html", null, null);
        request.setResourceResolver(resourceResolver);
        request.setResource(resource);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        resourceResolver = null;
        request = null;
        resource = null;
    }
    
}

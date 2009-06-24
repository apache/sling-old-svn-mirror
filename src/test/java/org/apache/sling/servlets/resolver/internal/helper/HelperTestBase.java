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
package org.apache.sling.servlets.resolver.internal.helper;

import junit.framework.TestCase;

import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;

public abstract class HelperTestBase extends TestCase {

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
        resourceTypePath = ResourceUtil.resourceTypeToPath(resourceType);

        resourcePath = "/content/page";
        resource = new MockResource(resourceResolver, resourcePath,
            resourceType);
        resourceResolver.addResource(resource);

        request = makeRequest("GET", "print.a4", "html");
    }

    protected MockSlingHttpServletRequest makeRequest(String method, String selectors, String extension) {
        final MockSlingHttpServletRequest result =
            new MockSlingHttpServletRequest(resourcePath, selectors, extension, null, null);
        result.setMethod(method);
        result.setResourceResolver(resourceResolver);
        result.setResource(resource);
        return result;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        resourceResolver = null;
        request = null;
        resource = null;
    }

}

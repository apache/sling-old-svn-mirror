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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;

public class ResourceCollectorGetCalculatePrefixWeightTest extends TestCase {

    private static final String LOCATION_NAME = "sample";

    private static final String LOCATION_PREFIX = LOCATION_NAME;

    private static final String LOCATION = "/apps/sling/" + LOCATION_NAME;

    private static final String SCRIPT_BASE = LOCATION + "/" + LOCATION_NAME;

    public void testCreateLocationResourceGET() {

        final MockResource res0 = new MockResource(null, SCRIPT_BASE, "foo:bar");

        // this resource has no extension, we require one
        final WeightedResource lr0 = createLocationResource(res0, "GET", null,
            "html");
        assertNull(lr0);

        // this resource has no extension, we require one
        final WeightedResource lr1 = createLocationResource(res0, "GET", null,
            "foo");
        assertNull(lr1);
    }

    public void testCreateLocationResourceGETEsp() {

        final MockResource res1 = new MockResource(null, SCRIPT_BASE + ".esp",
            "foo:bar");

        // allow simple name for GET and html
        final WeightedResource lr10 = createLocationResource(res1, "GET", null,
            "html");
        assertNotNull(lr10);
        assertEquals(0, lr10.getNumSelectors());
        assertEquals(WeightedResource.WEIGHT_PREFIX, lr10.getMethodPrefixWeight());

        // this resource has no extension, we require one
        final WeightedResource lr11 = createLocationResource(res1, "GET", null,
            "foo");
        assertNull(lr11);
    }

    public void testCreateLocationResourceGETHtmlEsp() {
        final MockResource res2 = new MockResource(null, SCRIPT_BASE
            + ".html.esp", "foo:bar");

        // allow simple name for GET and html
        final WeightedResource lr20 = createLocationResource(res2, "GET", null,
            "html");
        assertNotNull(lr20);
        assertEquals(0, lr20.getNumSelectors());
        assertEquals(WeightedResource.WEIGHT_PREFIX+WeightedResource.WEIGHT_EXTENSION, lr20.getMethodPrefixWeight());

        // script does not match for .foo request
        final WeightedResource lr21 = createLocationResource(res2, "GET", null,
            "foo");
        assertNull(lr21);
    }

    public void testCreateLocationResourceGETFooEsp() {
        final MockResource res3 = new MockResource(null, SCRIPT_BASE
            + ".foo.esp", "foo:bar");

        // script does not match for .html request
        final WeightedResource lr30 = createLocationResource(res3, "GET", null,
            "html");
        assertNull(lr30);

        // allow simple name for GET and foo
        final WeightedResource lr31 = createLocationResource(res3, "GET", null,
            "foo");
        assertNotNull(lr31);
        assertEquals(0, lr31.getNumSelectors());
        assertEquals(WeightedResource.WEIGHT_PREFIX+WeightedResource.WEIGHT_EXTENSION, lr31.getMethodPrefixWeight());
    }

    public void testCreateLocationResourceGETGET() {

        final MockResource res0 = new MockResource(null, SCRIPT_BASE + ".GET",
            "foo:bar");

        // side-effect: .GET is assumed to be the script extension
        final WeightedResource lr0 = createLocationResource(res0, "GET", null,
            "html");
        assertNotNull(lr0);
        assertEquals(WeightedResource.WEIGHT_PREFIX, lr0.getMethodPrefixWeight());

        // this resource has no extension, we require one
        final WeightedResource lr1 = createLocationResource(res0, "GET", null,
            "foo");
        assertNull(lr1);
    }

    public void testCreateLocationResourceGETGETEsp() {
        final MockResource res1 = new MockResource(null, SCRIPT_BASE
            + ".GET.esp", "foo:bar");

        // GET would be the extension and is not allowed like this
        final WeightedResource lr10 = createLocationResource(res1, "GET", null,
            "html");
        assertNull(lr10);

        // GET would be the extension and is not allowed like this
        final WeightedResource lr11 = createLocationResource(res1, "GET", null,
            "foo");
        assertNull(lr11);
    }

    private WeightedResource createLocationResource(
            final Resource scriptResource, final String method,
            final String selectorString, final String extension) {

        final SlingHttpServletRequest request = new MockSlingHttpServletRequest(
            "", selectorString, extension, null, null) {
            @Override
            public String getMethod() {
                return method;
            }
        };
        final ResourceCollectorGet locationUtil = new ResourceCollectorGet(request);
        final int methodPrefixWeight = locationUtil.calculatePrefixMethodWeight(scriptResource, LOCATION_PREFIX, true);
        if (methodPrefixWeight != ResourceCollectorGet.WEIGHT_NO_MATCH) {
            return new WeightedResource(0, scriptResource, 0, methodPrefixWeight);
        }
        
        return null;
    }
}

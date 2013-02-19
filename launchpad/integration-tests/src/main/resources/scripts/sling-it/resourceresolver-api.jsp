<%@page session="false" %><%
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
%><%@page import="org.junit.Assert,
                  org.apache.sling.launchpad.testservices.exported.FakeSlingHttpServletRequest,
                  org.apache.sling.api.resource.ResourceUtil,
                  org.apache.sling.api.resource.Resource,
                  org.apache.sling.api.resource.ResourceResolver"%><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%

    final ResourceResolver resResolver = slingRequest.getResourceResolver();

    // null resource is accessing /, which exists of course
    final Resource res00 = resResolver.resolve((String) null);
    Assert.assertNotNull(res00);
    Assert.assertEquals("Null path is expected to return root", "/",
            res00.getPath());

    // relative paths are treated as if absolute
    final String path01 = "relPath/relPath";
    final Resource res01 = resResolver.resolve(path01);
    Assert.assertNotNull(res01);
    Assert.assertEquals("Expecting absolute path for relative path", "/" + path01,
            res01.getPath());
    Assert.assertTrue("Resource must be NonExistingResource: " + res01.getClass().getName(),
            ResourceUtil.isNonExistingResource(res01));

    final String no_resource_path = "/no_resource/at/this/location";
    final Resource res02 = resResolver.resolve(no_resource_path);
    Assert.assertNotNull(res02);
    Assert.assertEquals("Expecting absolute path for relative path",
            no_resource_path, res02.getPath());
    Assert.assertTrue("Resource must be NonExistingResource",
            ResourceUtil.isNonExistingResource(res02));

        try {
            resResolver.resolve((HttpServletRequest) null);
            Assert.fail("Expected NullPointerException trying to resolve null request");
        } catch (NullPointerException npe) {
            // expected
        }

        final Resource res0 = resResolver.resolve(null, no_resource_path);
        Assert.assertNotNull("Expecting resource if resolution fails", res0);
        Assert.assertTrue("Resource must be NonExistingResource",
                ResourceUtil.isNonExistingResource(res0));
        Assert.assertEquals("Path must be the original path", no_resource_path,
            res0.getPath());

        final HttpServletRequest req1 = new FakeSlingHttpServletRequest(
            no_resource_path);
        final Resource res1 = resResolver.resolve(req1);
        Assert.assertNotNull("Expecting resource if resolution fails", res1);
        Assert.assertTrue("Resource must be NonExistingResource",
                ResourceUtil.isNonExistingResource(res1));
        Assert.assertEquals("Path must be the original path", no_resource_path,
            res1.getPath());

        final HttpServletRequest req2 = new FakeSlingHttpServletRequest(null);
        final Resource res2 = resResolver.resolve(req2);
        Assert.assertNotNull("Expecting resource if resolution fails", res2);
        Assert.assertFalse("Resource must not be NonExistingResource was ",
            ResourceUtil.isNonExistingResource(res2));
        Assert.assertEquals("Path must be the the root path", "/", res2.getPath());
%>TEST_PASSED
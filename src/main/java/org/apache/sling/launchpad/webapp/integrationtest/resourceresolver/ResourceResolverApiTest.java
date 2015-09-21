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
package org.apache.sling.launchpad.webapp.integrationtest.resourceresolver;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.launchpad.testservices.exported.FakeSlingHttpServletRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Various ResourceResolver API tests, converted to teleported tests from
 *  the previous resourceresolver-api.jsp script.
 */
public class ResourceResolverApiTest {
    private ResourceResolver resResolver;
    private final String no_resource_path = "/no_resource/at/this/location";
    
    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "Launchpad");

    @Before
    public void setup() throws LoginException {
        final ResourceResolverFactory rrf = teleporter.getService(ResourceResolverFactory.class);
        resResolver = rrf.getAdministrativeResourceResolver(null);
    }
    
    @After
    public void cleanup() throws LoginException {
        if(resResolver != null) {
            resResolver.close();
        }
    }
    
    @Test
    public void testNullResourceMapsToRoot() {
        // null resource is accessing /, which exists of course
        final Resource res00 = resResolver.resolve((String) null);
        Assert.assertNotNull(res00);
        Assert.assertEquals("Null path is expected to return root", "/",
                res00.getPath());
    }
    
    @Test
    public void testRelativePathsA() {
        // relative paths are treated as if absolute
        final String path01 = "relPath/relPath";
        final Resource res01 = resResolver.resolve(path01);
        Assert.assertNotNull(res01);
        Assert.assertEquals("Expecting absolute path for relative path", "/" + path01,
                res01.getPath());
        Assert.assertTrue("Resource must be NonExistingResource: " + res01.getClass().getName(),
                ResourceUtil.isNonExistingResource(res01));

    }
    
    @Test
    public void testRelativePathsB() {
        final Resource res02 = resResolver.resolve(no_resource_path);
        Assert.assertNotNull(res02);
        Assert.assertEquals("Expecting absolute path for relative path",
                no_resource_path, res02.getPath());
        Assert.assertTrue("Resource must be NonExistingResource",
                ResourceUtil.isNonExistingResource(res02));
    }
    
    @SuppressWarnings("deprecation")
    @Test(expected=NullPointerException.class)
    public void testNullRequest() {
        resResolver.resolve((HttpServletRequest) null);
    }
    
    @Test
    public void testResolutionFailsA() {
        final Resource res0 = resResolver.resolve(null, no_resource_path);
        Assert.assertNotNull("Expecting resource if resolution fails", res0);
        Assert.assertTrue("Resource must be NonExistingResource",
                ResourceUtil.isNonExistingResource(res0));
        Assert.assertEquals("Path must be the original path", no_resource_path,
            res0.getPath());
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void testResolutionFailsB() {
        final HttpServletRequest req1 = new FakeSlingHttpServletRequest(
                no_resource_path);
            final Resource res1 = resResolver.resolve(req1);
            Assert.assertNotNull("Expecting resource if resolution fails", res1);
            Assert.assertTrue("Resource must be NonExistingResource",
                    ResourceUtil.isNonExistingResource(res1));
            Assert.assertEquals("Path must be the original path", no_resource_path,
                res1.getPath());
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void testResolutionFailsC() {
        final HttpServletRequest req2 = new FakeSlingHttpServletRequest(null);
        final Resource res2 = resResolver.resolve(req2);
        Assert.assertNotNull("Expecting resource if resolution fails", res2);
        Assert.assertFalse("Resource must not be NonExistingResource was ",
            ResourceUtil.isNonExistingResource(res2));
        Assert.assertEquals("Path must be the the root path", "/", res2.getPath());
    }
    
    @Test
    public void testDelete() throws PersistenceException {
        final String nodeName = "node-" + UUID.randomUUID();
        final String nodePath = "/" + nodeName;
        resResolver.create(resResolver.getResource("/"), nodeName, null);
        Assert.assertEquals(nodePath, resResolver.getResource(nodePath).getPath());
        resResolver.delete(resResolver.getResource(nodePath));
        resResolver.commit();
        Assert.assertNull(resResolver.getResource(nodePath));
    }
}
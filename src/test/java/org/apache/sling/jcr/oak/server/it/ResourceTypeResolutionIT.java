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
package org.apache.sling.jcr.oak.server.it;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@RunWith(PaxExam.class)
public class ResourceTypeResolutionIT extends OakServerTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceTypeResolutionIT.class);

    @Test
    public void checkResourceType() throws Exception {
        JackrabbitSession adminSession = (JackrabbitSession)slingRepository.loginAdministrative(null);
        Node contentBar = JcrUtils.getOrCreateByPath("/content/foo/bar", "nt:unstructured", adminSession);
        contentBar.setProperty("sling:resourceType", "types/foo/bar");

        Node appsBar = JcrUtils.getOrCreateByPath("/apps/types/foo/bar", "nt:unstructured", adminSession);
        appsBar.setProperty("sling:resourceSuperType", "types/foo/parent");

        JcrUtils.getOrCreateByPath("/apps/types/foo/parent", "nt:unstructured", adminSession);
        adminSession.getUserManager().createUser("test-user", "test");
        adminSession.save();
        AccessControlUtils.allow(contentBar, "test-user", "jcr:read");
        adminSession.save();
        adminSession.logout();

        HashMap<String, Object> authenticationInfo = new HashMap<>();
        authenticationInfo.put(ResourceResolverFactory.USER, "test-user");
        authenticationInfo.put(ResourceResolverFactory.PASSWORD, "test".toCharArray());

        ResourceResolver testResolver = resourceResolverFactory.getResourceResolver(authenticationInfo);
        try {
            Resource resource = testResolver.getResource("/content/foo/bar");
            assertNotNull(resource);
            assertEquals("/content/foo/bar", resource.getPath());
            assertTrue(resource.isResourceType("types/foo/bar"));

            // this assertion causes the private ResourceResolverControl#getResourceTypeResourceResolver
            // to be called, which needs to inject the resourceresolver bundle via the authenticationInfo
            // see SLING-6329
            assertTrue(resource.isResourceType("types/foo/parent"));
        } finally {
            testResolver.close();
        }
    }
}

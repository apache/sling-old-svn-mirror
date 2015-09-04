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
package org.apache.sling.testing.mock.sling.oak.contentimport;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.loader.AbstractContentLoaderJsonTest;

public class ContentLoaderJsonTest extends AbstractContentLoaderJsonTest {

    @Override
    protected ResourceResolverType getResourceResolverType() {
        return ResourceResolverType.JCR_OAK;
    }

    @Override
    protected ResourceResolver newResourceResolver() {
        ResourceResolver resolver = MockSling.newResourceResolver(getResourceResolverType());

        // register sling and app node types
        try {
            Session session = resolver.adaptTo(Session.class);
            RepositoryUtil.registerSlingNodeTypes(session);
            RepositoryUtil.registerNodeType(session,
                    ContentLoaderJsonTest.class.getResourceAsStream("/SLING-INF/nodetypes/app.cnd"));
        } catch (IOException ex) {
            throw new RuntimeException("Unable to register sling node types.", ex);
        } catch (RepositoryException ex) {
            throw new RuntimeException("Unable to register sling node types.", ex);
        }

        return resolver;
    }

}

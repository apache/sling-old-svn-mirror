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
package org.apache.sling.testing.mock.sling.oak.resource;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.resource.AbstractMultipleResourceResolverTest;
import org.junit.Ignore;

//TEST IS DISABLED currently, it does not work with jackrabbit repository yet
@Ignore
public class MultipleResourceResolverTest extends AbstractMultipleResourceResolverTest {

    @Override
    protected ResourceResolverType getResourceResolverType() {
        return ResourceResolverType.JCR_OAK;
    }

    @Override
    protected ResourceResolverFactory newResourceResolerFactory() {
        ResourceResolverFactory factory = MockSling.newResourceResolverFactory(getResourceResolverType());

        // register sling node types
        try {
            ResourceResolver resolver = factory.getResourceResolver(null);
            RepositoryUtil.registerSlingNodeTypes(resolver.adaptTo(Session.class));
        } catch (LoginException ex) {
            throw new RuntimeException("Unable to register sling node types.", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to register sling node types.", ex);
        } catch (RepositoryException ex) {
            throw new RuntimeException("Unable to register sling node types.", ex);
        }

        return factory;
    }

}

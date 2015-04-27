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
package org.apache.sling.testing.mock.sling;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.internal.helper.jcr.PathMapper;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.osgi.framework.BundleContext;

public class MockResolverProvider {
    private MockResolverProvider() {
    }
    
    public static ResourceResolver getResourceResolver() throws RepositoryException, LoginException {
        final SlingRepository repository = RepositoryProvider.instance().getRepository();
        final BundleContext bundleContext = MockOsgi.newBundleContext();
        bundleContext.registerService(PathMapper.class.getName(), new PathMapper(), null);
        return new MockJcrResourceResolverFactory(repository, bundleContext).getAdministrativeResourceResolver(null);
    }
}

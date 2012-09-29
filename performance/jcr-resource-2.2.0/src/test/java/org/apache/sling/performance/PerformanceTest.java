/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.performance;

import static org.mockito.Mockito.mock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Session;
import junitx.util.PrivateAccessor;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.QueriableResourceProvider;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderFactory;
import org.apache.sling.performance.annotation.PerformanceTestSuite;
import org.apache.sling.performance.tests.ResolveNonExistingWith10000AliasTest;
import org.apache.sling.performance.tests.ResolveNonExistingWith10000VanityPathTest;
import org.apache.sling.performance.tests.ResolveNonExistingWith1000AliasTest;
import org.apache.sling.performance.tests.ResolveNonExistingWith1000VanityPathTest;
import org.apache.sling.performance.tests.ResolveNonExistingWith5000AliasTest;
import org.apache.sling.performance.tests.ResolveNonExistingWith5000VanityPathTest;
import org.apache.sling.resourceresolver.impl.ResourceResolverFactoryActivator;
import org.apache.sling.resourceresolver.impl.ResourceResolverFactoryImpl;
import org.apache.sling.resourceresolver.impl.mapping.MapEntries;
import org.apache.sling.resourceresolver.impl.mapping.Mapping;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.EventAdmin;

@RunWith(PerformanceRunner.class)
public class PerformanceTest {

    private class Helper implements TestHelper {

        private MapEntries mapEntries;

        private ResourceResolver resourceResolver;
        
        public void dispose() {
             mapEntries.dispose();
        }

        public ResourceResolver getResourceResolver() {
            return resourceResolver;
        }

        public void init(String rootPath, Session session, SlingRepository repository) throws Exception {
            ResourceResolverFactoryActivator activator = new ResourceResolverFactoryActivator();

            JcrResourceProviderFactory providerFactory = new JcrResourceProviderFactory();
            PrivateAccessor.setField(providerFactory, "repository", repository);

            Map<String, Object> props = new HashMap<String, Object>();
            props.put(Constants.SERVICE_ID, -1l);
            props.put(ResourceProviderFactory.PROPERTY_REQUIRED, true);
            props.put(ResourceProvider.ROOTS, "/");
            props.put(QueriableResourceProvider.LANGUAGES, new String[] { "xpath", "sql" });

            try {
                PrivateAccessor.invoke(activator, "bindResourceProviderFactory", new Class[] { ResourceProviderFactory.class,
                        Map.class }, new Object[] { providerFactory, props });
            } catch (Throwable e) {
                throw new Exception(e);
            }

            // setup mappings
            PrivateAccessor.setField(activator, "mappings", new Mapping[] { new Mapping("/-/"), new Mapping(rootPath + "/-/") });

            // ensure namespace mangling
            PrivateAccessor.setField(activator, "mangleNamespacePrefixes", true);

            // setup mapping root
            PrivateAccessor.setField(activator, "mapRoot", "/etc/map");
            ResourceResolverFactoryImpl resFac = new ResourceResolverFactoryImpl(activator);

            mapEntries = new MapEntries(resFac, mock(BundleContext.class), mock(EventAdmin.class));
            PrivateAccessor.setField(resFac, "mapEntries", mapEntries);

            try {
                NamespaceRegistry nsr = session.getWorkspace().getNamespaceRegistry();
                nsr.registerNamespace(SlingConstants.NAMESPACE_PREFIX, JcrResourceConstants.SLING_NAMESPACE_URI);
            } catch (Exception e) {
                // don't care for now
            }

            Map<String, Object> authInfo = Collections.<String, Object> singletonMap(
                    JcrResourceConstants.AUTHENTICATION_INFO_SESSION, session);
            resourceResolver = resFac.getResourceResolver(authInfo);
        }
    }
    
    @PerformanceTestSuite
    public ParameterizedTestList testPerformance() throws Exception {
        Helper helper = new Helper();
        
        ParameterizedTestList testCenter = new ParameterizedTestList();
        testCenter.setTestSuiteTitle("jcr.resource-2.2.0");
        testCenter.addTestObject(new ResolveNonExistingWith1000VanityPathTest(helper));
        testCenter.addTestObject(new ResolveNonExistingWith5000VanityPathTest(helper));
        testCenter.addTestObject(new ResolveNonExistingWith10000VanityPathTest(helper));
        //tests.add(new ResolveNonExistingWith30000VanityPathTest(helper));
        testCenter.addTestObject(new ResolveNonExistingWith1000AliasTest(helper));
        testCenter.addTestObject(new ResolveNonExistingWith5000AliasTest(helper));
        testCenter.addTestObject(new ResolveNonExistingWith10000AliasTest(helper));
        //tests.add(new ResolveNonExistingWith30000AliasTest(helper));
        
        return testCenter;
    }
}

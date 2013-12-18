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

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Session;
import junitx.util.PrivateAccessor;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl;
import org.apache.sling.jcr.resource.internal.helper.MapEntries;
import org.apache.sling.jcr.resource.internal.helper.Mapping;
import org.apache.sling.performance.annotation.PerformanceTestSuite;
import org.apache.sling.performance.tests.ResolveNonExistingWithManyAliasTest;
import org.apache.sling.performance.tests.ResolveNonExistingWithManyVanityPathTest;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;

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
            JcrResourceResolverFactoryImpl resFac = new JcrResourceResolverFactoryImpl();

            PrivateAccessor.setField(resFac, "repository", repository);

            // setup mappings
            PrivateAccessor.setField(resFac, "mappings",
                    new Mapping[] { new Mapping("/-/"), new Mapping(rootPath + "/-/") });

            // ensure namespace mangling
            PrivateAccessor.setField(resFac, "mangleNamespacePrefixes", true);

            // setup mapping root
            PrivateAccessor.setField(resFac, "mapRoot", "/etc/map");
            
            final EventAdmin mockVoidEA = new EventAdmin() {

                public void postEvent(Event event) {
                    // nothing to do
                }

                public void sendEvent(Event event) {
                    // nothing to do
                }
            };
            final ServiceTracker voidTracker = mock(ServiceTracker.class);
            when(voidTracker.getService()).thenReturn(mockVoidEA);

            mapEntries = new MapEntries(resFac, mock(BundleContext.class), voidTracker);
            PrivateAccessor.setField(resFac, "mapEntries", mapEntries);

            try {
                NamespaceRegistry nsr = session.getWorkspace().getNamespaceRegistry();
                nsr.registerNamespace(SlingConstants.NAMESPACE_PREFIX, JcrResourceConstants.SLING_NAMESPACE_URI);
            } catch (Exception e) {
                // don't care for now
            }

            PrivateAccessor.setField(resFac, "useMultiWorkspaces", true);
            
            resourceResolver = resFac.getResourceResolver(session);
        }
    }
    
    @PerformanceTestSuite
    public ParameterizedTestList testPerformance() throws Exception {
        Helper helper = new Helper();
        
        ParameterizedTestList testCenter = new ParameterizedTestList();
        testCenter.addTestObject(new ResolveNonExistingWithManyVanityPathTest("ResolveNonExistingWith1000VanityPathTest",helper, 100, 10));
        testCenter.addTestObject(new ResolveNonExistingWithManyVanityPathTest("ResolveNonExistingWith5000VanityPathTest",helper, 100, 50));
        testCenter.addTestObject(new ResolveNonExistingWithManyVanityPathTest("ResolveNonExistingWith10000VanityPathTest",helper, 100, 100));
        
        testCenter.addTestObject(new ResolveNonExistingWithManyAliasTest("ResolveNonExistingWithManyAliasTest",helper, 1000));
        testCenter.addTestObject(new ResolveNonExistingWithManyAliasTest("ResolveNonExistingWith5000AliasTest",helper, 5000));
        testCenter.addTestObject(new ResolveNonExistingWithManyAliasTest("ResolveNonExistingWith10000AliasTest",helper, 10000));
                
        return testCenter;
    }
}

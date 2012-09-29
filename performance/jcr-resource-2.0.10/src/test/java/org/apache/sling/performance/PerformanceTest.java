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
import org.apache.sling.performance.tests.ResolveNonExistingWith10000AliasTest;
import org.apache.sling.performance.tests.ResolveNonExistingWith10000VanityPathTest;
import org.apache.sling.performance.tests.ResolveNonExistingWith1000AliasTest;
import org.apache.sling.performance.tests.ResolveNonExistingWith1000VanityPathTest;
import org.apache.sling.performance.tests.ResolveNonExistingWith5000AliasTest;
import org.apache.sling.performance.tests.ResolveNonExistingWith5000VanityPathTest;
import org.junit.runner.RunWith;


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

            this.mapEntries = new MapEntries(resFac, repository);
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
        testCenter.setTestSuiteTitle("jcr.resource-2.0.10");
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

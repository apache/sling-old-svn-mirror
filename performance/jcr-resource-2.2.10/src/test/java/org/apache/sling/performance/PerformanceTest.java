package org.apache.sling.performance;
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
import org.apache.sling.performance.tests.ResolveNonExistingWithManyAliasTest;
import org.apache.sling.performance.tests.ResolveNonExistingWithManyVanityPathTest;
import org.apache.sling.performance.tests.StartupWithManyAliasTest;
import org.apache.sling.performance.tests.StartupWithManyVanityTest;
import org.apache.sling.resourceresolver.impl.CommonResourceResolverFactoryImpl;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
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
            
            ResourceAccessSecurityTracker rast = new ResourceAccessSecurityTracker();
            PrivateAccessor.setField(activator, "resourceAccessSecurityTracker",rast);
            

            CommonResourceResolverFactoryImpl commonFactory = new CommonResourceResolverFactoryImpl(activator);
            
            // setup mapping root
            PrivateAccessor.setField(activator, "mapRoot", "/etc/map");
            ResourceResolverFactoryImpl resFac = new ResourceResolverFactoryImpl(commonFactory, /* TODO: using Bundle */ null, null);
             
            mapEntries = new MapEntries(commonFactory, mock(BundleContext.class), mock(EventAdmin.class));
            PrivateAccessor.setField(commonFactory, "mapEntries", mapEntries);

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
        testCenter.setTestSuiteTitle("jcr.resource-2.2.10");
 
        testCenter.addTestObject(new ResolveNonExistingWithManyVanityPathTest("ResolveNonExistingWith1000VanityPathTest",helper, 100, 10));
        testCenter.addTestObject(new ResolveNonExistingWithManyVanityPathTest("ResolveNonExistingWith5000VanityPathTest",helper, 100, 50));
        testCenter.addTestObject(new ResolveNonExistingWithManyVanityPathTest("ResolveNonExistingWith10000VanityPathTest",helper, 100, 100));
        testCenter.addTestObject(new ResolveNonExistingWithManyAliasTest("ResolveNonExistingWith100AliasTest",helper, 100));
        testCenter.addTestObject(new ResolveNonExistingWithManyAliasTest("ResolveNonExistingWith1000AliasTest",helper, 1000));
        testCenter.addTestObject(new ResolveNonExistingWithManyAliasTest("ResolveNonExistingWith5000AliasTest",helper, 5000));
        testCenter.addTestObject(new ResolveNonExistingWithManyAliasTest("ResolveNonExistingWith10000AliasTest",helper, 10000));
        
        testCenter.addTestObject(new StartupWithManyAliasTest("StartupWithManyAliasTest",helper, 10000));
        testCenter.addTestObject(new StartupWithManyVanityTest("StartupWith10VanityTest",helper, 1, 10));
        testCenter.addTestObject(new StartupWithManyVanityTest("StartupWith100ManyVanityTest",helper, 10, 10));
        testCenter.addTestObject(new StartupWithManyVanityTest("StartupWith1000ManyVanityTest",helper, 10, 100));
        testCenter.addTestObject(new StartupWithManyVanityTest("StartupWith10000ManyVanityTest",helper, 100, 100));
        
        
        return testCenter;
    }
}

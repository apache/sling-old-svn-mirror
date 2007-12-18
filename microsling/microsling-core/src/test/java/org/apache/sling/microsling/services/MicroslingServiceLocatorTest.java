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
package org.apache.sling.microsling.services;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.services.InvalidServiceFilterSyntaxException;
import org.apache.sling.api.services.ServiceLocator;
import org.apache.sling.api.services.ServiceNotAvailableException;

public class MicroslingServiceLocatorTest extends TestCase {
    private ServiceLocator serviceLocator;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        MicroslingServiceLocator sl = new MicroslingServiceLocator();
        sl.registerService(ResourceResolver.class, new ResourceResolver() {

            public Iterator<Resource> findResources(String query,
                    String language) {
                return null;
            }

            public Resource getResource(String path) {
                return null;
            }

            public Resource getResource(Resource base, String path) {
                return null;
            }

            public Iterator<Resource> listChildren(Resource parent) {
                return null;
            }

            public Iterator<Map<String, Object>> queryResources(String query,
                    String language) {
                return null;
            }

            public Resource resolve(HttpServletRequest request) {
                return null;
            }
            
            public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
                return null;
            }

        });
        serviceLocator = sl;
    }

    public void testServiceFound() {
        final Object svc = serviceLocator.getService(ResourceResolver.class);
        assertNotNull(svc);
        assertTrue(svc instanceof ResourceResolver);
    }

    public void testServiceNotFound() {
        final Object svc = serviceLocator.getService(Iterator.class);
        assertNull(svc);
    }

    public void testRequiredServiceFound() throws ServiceNotAvailableException {
        final Object svc = serviceLocator.getRequiredService(ResourceResolver.class);
        assertNotNull(svc);
        assertTrue(svc instanceof ResourceResolver);
    }

    public void testRequiredServiceNotFound() {
        try {
            serviceLocator.getRequiredService(Iterator.class);
            fail("Expected Exception when service is not found");
        } catch (ServiceNotAvailableException sna) {
            // fine - as expected
        }
    }

    public void testGetServices() throws InvalidServiceFilterSyntaxException {
        final Object[] svc = serviceLocator.getServices(ResourceResolver.class,
            null);
        assertNotNull(svc);
        assertEquals(1, svc.length);
        assertTrue(svc[0] instanceof ResourceResolver);
    }
}

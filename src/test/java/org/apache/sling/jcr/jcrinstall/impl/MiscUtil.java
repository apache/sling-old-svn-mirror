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
package org.apache.sling.jcr.jcrinstall.impl;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

/** JcrInstall test utilities */
class MiscUtil {

    static final Mockery mockery = new Mockery();
    
    public static String SEARCH_PATHS [] = { "/libs/", "/apps/" };
    public static String RUN_MODES [] = { "dev", "staging" };
    
    static class MockResourceResolver implements ResourceResolver {

        public Iterator<Resource> findResources(String arg0, String arg1) {
            return null;
        }

        public Resource getResource(Resource arg0, String arg1) {
            return null;
        }

        public Resource getResource(String arg0) {
            return null;
        }

        public String[] getSearchPath() {
            return SEARCH_PATHS;
        }

        public Iterator<Resource> listChildren(Resource arg0) {
            return null;
        }

        public String map(String arg0) {
            return null;
        }

        public Iterator<Map<String, Object>> queryResources(String arg0,
                String arg1) {
            return null;
        }

        public Resource resolve(HttpServletRequest arg0) {
            return null;
        }

        public Resource resolve(String arg0) {
            return null;
        }

        public <AdapterType> AdapterType adaptTo(Class<AdapterType> arg0) {
            return null;
        }
    }
    
    static class MockJcrResourceResolverFactory implements JcrResourceResolverFactory {

        public ResourceResolver getResourceResolver(Session arg0) {
            return new MockResourceResolver();
        }
    }
    
    /** Set a non-public Field */
    static void setField(Object target, String fieldName, Object value) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
    
    /** Return a JcrInstaller setup for testing */ 
    static JcrInstaller getJcrInstaller(SlingRepository repository, OsgiInstaller osgiInstaller) throws Exception {
        final JcrInstaller installer = new JcrInstaller();
        setField(installer, "repository", repository);
        setField(installer, "resourceResolverFactory", new MockJcrResourceResolverFactory());
        setField(installer, "installer", osgiInstaller);
        setField(installer, "runMode", new MockRunMode(RUN_MODES));

        installer.activate(getMockComponentContext());
        return installer;
    }
    
    static ComponentContext getMockComponentContext() {
        // Setup fake ComponentContext to allow JcrInstaller to start
        final ComponentContext cc = mockery.mock(ComponentContext.class);
        final BundleContext bc = mockery.mock(BundleContext.class);
        
        final Dictionary<String, Object> emptyDict = new Hashtable<String, Object>();
        mockery.checking(new Expectations() {{
            allowing(cc).getProperties();
            will(returnValue(emptyDict));
            allowing(cc).getBundleContext();
            will(returnValue(bc));
            allowing(bc).getProperty(with(any(String.class)));
            will(returnValue(null));
        }});
        return cc;
    }
    
    static void waitForCycles(JcrInstaller installer, int nCycles, long timeoutMsec) throws Exception {
        final Field f = installer.getClass().getDeclaredField("cyclesCount");
        f.setAccessible(true);
        final int endCycles = ((Integer)f.get(installer)).intValue() + nCycles;
        final long endTime = System.currentTimeMillis() + timeoutMsec;
        while(System.currentTimeMillis() < endTime) {
            if(((Integer)f.get(installer)).intValue() > endCycles) {
                return;
            }
        }
        throw new Exception("did not get " + nCycles + " installer cycles in " + timeoutMsec + " msec"); 
    }
    
    /** Get the WatchedFolders of supplied JcrInstaller */
    @SuppressWarnings({ "unchecked"})
    static Collection<WatchedFolder> getWatchedFolders(JcrInstaller installer) throws Exception {
        final Field f = installer.getClass().getDeclaredField("watchedFolders");
        f.setAccessible(true);
        return (Collection<WatchedFolder>)f.get(installer);
    }
}

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
package org.apache.sling.jcr.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.commons.osgi.SortingServiceTracker;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.api.SlingRepositoryInitializer;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/** Test the SlingRepositoryInitializer mechanism */
public class RepositoryInitializersTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);
    
    private static final String DEFAULT_WORKSPACE = "defws";
    private AbstractSlingRepositoryManager asrm;
    private Repository repository;
    private Session session;
    private SortingServiceTracker<SlingRepository> repoTracker;
    
    /** We initially get a SlingRepository service from the Sling Mocks, and we're
     *  interested to find out whether the repository manager also registers one
     *  in our tests.
     */
    private int initialRepoServicesCount;
    
    @Before
    public void setup() throws RepositoryException {
        repoTracker = new SortingServiceTracker<SlingRepository>(context.bundleContext(), SlingRepository.class.getName());
        repoTracker.open();
        repository = MockJcr.newRepository();
        session = repository.login();
        asrm = new MockSlingRepositoryManager(repository);
        
        initialRepoServicesCount = countRepositoryServices();
        assertAdditionalRepositoryServices(0);
    }
    
    @After
    public void cleanup() {
        repoTracker.close();
        asrm.stop();
        session.logout();
    }
    
    private void registerInitializer(String id, int serviceRanking) {
        final SlingRepositoryInitializer init = new TestInitializer(id);
        final Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_RANKING, new Integer(serviceRanking));
        context.bundleContext().registerService(SlingRepositoryInitializer.class.getName(), init, props);
    }
    
    private void assertTestInitializerProperty(String expected) throws RepositoryException {
        final String path = TestInitializer.getPropertyPath();
        assertTrue("Expecting property at " + path, session.propertyExists(path));
        assertEquals("Expecting correct value at " + path, expected, session.getProperty(path).getString());
    }
    
    private int countRepositoryServices() {
        final ServiceReference [] refs = repoTracker.getServiceReferences();
        return (refs == null ? 0 : refs.length);
    }
    
    private void assertAdditionalRepositoryServices(int expected) {
        assertEquals("Expecting " + expected + " SlingRepository services", 
                expected + initialRepoServicesCount, countRepositoryServices());
    }
    
    private void assertStart(boolean expected) {
        final boolean actual = asrm.start(context.bundleContext(), DEFAULT_WORKSPACE, false);
        assertEquals("Expecting start to return " + expected, expected, actual);
    }
    
    @Test
    public void inOrderInitializers() throws RepositoryException {
        for(int i=1; i < 4; i++) {
            registerInitializer(String.valueOf(i), i * 100);
        }
        
        assertStart(true);
        
        // TODO this should really be 1,2,3 but the Sling OSGi mocks sort 
        // in the wrong order w.r.t service ranking, see SLING-5462,
        // will be fixed in osgi-mock 2.0.2
        assertTestInitializerProperty("3,2,1,");
        assertAdditionalRepositoryServices(1);
    }
    
    @Test
    public void reverseOrderInitializers() throws RepositoryException {
        for(int i=1; i < 4; i++) {
            registerInitializer(String.valueOf(i), i * -100);
        }
        
        assertStart(true);
        
        // TODO see comment in inOrderInitializers, this should really
        // be 3,2,1, caused by SLING-5462
        assertTestInitializerProperty("1,2,3,");
        assertAdditionalRepositoryServices(1);
    }
    
    @Test
    public void noRepositoryOnException() throws RepositoryException {
        registerInitializer("a", 1);
        registerInitializer("EXCEPTION", 2);
        registerInitializer("c", 3);

        assertStart(false);
        
        // The repository manager does not register a service in this case 
        assertAdditionalRepositoryServices(0);
    }
    
    @Test
    public void noRepositoryOnError() throws RepositoryException {
        registerInitializer("a", 1);
        registerInitializer("ERROR", 2);
        registerInitializer("c", 3);
        
        assertStart(false);
        
        // The repository manager does not register a service in this case 
        assertAdditionalRepositoryServices(0);
    }
}

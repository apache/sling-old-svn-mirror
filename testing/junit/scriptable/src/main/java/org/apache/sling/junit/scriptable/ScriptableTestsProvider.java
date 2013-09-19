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
package org.apache.sling.junit.scriptable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.apache.sling.junit.TestsProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** TestsProvider that provides test classes for repository
 *  nodes that have a sling:Test mixin.
 */
@Component
@Service
public class ScriptableTestsProvider implements TestsProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private String pid;
    private Session session;
    private ResourceResolver resolver;
    private long lastModified = System.currentTimeMillis();
    private long lastReloaded;
    
    /** List of resource paths that point to tests */
    private static List<String> testPaths = new LinkedList<String>();
    
    public static final String SLING_TEST_NODETYPE = "sling:Test";
    public static final String TEST_CLASS_NAME = ScriptableTestsProvider.class.getName();
    
    /** Context that's passed to TestAllPaths */
    static class TestContext {
        final List<String> testPaths;
        final SlingRequestProcessor requestProcessor;
        final ResourceResolver resourceResolver;

        TestContext(List<String> p, SlingRequestProcessor rp, ResourceResolver rr) {
            testPaths = p;
            requestProcessor = rp;
            resourceResolver = rr;
        }
    }
    
    /** Need a ThreadLocal to pass context, as it's JUnit who instantiates the
     *  test classes, we can't easily decorate them (AFAIK).
     */
    static final ThreadLocal<TestContext> testContext = new ThreadLocal<TestContext>(); 
    
    /** We only consider test resources under the search path
     *  of the JCR resource resolver. These paths are supposed 
     *  to be secured, as they contain other admin stuff anyway, 
     *  so non-admin users are prevented from creating test nodes. 
     */
    private String[] allowedRoots;
    
    @Reference
    private SlingRepository repository;
    
    @Reference
    private SlingRequestProcessor requestProcessor;
    
    @Reference
    private JcrResourceResolverFactory resolverFactory;
    
    // Need one listener per root path
    private List<EventListener> listeners = new ArrayList<EventListener>();
    
    class RootListener implements EventListener {
        private final String path;
        
        RootListener(String path) {
            this.path = path;
        }
        
        public void onEvent(EventIterator it) {
            log.debug("Change detected under {}, will reload list of test paths", path);
            lastModified = System.currentTimeMillis();
        }
    };
    
    protected void activate(ComponentContext ctx) throws Exception {
        pid = (String)ctx.getProperties().get(Constants.SERVICE_PID);
        session = repository.loginAdministrative(repository.getDefaultWorkspace());
        resolver = resolverFactory.getResourceResolver(session);
        
        // Copy resource resolver paths and make sure they end with a /
        final String [] paths = resolver.getSearchPath();
        allowedRoots = new String[paths.length];
        System.arraycopy(paths, 0, allowedRoots, 0, paths.length);
        for(int i=0; i < allowedRoots.length; i++) {
            if(!allowedRoots[i].endsWith("/")) {
                allowedRoots[i] += "/";
            }
        }
        
        // Listen to changes to sling:Test nodes under allowed roots
        final int eventTypes = 
            Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
        final boolean isDeep = true;
        final boolean noLocal = true;
        final String [] nodeTypes = { SLING_TEST_NODETYPE };
        final String [] uuid = null;
        for(String path : allowedRoots) {
            final EventListener listener = new RootListener(path);
            listeners.add(listener);
            session.getWorkspace().getObservationManager().addEventListener(listener, eventTypes, path, isDeep, uuid, nodeTypes, noLocal);
            log.debug("Listening for JCR events under {}", path);
            
        }
        
        log.info("Activated, will look for test resources under {}", Arrays.asList(allowedRoots));
    }
    
    protected void deactivate(ComponentContext ctx) throws RepositoryException {
        resolver = null;
        if(session != null) {
            for(EventListener listener : listeners) {
                session.getWorkspace().getObservationManager().removeEventListener(listener);
            }
            listeners.clear();
            session.logout();
        }
        session = null;
    }
    
    public Class<?> createTestClass(String testName) throws ClassNotFoundException {
        if(!testName.equals(TEST_CLASS_NAME)) {
            throw new ClassNotFoundException(testName + " - the only valid name is " + TEST_CLASS_NAME);
        }
        
        try {
            maybeQueryTestResources();
        } catch(Exception e) {
            throw new RuntimeException("Exception in maybeQueryTestResources()", e);
        }
        
        if(testPaths.size() == 0) {
            return ExplainTests.class;
        } else {
            testContext.set(new TestContext(testPaths, requestProcessor, resolver));
            return TestAllPaths.class;
        }
    }

    public String getServicePid() {
        return pid;
    }

    public List<String> getTestNames() {
        // We have a single test to run, would be better to have one
        // test class per test resource but that looks harder. Maybe
        // use the Sling compiler to generate test classes? 
        final List<String> result = new LinkedList<String>();
        result.add(TEST_CLASS_NAME);
        return result;
    }
    
    private List<String> maybeQueryTestResources() throws RepositoryException {
        if(lastModified <= lastReloaded) {
            log.debug("No changes detected, keeping existing list of {} test resources", testPaths.size());
            return testPaths;
        }
        
        log.info("Changes detected, reloading list of test resources");
        final long reloadTime = System.currentTimeMillis();
        final List<String> newList = new LinkedList<String>();
        
        for(String root : allowedRoots) {
            final String statement = "/jcr:root" + root + "/element(*, " + SLING_TEST_NODETYPE + ")";
            log.debug("Querying for test nodes: {}", statement);
            session.refresh(true);
            final Query q = session.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
            final NodeIterator it = q.execute().getNodes();
            while(it.hasNext()) {
                final String path = it.nextNode().getPath();
                newList.add(path);
                log.debug("Test resource found: {}", path);
            }
        }
        log.info("List of test resources updated, {} resource(s) found under {}", 
                newList.size(), Arrays.asList(allowedRoots));

        synchronized (testPaths) {
            testPaths.clear();
            testPaths.addAll(newList);
        }
        
        lastReloaded = reloadTime;
        
        return testPaths;
    }
    
    public long lastModified() {
        return lastModified;
    }
    
    static TestContext getTestContext() {
        return testContext.get();
    }
}

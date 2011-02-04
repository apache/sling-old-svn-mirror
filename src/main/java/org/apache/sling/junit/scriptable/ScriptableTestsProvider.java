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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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
    
    /** List of resource paths that point to tests */
    private static List<String> testPaths = new LinkedList<String>();
    
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
    
    protected void activate(ComponentContext ctx) throws Exception {
        pid = (String)ctx.getProperties().get(Constants.SERVICE_PID);
        session = repository.loginAdministrative(repository.getDefaultWorkspace());
        resolver = resolverFactory.getResourceResolver(session);
        
        // Copy resource resolver paths and make sure they end with a /
        final String [] paths = resolver.getSearchPath();
        allowedRoots = Arrays.copyOf(paths, paths.length);
        for(int i=0; i < allowedRoots.length; i++) {
            if(!allowedRoots[i].endsWith("/")) {
                allowedRoots[i] += "/";
            }
        }
        log.info("Activated, will look for test resources under {}", Arrays.asList(allowedRoots));
    }
    
    protected void deactivate(ComponentContext ctx) throws RepositoryException {
        resolver = null;
        if(session != null) {
            session.logout();
        }
        session = null;
    }
    
    public Class<?> createTestClass(String testName) throws ClassNotFoundException {
        queryTestPaths();
        
        if(testPaths.size() == 0) {
            return ExplainTests.class;
        } else {
            TestAllPaths.testPaths = testPaths;
            TestAllPaths.requestProcessor = requestProcessor;
            TestAllPaths.resolver = resolver;
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
        result.add(getClass().getSimpleName() + "Tests");
        return result;
    }
    
    private List<String> queryTestPaths() {
        final List<String> result = new LinkedList<String>();
        
        // TODO do we want to cache results, use observation, etc.
        try {
            for(String root : allowedRoots) {
                final String statement = "/jcr:root" + root + "/element(*, sling:Test)";
                log.debug("Querying for test nodes: {}", statement);
                final Query q = session.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
                final NodeIterator it = q.execute().getNodes();
                while(it.hasNext()) {
                    final String path = it.nextNode().getPath();
                    result.add(path);
                    log.debug("Test resource found: {}", path);
                }
            }
            log.info("List of test resources updated, {} resource(s) found under {}", 
                    result.size(), Arrays.asList(allowedRoots));
        } catch(RepositoryException re) {
            log.warn("RepositoryException in getTestNames()", re);
        }

        synchronized (testPaths) {
            testPaths.clear();
            testPaths.addAll(result);
        }
        
        return testPaths;
    }

    public long lastModified() {
        // TODO caching etc.
        return System.currentTimeMillis();
    }
}

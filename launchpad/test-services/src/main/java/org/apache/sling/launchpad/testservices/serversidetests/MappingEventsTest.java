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
package org.apache.sling.launchpad.testservices.serversidetests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.sling.launchpad.testservices.events.EventsCounter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SlingAnnotationsTestRunner.class)
public class MappingEventsTest {

    public static final String PROP_REDIRECT_EXTERNAL = "sling:redirect";
    
    private static final Logger logger = LoggerFactory.getLogger(MappingEventsTest.class);
    private static Session session;
    private Node mapRoot;
    private static List<String> toDelete = new ArrayList<String>();
    
    private static final int N_STEPS = 20;
    
    @TestReference
    private EventsCounter eventsCounter;
    
    @TestReference
    private SlingRepository repository;
    
    private Node maybeCreateNode(Node parent, String name, String type) throws RepositoryException {
        if(parent.hasNode(name)) {
            return parent.getNode(name);
        } else {
            return parent.addNode(name, type);
        }
    }

    @Before
    public synchronized void setup() throws Exception {
        // Do the mappings setup only once, and clean it up 
        // after all tests
        session = repository.loginAdministrative(null);
        final Node rootNode = maybeCreateNode(session.getRootNode(), "content", "nt:unstructured");
        session.save();
        
        assertTrue("toDelete should be empty before test", toDelete.isEmpty());
        
        mapRoot = maybeCreateNode(session.getRootNode(), "etc", "nt:folder");
        final Node map = maybeCreateNode(mapRoot, "map", "sling:Mapping");
        final Node http = maybeCreateNode(map, "http", "sling:Mapping");
        maybeCreateNode(http, "localhost.80", "sling:Mapping");
        final Node https = maybeCreateNode(map, "https", "sling:Mapping");
        maybeCreateNode(https, "localhost.443", "sling:Mapping");
        toDelete.add(map.getPath());
        toDelete.add(rootNode.getPath());
    }
    
    @After
    public void deleteTestNodes() throws Exception {
        logger.debug("{} test done, deleting test nodes", MappingEventsTest.class.getSimpleName());
        
        try {
            for(String path : toDelete) {
                if(session.itemExists(path)) {
                    session.getItem(path).remove();
                }
            }
            toDelete.clear();
            session.save();
        } finally {
            session.logout();
        }
    }
    
    /** Test SLING-4058 - unexpected timeouts in saveMappings */
    @Test public void testSaveMappings() throws Exception {
        final Node base = mapRoot.getNode("map/https/localhost.443");
        final MappingsFacade f = new MappingsFacade(eventsCounter);
        try {
            int count = N_STEPS;
            while(count-- > 0) {
                base.setProperty(PROP_REDIRECT_EXTERNAL,"http://somehost." + count);
                final String result = f.saveMappings(session);
                if(result != null) {
                    fail(result);
                }
            }
        } finally {
            base.setProperty(PROP_REDIRECT_EXTERNAL,"");
            session.save();
        }
    }
    
    @Test public void testVanityPaths() throws Exception {
        final MappingsFacade f = new MappingsFacade(eventsCounter);
        final Node vanityTest = maybeCreateNode(session.getRootNode(), "vanityTest", "sling:Folder");
        toDelete.add(vanityTest.getPath());
        int count = N_STEPS;
        while(count-- > 0) {
            final String [] paths = { "one", "two", "three_" + count };
            vanityTest.setProperty("sling:vanityPath", paths);
            final String result = f.saveMappings(session);
            if(result != null) {
                fail(result);
            }
        }
    }
}

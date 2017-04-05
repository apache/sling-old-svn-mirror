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
package org.apache.sling.jcr.contentloader.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.contentloader.ContentReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** TODO might need to consolidate this with DefaultContentCreatorTest */
public class CreateNodeTest {
    
    private DefaultContentCreator contentCreator;
    private Session session;
    private Node testRoot;
    private final static String DEFAULT_NAME = "default-name";
    public static final String MIX_VERSIONABLE = "mix:versionable";  
    
    private final String uniqueId() {
        return getClass().getSimpleName() + UUID.randomUUID();
    }
    
    @Before
    public void setup() throws Exception {
        final SlingRepository repo = RepositoryProvider.instance().getRepository();
        session = repo.loginAdministrative(null);
        contentCreator = new DefaultContentCreator(null);
        contentCreator.init(ImportOptionsFactory.createImportOptions(true, true, true, false, false),
                new HashMap<String, ContentReader>(), null, null);
        testRoot = session.getRootNode().addNode(getClass().getSimpleName()).addNode(uniqueId());
    }
    
    @After
    public void cleanup() throws RepositoryException {
        if(session != null) {
            session.save(); // to detect any invalid transient content
            session.logout();
            session = null;
        }
    }
    
    @Test
    public void testCreateNode() throws Exception {
        contentCreator.prepareParsing(testRoot, DEFAULT_NAME);
        final String name = uniqueId();
        assertFalse("Expecting " + name + " child node to be absent before test", testRoot.hasNode(name));
        contentCreator.createNode(name, null, null);
        assertTrue("Expecting " + name + " child node to be created", testRoot.hasNode(name));
    }
}
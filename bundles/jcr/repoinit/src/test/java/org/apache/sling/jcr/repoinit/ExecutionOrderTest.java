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
package org.apache.sling.jcr.repoinit;

import static org.junit.Assert.assertEquals;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.sling.jcr.repoinit.impl.TestUtil;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Verify that namespaces and nodetypes are executed before path creation */
public class ExecutionOrderTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    
    private TestUtil U;
    
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String NS_PREFIX = ExecutionOrderTest.class.getSimpleName();
    private static final String NS_URI = "uri:" + NS_PREFIX + ":" + TEST_ID;
    private static final String REL_PATH = ExecutionOrderTest.class.getSimpleName() + "-" + TEST_ID;
    
    @Before
    public void setup() throws RepositoryException, RepoInitParsingException {
        U = new TestUtil(context);
        
        final String stmt = 
            "create path (" + NS_PREFIX + ":foo) /" + REL_PATH + "\n"
            + U.getTestCndStatement(NS_PREFIX, NS_URI) + "\n"
            + "register namespace (" + NS_PREFIX + ") " + NS_URI + "\n";
        ;
        
        U.parseAndExecute(stmt);
    }

    @After
    public void cleanup() throws RepositoryException, RepoInitParsingException {
        U.cleanup();
    }
    
    @Test
    public void pathCreated() throws PathNotFoundException, RepositoryException {
        final Node n = U.getAdminSession().getNode("/" + REL_PATH);
        assertEquals(NS_PREFIX + ":foo", n.getProperty("jcr:primaryType").getString());
    }
}
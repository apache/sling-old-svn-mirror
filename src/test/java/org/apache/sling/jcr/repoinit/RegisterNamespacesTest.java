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

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.sling.jcr.repoinit.impl.TestUtil;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Test register namespace statements */
public class RegisterNamespacesTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    
    private TestUtil U;
    private NamespaceRegistry ns;
    
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String NS1 = "uri:ns:test1:" + TEST_ID;
    private static final String NS2 = "http://example.com/ns/" + TEST_ID;
   
    @Before
    public void setup() throws RepositoryException, RepoInitParsingException {
        U = new TestUtil(context);
        U.parseAndExecute("register namespace (one) " + NS1);
        U.parseAndExecute("register namespace (two) " + NS2);
        ns = U.getAdminSession().getWorkspace().getNamespaceRegistry();
    }

    @After
    public void cleanup() throws RepositoryException, RepoInitParsingException {
        U.cleanup();
    }
    
    @Test
    public void NS1registered() throws Exception {
        assertEquals(NS1, ns.getURI("one"));
    }
    
    @Test
    public void NS2registered() throws Exception {
        assertEquals(NS2, ns.getURI("two"));
    }
}

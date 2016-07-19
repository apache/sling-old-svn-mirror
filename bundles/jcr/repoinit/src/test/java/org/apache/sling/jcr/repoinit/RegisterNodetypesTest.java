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

/** Test register nodetypes statements. Also registers a namespace */
public class RegisterNodetypesTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    
    private TestUtil U;
    
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String NS_PREFIX = RegisterNodetypesTest.class.getSimpleName();
    private static final String NS_URI = "uri:" + NS_PREFIX + ":" + TEST_ID;
    
    @Before
    public void setup() throws RepositoryException, RepoInitParsingException {
        U = new TestUtil(context);
        U.parseAndExecute("register namespace (" + NS_PREFIX + ") " + NS_URI);
        U.parseAndExecute(U.getTestCndStatement(NS_PREFIX, NS_URI));
    }

    @After
    public void cleanup() throws RepositoryException, RepoInitParsingException {
        U.cleanup();
    }
    
    @Test
    public void NSregistered() throws Exception {
        final NamespaceRegistry ns = U.getAdminSession().getWorkspace().getNamespaceRegistry();
        assertEquals(NS_URI, ns.getURI(NS_PREFIX));
    }
    
    @Test
    public void fooNodetypeRegistered() throws Exception {
        U.getAdminSession().getRootNode().addNode("test_" + TEST_ID, NS_PREFIX + ":foo");
    }
 }
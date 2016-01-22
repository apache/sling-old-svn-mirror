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
package org.apache.sling.repoinit.jcr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.repoinit.parser.AclParsingException;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Test setting ACLS on non-existent paths */
public class NonExistentPathTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    
    private TestUtil U;
    private Session s;
    
    @Before
    public void setup() throws RepositoryException, AclParsingException {
        U = new TestUtil(context);
        U.parseAndExecute("create service user " + U.username);
        s = U.loginService(U.username);
    }

    @After
    public void cleanup() throws RepositoryException, AclParsingException {
        U.cleanupUser();
        s.logout();
    }
    
    @Test
    public void setAclOnFoo() throws Exception {
        final String aclDef =
            "set ACL on /foo_" + U.id + "\n"
            + "  allow jcr:all for " + U.username + "\n"
            + "end"
        ;
        try {
            U.parseAndExecute(aclDef);
            fail("Expecting a wrapped PathNotFoundException");
        } catch(RuntimeException rux) {
            assertEquals(PathNotFoundException.class, rux.getCause().getClass());
        }
    }
    
}

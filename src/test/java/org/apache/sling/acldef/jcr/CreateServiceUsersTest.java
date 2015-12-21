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
package org.apache.sling.acldef.jcr;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.acldef.parser.ACLDefinitions;
import org.apache.sling.acldef.parser.ParseException;
import org.apache.sling.acldef.parser.operations.Operation;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Test the creation and delete of service users */
public class CreateServiceUsersTest {
    
    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    
    private Session session;
    private UserManager userManager;
    private AtomicLong counter = new AtomicLong(System.currentTimeMillis());
    private long id; 
    
    private List<Operation> parse(String input) throws ParseException {
        final Reader r = new StringReader(input);
        try {
            return new ACLDefinitions(r).parse();
        } finally {
            IOUtils.closeQuietly(r);
        }
    }

    @Before
    public void setup() throws RepositoryException {
        session = context.resourceResolver().adaptTo(Session.class);
        userManager = ServiceUserUtil.getUserManager(session);
        id = counter.incrementAndGet();
    }

    @After
    public void cleanup() {
        session.logout();
    }
    
    private void assertServiceUser(String info, String id, boolean expectToExist) throws RepositoryException {
        final Authorizable a = userManager.getAuthorizable(id);
        if(!expectToExist) {
            assertNull(info + ", expecting Principal to be absent:" + id, a);
        } else {
            assertNotNull(info + ", expecting Principal to exist:" + id, a);
            final User u = (User)a;
            assertNotNull(info + ", expecting Principal to be a System user:" + id, u.isSystemUser());
        }
    }
    
    private void exec(String input) throws ParseException {
        final AclOperationVisitor v = new AclOperationVisitor(session);
        for(Operation o : parse(input)) {
            o.accept(v);
        }
    }

    @Test
    public void createDeleteTest() throws Exception {
        final String userId = "bob_" + id;
        assertServiceUser("at start of test", userId, false);
        exec("create service user " + userId);
        assertServiceUser("affter creating user", userId, true);
        exec("delete service user " + userId);
        assertServiceUser("after deleting user", userId, false);
    }
}

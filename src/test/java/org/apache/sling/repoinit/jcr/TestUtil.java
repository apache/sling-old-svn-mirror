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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.repoinit.jcr.impl.ServiceUserUtil;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.repoinit.parser.impl.RepoInitParserService;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.apache.sling.testing.mock.sling.junit.SlingContext;

/** Test utilities */
class TestUtil {
    
    public static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
    final Session adminSession;
    final String id;
    final String username;
    
    TestUtil(SlingContext ctx) {
        adminSession = ctx.resourceResolver().adaptTo(Session.class);
        id = UUID.randomUUID().toString();
        username = "user_" + id;
    }
    
    List<Operation> parse(String input) throws RepoInitParsingException {
        final Reader r = new StringReader(input);
        try {
            return new RepoInitParserService().parse(r);
        } finally {
            IOUtils.closeQuietly(r);
        }
    }

    void assertServiceUser(String info, String id, boolean expectToExist) throws RepositoryException {
        final Authorizable a = ServiceUserUtil.getUserManager(adminSession).getAuthorizable(id);
        if(!expectToExist) {
            assertNull(info + ", expecting Principal to be absent:" + id, a);
        } else {
            assertNotNull(info + ", expecting Principal to exist:" + id, a);
            final User u = (User)a;
            assertNotNull(info + ", expecting Principal to be a System user:" + id, u.isSystemUser());
        }
    }
    
    void assertNodeExists(String path) throws RepositoryException {
        assertNodeExists(path, null);
    }
    
    void assertNodeExists(String path, String primaryType) throws RepositoryException {
        if(!adminSession.nodeExists(path)) {
            fail("Node does not exist:" + path);
        }
        if(primaryType != null) {
            final Node n = adminSession.getNode(path);
            if(!n.hasProperty(JCR_PRIMARY_TYPE)) {
                fail("No " + JCR_PRIMARY_TYPE + " property at " + path);
            }
            final String actual = n.getProperty(JCR_PRIMARY_TYPE).getString();
            if(!primaryType.equals(actual)) {
                fail("Primary type mismatch for " + path + ", expected " + primaryType + " but got " + actual);
            }
        }
    }
    
    void parseAndExecute(String input) throws RepositoryException, RepoInitParsingException {
        final JcrRepoInitOpVisitor v = new JcrRepoInitOpVisitor(adminSession);
        for(Operation o : parse(input)) {
            o.accept(v);
        }
        adminSession.save();
    }
    
    void cleanupUser() throws RepositoryException, RepoInitParsingException {
        parseAndExecute("delete service user " + username);
        assertServiceUser("in cleanupUser()", username, false);
    }
    
    Session loginService(String serviceUsername) throws RepositoryException {
        final SimpleCredentials cred = new SimpleCredentials(serviceUsername, new char[0]);
        return adminSession.impersonate(cred);
    }
}

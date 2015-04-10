/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.commons.testing.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.naming.NamingException;

import junit.framework.TestCase;

import org.apache.sling.jcr.api.SlingRepository;

/** Base class for JUnit3-style tests which need a Repository. 
 *  Should eventually be deprecated in favor of {@link RepositoryProvider}
 *  which is less intrusive
 */
public class RepositoryTestBase extends TestCase {
    protected Node testRoot;
    protected Session session;
    private int counter;
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if(session != null) {
            session.logout();
        }
    }

    /** Return a JCR Session, initialized on demand */ 
    protected Session getSession() throws RepositoryException, NamingException {
        if(session == null) {
            session = getRepository().loginAdministrative(null);
        }
        return session;
    }
    
    /** Return a test root node, created on demand, with a unique path */ 
    protected Node getTestRootNode() throws RepositoryException, NamingException {
        if(testRoot==null) {
            final Node root = getSession().getRootNode();
            final Node classRoot = root.addNode(getClass().getSimpleName()); 
            testRoot = classRoot.addNode(System.currentTimeMillis() + "_" + (++counter));
        }
        return testRoot;
    }

    /** Return a Repository */
    protected SlingRepository getRepository() throws RepositoryException, NamingException {
        return RepositoryProvider.instance().getRepository();
    }
}
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
package org.apache.sling.jcr.base;

import org.apache.sling.jcr.api.NamespaceMapper;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;

import javax.jcr.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Dictionary;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class SessionProxyHandlerTest {

    /**
     * Method verifies that session proxy with given repository manager could be created
     * and proxy instance is a kind of <code>SessionProxyHandler.SessionProxyInvocationHandler</code>
     * class.
     */
    @Test
    public void testProxy() throws RepositoryException {
        /* Create a new mockSession and mock it to return another one on impersonate() method call,
         * since impersonate method doesn't implemented.
         */
        Session session = Mockito.spy(MockJcr.newSession("admin", "adminSpace"));
        Session imperSession = MockJcr.newSession("test", "testSpace");
        doReturn(imperSession).when(session).impersonate(null);

        //Create a proxy handle and proxy session
        SessionProxyHandler proxyHandler = new SessionProxyHandler(new CustomSlingRepositoryManager());
        Session proxySession = proxyHandler.createProxy(session);

        //sessionForTest is a proxy which contains a link to the imperSession object
        Session sessionForTest = proxySession.impersonate(null);
        InvocationHandler handler = Proxy.getInvocationHandler(sessionForTest);

        /* Checking that proxySession is a kind of SessionProxyHandler.SessionProxyInvocationHandler class.
         * So we sure that proxy object creation works fine.
         */
        assertEquals(SessionProxyHandler.SessionProxyInvocationHandler.class, handler.getClass());

        //Checking that others methods of proxy object works also well and doesn't call impersonate method
        assertNotNull(sessionForTest.getRootNode());
        assertNotNull(sessionForTest.getRepository());
    }

    /**
     * Create custom RepositoryManager to support namespace mapping, since the only one
     * available <code>OakSlingRepositoryManager</code> lies under <code>org.apache.sling.jcr.oak.server</code>
     * what creates circular dependencies with this module.
     */
    private static final class CustomSlingRepositoryManager extends AbstractSlingRepositoryManager{
        @Override
        protected ServiceUserMapper getServiceUserMapper() {
            return null;
        }

        @Override
        protected Repository acquireRepository() {
            return null;
        }

        @Override
        protected Dictionary<String, Object> getServiceRegistrationProperties() {
            return null;
        }

        @Override
        protected AbstractSlingRepository2 create(Bundle usingBundle) {
            return null;
        }

        @Override
        protected void destroy(AbstractSlingRepository2 repositoryServiceInstance) {

        }

        @Override
        protected void disposeRepository(Repository repository) {

        }

        @Override
        protected NamespaceMapper[] getNamespaceMapperServices() {
            return new NamespaceMapper[0];
        }
    }
}

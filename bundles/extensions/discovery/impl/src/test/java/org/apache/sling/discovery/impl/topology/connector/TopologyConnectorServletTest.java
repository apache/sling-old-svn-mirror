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
package org.apache.sling.discovery.impl.topology.connector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import junitx.util.PrivateAccessor;

import org.apache.sling.discovery.impl.Config;
import org.junit.Before;
import org.junit.Test;

public class TopologyConnectorServletTest {

    private TopologyConnectorServlet servlet;
    
    private HttpServletRequest getRequest(String host, String addr) {
        HttpServletRequest result = mock(HttpServletRequest.class);
        when(result.getRemoteAddr()).thenReturn(addr);
        when(result.getRemoteHost()).thenReturn(host);
        return result;
    }
    
    @Before
    public void setUp() throws Exception {
        servlet = new TopologyConnectorServlet();
        Config config = mock(Config.class);
        PrivateAccessor.setField(servlet, "config", config);
    }
    
    @Test
    public void testNull() throws Exception {
        servlet.initWhitelist(null); // should work fine
        servlet.initWhitelist(new String[0]); // should also work fine
    }
    
    @Test
    public void testPlaintextWhitelist_enabled() throws Exception {
        servlet.initWhitelist(new String[] {"foo", "bar"});
        assertTrue(servlet.isWhitelisted(getRequest("foo", "x")));
        assertTrue(servlet.isWhitelisted(getRequest("bar", "x")));
        assertTrue(servlet.isWhitelisted(getRequest("y", "foo")));
        assertTrue(servlet.isWhitelisted(getRequest("y", "bar")));
    }
    
    @Test
    public void testPlaintextWhitelist_disabled() throws Exception {
        servlet.initWhitelist(new String[] {});
        assertFalse(servlet.isWhitelisted(getRequest("foo", "x")));
        assertFalse(servlet.isWhitelisted(getRequest("bar", "x")));
        assertFalse(servlet.isWhitelisted(getRequest("y", "foo")));
        assertFalse(servlet.isWhitelisted(getRequest("y", "bar")));
    }
    
    @Test
    public void testWildcardWhitelist() throws Exception {
        servlet.initWhitelist(new String[] {"foo*", "b?r", "test"});
        assertTrue(servlet.isWhitelisted(getRequest("foo", "x")));
        assertTrue(servlet.isWhitelisted(getRequest("fooo", "x")));
        assertTrue(servlet.isWhitelisted(getRequest("foooo", "x")));
        assertTrue(servlet.isWhitelisted(getRequest("x", "foo")));
        assertTrue(servlet.isWhitelisted(getRequest("x", "fooo")));
        assertTrue(servlet.isWhitelisted(getRequest("x", "foooo")));
        assertTrue(servlet.isWhitelisted(getRequest("bur", "x")));
        assertTrue(servlet.isWhitelisted(getRequest("x", "bur")));
        assertTrue(servlet.isWhitelisted(getRequest("x", "test")));
        assertFalse(servlet.isWhitelisted(getRequest("fo", "x")));
        assertFalse(servlet.isWhitelisted(getRequest("x", "testy")));
    }
    
    @Test
    public void testSubnetMaskWhitelist() throws Exception {
        servlet.initWhitelist(new String[] {"1.2.3.4/24", "2.3.4.1/30", "3.4.5.6/31"});
        
        assertTrue(servlet.isWhitelisted(getRequest("foo", "1.2.3.4")));
        assertFalse(servlet.isWhitelisted(getRequest("1.2.3.4", "1.2.4.3")));
        assertTrue(servlet.isWhitelisted(getRequest("foo", "1.2.3.1")));
        assertTrue(servlet.isWhitelisted(getRequest("foo", "1.2.3.254")));
        assertFalse(servlet.isWhitelisted(getRequest("foo", "1.2.4.5")));

        assertTrue(servlet.isWhitelisted(getRequest("foo", "2.3.4.1")));
        assertTrue(servlet.isWhitelisted(getRequest("foo", "2.3.4.2")));
        assertFalse(servlet.isWhitelisted(getRequest("foo", "2.3.4.3")));
        assertFalse(servlet.isWhitelisted(getRequest("foo", "2.3.4.4")));

        assertFalse(servlet.isWhitelisted(getRequest("foo", "3.4.5.1")));
        assertFalse(servlet.isWhitelisted(getRequest("foo", "3.4.5.2")));
        assertFalse(servlet.isWhitelisted(getRequest("foo", "3.4.5.3")));
        assertFalse(servlet.isWhitelisted(getRequest("foo", "3.4.5.4")));
        assertFalse(servlet.isWhitelisted(getRequest("foo", "3.4.5.5")));
        assertFalse(servlet.isWhitelisted(getRequest("foo", "3.4.5.6")));
        assertFalse(servlet.isWhitelisted(getRequest("foo", "3.4.5.7")));
    }
}

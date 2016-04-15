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
package org.apache.sling.servlethelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.sling.servlethelpers.MockHttpSession;
import org.junit.Before;
import org.junit.Test;

public class MockHttpSessionTest {

    private MockHttpSession httpSession;

    @Before
    public void setUp() throws Exception {
        httpSession = new MockHttpSession();
    }

    @Test
    public void testServletContext() {
        assertNotNull(httpSession.getServletContext());
    }

    @Test
    public void testId() {
        assertNotNull(httpSession.getId());
    }

    @Test
    public void testCreationTime() {
        assertNotNull(httpSession.getCreationTime());
    }

    @Test
    public void testAttributes() {
        httpSession.setAttribute("attr1", "value1");
        assertTrue(httpSession.getAttributeNames().hasMoreElements());
        assertEquals("value1", httpSession.getAttribute("attr1"));
        httpSession.removeAttribute("attr1");
        assertFalse(httpSession.getAttributeNames().hasMoreElements());
    }

    @Test
    public void testValues() {
        httpSession.putValue("attr1", "value1");
        assertEquals(1, httpSession.getValueNames().length);
        assertEquals("value1", httpSession.getValue("attr1"));
        httpSession.removeValue("attr1");
        assertEquals(0, httpSession.getValueNames().length);
    }

    @Test
    public void testInvalidate() {
        httpSession.invalidate();
        assertTrue(httpSession.isInvalidated());
    }

    @Test(expected = IllegalStateException.class)
    public void testInvalidateStateCheck() {
        httpSession.invalidate();
        httpSession.getAttribute("attr1");
    }

    @Test
    public void testIsNew() {
        assertTrue(httpSession.isNew());
        httpSession.setNew(false);
        assertFalse(httpSession.isNew());
   }

    @Test
    public void testGetLastAccessedTime() {
        assertNotNull(httpSession.getLastAccessedTime());
    }

    @Test
    public void testGetMaxInactiveInterval() {
        assertTrue(httpSession.getMaxInactiveInterval() > 0);
        httpSession.setMaxInactiveInterval(123);
        assertEquals(123, httpSession.getMaxInactiveInterval());
    }

}

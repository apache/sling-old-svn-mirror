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
package org.apache.sling.testing.mock.sling.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;

public class MockHttpSessionTest {

    private HttpSession httpSession;

    @Before
    public void setUp() throws Exception {
        this.httpSession = new MockHttpSession();
    }

    @Test
    public void testServletContext() {
        assertNotNull(this.httpSession.getServletContext());
    }

    @Test
    public void testId() {
        assertNotNull(this.httpSession.getId());
    }

    @Test
    public void testCreationTime() {
        assertNotNull(this.httpSession.getCreationTime());
    }

    @Test
    public void testAttributes() {
        this.httpSession.setAttribute("attr1", "value1");
        assertTrue(this.httpSession.getAttributeNames().hasMoreElements());
        assertEquals("value1", this.httpSession.getAttribute("attr1"));
        this.httpSession.removeAttribute("attr1");
        assertFalse(this.httpSession.getAttributeNames().hasMoreElements());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testValues() {
        this.httpSession.putValue("attr1", "value1");
        assertEquals(1, this.httpSession.getValueNames().length);
        assertEquals("value1", this.httpSession.getValue("attr1"));
        this.httpSession.removeValue("attr1");
        assertEquals(0, this.httpSession.getValueNames().length);
    }

}

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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

public class MockRequestPathInfoTest {

    private MockRequestPathInfo requestPathInfo;

    @Before
    public void setUp() throws Exception {
        this.requestPathInfo = new MockRequestPathInfo();
    }

    @Test
    public void testExtension() {
        assertNull(this.requestPathInfo.getExtension());
        this.requestPathInfo.setExtension("ext");
        assertEquals("ext", this.requestPathInfo.getExtension());
    }

    @Test
    public void testResourcePath() {
        assertNull(this.requestPathInfo.getResourcePath());
        this.requestPathInfo.setResourcePath("/path");
        assertEquals("/path", this.requestPathInfo.getResourcePath());
    }

    @Test
    public void testSelector() {
        assertNull(this.requestPathInfo.getSelectorString());
        assertEquals(0, this.requestPathInfo.getSelectors().length);
        this.requestPathInfo.setSelectorString("aa.bb");
        assertEquals("aa.bb", this.requestPathInfo.getSelectorString());
        assertEquals(2, this.requestPathInfo.getSelectors().length);
        assertArrayEquals(new String[] { "aa", "bb" }, this.requestPathInfo.getSelectors());
    }

    @Test
    public void testSuffix() {
        assertNull(this.requestPathInfo.getSuffix());
        this.requestPathInfo.setSuffix("/suffix");
        assertEquals("/suffix", this.requestPathInfo.getSuffix());
    }

}

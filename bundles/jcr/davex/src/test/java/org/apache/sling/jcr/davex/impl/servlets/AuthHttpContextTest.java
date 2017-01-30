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
package org.apache.sling.jcr.davex.impl.servlets;

import java.lang.reflect.Method;

import junit.framework.TestCase;

import org.junit.BeforeClass;
import org.junit.Test;

public class AuthHttpContextTest {

    private static Method getWorkspace;

    @BeforeClass
    public static void getGetWorkspaceMethod() throws Throwable {
        getWorkspace = AuthHttpContext.class.getDeclaredMethod("getWorkspace", String.class);
        getWorkspace.setAccessible(true);
    }

    @Test
    public void test_getWorkspace_null() throws Throwable {
        final AuthHttpContext ahc = new AuthHttpContext("/server");
        TestCase.assertNull(getWorkspace.invoke(ahc, (String) null));
    }

    @Test
    public void test_getWorkspace_root() throws Throwable {
        final AuthHttpContext ahc = new AuthHttpContext("/server");
        TestCase.assertNull(getWorkspace.invoke(ahc, "/server"));
    }

    @Test
    public void test_getWorkspace_root_slash() throws Throwable {
        final AuthHttpContext ahc = new AuthHttpContext("/server");
        TestCase.assertNull(getWorkspace.invoke(ahc, "/server/"));
    }

    @Test
    public void test_getWorkspace_root_char() throws Throwable {
        final AuthHttpContext ahc = new AuthHttpContext("/server");
        TestCase.assertNull(getWorkspace.invoke(ahc, "/serverxyz"));
    }

    @Test
    public void test_getWorkspace_wsp() throws Throwable {
        final AuthHttpContext ahc = new AuthHttpContext("/server");
        TestCase.assertEquals("w", getWorkspace.invoke(ahc, "/server/w"));
        TestCase.assertEquals("wsp", getWorkspace.invoke(ahc, "/server/wsp"));
    }

    @Test
    public void test_getWorkspace_wsp_slash() throws Throwable {
        final AuthHttpContext ahc = new AuthHttpContext("/server");
        TestCase.assertEquals("w", getWorkspace.invoke(ahc, "/server/w/"));
        TestCase.assertEquals("wsp", getWorkspace.invoke(ahc, "/server/wsp/"));
    }

    @Test
    public void test_getWorkspace_wsp_path() throws Throwable {
        final AuthHttpContext ahc = new AuthHttpContext("/server");
        TestCase.assertEquals("w", getWorkspace.invoke(ahc, "/server/w/abc"));
        TestCase.assertEquals("wsp", getWorkspace.invoke(ahc, "/server/wsp/abc"));

        TestCase.assertEquals("w", getWorkspace.invoke(ahc, "/server/w/abc/xyz"));
        TestCase.assertEquals("wsp", getWorkspace.invoke(ahc, "/server/wsp/abc/xyz"));
    }

}

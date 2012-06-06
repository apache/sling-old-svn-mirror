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
package org.apache.sling.auth.core.spi;

import static junit.framework.Assert.failNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.junit.Assert;
import org.junit.Test;

public class AuthenticationInfoTest {

    // backwards compatible constant tied to jcr/resource
    // JcrResourceResolverFactory.CREDENTIALS
    private static final String CREDENTIALS = "user.jcr.credentials";

    @Test
    public void testClear() {
        final char[] pwd = new char[6];
        final AuthenticationInfo info = new AuthenticationInfo("test", "name",
            pwd);
        Assert.assertEquals("test", info.getAuthType());
        Assert.assertEquals("name", info.getUser());
        assertSame(pwd, info.getPassword());

        info.clear();

        Assert.assertEquals(1, info.size()); // AUTH_TYPE still contained
        Assert.assertEquals("test", info.getAuthType());
        assertFalse(info.containsKey(ResourceResolverFactory.USER));
        assertFalse(info.containsKey(ResourceResolverFactory.PASSWORD));
    }

    @Test
    public void testDOING_AUTH() {
        final AuthenticationInfo da = AuthenticationInfo.DOING_AUTH;
        Assert.assertEquals("DOING_AUTH", da.getAuthType());
        da.clear();
        Assert.assertEquals("DOING_AUTH", da.getAuthType());
        da.put("test", "test");
        assertFalse(da.containsKey("test"));
    }

    @Test
    public void testFAIL_AUTH() {
        final AuthenticationInfo fa = AuthenticationInfo.FAIL_AUTH;
        Assert.assertEquals("FAIL_AUTH", fa.getAuthType());
        fa.clear();
        Assert.assertEquals("FAIL_AUTH", fa.getAuthType());
        fa.put("test", "test");
        assertFalse(fa.containsKey("test"));
    }

    @Test
    public void testAuthenticationInfoString() {
        final AuthenticationInfo info = new AuthenticationInfo("test");
        Assert.assertEquals("test", info.getAuthType());
        assertNull(info.getUser());
        assertNull(info.getPassword());
    }

    @Test
    public void testAuthenticationInfoStringString() {
        final AuthenticationInfo info = new AuthenticationInfo("test", "name");
        Assert.assertEquals("test", info.getAuthType());
        Assert.assertEquals("name", info.getUser());
        assertNull(info.getPassword());
    }

    @Test
    public void testAuthenticationInfoStringStringCharArray() {
        final char[] pwd = new char[6];
        final AuthenticationInfo info = new AuthenticationInfo("test", "name",
            pwd);
        Assert.assertEquals("test", info.getAuthType());
        Assert.assertEquals("name", info.getUser());
        assertSame(pwd, info.getPassword());
    }

    @Test
    public void testAuthenticationInfoStringStringCharArrayString() {
        final char[] pwd = new char[6];
        final AuthenticationInfo info = new AuthenticationInfo("test", "name",
            pwd);
        Assert.assertEquals("test", info.getAuthType());
        Assert.assertEquals("name", info.getUser());
        assertSame(pwd, info.getPassword());
    }

    @Test
    public void testSetAuthType() {
        final AuthenticationInfo info = new AuthenticationInfo("test");
        Assert.assertEquals("test", info.getAuthType());

        info.setAuthType(null);
        Assert.assertEquals("test", info.getAuthType());

        info.setAuthType("dummy");
        Assert.assertEquals("dummy", info.getAuthType());

        info.setAuthType("");
        Assert.assertEquals("", info.getAuthType());
    }

    @Test
    public void testGetAuthType() {
        final AuthenticationInfo info = new AuthenticationInfo("test");
        Assert.assertEquals("test", info.getAuthType());
        Assert.assertEquals("test", info.get(AuthenticationInfo.AUTH_TYPE));
        Assert.assertEquals(info.get(AuthenticationInfo.AUTH_TYPE),
            info.getAuthType());
    }

    @Test
    public void testSetUser() {
        final AuthenticationInfo info = new AuthenticationInfo("test", "user");
        Assert.assertEquals("user", info.getUser());

        info.setUser(null);
        Assert.assertEquals("user", info.getUser());

        info.setUser("dummy");
        Assert.assertEquals("dummy", info.getUser());

        info.setUser("");
        Assert.assertEquals("", info.getUser());
    }

    @Test
    public void testGetUser() {
        final AuthenticationInfo info = new AuthenticationInfo("test");
        info.put(ResourceResolverFactory.USER, "name");

        Assert.assertEquals("name", info.getUser());
        Assert.assertEquals("name", info.get(ResourceResolverFactory.USER));
        Assert.assertEquals(info.get(ResourceResolverFactory.USER), info.getUser());
    }

    @Test
    public void testSetPassword() {
        final char[] pwd = new char[6];
        final AuthenticationInfo info = new AuthenticationInfo("test", "name");

        assertFalse(info.containsKey(ResourceResolverFactory.PASSWORD));

        info.setPassword(pwd);
        assertSame(pwd, info.get(ResourceResolverFactory.PASSWORD));

        info.setPassword(null);
        assertSame(pwd, info.get(ResourceResolverFactory.PASSWORD));
    }

    @Test
    public void testGetPassword() {
        final char[] pwd = new char[6];
        final AuthenticationInfo info = new AuthenticationInfo("test", "name",
            pwd);

        assertSame(pwd, info.getPassword());
        assertEquals(pwd, (char[]) info.get(ResourceResolverFactory.PASSWORD));
        Assert.assertEquals(info.get(ResourceResolverFactory.PASSWORD),
            info.getPassword());
    }

    @Test
    public void testSetCredentials() {
        final Credentials creds = new SimpleCredentials("user", new char[0]);
        final AuthenticationInfo info = new AuthenticationInfo("test");

        info.put(CREDENTIALS, creds);
        Assert.assertSame(creds, info.get(CREDENTIALS));
    }

    @Test
    public void testGetCredentials() {
        final AuthenticationInfo info = new AuthenticationInfo("test");

        assertNull(info.get(CREDENTIALS));
        assertFalse(info.containsKey(CREDENTIALS));

        final Credentials creds = new SimpleCredentials("user", new char[0]);
        info.put(CREDENTIALS, creds);

        assertSame(creds, info.get(CREDENTIALS));

        final String user = "user";
        final char[] pwd = new char[5];
        final AuthenticationInfo infoCred = new AuthenticationInfo("TEST",
            user, pwd);

        // credentials not stored in the object
        assertFalse(infoCred.containsKey(CREDENTIALS));
    }

    @Test
    public void testRemoveObject() {
        final AuthenticationInfo info = new AuthenticationInfo("test");

        final Object value = "test";
        info.put("test", value);
        assertSame(value, info.get("test"));

        final Object removed = info.remove("test");
        assertSame(value, removed);
        assertFalse(info.containsKey("test"));

        assertNull(info.remove(AuthenticationInfo.AUTH_TYPE));
        Assert.assertEquals("test", info.get("sling.authType"));
        assertNull(info.remove("sling.authType"));
        Assert.assertEquals("test", info.getAuthType());
    }

    @Test
    public void testPutStringObject() {
        final AuthenticationInfo info = new AuthenticationInfo("test", "user",
            new char[2]);
        info.put(CREDENTIALS,
                new SimpleCredentials("user", new char[2]));

        test_put_fail(info, AuthenticationInfo.AUTH_TYPE, null);
        test_put_fail(info, ResourceResolverFactory.USER, null);
        test_put_fail(info, ResourceResolverFactory.PASSWORD, null);

        test_put_fail(info, AuthenticationInfo.AUTH_TYPE, 42);
        test_put_fail(info, ResourceResolverFactory.USER, 42);
        test_put_fail(info, ResourceResolverFactory.PASSWORD, "string");

        test_put_success(info, AuthenticationInfo.AUTH_TYPE, "new_type");
        test_put_success(info, ResourceResolverFactory.USER, "new_user");
        test_put_success(info, ResourceResolverFactory.PASSWORD,
            "new_pwd".toCharArray());
    }

    private void test_put_success(final AuthenticationInfo info,
            final String key, final Object value) {
        final Object oldValue = info.get(key);
        final Object replacedValue = info.put(key, value);
        assertSame(oldValue, replacedValue);
        assertSame(value, info.get(key));
    }

    private void test_put_fail(final AuthenticationInfo info, final String key,
            final Object value) {
        final Object oldValue = info.get(key);
        try {
            info.put(key, value);
            fail("Expected put failure for " + key + "=" + value);
        } catch (IllegalArgumentException iae) {
            // expected
        }
        assertSame(oldValue, info.get(key));
    }

    static void assertEquals(final char[] expected, final char[] actual) {
        if (expected == null && actual != null) {
            failNotEquals(null, expected, actual);
        }
        if (expected != null && actual == null) {
            failNotEquals(null, expected, actual);
        }
        if (expected.length != actual.length) {
            failNotEquals(null, expected, actual);
        }

        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != actual[i]) {
                failNotEquals(null, expected, actual);
            }
        }
    }
}

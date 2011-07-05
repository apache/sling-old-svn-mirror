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

import javax.servlet.http.HttpServletRequest;
import junit.framework.TestCase;

import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class AbstractAuthenticationHandlerTest {

    final Mockery context = new JUnit4Mockery();
    final ResourceResolver resolver = context.mock(ResourceResolver.class);
    final HttpServletRequest request = context.mock(HttpServletRequest.class);

    @Test
    public void test_isRedirectValid_null_empty() {
        TestCase.assertFalse(AbstractAuthenticationHandler.isRedirectValid(
            null, null));
        TestCase.assertFalse(AbstractAuthenticationHandler.isRedirectValid(
            null, ""));
    }

    @Test
    public void test_isRedirectValid_url() {
        TestCase.assertFalse(AbstractAuthenticationHandler.isRedirectValid(
            null, "http://www.google.com"));
    }

    @Test
    public void test_isRedirectValid_no_request() {
        TestCase.assertFalse(AbstractAuthenticationHandler.isRedirectValid(
            null, "relative/path"));
        TestCase.assertTrue(AbstractAuthenticationHandler.isRedirectValid(null,
            "/absolute/path"));
    }

    @Test
    public void test_isRedirectValid_no_resource_resolver() {
        context.checking(new Expectations(){{
            allowing(request).getAttribute(with(any(String.class)));
            will(returnValue(null));
        }});

        TestCase.assertFalse(AbstractAuthenticationHandler.isRedirectValid(
            request, "relative/path"));
        TestCase.assertTrue(AbstractAuthenticationHandler.isRedirectValid(
            request, "/absolute/path"));
    }

    @Test
    public void test_isRedirectValid_resource_resolver() {
        context.checking(new Expectations(){{
            allowing(resolver).resolve(with(any(HttpServletRequest.class)), with(equal("/absolute/path")));
            will(returnValue(new SyntheticResource(resolver, "/absolute/path", "test")));

            allowing(resolver).resolve(with(any(HttpServletRequest.class)), with(equal("relative/path")));
            will(returnValue(new NonExistingResource(resolver, "relative/path")));

            allowing(request).getAttribute(with(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER));
            will(returnValue(resolver));

            allowing(request).getAttribute(with(any(String.class)));
            will(returnValue(null));
        }});

        TestCase.assertFalse(AbstractAuthenticationHandler.isRedirectValid(
            request, "relative/path"));
        TestCase.assertTrue(AbstractAuthenticationHandler.isRedirectValid(
            request, "/absolute/path"));
    }

}

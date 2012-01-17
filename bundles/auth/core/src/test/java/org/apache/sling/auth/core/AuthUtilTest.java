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
package org.apache.sling.auth.core;

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
public class AuthUtilTest {

    final Mockery context = new JUnit4Mockery();

    final ResourceResolver resolver = context.mock(ResourceResolver.class);

    final HttpServletRequest request = context.mock(HttpServletRequest.class);

    @Test
    public void test_isRedirectValid_null_empty() {
        TestCase.assertFalse(AuthUtil.isRedirectValid(null, null));
        TestCase.assertFalse(AuthUtil.isRedirectValid(null, ""));
    }

    @Test
    public void test_isRedirectValid_url() {
        TestCase.assertFalse(AuthUtil.isRedirectValid(null, "http://www.google.com"));
    }

    @Test
    public void test_isRedirectValid_no_request() {
        TestCase.assertFalse(AuthUtil.isRedirectValid(null, "relative/path"));
        TestCase.assertTrue(AuthUtil.isRedirectValid(null, "/absolute/path"));
    }

    @Test
    public void test_isRedirectValid_normalized() {
        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/unnormalized//double/slash"));
        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/unnormalized/double/slash//"));
        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/unnormalized/./dot"));
        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/unnormalized/../dot"));
        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/unnormalized/dot/."));
        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/unnormalized/dot/.."));
    }

    @Test
    public void test_isRedirectValid_invalid_characters() {
        context.checking(new Expectations() {
            {
                allowing(request).getContextPath();
                will(returnValue(""));
                allowing(request).getAttribute(with(any(String.class)));
                will(returnValue(null));
            }
        });

        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/illegal/</x"));
        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/illegal/>/x"));
        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/illegal/'/x"));
        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/illegal/\"/x"));
    }

    @Test
    public void test_isRedirectValid_no_resource_resolver_root_context() {
        context.checking(new Expectations() {
            {
                allowing(request).getContextPath();
                will(returnValue(""));
                allowing(request).getAttribute(with(any(String.class)));
                will(returnValue(null));
            }
        });

        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "relative/path"));
        TestCase.assertTrue(AuthUtil.isRedirectValid(request, "/absolute/path"));
        TestCase.assertTrue(AuthUtil.isRedirectValid(request, "/"));
    }

    @Test
    public void test_isRedirectValid_no_resource_resolver_non_root_context() {
        context.checking(new Expectations() {
            {
                allowing(request).getContextPath();
                will(returnValue("/ctx"));
                allowing(request).getAttribute(with(any(String.class)));
                will(returnValue(null));
            }
        });

        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "relative/path"));
        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/absolute/path"));

        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "ctx/relative/path"));
        TestCase.assertTrue(AuthUtil.isRedirectValid(request, "/ctx/absolute/path"));

        TestCase.assertTrue(AuthUtil.isRedirectValid(request, "/ctx/"));
        TestCase.assertTrue(AuthUtil.isRedirectValid(request, "/ctx"));
    }

    @Test
    public void test_isRedirectValid_resource_resolver_root_context() {
        context.checking(new Expectations() {
            {
                allowing(resolver).resolve(with(any(HttpServletRequest.class)), with(equal("/absolute/path")));
                will(returnValue(new SyntheticResource(resolver, "/absolute/path", "test")));

                allowing(resolver).resolve(with(any(HttpServletRequest.class)), with(equal("relative/path")));
                will(returnValue(new NonExistingResource(resolver, "relative/path")));

                allowing(resolver).resolve(with(any(HttpServletRequest.class)), with(any(String.class)));
                will(returnValue(new NonExistingResource(resolver, "/absolute/missing")));

                allowing(request).getAttribute(with(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER));
                will(returnValue(resolver));

                allowing(request).getAttribute(with(any(String.class)));
                will(returnValue(null));

                allowing(request).getContextPath();
                will(returnValue(""));
            }
        });

        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "relative/path"));
        TestCase.assertTrue(AuthUtil.isRedirectValid(request, "/absolute/path"));

        TestCase.assertTrue(AuthUtil.isRedirectValid(request, "/absolute/missing"));
        TestCase.assertTrue(AuthUtil.isRedirectValid(request, "/absolute/missing/valid"));
        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/absolute/missing/invalid/<"));
        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/absolute/missing/invalid/>"));
        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/absolute/missing/invalid/'"));
        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/absolute/missing/invalid/\""));
    }

    @Test
    public void test_isRedirectValid_resource_resolver_non_root_context() {
        context.checking(new Expectations() {
            {
                allowing(resolver).resolve(with(any(HttpServletRequest.class)), with(equal("/absolute/path")));
                will(returnValue(new SyntheticResource(resolver, "/absolute/path", "test")));

                allowing(resolver).resolve(with(any(HttpServletRequest.class)), with(equal("relative/path")));
                will(returnValue(new NonExistingResource(resolver, "relative/path")));

                allowing(request).getAttribute(with(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER));
                will(returnValue(resolver));

                allowing(request).getAttribute(with(any(String.class)));
                will(returnValue(null));

                allowing(request).getContextPath();
                will(returnValue("/ctx"));
            }
        });

        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "relative/path"));
        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/absolute/path"));

        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "ctx/relative/path"));
        TestCase.assertTrue(AuthUtil.isRedirectValid(request, "/ctx/absolute/path"));

        TestCase.assertFalse(AuthUtil.isRedirectValid(request, "/ctxrelative/path"));
    }

    @Test
    public void test_isBrowserRequest_null() {
        context.checking(new Expectations() {
            {
                allowing(request).getHeader(with("User-Agent"));
                will(returnValue(null));
            }
        });
        TestCase.assertFalse(AuthUtil.isBrowserRequest(request));
    }

    @Test
    public void test_isBrowserRequest_Mozilla() {
        context.checking(new Expectations() {
            {
                allowing(request).getHeader(with("User-Agent"));
                will(returnValue("This is firefox (Mozilla)"));
            }
        });
        TestCase.assertTrue(AuthUtil.isBrowserRequest(request));
    }

    @Test
    public void test_isBrowserRequest_Opera() {
        context.checking(new Expectations() {
            {
                allowing(request).getHeader(with("User-Agent"));
                will(returnValue("This is opera (Opera)"));
            }
        });
        TestCase.assertTrue(AuthUtil.isBrowserRequest(request));
    }

    @Test
    public void test_isBrowserRequest_WebDAV() {
        context.checking(new Expectations() {
            {
                allowing(request).getHeader(with("User-Agent"));
                will(returnValue("WebDAV Client"));
            }
        });
        TestCase.assertFalse(AuthUtil.isBrowserRequest(request));
    }
}

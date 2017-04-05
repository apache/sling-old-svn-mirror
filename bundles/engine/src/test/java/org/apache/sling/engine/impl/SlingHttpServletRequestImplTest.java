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
package org.apache.sling.engine.impl;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.impl.request.RequestData;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Test;

public class SlingHttpServletRequestImplTest {

    SlingHttpServletRequestImpl slingHttpServletRequestImpl;
    
    private Mockery context = new JUnit4Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
    
    @Test
    public void getUserPrincipal_test() {
        final HttpServletRequest servletRequest = context.mock(HttpServletRequest.class);
        
        context.checking(new Expectations() {{
            one(servletRequest).getServletPath();
            will(returnValue("/path"));
            allowing(servletRequest).getPathInfo();
            will(returnValue("/path"));
            allowing(servletRequest).getRemoteUser();
            will(returnValue("remoteUser"));
        }});
        
        final RequestData requestData = context.mock(RequestData.class, "requestData");        
        final ResourceResolver resourceResolver = context.mock(ResourceResolver.class);
        
        context.checking(new Expectations() {{
            allowing(requestData).getResourceResolver();
            will(returnValue(resourceResolver));
            allowing(resourceResolver).adaptTo(Principal.class);
            will(returnValue(null));
        }});
        
        slingHttpServletRequestImpl = new SlingHttpServletRequestImpl(requestData, servletRequest);
        Assert.assertEquals("UserPrincipal: remoteUser", slingHttpServletRequestImpl.getUserPrincipal().toString());
    }

    @Test
    public void getUserPrincipal_test2() {
        final HttpServletRequest servletRequest = context.mock(HttpServletRequest.class);
        
        context.checking(new Expectations() {{
            one(servletRequest).getServletPath();
            will(returnValue("/path"));
            allowing(servletRequest).getPathInfo();
            will(returnValue("/path"));
            allowing(servletRequest).getRemoteUser();
            will(returnValue(null));
        }});
        
        final RequestData requestData = context.mock(RequestData.class, "requestData");        
        final ResourceResolver resourceResolver = context.mock(ResourceResolver.class);
        
        context.checking(new Expectations() {{
            allowing(requestData).getResourceResolver();
            will(returnValue(resourceResolver));
            allowing(resourceResolver).adaptTo(Principal.class);
            will(returnValue(null));
        }});
        
        slingHttpServletRequestImpl = new SlingHttpServletRequestImpl(requestData, servletRequest);
        Assert.assertNull(slingHttpServletRequestImpl.getUserPrincipal());
    }
    
    @Test
    public void getUserPrincipal_test3() {
        final HttpServletRequest servletRequest = context.mock(HttpServletRequest.class);
        
        context.checking(new Expectations() {{
            one(servletRequest).getServletPath();
            will(returnValue("/path"));
            allowing(servletRequest).getPathInfo();
            will(returnValue("/path"));
        }});
        
        final RequestData requestData = context.mock(RequestData.class, "requestData");        
        final ResourceResolver resourceResolver = context.mock(ResourceResolver.class);
        final Principal principal = context.mock(Principal.class);
        
        context.checking(new Expectations() {{
            allowing(requestData).getResourceResolver();
            will(returnValue(resourceResolver));
            allowing(resourceResolver).adaptTo(Principal.class);
            will(returnValue(principal));
        }});
        
        slingHttpServletRequestImpl = new SlingHttpServletRequestImpl(requestData, servletRequest);
        Assert.assertEquals(principal, slingHttpServletRequestImpl.getUserPrincipal());
    }
}

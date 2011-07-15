/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.security.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

public class ReferrerFilterTest {

    protected ReferrerFilter filter;

    @Before public void setup() {
        filter = new ReferrerFilter();
        final ComponentContext ctx = mock(ComponentContext.class);
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        when(ctx.getProperties()).thenReturn(props);
        filter.activate(ctx);
    }

    @Test public void testHostName() {
        Assert.assertEquals("somehost", filter.getHost("http://somehost"));
        Assert.assertEquals("somehost", filter.getHost("http://somehost/somewhere"));
        Assert.assertEquals("somehost", filter.getHost("http://somehost:4242/somewhere"));
        Assert.assertEquals("somehost", filter.getHost("http://admin@somehost/somewhere"));
        Assert.assertEquals("somehost", filter.getHost("http://admin@somehost:1/somewhere"));
        Assert.assertEquals("somehost", filter.getHost("http://admin:admin@somehost/somewhere"));
        Assert.assertEquals("somehost", filter.getHost("http://admin:admin@somehost:4343/somewhere"));
        Assert.assertEquals(null, filter.getHost("http:/admin:admin@somehost:4343/somewhere"));
    }

    private HttpServletRequest getRequest(final String referrer) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("http://somehost/somewhere");
        when(request.getHeader("referer")).thenReturn(referrer);
        when(request.getServerName()).thenReturn("me");
        return request;
    }

    @Test public void testValidRequest() {
        Assert.assertEquals(true, filter.isValidRequest(getRequest(null)));
        Assert.assertEquals(true, filter.isValidRequest(getRequest("relative")));
        Assert.assertEquals(true, filter.isValidRequest(getRequest("/relative/too")));
        Assert.assertEquals(true, filter.isValidRequest(getRequest("/relative/but/[illegal]")));
        Assert.assertEquals(false, filter.isValidRequest(getRequest("http://somehost")));
        Assert.assertEquals(true, filter.isValidRequest(getRequest("http://me")));
        Assert.assertEquals(false, filter.isValidRequest(getRequest("http://somehost/but/[illegal]")));
        Assert.assertEquals(true, filter.isValidRequest(getRequest("http://me/but/[illegal]")));
    }
}

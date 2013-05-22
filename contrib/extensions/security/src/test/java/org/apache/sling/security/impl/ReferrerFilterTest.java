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

import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

public class ReferrerFilterTest {

    protected ReferrerFilter filter;

    @Before public void setup() {
        filter = new ReferrerFilter();
        final ComponentContext ctx = mock(ComponentContext.class);
        final BundleContext bundleCtx = mock(BundleContext.class);
        final ServiceRegistration reg = mock(ServiceRegistration.class);
        final Dictionary<String, Object> props = new Hashtable<String, Object>(){{
            put("allow.hosts", new String[]{"relhost"});
            put("allow.hosts.regexp", new String[]{"http://([^.]*.)?abshost:80"});
        }};
        doReturn(props).when(ctx).getProperties();
        doReturn(bundleCtx).when(ctx).getBundleContext();
        doReturn(reg).when(bundleCtx).registerService(any(String[].class), any(), any(Dictionary.class));
        doNothing().when(reg).unregister();
        filter.activate(ctx);
    }

    @Test public void testHostName() {
        Assert.assertEquals("somehost", filter.getHost("http://somehost").host);
        Assert.assertEquals("somehost", filter.getHost("http://somehost/somewhere").host);
        Assert.assertEquals("somehost", filter.getHost("http://somehost:4242/somewhere").host);
        Assert.assertEquals("somehost", filter.getHost("http://admin@somehost/somewhere").host);
        Assert.assertEquals("somehost", filter.getHost("http://admin@somehost/somewhere?invald=@gagga").host);
        Assert.assertEquals("somehost", filter.getHost("http://admin@somehost:1/somewhere").host);
        Assert.assertEquals("somehost", filter.getHost("http://admin:admin@somehost/somewhere").host);
        Assert.assertEquals("somehost", filter.getHost("http://admin:admin@somehost:4343/somewhere").host);
        Assert.assertEquals("localhost", filter.getHost("http://localhost").host);
        Assert.assertEquals("127.0.0.1", filter.getHost("http://127.0.0.1").host);
        Assert.assertEquals("localhost", filter.getHost("http://localhost:535").host);
        Assert.assertEquals("127.0.0.1", filter.getHost("http://127.0.0.1:242").host);
        Assert.assertEquals("localhost", filter.getHost("http://localhost:256235/etewteq.ff").host);
        Assert.assertEquals("127.0.0.1", filter.getHost("http://127.0.0.1/wetew.qerq").host);
        Assert.assertEquals(null, filter.getHost("http:/admin:admin@somehost:4343/somewhere"));
    }

    private HttpServletRequest getRequest(final String referrer) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("http://somehost/somewhere");
        when(request.getHeader("referer")).thenReturn(referrer);
        return request;
    }

    @Test public void testValidRequest() {
        Assert.assertEquals(true, filter.isValidRequest(getRequest(null)));
        Assert.assertEquals(true, filter.isValidRequest(getRequest("relative")));
        Assert.assertEquals(true, filter.isValidRequest(getRequest("/relative/too")));
        Assert.assertEquals(true, filter.isValidRequest(getRequest("/relative/but/[illegal]")));
        Assert.assertEquals(false, filter.isValidRequest(getRequest("http://somehost")));
        Assert.assertEquals(true, filter.isValidRequest(getRequest("http://localhost")));
        Assert.assertEquals(true, filter.isValidRequest(getRequest("http://127.0.0.1")));
        Assert.assertEquals(false, filter.isValidRequest(getRequest("http://somehost/but/[illegal]")));
        Assert.assertEquals(true, filter.isValidRequest(getRequest("http://relhost")));
        Assert.assertEquals(true, filter.isValidRequest(getRequest("http://relhost:9001")));
        Assert.assertEquals(false, filter.isValidRequest(getRequest("http://abshost:9001")));
        Assert.assertEquals(false, filter.isValidRequest(getRequest("https://abshost:80")));
        Assert.assertEquals(true, filter.isValidRequest(getRequest("http://abshost:80")));
        Assert.assertEquals(false, filter.isValidRequest(getRequest("http://abshost:9001")));
        Assert.assertEquals(true, filter.isValidRequest(getRequest("http://another.abshost:80")));
        Assert.assertEquals(false, filter.isValidRequest(getRequest("http://yet.another.abshost:80")));
    }
}

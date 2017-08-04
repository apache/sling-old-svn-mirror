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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.Dictionary;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class ReferrerFilterTest {

    protected ReferrerFilter filter;

    @Before
    public void setup() {
        filter = new ReferrerFilter();
        final BundleContext bundleCtx = mock(BundleContext.class);
        final ServiceRegistration reg = mock(ServiceRegistration.class);

        ReferrerFilter.Config config = new ReferrerFilter.Config() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public boolean allow_empty() {
                return false;
            }

            @Override
            public String[] allow_hosts() {
                return new String[]{"relhost"};
            }

            @Override
            public String[] allow_hosts_regexp() {
                return new String[]{"http://([^.]*.)?abshost:80"};
            }

            @Override
            public String[] filter_methods() {
                return new String[0];
            }

            @Override
            public String[] exclude_agents_regexp() {
                return new String[]{"[a-zA-Z]*\\/[0-9]*\\.[0-9]*;Some-Agent\\s.*"};
            }
        };

        doReturn(reg).when(bundleCtx).registerService(any(String[].class), any(), any(Dictionary.class));
        doNothing().when(reg).unregister();
        filter.activate(bundleCtx, config);
    }

    @Test
    public void testHostName() {
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

    private HttpServletRequest getRequest(final String referrer, final String userAgent) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("http://somehost/somewhere");
        when(request.getHeader("referer")).thenReturn(referrer);
        if ( userAgent != null && userAgent.length() > 0 ) {
            when(request.getHeader("User-Agent")).thenReturn(userAgent);
        }
        return request;
    }

    private HttpServletRequest getRequest(final String referrer) {
        return getRequest(referrer, null);
    }

    @Test
    public void testValidRequest() {
        Assert.assertEquals(false, filter.isValidRequest(getRequest(null)));
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

    @Test
    public void testIsBrowserRequest() {
        String userAgent = "Mozilla/5.0;Some-Agent (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/603.2.4 (KHTML, like Gecko)";
        Assert.assertEquals(false, filter.isBrowserRequest(getRequest(null, userAgent)));
        userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/603.2.4 (KHTML, like Gecko)";
        Assert.assertEquals(true, filter.isBrowserRequest(getRequest(null, userAgent)));
    }
}

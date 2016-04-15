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
package org.apache.sling.engine.impl.request;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.request.TooManyCallsException;
import org.apache.sling.engine.impl.SlingHttpServletRequestImpl;
import org.apache.sling.engine.impl.SlingHttpServletResponseImpl;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.fail;

@RunWith(JMock.class)
public class RequestDataTest {

    private Mockery context;
    private RequestData requestData;
    private HttpServletRequest req;
    private HttpServletResponse resp;
    private SlingHttpServletRequest slingRequest;
    private SlingHttpServletResponse slingResponse;
    
    @Before
    public void setup() throws Exception {
        context = new JUnit4Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        req = context.mock(HttpServletRequest.class);
        resp = context.mock(HttpServletResponse.class);
        
        final ContentData contentData = context.mock(ContentData.class);
        final Servlet servlet = context.mock(Servlet.class);
        final ServletConfig servletConfig = context.mock(ServletConfig.class);
        
        context.checking(new Expectations() {{
            allowing(req).getServletPath();
            will(returnValue("/"));
            
            allowing(req).getPathInfo();
            will(returnValue(""));
            
            allowing(req).getMethod();
            will(returnValue("GET"));
            
            allowing(req).setAttribute(with(any(String.class)), with(any(Object.class)));
            
            allowing(contentData).getServlet();
            will(returnValue(servlet));
            
            allowing(servlet).getServletConfig();
            will(returnValue(servletConfig));
            
            allowing(servlet).service(with(any(ServletRequest.class)), with(any(ServletResponse.class)));
            
            allowing(servletConfig).getServletName();
            will(returnValue("SERVLET_NAME"));
            
            allowing(req).getAttribute(RequestProgressTracker.class.getName());
            will(returnValue(null));
        }});
        
        requestData = new RequestData(null, req, resp) {
            @Override
            public ContentData getContentData() {
                return contentData;
            }
        };
        
        slingRequest = new SlingHttpServletRequestImpl(requestData, req);
        slingResponse = new SlingHttpServletResponseImpl(requestData, resp);
        
        RequestData.setMaxCallCounter(2);
    }
    
    private void assertTooManyCallsException(int failAtCall) throws Exception {
        for(int i=0; i  < failAtCall - 1; i++) {
            RequestData.service(slingRequest, slingResponse);
        }
        try {
            RequestData.service(slingRequest, slingResponse);
            fail("Expected RequestData.service to fail when called " + failAtCall + " times");
        } catch(TooManyCallsException tme) {
            // as expected
        }
    }
    
    @Test
    public void testTooManyCallsDefault() throws Exception {
        context.checking(new Expectations() {{
            allowing(req).getAttribute(with(any(String.class)));
            will(returnValue(null));
        }});
        assertTooManyCallsException(3);
    }
    
    @Test
    public void testTooManyCallsOverride() throws Exception {
        context.checking(new Expectations() {{
            allowing(req).getAttribute(with(any(String.class)));
            will(returnValue(1));
        }});
        assertTooManyCallsException(2);
    }
}

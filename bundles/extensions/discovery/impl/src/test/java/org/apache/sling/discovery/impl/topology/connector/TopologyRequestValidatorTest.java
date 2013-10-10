/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.discovery.impl.topology.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.sling.discovery.impl.Config;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TopologyRequestValidatorTest {
    
    private TopologyRequestValidator topologyRequestValidator;
    private Mockery context = new JUnit4Mockery();


    @Before
    public void before() throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        Config config= new Config();
        setPrivate(config, "sharedKey", "testKey");
        setPrivate(config, "hmacEnabled", true);
        setPrivate(config, "encryptionEnabled", true);
        setPrivate(config, "keyInterval", 3600*100*4);
        topologyRequestValidator = new TopologyRequestValidator(config);
    }
    
    private void setPrivate(Object o, String field, Object value) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field f = o.getClass().getDeclaredField(field);
        if ( !f.isAccessible()) {
            f.setAccessible(true);
        }
        f.set(o, value);
    }

    @Test
    public void testTrustRequest() throws IOException {
        final PutMethod method = new PutMethod("/TestUri");
        String clearMessage = "TestMessage";
        final String message = topologyRequestValidator.encodeMessage(clearMessage);
        Assert.assertNotNull(message);
        Assert.assertNotEquals(message, clearMessage);
        topologyRequestValidator.trustMessage(method, message);
        
        Assert.assertNotNull(method.getRequestHeader(TopologyRequestValidator.HASH_HEADER));
        Assert.assertNotNull(method.getRequestHeader(TopologyRequestValidator.HASH_HEADER).getValue());
        Assert.assertTrue(method.getRequestHeader(TopologyRequestValidator.HASH_HEADER).getValue().length() > 0);
        Assert.assertNotNull(method.getRequestHeader(TopologyRequestValidator.SIG_HEADER));
        Assert.assertNotNull(method.getRequestHeader(TopologyRequestValidator.SIG_HEADER).getValue());
        Assert.assertTrue(method.getRequestHeader(TopologyRequestValidator.SIG_HEADER).getValue().length() > 0);
        final HttpServletRequest request = context.mock(HttpServletRequest.class);
        context.checking(new Expectations() {
            {
                allowing(request).getHeader(with(TopologyRequestValidator.HASH_HEADER));
                will(returnValue(method.getRequestHeader(TopologyRequestValidator.HASH_HEADER).getValue()));
                
                allowing(request).getHeader(with(TopologyRequestValidator.SIG_HEADER));
                will(returnValue(method.getRequestHeader(TopologyRequestValidator.SIG_HEADER).getValue()));
                
                allowing(request).getRequestURI();
                will(returnValue(method.getPath()));
                
                allowing(request).getReader();
                will(returnValue(new BufferedReader(new StringReader(message))));
            }
        });
        
        Assert.assertTrue(topologyRequestValidator.isTrusted(request));
        Assert.assertEquals(clearMessage, topologyRequestValidator.decodeMessage(request));
    }
    
    
    
    @Test
    public void testTrustResponse() throws IOException {
        final HttpServletRequest request = context.mock(HttpServletRequest.class);
        context.checking(new Expectations() {
            {
                allowing(request).getRequestURI();
                will(returnValue("/Test/Uri2"));
            }
        });

        final HttpServletResponse response = context.mock(HttpServletResponse.class);
        final Map<Object, Object> headers = new HashMap<Object, Object>();
        context.checking(new Expectations() {
            {
                allowing(response).setHeader(with(any(String.class)), with(any(String.class)));
                will(new Action(){

                    public void describeTo(Description desc) {
                        desc.appendText("Setting header ");
                    }

                    public Object invoke(Invocation invocation) throws Throwable {
                        headers.put(invocation.getParameter(0), invocation.getParameter(1));
                        return null;
                    }
                    
                });
            }
        });

        String clearMessage =  "TestMessage2";
        final String message = topologyRequestValidator.encodeMessage(clearMessage);
        topologyRequestValidator.trustMessage(response, request, message);
        
        final HttpMethod method = context.mock(HttpMethod.class);
        context.checking(new Expectations(){
            {
                allowing(method).getResponseHeader(with(any(String.class)));
                will(new Action() {
                    public void describeTo(Description desc) {
                        desc.appendText("Getting header ");
                    }

                    public Object invoke(Invocation invocation) throws Throwable {
                        return new Header((String)invocation.getParameter(0), (String)headers.get(invocation.getParameter(0)));
                    }
                    
                });
                
                allowing(method).getPath();
                will(returnValue("/Test/Uri2"));
                
                allowing(method).getResponseBodyAsString();
                will(returnValue(message));
            } 
        });
        topologyRequestValidator.isTrusted(method);
        topologyRequestValidator.decodeMessage(method);
        
    }
    
    

}

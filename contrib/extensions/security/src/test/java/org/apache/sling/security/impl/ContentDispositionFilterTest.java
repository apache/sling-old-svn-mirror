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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import junitx.util.PrivateAccessor;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

public class ContentDispositionFilterTest {
    
    private ContentDispositionFilter contentDispositionFilter;
    private final Mockery context = new JUnit4Mockery();

    @Test
    public void test_activator1() throws Throwable{
        contentDispositionFilter = new ContentDispositionFilter();
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        Set<String> contentDispositionPaths = ( Set<String> ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPaths");
        Assert.assertEquals(1, contentDispositionPaths.size());   
        String[] contentDispositionPathsPfx = ( String[] ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPathsPfx");
        Assert.assertEquals(0, contentDispositionPathsPfx.length);   
        Map <String, Set<String>> contentTypesMapping = ( Map <String, Set<String>> ) PrivateAccessor.getField(contentDispositionFilter, "contentTypesMapping");
        Assert.assertEquals(0, contentTypesMapping.size());           
    }
    
    @Test
    public void test_activator2() throws Throwable{
        contentDispositionFilter = new ContentDispositionFilter();
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        Set<String> contentDispositionPaths = ( Set<String> ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPaths");
        Assert.assertEquals(0, contentDispositionPaths.size());   
        String[] contentDispositionPathsPfx = ( String[] ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPathsPfx");
        Assert.assertEquals(1, contentDispositionPathsPfx.length);   
        Map <String, Set<String>> contentTypesMapping = ( Map <String, Set<String>> ) PrivateAccessor.getField(contentDispositionFilter, "contentTypesMapping");
        Assert.assertEquals(0, contentTypesMapping.size());           
    }
    
    @Test
    public void test_activator3() throws Throwable{
        contentDispositionFilter = new ContentDispositionFilter();
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/libs", "/content/usergenerated/*"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        Set<String> contentDispositionPaths = ( Set<String> ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPaths");
        Assert.assertEquals(1, contentDispositionPaths.size());   
        String[] contentDispositionPathsPfx = ( String[] ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPathsPfx");
        Assert.assertEquals(1, contentDispositionPathsPfx.length);   
        Map <String, Set<String>> contentTypesMapping = ( Map <String, Set<String>> ) PrivateAccessor.getField(contentDispositionFilter, "contentTypesMapping");
        Assert.assertEquals(0, contentTypesMapping.size());           
    }
    
    @Test
    public void test_activator5() throws Throwable{
        contentDispositionFilter = new ContentDispositionFilter();
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"*"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        Set<String> contentDispositionPaths = ( Set<String> ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPaths");
        Assert.assertEquals(0, contentDispositionPaths.size());   
        String[] contentDispositionPathsPfx = ( String[] ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPathsPfx");
        Assert.assertEquals(0, contentDispositionPathsPfx.length);   
        Map <String, Set<String>> contentTypesMapping = ( Map <String, Set<String>> ) PrivateAccessor.getField(contentDispositionFilter, "contentTypesMapping");
        Assert.assertEquals(0, contentTypesMapping.size());           
    }
    
    @Test
    public void test_activator6() throws Throwable{
        contentDispositionFilter = new ContentDispositionFilter();
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/libs:*"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        Set<String> contentDispositionPaths = ( Set<String> ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPaths");
        Assert.assertEquals(0, contentDispositionPaths.size());   
        String[] contentDispositionPathsPfx = ( String[] ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPathsPfx");
        Assert.assertEquals(0, contentDispositionPathsPfx.length);   
        Map <String, Set<String>> contentTypesMapping = ( Map <String, Set<String>> ) PrivateAccessor.getField(contentDispositionFilter, "contentTypesMapping");
        Assert.assertEquals(0, contentTypesMapping.size());           
    }
    
    @Test
    public void test_activator7() throws Throwable{
        contentDispositionFilter = new ContentDispositionFilter();
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/libs:text/html,text/plain","/content/usergenerated/*:image/jpeg"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        Set<String> contentDispositionPaths = ( Set<String> ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPaths");
        Assert.assertEquals(1, contentDispositionPaths.size());   
        String[] contentDispositionPathsPfx = ( String[] ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPathsPfx");
        Assert.assertEquals(1, contentDispositionPathsPfx.length);   
        Map <String, Set<String>> contentTypesMapping = ( Map <String, Set<String>> ) PrivateAccessor.getField(contentDispositionFilter, "contentTypesMapping");
        Assert.assertEquals(2, contentTypesMapping.size()); 
        Set<String> libsMapping = contentTypesMapping.get("/libs");
        Assert.assertEquals(2, libsMapping.size());
        libsMapping.contains("text/html");
        libsMapping.contains("text/plain");
 
        Set<String> userGeneratedMapping = contentTypesMapping.get("/content/usergenerated/");
        Assert.assertEquals(1, userGeneratedMapping.size()); 
        userGeneratedMapping.contains("image/jpeg");
     }
    
    @Test
    public void test_getContentTypes() throws Throwable{
        // null content types
        String contentType = null;
        Set <String> contentTypesSet = ( Set <String>) PrivateAccessor.invoke(ContentDispositionFilter.class,"getContentTypes",  new Class[]{String.class},new Object[]{contentType});
        Assert.assertEquals(0, contentTypesSet.size());   
        // empty content types
        contentType = "";
        contentTypesSet = ( Set <String>) PrivateAccessor.invoke(ContentDispositionFilter.class,"getContentTypes",  new Class[]{String.class},new Object[]{contentType});
        Assert.assertEquals(0, contentTypesSet.size());  
        contentType = "text/html";
        contentTypesSet = ( Set <String>) PrivateAccessor.invoke(ContentDispositionFilter.class,"getContentTypes",  new Class[]{String.class},new Object[]{contentType});
        Assert.assertEquals(1, contentTypesSet.size()); 
        contentType = "text/html,text/plain";
        contentTypesSet = ( Set <String>) PrivateAccessor.invoke(ContentDispositionFilter.class,"getContentTypes",  new Class[]{String.class},new Object[]{contentType});
        Assert.assertEquals(2, contentTypesSet.size()); 
    }
    
    @Test
    public void test_doFilter1() throws Throwable{       
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        contentDispositionFilter = new ContentDispositionFilter();
        
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        
        context.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/libs"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
                
            }
        });       
        rewriterResponse.setContentType("text/html");
    }
    
    @Test
    public void test_doFilter2() throws Throwable{       
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        contentDispositionFilter = new ContentDispositionFilter();
        
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        
        context.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/content/usergenerated/author"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
            }
        });       
        rewriterResponse.setContentType("text/html");
    }
    
    @Test
    public void test_doFilter3() throws Throwable{       
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        contentDispositionFilter = new ContentDispositionFilter();
        
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        
        context.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/content/usergenerated"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION IS SET
                exactly(1).of(response).addHeader("Content-Disposition", "attachment");
            }
        });       
        rewriterResponse.setContentType("text/html");
    }
    
    @Test
    public void test_doFilter4() throws Throwable{       
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        contentDispositionFilter = new ContentDispositionFilter();
        
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        
        context.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/libs"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
                
            }
        });       
        rewriterResponse.setContentType("text/html");
    }
    
    @Test
    public void test_doFilter5() throws Throwable{       
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        contentDispositionFilter = new ContentDispositionFilter();
        
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        
        context.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/content/usergenerated/author"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION IS SET
                exactly(1).of(response).addHeader("Content-Disposition", "attachment");
            }
        });       
        rewriterResponse.setContentType("text/html");
    }
    
    @Test
    public void test_doFilter6() throws Throwable{       
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        contentDispositionFilter = new ContentDispositionFilter();
        
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        
        context.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/content/usergenerated"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION IS SET
                exactly(1).of(response).addHeader("Content-Disposition", "attachment");
            }
        });       
        rewriterResponse.setContentType("text/html");
    }
    
    @Test
    public void test_doFilter7() throws Throwable{       
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        contentDispositionFilter = new ContentDispositionFilter();
        
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated:text/html,text/plain"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        
        context.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/libs"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
                
            }
        });       
        rewriterResponse.setContentType("text/html");
    }
    
    @Test
    public void test_doFilter8() throws Throwable{       
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        contentDispositionFilter = new ContentDispositionFilter();
        
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated:text/html,text/plain"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        
        context.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/content/usergenerated/author"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
            }
        });       
        rewriterResponse.setContentType("text/html");
    }
    
    @Test
    public void test_doFilter9() throws Throwable{       
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        contentDispositionFilter = new ContentDispositionFilter();
        
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated:text/html,text/plain"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        
        context.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/content/usergenerated"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
            }
        });       
        rewriterResponse.setContentType("text/html");
    }
    
    @Test
    public void test_doFilter10() throws Throwable{       
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        contentDispositionFilter = new ContentDispositionFilter();
        
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated:text/html,text/plain"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        
        context.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/content/usergenerated"));
                allowing(response).setContentType("image/jpeg");
                //CONTENT DISPOSITION IS SET
                exactly(1).of(response).addHeader("Content-Disposition", "attachment");
            }
        });       
        rewriterResponse.setContentType("image/jpeg");
    }
    
    @Test
    public void test_doFilter11() throws Throwable{       
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        contentDispositionFilter = new ContentDispositionFilter();
        
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*:text/html,text/plain"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        
        context.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/libs"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
                
            }
        });       
        rewriterResponse.setContentType("text/html");
    }
    
    @Test
    public void test_doFilter12() throws Throwable{       
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        contentDispositionFilter = new ContentDispositionFilter();
        
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*:text/html,text/plain"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        
        context.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/content/usergenerated/author"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
            }
        });       
        rewriterResponse.setContentType("text/html");
    }
    
    @Test
    public void test_doFilter13() throws Throwable{       
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        contentDispositionFilter = new ContentDispositionFilter();
        
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*:text/html,text/plain"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        
        context.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/content/usergenerated/author"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
            }
        });       
        rewriterResponse.setContentType("text/html");
    }
    
    @Test
    public void test_doFilter14() throws Throwable{       
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        contentDispositionFilter = new ContentDispositionFilter();
        
        final ComponentContext ctx = context.mock(ComponentContext.class);
        final Dictionary props = new Hashtable<String, String[]>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*:text/html,text/plain"});
        
        context.checking(new Expectations() {
            {
                allowing(ctx).getProperties();
                will(returnValue(props));
                
            }
        });    
        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{ComponentContext.class},new Object[]{ctx});
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        
        context.checking(new Expectations() {
            {
                allowing(request).getPathInfo();
                will(returnValue("/content/usergenerated/author"));
                allowing(response).setContentType("image/jpeg");
                //CONTENT DISPOSITION IS SET
                exactly(1).of(response).addHeader("Content-Disposition", "attachment");
            }
        });       
        rewriterResponse.setContentType("image/jpeg");
    }
}

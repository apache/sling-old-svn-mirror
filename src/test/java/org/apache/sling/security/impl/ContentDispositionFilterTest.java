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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.security.impl.ContentDispositionFilter.RewriterResponse;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Test;

import junitx.util.PrivateAccessor;

public class ContentDispositionFilterTest {

    private ContentDispositionFilter contentDispositionFilter;
    private final Mockery context = new JUnit4Mockery();

    private static final String PROP_JCR_DATA = "jcr:data";

    private static final String JCR_CONTENT_LEAF = "jcr:content";

    @SuppressWarnings("unchecked")
    @Test
    public void test_activator1() throws Throwable{
        contentDispositionFilter = new ContentDispositionFilter();
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});
        Set<String> contentDispositionPaths = ( Set<String> ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPaths");
        Assert.assertEquals(1, contentDispositionPaths.size());
        String[] contentDispositionPathsPfx = ( String[] ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPathsPfx");
        Assert.assertEquals(0, contentDispositionPathsPfx.length);
        Map <String, Set<String>> contentTypesMapping = ( Map <String, Set<String>> ) PrivateAccessor.getField(contentDispositionFilter, "contentTypesMapping");
        Assert.assertEquals(0, contentTypesMapping.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_activator2() throws Throwable{
        contentDispositionFilter = new ContentDispositionFilter();
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        Set<String> contentDispositionPaths = ( Set<String> ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPaths");
        Assert.assertEquals(0, contentDispositionPaths.size());
        String[] contentDispositionPathsPfx = ( String[] ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPathsPfx");
        Assert.assertEquals(1, contentDispositionPathsPfx.length);
        Map <String, Set<String>> contentTypesMapping = ( Map <String, Set<String>> ) PrivateAccessor.getField(contentDispositionFilter, "contentTypesMapping");
        Assert.assertEquals(0, contentTypesMapping.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_activator3() throws Throwable{
        contentDispositionFilter = new ContentDispositionFilter();
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/libs", "/content/usergenerated/*"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        Set<String> contentDispositionPaths = ( Set<String> ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPaths");
        Assert.assertEquals(1, contentDispositionPaths.size());
        String[] contentDispositionPathsPfx = ( String[] ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPathsPfx");
        Assert.assertEquals(1, contentDispositionPathsPfx.length);
        Map <String, Set<String>> contentTypesMapping = ( Map <String, Set<String>> ) PrivateAccessor.getField(contentDispositionFilter, "contentTypesMapping");
        Assert.assertEquals(0, contentTypesMapping.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_activator5() throws Throwable{
        contentDispositionFilter = new ContentDispositionFilter();
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"*"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        Set<String> contentDispositionPaths = ( Set<String> ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPaths");
        Assert.assertEquals(0, contentDispositionPaths.size());
        String[] contentDispositionPathsPfx = ( String[] ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPathsPfx");
        Assert.assertEquals(0, contentDispositionPathsPfx.length);
        Map <String, Set<String>> contentTypesMapping = ( Map <String, Set<String>> ) PrivateAccessor.getField(contentDispositionFilter, "contentTypesMapping");
        Assert.assertEquals(0, contentTypesMapping.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_activator6() throws Throwable{
        contentDispositionFilter = new ContentDispositionFilter();
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/libs:*"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        Set<String> contentDispositionPaths = ( Set<String> ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPaths");
        Assert.assertEquals(0, contentDispositionPaths.size());
        String[] contentDispositionPathsPfx = ( String[] ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionPathsPfx");
        Assert.assertEquals(0, contentDispositionPathsPfx.length);
        Map <String, Set<String>> contentTypesMapping = ( Map <String, Set<String>> ) PrivateAccessor.getField(contentDispositionFilter, "contentTypesMapping");
        Assert.assertEquals(0, contentTypesMapping.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_activator7() throws Throwable{
        contentDispositionFilter = new ContentDispositionFilter();
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/libs:text/html,text/plain","/content/usergenerated/*:image/jpeg"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

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

    @SuppressWarnings("unchecked")
    @Test
    public void test_activator8() throws Throwable{
        contentDispositionFilter = new ContentDispositionFilter();
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/libs:text/html,text/plain","/content/usergenerated/*:image/jpeg"});
        props.put("sling.content.disposition.excluded.paths", new String []{});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        Set<String> contentDispositionExcludedPaths = ( Set<String> ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionExcludedPaths");
        Assert.assertEquals(0, contentDispositionExcludedPaths.size());
     }

    @SuppressWarnings("unchecked")
    @Test
    public void test_activator9() throws Throwable{
        contentDispositionFilter = new ContentDispositionFilter();
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/libs:text/html,text/plain","/content/usergenerated/*:image/jpeg"});
        props.put("sling.content.disposition.excluded.paths", new String []{"/content", "/libs"});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        Set<String> contentDispositionExcludedPaths = ( Set<String> ) PrivateAccessor.getField(contentDispositionFilter, "contentDispositionExcludedPaths");
        Assert.assertEquals(2, contentDispositionExcludedPaths.size());
     }

    @SuppressWarnings("unchecked")
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
        final Resource resource = context.mock(Resource.class, "resource" );

        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});


        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/libs"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");

            }
        });
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);

        rewriterResponse.setContentType("text/html");
    }

    @Test
    public void test_doFilter2() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});


        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/usergenerated/author"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
            }
        });
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);

        rewriterResponse.setContentType("text/html");
    }

    @Test
    public void test_doFilter3() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});


        final AtomicInteger counter =  new AtomicInteger();

        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(response).containsHeader("Content-Disposition");
                will(returnValue(false));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/usergenerated"));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(true));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION IS SET
                exactly(1).of(response).addHeader("Content-Disposition", "attachment");
            }
        });
        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response) {
            @Override
            public void addHeader(String name, String value) {
                counter.incrementAndGet();
            }
        };
        rewriterResponse.setContentType("text/html");
        Assert.assertEquals(1, counter.intValue());
    }

    @Test
    public void test_doFilter4() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});


        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/libs"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");

            }
        });
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        rewriterResponse.setContentType("text/html");
    }

    @Test
    public void test_doFilter5() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        final AtomicInteger counter =  new AtomicInteger();

        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(response).containsHeader("Content-Disposition");
                will(returnValue(false));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/usergenerated/author"));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(true));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION IS SET
                exactly(1).of(response).addHeader("Content-Disposition", "attachment");
            }
        });
        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response) {
            @Override
            public void addHeader(String name, String value) {
                counter.incrementAndGet();
            }
        };
        rewriterResponse.setContentType("text/html");
        Assert.assertEquals(1, counter.intValue());
    }

    @Test
    public void test_doFilter6() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        final AtomicInteger counter =  new AtomicInteger();

        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(response).containsHeader("Content-Disposition");
                will(returnValue(false));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/usergenerated/"));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(true));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION IS SET
                exactly(1).of(response).addHeader("Content-Disposition", "attachment");
            }
        });
        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response) {
            @Override
            public void addHeader(String name, String value) {
                counter.incrementAndGet();
            }
        };
        rewriterResponse.setContentType("text/html");
        Assert.assertEquals(1, counter.intValue());
    }

    @Test
    public void test_doFilter7() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated:text/html,text/plain"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});


        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/libs"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");

            }
        });
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);

        rewriterResponse.setContentType("text/html");
    }

    @Test
    public void test_doFilter8() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated:text/html,text/plain"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});


        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/usergenerated/author"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
            }
        });
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);

        rewriterResponse.setContentType("text/html");
    }

    @Test
    public void test_doFilter9() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated:text/html,text/plain"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});


        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/usergenerated"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
            }
        });
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);

        rewriterResponse.setContentType("text/html");
    }

    @Test
    public void test_doFilter10() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated:text/html,text/plain"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        final AtomicInteger counter =  new AtomicInteger();

        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(response).containsHeader("Content-Disposition");
                will(returnValue(false));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "image/jpeg");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/usergenerated"));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(true));
                allowing(response).setContentType("image/jpeg");
                //CONTENT DISPOSITION IS SET
                exactly(1).of(response).addHeader("Content-Disposition", "attachment");
            }
        });
        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response) {
            @Override
            public void addHeader(String name, String value) {
                counter.incrementAndGet();
            }
        };
        rewriterResponse.setContentType("image/jpeg");
        Assert.assertEquals(1, counter.intValue());
    }

    @Test
    public void test_doFilter11() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*:text/html,text/plain"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});


        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/libs"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");

            }
        });
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);

        rewriterResponse.setContentType("text/html");
    }

    @Test
    public void test_doFilter12() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*:text/html,text/plain"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});


        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/usergenerated/author"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
            }
        });
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);

        rewriterResponse.setContentType("text/html");
    }

    @Test
    public void test_doFilter13() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*:text/html,text/plain"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});


        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/usergenerated/author"));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION MUST NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
            }
        });
        ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);

        rewriterResponse.setContentType("text/html");
    }

    @Test
    public void test_doFilter14() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated/*:text/html,text/plain"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        final AtomicInteger counter =  new AtomicInteger();

        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(response).containsHeader("Content-Disposition");
                will(returnValue(false));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "image/jpeg");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/usergenerated/author"));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(true));
                allowing(response).setContentType("image/jpeg");
                //CONTENT DISPOSITION IS SET
                exactly(1).of(response).addHeader("Content-Disposition", "attachment");
            }
        });
        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response) {
            @Override
            public void addHeader(String name, String value) {
                counter.incrementAndGet();
            }
        };
        rewriterResponse.setContentType("image/jpeg");
        Assert.assertEquals(1, counter.intValue());
    }

    /**
     * Test repeated setContentType calls don't add multiple headers, case 1 resetting the same mimetype
     * @throws Throwable
     */
    @Test
    public void test_doFilter15() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        final AtomicInteger counter =  new AtomicInteger();

        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(response).containsHeader("Content-Disposition");
                will(returnValue(false));
                exactly(1).of(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                exactly(1).of(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue("text/html"));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/usergenerated"));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(true));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION IS SET
                exactly(1).of(response).addHeader("Content-Disposition", "attachment");
            }
        });
        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response) {
            @Override
            public void addHeader(String name, String value) {
                counter.incrementAndGet();
            }
        };
        rewriterResponse.setContentType("text/html");
        rewriterResponse.setContentType("text/html");
        Assert.assertEquals(1, counter.intValue());
    }

    /**
     * Test repeated setContentType calls don't add multiple headers, case 2 changing mime type
     * @throws Throwable
     */
    @Test
    public void test_doFilter16() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        final AtomicInteger counter =  new AtomicInteger();

        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                exactly(1).of(response).containsHeader("Content-Disposition");
                will(returnValue(false));
                exactly(1).of(response).containsHeader("Content-Disposition");
                will(returnValue(true));
                exactly(1).of(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                exactly(1).of(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue("text/html"));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/xml");
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/usergenerated"));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(true));
                allowing(response).setContentType("text/html");
                allowing(response).setContentType("text/xml");
                //CONTENT DISPOSITION IS SET
                exactly(1).of(response).addHeader("Content-Disposition", "attachment");
            }
        });
        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response) {
            @Override
            public void addHeader(String name, String value) {
                counter.incrementAndGet();
            }
        };
        rewriterResponse.setContentType("text/html");
        rewriterResponse.setContentType("text/xml");
        Assert.assertEquals(1, counter.intValue());
    }


    @Test
    public void test_doFilter17() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});
        props.put("sling.content.disposition.all.paths", false);

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        final AtomicInteger counter =  new AtomicInteger();

        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                exactly(1).of(response).containsHeader("Content-Disposition");
                will(returnValue(false));
                exactly(1).of(response).containsHeader("Content-Disposition");
                will(returnValue(true));
                exactly(1).of(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                exactly(1).of(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue("text/html"));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/xml");
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/other"));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(true));
                allowing(response).setContentType("text/html");
                allowing(response).setContentType("text/xml");
                //CONTENT DISPOSITION IS NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
            }
        });
        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response) {
            @Override
            public void addHeader(String name, String value) {
                counter.incrementAndGet();
            }
        };
        rewriterResponse.setContentType("text/html");
        Assert.assertEquals(0, counter.intValue());
    }


    @Test
    public void test_doFilter18() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        props.put("sling.content.disposition.excluded.paths", new String []{""});
        props.put("sling.content.disposition.all.paths", true);

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        final AtomicInteger counter =  new AtomicInteger();

        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                exactly(1).of(response).containsHeader("Content-Disposition");
                will(returnValue(false));
                exactly(1).of(response).containsHeader("Content-Disposition");
                will(returnValue(true));
                exactly(1).of(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                exactly(1).of(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue("text/html"));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/xml");
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/other"));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(true));
                allowing(response).setContentType("text/html");
                allowing(response).setContentType("text/xml");
                //CONTENT DISPOSITION IS SET
                exactly(1).of(response).addHeader("Content-Disposition", "attachment");
            }
        });
        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response) {
            @Override
            public void addHeader(String name, String value) {
                counter.incrementAndGet();
            }
        };
        rewriterResponse.setContentType("text/html");
        Assert.assertEquals(1, counter.intValue());
    }

    @Test
    public void test_doFilter19() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        props.put("sling.content.disposition.excluded.paths", new String []{"/content"});
        props.put("sling.content.disposition.all.paths", true);

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        final AtomicInteger counter =  new AtomicInteger();

        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                exactly(1).of(response).containsHeader("Content-Disposition");
                will(returnValue(false));
                exactly(1).of(response).containsHeader("Content-Disposition");
                will(returnValue(true));
                exactly(1).of(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                exactly(1).of(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue("text/html"));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/xml");
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/other"));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(true));
                allowing(response).setContentType("text/html");
                allowing(response).setContentType("text/xml");
                //CONTENT DISPOSITION IS SET
                exactly(1).of(response).addHeader("Content-Disposition", "attachment");
            }
        });
        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response) {
            @Override
            public void addHeader(String name, String value) {
                counter.incrementAndGet();
            }
        };
        rewriterResponse.setContentType("text/html");
        Assert.assertEquals(1, counter.intValue());
    }

    @Test
    public void test_doFilter20() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        props.put("sling.content.disposition.excluded.paths", new String []{"/content/other"});
        props.put("sling.content.disposition.all.paths", true);

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        final AtomicInteger counter =  new AtomicInteger();

        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                exactly(1).of(response).containsHeader("Content-Disposition");
                will(returnValue(false));
                exactly(1).of(response).containsHeader("Content-Disposition");
                will(returnValue(true));
                exactly(1).of(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                exactly(1).of(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue("text/html"));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/xml");
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/other"));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(true));
                allowing(response).setContentType("text/html");
                allowing(response).setContentType("text/xml");
                //CONTENT DISPOSITION IS NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
            }
        });
        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response) {
            @Override
            public void addHeader(String name, String value) {
                counter.incrementAndGet();
            }
        };
        rewriterResponse.setContentType("text/html");
        Assert.assertEquals(0, counter.intValue());
    }

    @Test
    public void test_doFilter21() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        props.put("sling.content.disposition.excluded.paths", new String []{"/content"});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});


        final AtomicInteger counter =  new AtomicInteger();

        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(response).containsHeader("Content-Disposition");
                will(returnValue(false));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/usergenerated"));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(true));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION IS SET
                exactly(1).of(response).addHeader("Content-Disposition", "attachment");
            }
        });
        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response) {
            @Override
            public void addHeader(String name, String value) {
                counter.incrementAndGet();
            }
        };
        rewriterResponse.setContentType("text/html");
        Assert.assertEquals(1, counter.intValue());
    }

    @Test
    public void test_doFilter22() throws Throwable{
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);
        contentDispositionFilter = new ContentDispositionFilter();

        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling.content.disposition.paths", new String []{"/content/usergenerated"});
        props.put("sling.content.disposition.excluded.paths", new String []{"/content/usergenerated"});

        PrivateAccessor.invoke(contentDispositionFilter,"activate",  new Class[]{Map.class},new Object[]{props});

        final AtomicInteger counter =  new AtomicInteger();
        context.checking(new Expectations() {
            {
                allowing(request).getMethod();
                will(returnValue("GET"));
                allowing(response).containsHeader("Content-Disposition");
                will(returnValue(false));
                allowing(request).getAttribute(RewriterResponse.ATTRIBUTE_NAME);
                will(returnValue(null));
                allowing(request).setAttribute(RewriterResponse.ATTRIBUTE_NAME, "text/html");
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).getPath();
                will(returnValue("/content/usergenerated"));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(true));
                allowing(response).setContentType("text/html");
                //CONTENT DISPOSITION IS NOT SET
                never(response).addHeader("Content-Disposition", "attachment");
            }
        });
        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response) {
            @Override
            public void addHeader(String name, String value) {
                counter.incrementAndGet();
            }
        };

        rewriterResponse.setContentType("text/html");
        Assert.assertEquals(0, counter.intValue());
    }

    @Test
    public void test_isJcrData1() throws Throwable {
        contentDispositionFilter = new ContentDispositionFilter();
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = null;

        context.checking(new Expectations() {
            {
                allowing(request).getResource();
                will(returnValue(resource));
            }
        });

        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);

        Boolean result = (Boolean) PrivateAccessor.invoke(rewriterResponse,"isJcrData",  new Class[]{Resource.class},new Object[]{resource});

        Assert.assertFalse(result);
    }

    @Test
    public void test_isJcrData2() throws Throwable {
        contentDispositionFilter = new ContentDispositionFilter();
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource resource = context.mock(Resource.class);

        context.checking(new Expectations() {
            {
                allowing(request).getResource();
                will(returnValue(resource));
            }
        });

        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        final ValueMap properties = context.mock(ValueMap.class);

        context.checking(new Expectations() {
            {
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(true));
            }
        });

        Boolean result = (Boolean) PrivateAccessor.invoke(rewriterResponse,"isJcrData",  new Class[]{Resource.class},new Object[]{resource});

        Assert.assertTrue(result);
    }

    @Test
    public void test_isJcrData3() throws Throwable {
        contentDispositionFilter = new ContentDispositionFilter();
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);

        final Resource resource = context.mock(Resource.class);
        final ValueMap properties = context.mock(ValueMap.class);

        context.checking(new Expectations() {
            {
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(false));
                allowing(resource).getChild(JCR_CONTENT_LEAF);
                will(returnValue(null));
            }
        });

        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);

        Boolean result = (Boolean) PrivateAccessor.invoke(rewriterResponse,"isJcrData",  new Class[]{Resource.class},new Object[]{resource});

        Assert.assertFalse(result);
    }

    @Test
    public void test_isJcrData4() throws Throwable {
        contentDispositionFilter = new ContentDispositionFilter();
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);

        final Resource child = context.mock(Resource.class, "child");
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);
        final ValueMap childPropoerties = context.mock(ValueMap.class, "childPropoerties");


        context.checking(new Expectations() {
            {
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(false));
                allowing(resource).getChild(JCR_CONTENT_LEAF);
                will(returnValue(child));
                allowing(child).adaptTo(ValueMap.class);
                will(returnValue(childPropoerties));
                allowing(childPropoerties).containsKey(PROP_JCR_DATA);
                will(returnValue(false));
            }
        });

        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);
        Boolean result = (Boolean) PrivateAccessor.invoke(rewriterResponse,"isJcrData",  new Class[]{Resource.class},new Object[]{resource});

        Assert.assertFalse(result);
    }

    @Test
    public void test_isJcrData5() throws Throwable {
        contentDispositionFilter = new ContentDispositionFilter();
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);

        final Resource child = context.mock(Resource.class, "child");
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);
        final ValueMap childPropoerties = context.mock(ValueMap.class, "childPropoerties");


        context.checking(new Expectations() {
            {
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(false));
                allowing(resource).getChild(JCR_CONTENT_LEAF);
                will(returnValue(child));
                allowing(child).adaptTo(ValueMap.class);
                will(returnValue(childPropoerties));
                allowing(childPropoerties).containsKey(PROP_JCR_DATA);
                will(returnValue(true));
            }
        });

        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);

        Boolean result = (Boolean) PrivateAccessor.invoke(rewriterResponse,"isJcrData",  new Class[]{Resource.class},new Object[]{resource});

        Assert.assertTrue(result);
    }

    @Test
    public void test_isJcrData6() throws Throwable {
        contentDispositionFilter = new ContentDispositionFilter();
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);

        final Resource resource = context.mock(Resource.class);

        context.checking(new Expectations() {
            {
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(null));
                allowing(resource).getChild(JCR_CONTENT_LEAF);
                will(returnValue(null));
            }
        });
        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);

        Boolean result = (Boolean) PrivateAccessor.invoke(rewriterResponse,"isJcrData",  new Class[]{Resource.class},new Object[]{resource});

        Assert.assertFalse(result);
    }


    @Test
    public void test_isJcrData7() throws Throwable {
        contentDispositionFilter = new ContentDispositionFilter();
        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        final SlingHttpServletResponse response = context.mock(SlingHttpServletResponse.class);
        final Resource child = context.mock(Resource.class, "child");
        final Resource resource = context.mock(Resource.class, "resource" );
        final ValueMap properties = context.mock(ValueMap.class);


        context.checking(new Expectations() {
            {
                allowing(request).getResource();
                will(returnValue(resource));
                allowing(resource).adaptTo(ValueMap.class);
                will(returnValue(properties));
                allowing(properties).containsKey(PROP_JCR_DATA);
                will(returnValue(false));
                allowing(resource).getChild(JCR_CONTENT_LEAF);
                will(returnValue(child));
                allowing(child).adaptTo(ValueMap.class);
                will(returnValue(null));
            }
        });

        final ContentDispositionFilter.RewriterResponse rewriterResponse = contentDispositionFilter. new RewriterResponse(request, response);

        Boolean result = (Boolean) PrivateAccessor.invoke(rewriterResponse,"isJcrData",  new Class[]{Resource.class},new Object[]{resource});

        Assert.assertFalse(result);
    }
}
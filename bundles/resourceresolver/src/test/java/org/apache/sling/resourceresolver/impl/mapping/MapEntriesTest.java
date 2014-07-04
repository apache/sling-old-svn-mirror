/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.resourceresolver.impl.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.resourceresolver.impl.mapping.MapConfigurationProvider.VanityPathConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;

public class MapEntriesTest {

    private MapEntries mapEntries;

    @Mock
    private MapConfigurationProvider resourceResolverFactory;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private EventAdmin eventAdmin;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        final List<VanityPathConfig> configs = new ArrayList<MapConfigurationProvider.VanityPathConfig>();
        configs.add(new VanityPathConfig("/libs/", false));
        configs.add(new VanityPathConfig("/libs/denied", true));
        configs.add(new VanityPathConfig("/foo/", false));
        configs.add(new VanityPathConfig("/baa/", false));
        configs.add(new VanityPathConfig("/justVanityPath", false));
        configs.add(new VanityPathConfig("/justVanityPath2", false));
        configs.add(new VanityPathConfig("/badVanityPath", false));
        configs.add(new VanityPathConfig("/redirectingVanityPath", false));
        configs.add(new VanityPathConfig("/redirectingVanityPath301", false));
        configs.add(new VanityPathConfig("/vanityPathOnJcrContent", false));

        Collections.sort(configs);
        when(resourceResolverFactory.getAdministrativeResourceResolver(null)).thenReturn(resourceResolver);
        when(resourceResolverFactory.isVanityPathEnabled()).thenReturn(true);
        when(resourceResolverFactory.getVanityPathConfig()).thenReturn(configs);
        when(resourceResolverFactory.isOptimizeAliasResolutionEnabled()).thenReturn(true);
        when(resourceResolver.findResources(anyString(), eq("sql"))).thenReturn(
                Collections.<Resource> emptySet().iterator());

        mapEntries = new MapEntries(resourceResolverFactory, bundleContext, eventAdmin);

    }

    @Test
    public void test_simple_alias_support() {
        Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/parent");

        final Resource result = mock(Resource.class);
        when(result.getParent()).thenReturn(parent);
        when(result.getPath()).thenReturn("/parent/child");
        when(result.getName()).thenReturn("child");
        when(result.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "alias"));

        when(resourceResolver.findResources(anyString(), eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

            public Iterator<Resource> answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].toString().contains("sling:alias")) {
                    return Collections.singleton(result).iterator();
                } else {
                    return Collections.<Resource> emptySet().iterator();
                }
            }
        });

        mapEntries.doInit();

        Map<String, String> aliasMap = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMap);
        assertTrue(aliasMap.containsKey("alias"));
        assertEquals("child", aliasMap.get("alias"));
    }

    @Test
    public void test_that_duplicate_alias_doesnt_replace_first_alias() {
        Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/parent");

        final Resource result = mock(Resource.class);
        when(result.getParent()).thenReturn(parent);
        when(result.getPath()).thenReturn("/parent/child");
        when(result.getName()).thenReturn("child");
        when(result.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "alias"));

        final Resource secondResult = mock(Resource.class);
        when(secondResult.getParent()).thenReturn(parent);
        when(secondResult.getPath()).thenReturn("/parent/child2");
        when(secondResult.getName()).thenReturn("child2");
        when(secondResult.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "alias"));

        when(resourceResolver.findResources(anyString(), eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

            public Iterator<Resource> answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].toString().contains("sling:alias")) {
                    return Arrays.asList(result, secondResult).iterator();
                } else {
                    return Collections.<Resource> emptySet().iterator();
                }
            }
        });

        mapEntries.doInit();

        Map<String, String> aliasMap = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMap);
        assertTrue(aliasMap.containsKey("alias"));
        assertEquals("child", aliasMap.get("alias"));
    }

    @Test
    public void test_vanity_path_registration() throws Exception {
        // specifically making this a weird value because we want to verify that
        // the configuration value is being used
        int DEFAULT_VANITY_STATUS = 333333;

        when(resourceResolverFactory.getDefaultVanityPathRedirectStatus()).thenReturn(DEFAULT_VANITY_STATUS);

        final List<Resource> resources = new ArrayList<Resource>();

        Resource justVanityPath = mock(Resource.class, "justVanityPath");
        when(justVanityPath.getPath()).thenReturn("/justVanityPath");
        when(justVanityPath.getName()).thenReturn("justVanityPath");
        when(justVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath"));
        resources.add(justVanityPath);

        Resource badVanityPath = mock(Resource.class, "badVanityPath");
        when(badVanityPath.getPath()).thenReturn("/badVanityPath");
        when(badVanityPath.getName()).thenReturn("badVanityPath");
        when(badVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/content/mypage/en-us-{132"));
        resources.add(badVanityPath);


        Resource redirectingVanityPath = mock(Resource.class, "redirectingVanityPath");
        when(redirectingVanityPath.getPath()).thenReturn("/redirectingVanityPath");
        when(redirectingVanityPath.getName()).thenReturn("redirectingVanityPath");
        when(redirectingVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/redirectingVanityPath", "sling:redirect", true));
        resources.add(redirectingVanityPath);

        Resource redirectingVanityPath301 = mock(Resource.class, "redirectingVanityPath301");
        when(redirectingVanityPath301.getPath()).thenReturn("/redirectingVanityPath301");
        when(redirectingVanityPath301.getName()).thenReturn("redirectingVanityPath301");
        when(redirectingVanityPath301.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/redirectingVanityPath301", "sling:redirect", true, "sling:redirectStatus", 301));
        resources.add(redirectingVanityPath301);

        Resource vanityPathOnJcrContentParent = mock(Resource.class, "vanityPathOnJcrContentParent");
        when(vanityPathOnJcrContentParent.getPath()).thenReturn("/vanityPathOnJcrContent");
        when(vanityPathOnJcrContentParent.getName()).thenReturn("vanityPathOnJcrContent");

        Resource vanityPathOnJcrContent = mock(Resource.class, "vanityPathOnJcrContent");
        when(vanityPathOnJcrContent.getPath()).thenReturn("/vanityPathOnJcrContent/jcr:content");
        when(vanityPathOnJcrContent.getName()).thenReturn("jcr:content");
        when(vanityPathOnJcrContent.getParent()).thenReturn(vanityPathOnJcrContentParent);
        when(vanityPathOnJcrContent.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/vanityPathOnJcrContent"));
        resources.add(vanityPathOnJcrContent);

        when(resourceResolver.findResources(anyString(), eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

            public Iterator<Resource> answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].toString().contains("sling:vanityPath")) {
                    return resources.iterator();
                } else {
                    return Collections.<Resource> emptySet().iterator();
                }
            }
        });

        mapEntries.doInit();

        List<MapEntry> entries = mapEntries.getResolveMaps();
        assertEquals(8, entries.size());
        for (MapEntry entry : entries) {
            if (entry.getPattern().contains("/target/redirectingVanityPath301")) {
                assertEquals(301, entry.getStatus());
                assertFalse(entry.isInternal());
            } else if (entry.getPattern().contains("/target/redirectingVanityPath")) {
                assertEquals(DEFAULT_VANITY_STATUS, entry.getStatus());
                assertFalse(entry.isInternal());
            } else if (entry.getPattern().contains("/target/justVanityPath")) {
                assertTrue(entry.isInternal());
            } else if (entry.getPattern().contains("/target/vanityPathOnJcrContent")) {
                for (String redirect : entry.getRedirect()) {
                    assertFalse(redirect.contains("jcr:content"));
                }
            }
        }
        
        Field field = MapEntries.class.getDeclaredField("vanityTargets");
        field.setAccessible(true);
        Map<String, List<String>> vanityTargets = (Map<String, List<String>>) field.get(mapEntries);
        assertEquals(4, vanityTargets.size());        
        
    }

    private ValueMap buildValueMap(Object... string) {
        final Map<String, Object> data = new HashMap<String, Object>();
        for (int i = 0; i < string.length; i = i + 2) {
            data.put((String) string[i], string[i+1]);
        }
        return new ValueMapDecorator(data);
    }

    private Resource getVanityPathResource(final String path) {
        Resource rsrc = mock(Resource.class);
        when(rsrc.getPath()).thenReturn(path);
        when(rsrc.getName()).thenReturn(ResourceUtil.getName(path));
        when(rsrc.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/vanity" + path));
        return rsrc;
    }

    @Test
    public void test_vanity_path_registration_include_exclude() {
        final String[] validPaths = {"/libs/somewhere", "/libs/a/b", "/foo/a", "/baa/a"};
        final String[] invalidPaths = {"/libs/denied/a", "/libs/denied/b/c", "/nowhere"};

        final List<Resource> resources = new ArrayList<Resource>();
        for(final String val : validPaths) {
            resources.add(getVanityPathResource(val));
        }
        for(final String val : invalidPaths) {
            resources.add(getVanityPathResource(val));
        }


        when(resourceResolver.findResources(anyString(), eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

            public Iterator<Resource> answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].toString().contains("sling:vanityPath")) {
                    return resources.iterator();
                } else {
                    return Collections.<Resource> emptySet().iterator();
                }
            }
        });

        mapEntries.doInit();

        List<MapEntry> entries = mapEntries.getResolveMaps();
        // each valid resource results in 2 entries
        assertEquals(validPaths.length * 2, entries.size());

        final Set<String> resultSet = new HashSet<String>();
        for(final String p : validPaths) {
            resultSet.add(p + "$1");
            resultSet.add(p + ".html");
        }
        for (final MapEntry entry : entries) {
            assertTrue(resultSet.remove(entry.getRedirect()[0]));
        }
    }
    
    @Test
    public void test_getActualContentPath() throws Exception {

        Method method = MapEntries.class.getDeclaredMethod("getActualContentPath", String.class);
        method.setAccessible(true);
        
        String actualContent = (String) method.invoke(mapEntries, "/content");
        assertEquals("/content", actualContent);
        
        actualContent = (String) method.invoke(mapEntries, "/content/jcr:content");
        assertEquals("/content", actualContent);
    }
    
    @Test
    public void test_getMapEntryRedirect() throws Exception {

        Method method = MapEntries.class.getDeclaredMethod("getMapEntryRedirect", MapEntry.class);
        method.setAccessible(true);
        
        MapEntry mapEntry = new MapEntry("/content", -1, false, 0, "/content");     
        String actualContent = (String) method.invoke(mapEntries, mapEntry);
        assertEquals("/content", actualContent);
        
        mapEntry = new MapEntry("/content", -1, false, 0, "/content$1");     
        actualContent = (String) method.invoke(mapEntries, mapEntry);
        assertEquals("/content", actualContent);
        
        mapEntry = new MapEntry("/content", -1, false, 0, "/content.html");     
        actualContent = (String) method.invoke(mapEntries, mapEntry);
        assertEquals("/content", actualContent);
    }
    
    @Test
    public void test_doAddVanity() throws Exception {
        List<MapEntry> entries = mapEntries.getResolveMaps();
        assertEquals(0, entries.size());
        Field field = MapEntries.class.getDeclaredField("vanityTargets");
        field.setAccessible(true);
        Map<String, List<String>> vanityTargets = (Map<String, List<String>>) field.get(mapEntries);
        assertEquals(0, vanityTargets.size());
        
        Method method = MapEntries.class.getDeclaredMethod("doAddVanity", String.class);
        method.setAccessible(true);
        
        Resource justVanityPath = mock(Resource.class, "justVanityPath");
        when(resourceResolver.getResource("/justVanityPath")).thenReturn(justVanityPath);
        when(justVanityPath.getPath()).thenReturn("/justVanityPath");                 
        when(justVanityPath.getName()).thenReturn("justVanityPath");
        when(justVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath"));
        
        method.invoke(mapEntries, "/justVanityPath");

        entries = mapEntries.getResolveMaps();
        assertEquals(2, entries.size());
        
        //bad vanity
        Resource badVanityPath = mock(Resource.class, "badVanityPath");
        when(resourceResolver.getResource("/badVanityPath")).thenReturn(badVanityPath);
        when(badVanityPath.getPath()).thenReturn("/badVanityPath");
        when(badVanityPath.getName()).thenReturn("badVanityPath");        
        when(badVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/content/mypage/en-us-{132"));
        
        method.invoke(mapEntries, "/badVanityPath");
        

        assertEquals(2, entries.size());

        vanityTargets = (Map<String, List<String>>) field.get(mapEntries);
        assertEquals(1, vanityTargets.size());
        
        //vanity under jcr:content
        Resource vanityPathOnJcrContentParent = mock(Resource.class, "vanityPathOnJcrContentParent");
        when(vanityPathOnJcrContentParent.getPath()).thenReturn("/vanityPathOnJcrContent");
        when(vanityPathOnJcrContentParent.getName()).thenReturn("vanityPathOnJcrContent");

        Resource vanityPathOnJcrContent = mock(Resource.class, "vanityPathOnJcrContent");
        when(resourceResolver.getResource("/vanityPathOnJcrContent/jcr:content")).thenReturn(vanityPathOnJcrContent);
        when(vanityPathOnJcrContent.getPath()).thenReturn("/vanityPathOnJcrContent/jcr:content");
        when(vanityPathOnJcrContent.getName()).thenReturn("jcr:content");
        when(vanityPathOnJcrContent.getParent()).thenReturn(vanityPathOnJcrContentParent);
        when(vanityPathOnJcrContent.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/vanityPathOnJcrContent"));
        
        method.invoke(mapEntries, "/vanityPathOnJcrContent/jcr:content");
        
        entries = mapEntries.getResolveMaps();
        assertEquals(4, entries.size());

        vanityTargets = (Map<String, List<String>>) field.get(mapEntries);
        assertEquals(2, vanityTargets.size());
        
        assertNull(vanityTargets.get("/vanityPathOnJcrContent/jcr:content"));
        assertNotNull(vanityTargets.get("/vanityPathOnJcrContent"));
    }
    
    @Test
    public void test_doUpdateVanity() throws Exception {
        Field field0 = MapEntries.class.getDeclaredField("resolveMapsMap");
        field0.setAccessible(true);   
        Map<String, List<MapEntry>> resolveMapsMap = (Map<String, List<MapEntry>>) field0.get(mapEntries);
        assertEquals(1, resolveMapsMap.size());
        
        Field field = MapEntries.class.getDeclaredField("vanityTargets");
        field.setAccessible(true);
        Map<String, List<String>> vanityTargets = (Map<String, List<String>>) field.get(mapEntries);
        assertEquals(0, vanityTargets.size());
        
        Method method = MapEntries.class.getDeclaredMethod("doAddVanity", String.class);
        method.setAccessible(true);
        
        Method method1 = MapEntries.class.getDeclaredMethod("doUpdateVanity", String.class);
        method1.setAccessible(true);
        
        Resource justVanityPath = mock(Resource.class, "justVanityPath");
        when(resourceResolver.getResource("/justVanityPath")).thenReturn(justVanityPath);
        when(justVanityPath.getPath()).thenReturn("/justVanityPath");                 
        when(justVanityPath.getName()).thenReturn("justVanityPath");
        when(justVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath"));
        
        method.invoke(mapEntries, "/justVanityPath");
 
        assertEquals(2, resolveMapsMap.size());
        assertEquals(1, vanityTargets.size());
        assertNotNull(resolveMapsMap.get("/target/justVanityPath"));
        assertNull(resolveMapsMap.get("/target/justVanityPathUpdated"));
        assertEquals(1, vanityTargets.get("/justVanityPath").size());
        assertEquals("/target/justVanityPath", vanityTargets.get("/justVanityPath").get(0));
        
        //update vanity path
        when(justVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPathUpdated"));
        method1.invoke(mapEntries, "/justVanityPath");
               
        assertEquals(2, resolveMapsMap.size());
        assertEquals(1, vanityTargets.size());
        assertNull(resolveMapsMap.get("/target/justVanityPath"));
        assertNotNull(resolveMapsMap.get("/target/justVanityPathUpdated"));
        assertEquals(1, vanityTargets.get("/justVanityPath").size());
        assertEquals("/target/justVanityPathUpdated", vanityTargets.get("/justVanityPath").get(0));
        
        //vanity under jcr:content
        Resource vanityPathOnJcrContentParent = mock(Resource.class, "vanityPathOnJcrContentParent");
        when(vanityPathOnJcrContentParent.getPath()).thenReturn("/vanityPathOnJcrContent");
        when(vanityPathOnJcrContentParent.getName()).thenReturn("vanityPathOnJcrContent");

        Resource vanityPathOnJcrContent = mock(Resource.class, "vanityPathOnJcrContent");
        when(resourceResolver.getResource("/vanityPathOnJcrContent/jcr:content")).thenReturn(vanityPathOnJcrContent);
        when(vanityPathOnJcrContent.getPath()).thenReturn("/vanityPathOnJcrContent/jcr:content");
        when(vanityPathOnJcrContent.getName()).thenReturn("jcr:content");
        when(vanityPathOnJcrContent.getParent()).thenReturn(vanityPathOnJcrContentParent);
        when(vanityPathOnJcrContent.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/vanityPathOnJcrContent"));
        
        method.invoke(mapEntries, "/vanityPathOnJcrContent/jcr:content");
        
        assertEquals(3, resolveMapsMap.size());
        assertEquals(2, vanityTargets.size());
        assertNotNull(resolveMapsMap.get("/target/vanityPathOnJcrContent"));
        assertNull(resolveMapsMap.get("/target/vanityPathOnJcrContentUpdated"));
        assertEquals(1, vanityTargets.get("/vanityPathOnJcrContent").size());
        assertEquals("/target/vanityPathOnJcrContent", vanityTargets.get("/vanityPathOnJcrContent").get(0));
        
        //update vanity path
        when(vanityPathOnJcrContent.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/vanityPathOnJcrContentUpdated"));     
        method1.invoke(mapEntries, "/vanityPathOnJcrContent/jcr:content");
        
        assertEquals(3, resolveMapsMap.size());
        assertEquals(2, vanityTargets.size());
        assertNull(resolveMapsMap.get("/target/vanityPathOnJcrContent"));
        assertNotNull(resolveMapsMap.get("/target/vanityPathOnJcrContentUpdated"));
        assertEquals(1, vanityTargets.get("/vanityPathOnJcrContent").size());
        assertEquals("/target/vanityPathOnJcrContentUpdated", vanityTargets.get("/vanityPathOnJcrContent").get(0));
    }
    
    @Test
    public void test_doRemoveVanity() throws Exception {
        Field field0 = MapEntries.class.getDeclaredField("resolveMapsMap");
        field0.setAccessible(true);   
        Map<String, List<MapEntry>> resolveMapsMap = (Map<String, List<MapEntry>>) field0.get(mapEntries);
        assertEquals(1, resolveMapsMap.size());
        
        Field field = MapEntries.class.getDeclaredField("vanityTargets");
        field.setAccessible(true);
        Map<String, List<String>> vanityTargets = (Map<String, List<String>>) field.get(mapEntries);
        assertEquals(0, vanityTargets.size());
        
        Method method = MapEntries.class.getDeclaredMethod("doAddVanity", String.class);
        method.setAccessible(true);
        
        Method method1 = MapEntries.class.getDeclaredMethod("doRemoveVanity", String.class);
        method1.setAccessible(true);
        
        Resource justVanityPath = mock(Resource.class, "justVanityPath");
        when(resourceResolver.getResource("/justVanityPath")).thenReturn(justVanityPath);
        when(justVanityPath.getPath()).thenReturn("/justVanityPath");                 
        when(justVanityPath.getName()).thenReturn("justVanityPath");
        when(justVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath"));
        
        method.invoke(mapEntries, "/justVanityPath");
        
        assertEquals(2, resolveMapsMap.size());
        assertEquals(1, vanityTargets.size());
        assertNotNull(resolveMapsMap.get("/target/justVanityPath"));
        assertEquals(1, vanityTargets.get("/justVanityPath").size());
        assertEquals("/target/justVanityPath", vanityTargets.get("/justVanityPath").get(0));
        
        //remove vanity path
        method1.invoke(mapEntries, "/justVanityPath");
        
        assertEquals(1, resolveMapsMap.size());
        assertEquals(0, vanityTargets.size());      
        assertNull(resolveMapsMap.get("/target/justVanityPath"));
        
        //vanity under jcr:content
        Resource vanityPathOnJcrContentParent = mock(Resource.class, "vanityPathOnJcrContentParent");
        when(vanityPathOnJcrContentParent.getPath()).thenReturn("/vanityPathOnJcrContent");
        when(vanityPathOnJcrContentParent.getName()).thenReturn("vanityPathOnJcrContent");

        Resource vanityPathOnJcrContent = mock(Resource.class, "vanityPathOnJcrContent");
        when(resourceResolver.getResource("/vanityPathOnJcrContent/jcr:content")).thenReturn(vanityPathOnJcrContent);
        when(vanityPathOnJcrContent.getPath()).thenReturn("/vanityPathOnJcrContent/jcr:content");
        when(vanityPathOnJcrContent.getName()).thenReturn("jcr:content");
        when(vanityPathOnJcrContent.getParent()).thenReturn(vanityPathOnJcrContentParent);
        when(vanityPathOnJcrContent.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/vanityPathOnJcrContent"));
        
        method.invoke(mapEntries, "/vanityPathOnJcrContent/jcr:content");
        
        assertEquals(2, resolveMapsMap.size());
        assertEquals(1, vanityTargets.size());
        assertNotNull(resolveMapsMap.get("/target/vanityPathOnJcrContent"));
        assertEquals(1,vanityTargets.get("/vanityPathOnJcrContent").size());
        assertEquals("/target/vanityPathOnJcrContent", vanityTargets.get("/vanityPathOnJcrContent").get(0));
        
        //remove vanity path
        method1.invoke(mapEntries, "/vanityPathOnJcrContent/jcr:content");
        
        assertEquals(1, resolveMapsMap.size());
        assertEquals(0, vanityTargets.size());      
        assertNull(resolveMapsMap.get("/target/vanityPathOnJcrContent"));
        
    }
    
    @Test
    public void test_doUpdateVanityOrder() throws Exception {
        Field field0 = MapEntries.class.getDeclaredField("resolveMapsMap");
        field0.setAccessible(true);   
        Map<String, List<MapEntry>> resolveMapsMap = (Map<String, List<MapEntry>>) field0.get(mapEntries);
        assertEquals(1, resolveMapsMap.size());
        
        Field field = MapEntries.class.getDeclaredField("vanityTargets");
        field.setAccessible(true);
        Map<String, List<String>> vanityTargets = (Map<String, List<String>>) field.get(mapEntries);
        assertEquals(0, vanityTargets.size());
        
        Method method = MapEntries.class.getDeclaredMethod("doAddVanity", String.class);
        method.setAccessible(true);
        
        Method method1 = MapEntries.class.getDeclaredMethod("doUpdateVanityOrder", String.class, boolean.class);
        method1.setAccessible(true);
        
        Resource justVanityPath = mock(Resource.class, "justVanityPath");
        when(resourceResolver.getResource("/justVanityPath")).thenReturn(justVanityPath);
        when(justVanityPath.getPath()).thenReturn("/justVanityPath");                 
        when(justVanityPath.getName()).thenReturn("justVanityPath");
        when(justVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath"));
        
        method.invoke(mapEntries, "/justVanityPath");
        
        Resource justVanityPath2 = mock(Resource.class, "justVanityPath2");
        when(resourceResolver.getResource("/justVanityPath2")).thenReturn(justVanityPath2);
        when(justVanityPath2.getPath()).thenReturn("/justVanityPath2");                 
        when(justVanityPath2.getName()).thenReturn("justVanityPath2");
        when(justVanityPath2.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath","sling:vanityOrder", 100));
        
        method.invoke(mapEntries, "/justVanityPath2");
              
        assertEquals(2, resolveMapsMap.size());
        assertEquals(2, vanityTargets.size());    
        assertNotNull(resolveMapsMap.get("/target/justVanityPath"));
        
        Iterator <MapEntry> iterator = resolveMapsMap.get("/target/justVanityPath").iterator();
        assertEquals("/justVanityPath2$1", iterator.next().getRedirect()[0]);
        assertEquals("/justVanityPath$1", iterator.next().getRedirect()[0]);
        assertEquals("/justVanityPath2.html", iterator.next().getRedirect()[0]);
        assertEquals("/justVanityPath.html", iterator.next().getRedirect()[0]);    
        assertFalse(iterator.hasNext());
        
        when(justVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath","sling:vanityOrder", 1000));
        method1.invoke(mapEntries, "/justVanityPath",false);
        
        iterator = resolveMapsMap.get("/target/justVanityPath").iterator();
        assertEquals("/justVanityPath$1", iterator.next().getRedirect()[0]);
        assertEquals("/justVanityPath2$1", iterator.next().getRedirect()[0]);
        assertEquals("/justVanityPath.html", iterator.next().getRedirect()[0]);
        assertEquals("/justVanityPath2.html", iterator.next().getRedirect()[0]);    
        assertFalse(iterator.hasNext());
        
        when(justVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath"));
        method1.invoke(mapEntries, "/justVanityPath",true);
        
        iterator = resolveMapsMap.get("/target/justVanityPath").iterator();
        assertEquals("/justVanityPath2$1", iterator.next().getRedirect()[0]);
        assertEquals("/justVanityPath$1", iterator.next().getRedirect()[0]);
        assertEquals("/justVanityPath2.html", iterator.next().getRedirect()[0]);
        assertEquals("/justVanityPath.html", iterator.next().getRedirect()[0]);    
        assertFalse(iterator.hasNext());
    }
    
    //SLING-3727
    @Test
    public void test_doAddAliasAttributesWithDisableAliasOptimization() throws Exception {
        Method method = MapEntries.class.getDeclaredMethod("doAddAttributes", String.class, String[].class, boolean.class);
        method.setAccessible(true);
        
        when(resourceResolverFactory.isOptimizeAliasResolutionEnabled()).thenReturn(false);
        mapEntries = new MapEntries(resourceResolverFactory, bundleContext, eventAdmin);
        
        Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/parent");

        final Resource result = mock(Resource.class);
        when(resourceResolver.getResource("/parent/child")).thenReturn(result);
        when(result.getParent()).thenReturn(parent);
        when(result.getPath()).thenReturn("/parent/child");
        when(result.getName()).thenReturn("child");
        when(result.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "alias"));
        
        method.invoke(mapEntries, "/parent/child",
                new String[] { "sling:alias" }, false);
        
        Map<String, String> aliasMap = mapEntries.getAliasMap("/parent");
        assertNull(aliasMap);
    }
    
    //SLING-3727
    @Test
    public void test_doUpdateAttributesWithDisableAliasOptimization() throws Exception {
        Method method = MapEntries.class.getDeclaredMethod("doUpdateAttributes", String.class, String[].class, boolean.class);
        method.setAccessible(true);
        
        when(resourceResolverFactory.isOptimizeAliasResolutionEnabled()).thenReturn(false);
        mapEntries = new MapEntries(resourceResolverFactory, bundleContext, eventAdmin);
        
        Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/parent");

        final Resource result = mock(Resource.class);
        when(resourceResolver.getResource("/parent/child")).thenReturn(result);
        when(result.getParent()).thenReturn(parent);
        when(result.getPath()).thenReturn("/parent/child");
        when(result.getName()).thenReturn("child");
        when(result.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "alias"));
        
        method.invoke(mapEntries, "/parent/child",
                new String[] { "sling:alias" }, false);
        
        Map<String, String> aliasMap = mapEntries.getAliasMap("/parent");
        assertNull(aliasMap);
    }
    
    //SLING-3727
    @Test
    public void test_doRemoveAttributessWithDisableAliasOptimization() throws Exception {
        Method method = MapEntries.class.getDeclaredMethod("doRemoveAttributes", String.class, String[].class, boolean.class, boolean.class);
        method.setAccessible(true);
        
        when(resourceResolverFactory.isOptimizeAliasResolutionEnabled()).thenReturn(false);
        mapEntries = new MapEntries(resourceResolverFactory, bundleContext, eventAdmin);
        
        Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/parent");

        final Resource result = mock(Resource.class);
        when(resourceResolver.getResource("/parent/child")).thenReturn(result);
        when(result.getParent()).thenReturn(parent);
        when(result.getPath()).thenReturn("/parent/child");
        when(result.getName()).thenReturn("child");
        when(result.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "alias"));
        
        method.invoke(mapEntries, "/parent/child",
                new String[] { "sling:alias" }, false, false);
        
        Map<String, String> aliasMap = mapEntries.getAliasMap("/parent");
        assertNull(aliasMap);
    }    
    
    @Test
    public void test_doAddAlias() throws Exception {
        Method method = MapEntries.class.getDeclaredMethod("doAddAlias", String.class);
        method.setAccessible(true);
        
        Field field0 = MapEntries.class.getDeclaredField("aliasMap");
        field0.setAccessible(true);   
        
        Map<String, Map<String, String>> aliasMap = ( Map<String, Map<String, String>>) field0.get(mapEntries);
        assertEquals(0, aliasMap.size());        
        
        Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/parent");
        
        final Resource result = mock(Resource.class);
        when(resourceResolver.getResource("/parent/child")).thenReturn(result);
        when(result.getParent()).thenReturn(parent);
        when(result.getPath()).thenReturn("/parent/child");
        when(result.getName()).thenReturn("child");
        when(result.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "alias"));
        
        method.invoke(mapEntries, "/parent/child");
        
        Map<String, String> aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("alias"));
        assertEquals("child", aliasMapEntry.get("alias"));
        
        assertEquals(1, aliasMap.size()); 
        
        //test_that_duplicate_alias_doesnt_replace_first_alias
        final Resource secondResult = mock(Resource.class);
        when(resourceResolver.getResource("/parent/child2")).thenReturn(secondResult);
        when(secondResult.getParent()).thenReturn(parent);
        when(secondResult.getPath()).thenReturn("/parent/child2");
        when(secondResult.getName()).thenReturn("child2");
        when(secondResult.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "alias"));
        
        method.invoke(mapEntries, "/parent/child2");
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("alias"));
        assertEquals("child", aliasMapEntry.get("alias"));
        
        assertEquals(1, aliasMap.size()); 
        
        //testing jcr:content node
        final Resource jcrContentResult = mock(Resource.class);
        when(resourceResolver.getResource("/parent/child/jcr:content")).thenReturn(jcrContentResult);
        when(jcrContentResult.getParent()).thenReturn(result);
        when(jcrContentResult.getPath()).thenReturn("/parent/child/jcr:content");
        when(jcrContentResult.getName()).thenReturn("jcr:content");
        when(jcrContentResult.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "aliasJcrContent"));
        
        method.invoke(mapEntries, "/parent/child/jcr:content");
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertEquals(2, aliasMapEntry.size()); 
        assertTrue(aliasMapEntry.containsKey("aliasJcrContent"));
        assertEquals("child", aliasMapEntry.get("aliasJcrContent"));
        
        assertEquals(1, aliasMap.size()); 
    }
    
    @Test
    public void test_doUpdateAlias() throws Exception {
        Method method = MapEntries.class.getDeclaredMethod("doAddAlias", String.class);
        method.setAccessible(true);
        
        Method method1 = MapEntries.class.getDeclaredMethod("doUpdateAttributes", String.class , String[].class, boolean.class);
        method1.setAccessible(true);
        
        Field field0 = MapEntries.class.getDeclaredField("aliasMap");
        field0.setAccessible(true);   
        
        Map<String, Map<String, String>> aliasMap = ( Map<String, Map<String, String>>) field0.get(mapEntries);
        assertEquals(0, aliasMap.size());        
        
        Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/parent");
        
        final Resource result = mock(Resource.class);
        when(resourceResolver.getResource("/parent/child")).thenReturn(result);
        when(result.getParent()).thenReturn(parent);
        when(result.getPath()).thenReturn("/parent/child");
        when(result.getName()).thenReturn("child");
        when(result.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "alias"));
        
        method.invoke(mapEntries, "/parent/child");
        
        Map<String, String> aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("alias"));
        assertFalse(aliasMapEntry.containsKey("aliasUpdated"));
        assertEquals("child", aliasMapEntry.get("alias"));
        
        assertEquals(1, aliasMap.size()); 
        
        when(result.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "aliasUpdated"));
        
        method1.invoke(mapEntries, "/parent/child", new String[] { "sling:alias" }, false);
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertFalse(aliasMapEntry.containsKey("alias"));
        assertTrue(aliasMapEntry.containsKey("aliasUpdated"));
        assertEquals("child", aliasMapEntry.get("aliasUpdated"));
        
        assertEquals(1, aliasMap.size()); 
        
        //testing jcr:content node update
        final Resource jcrContentResult = mock(Resource.class);
        when(resourceResolver.getResource("/parent/child/jcr:content")).thenReturn(jcrContentResult);
        when(jcrContentResult.getParent()).thenReturn(result);
        when(jcrContentResult.getPath()).thenReturn("/parent/child/jcr:content");
        when(jcrContentResult.getName()).thenReturn("jcr:content");
        when(jcrContentResult.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "aliasJcrContent"));
        when(result.getChild("jcr:content")).thenReturn(jcrContentResult);
        
        method.invoke(mapEntries, "/parent/child/jcr:content");
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertEquals(2, aliasMapEntry.size()); 
        assertTrue(aliasMapEntry.containsKey("aliasJcrContent"));
        assertFalse(aliasMapEntry.containsKey("aliasJcrContentUpdated"));
        assertEquals("child", aliasMapEntry.get("aliasJcrContent"));
        
        assertEquals(1, aliasMap.size()); 
        
        when(jcrContentResult.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "aliasJcrContentUpdated"));
        method1.invoke(mapEntries, "/parent/child/jcr:content",  new String[] { "sling:alias" }, false);
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertEquals(2, aliasMapEntry.size()); 
        assertFalse(aliasMapEntry.containsKey("aliasJcrContent"));
        assertTrue(aliasMapEntry.containsKey("aliasJcrContentUpdated"));
        assertEquals("child", aliasMapEntry.get("aliasJcrContentUpdated"));
        
        assertEquals(1, aliasMap.size()); 
        
        //re-update alias
        method1.invoke(mapEntries, "/parent/child",  new String[] { "sling:alias" }, false);
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertEquals(2, aliasMapEntry.size());
        assertFalse(aliasMapEntry.containsKey("alias"));
        assertTrue(aliasMapEntry.containsKey("aliasUpdated"));
        assertEquals("child", aliasMapEntry.get("aliasUpdated"));
        
        //add another node with different alias and check that the update doesn't break anything (see also SLING-3728)
        final Resource secondResult = mock(Resource.class);
        when(resourceResolver.getResource("/parent/child2")).thenReturn(secondResult);
        when(secondResult.getParent()).thenReturn(parent);
        when(secondResult.getPath()).thenReturn("/parent/child2");
        when(secondResult.getName()).thenReturn("child2");
        when(secondResult.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "alias2"));
               
        method.invoke(mapEntries, "/parent/child2");
        assertEquals(1, aliasMap.size()); 
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertEquals(3, aliasMapEntry.size());
        
        when(jcrContentResult.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "aliasJcrContentUpdated"));
        method1.invoke(mapEntries, "/parent/child/jcr:content",  new String[] { "sling:alias" }, false);
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertEquals(3, aliasMapEntry.size()); 
        assertFalse(aliasMapEntry.containsKey("aliasJcrContent"));
        assertTrue(aliasMapEntry.containsKey("aliasJcrContentUpdated"));
        assertEquals("child", aliasMapEntry.get("aliasJcrContentUpdated"));
        
        assertEquals(1, aliasMap.size()); 
 
        
        when(result.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", null));
        when(jcrContentResult.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "aliasJcrContentUpdated"));
        method1.invoke(mapEntries, "/parent/child/jcr:content",  new String[] { "sling:alias" }, false);
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertEquals(2, aliasMapEntry.size()); 
        assertFalse(aliasMapEntry.containsKey("aliasJcrContent"));
        assertTrue(aliasMapEntry.containsKey("aliasJcrContentUpdated"));
        assertEquals("child", aliasMapEntry.get("aliasJcrContentUpdated"));
        
        assertEquals(1, aliasMap.size()); 
 
    }
    
    @Test
    public void test_doRemoveAlias() throws Exception {       
        Method method = MapEntries.class.getDeclaredMethod("doAddAlias", String.class);
        method.setAccessible(true);
        
        Method method1 = MapEntries.class.getDeclaredMethod("doRemoveAttributes", String.class, String[].class, boolean.class, boolean.class);
        method1.setAccessible(true);
        
        Field field0 = MapEntries.class.getDeclaredField("aliasMap");
        field0.setAccessible(true);   
        
        Map<String, Map<String, String>> aliasMap = ( Map<String, Map<String, String>>) field0.get(mapEntries);
        assertEquals(0, aliasMap.size()); 
        
        Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/parent");
        
        final Resource result = mock(Resource.class);
        when(resourceResolver.getResource("/parent/child")).thenReturn(result);
        when(result.getParent()).thenReturn(parent);
        when(result.getPath()).thenReturn("/parent/child");
        when(result.getName()).thenReturn("child");
        when(result.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "alias"));
        
        method.invoke(mapEntries, "/parent/child");
        
        Map<String, String> aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("alias"));
        assertEquals("child", aliasMapEntry.get("alias"));
        
        assertEquals(1, aliasMap.size()); 
        
        method1.invoke(mapEntries, "/parent/child", new String[] { "sling:alias" }, false, false);
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNull(aliasMapEntry);
        
        assertEquals(0, aliasMap.size()); 
        
        //re-add node and test nodeDeletion true
        method.invoke(mapEntries, "/parent/child");
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("alias"));
        assertEquals("child", aliasMapEntry.get("alias"));
        
        assertEquals(1, aliasMap.size()); 
        
        when(resourceResolver.getResource("/parent/child")).thenReturn(null);
        method1.invoke(mapEntries, "/parent/child", new String[] { "sling:alias" }, true, false);
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNull(aliasMapEntry);
        
        assertEquals(0, aliasMap.size()); 
    } 

    @Test
    public void test_doRemoveAlias2() throws Exception { 
        Method method = MapEntries.class.getDeclaredMethod("doAddAlias", String.class);
        method.setAccessible(true);
        
        Method method1 = MapEntries.class.getDeclaredMethod("doRemoveAttributes", String.class, String[].class, boolean.class, boolean.class);
        method1.setAccessible(true);
        
        Field field0 = MapEntries.class.getDeclaredField("aliasMap");
        field0.setAccessible(true);   
        
        Map<String, Map<String, String>> aliasMap = ( Map<String, Map<String, String>>) field0.get(mapEntries);
        assertEquals(0, aliasMap.size()); 
        
        Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/parent");
        
        final Resource result = mock(Resource.class);
        when(resourceResolver.getResource("/parent/child")).thenReturn(result);
        when(result.getParent()).thenReturn(parent);
        when(result.getPath()).thenReturn("/parent/child");
        when(result.getName()).thenReturn("child");
        when(result.adaptTo(ValueMap.class)).thenReturn(buildValueMap());
        
        //testing jcr:content node removal
        final Resource jcrContentResult = mock(Resource.class);
        when(resourceResolver.getResource("/parent/child/jcr:content")).thenReturn(jcrContentResult);
        when(jcrContentResult.getParent()).thenReturn(result);
        when(jcrContentResult.getPath()).thenReturn("/parent/child/jcr:content");
        when(jcrContentResult.getName()).thenReturn("jcr:content");
        when(jcrContentResult.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "aliasJcrContent"));
        when(result.getChild("jcr:content")).thenReturn(jcrContentResult);
        
        method.invoke(mapEntries, "/parent/child/jcr:content");
        
        Map<String, String> aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("aliasJcrContent"));
        assertEquals("child", aliasMapEntry.get("aliasJcrContent"));
        
        assertEquals(1, aliasMap.size());        
        
        method1.invoke(mapEntries, "/parent/child/jcr:content", new String[] { "sling:alias" }, false, false);
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNull(aliasMapEntry);
        
        assertEquals(0, aliasMap.size()); 
        
        //re-add node and test nodeDeletion true       
        method.invoke(mapEntries, "/parent/child/jcr:content");
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("aliasJcrContent"));
        assertEquals("child", aliasMapEntry.get("aliasJcrContent"));
        
        assertEquals(1, aliasMap.size());        
        when(resourceResolver.getResource("/parent/child/jcr:content")).thenReturn(null);
        when(result.getChild("jcr:content")).thenReturn(null);
        method1.invoke(mapEntries, "/parent/child/jcr:content", new String[] { "sling:alias" }, true, false);
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNull(aliasMapEntry);
        
        assertEquals(0, aliasMap.size());
    }
    
    @Test
    public void test_doRemoveAlias3() throws Exception { 
        Method method = MapEntries.class.getDeclaredMethod("doAddAlias", String.class);
        method.setAccessible(true);
        
        Method method1 = MapEntries.class.getDeclaredMethod("doRemoveAttributes", String.class, String[].class, boolean.class, boolean.class);
        method1.setAccessible(true);
        
        Field field0 = MapEntries.class.getDeclaredField("aliasMap");
        field0.setAccessible(true);   
        
        Map<String, Map<String, String>> aliasMap = ( Map<String, Map<String, String>>) field0.get(mapEntries);
        assertEquals(0, aliasMap.size()); 
        
        Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/parent");
        
        final Resource result = mock(Resource.class);
        when(resourceResolver.getResource("/parent/child")).thenReturn(result);
        when(result.getParent()).thenReturn(parent);
        when(result.getPath()).thenReturn("/parent/child");
        when(result.getName()).thenReturn("child");
        when(result.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "alias"));
        
        method.invoke(mapEntries, "/parent/child");
        
        final Resource jcrContentResult = mock(Resource.class);
        when(resourceResolver.getResource("/parent/child/jcr:content")).thenReturn(jcrContentResult);
        when(jcrContentResult.getParent()).thenReturn(result);
        when(jcrContentResult.getPath()).thenReturn("/parent/child/jcr:content");
        when(jcrContentResult.getName()).thenReturn("jcr:content");
        when(jcrContentResult.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "aliasJcrContent"));
        when(result.getChild("jcr:content")).thenReturn(jcrContentResult);
        
        method.invoke(mapEntries, "/parent/child/jcr:content");
        
        Map<String, String> aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("aliasJcrContent"));
        assertEquals("child", aliasMapEntry.get("aliasJcrContent"));
  
        //test with two nodes 
        assertEquals(1, aliasMap.size()); 
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertEquals(2, aliasMapEntry.size()); 
        
        method1.invoke(mapEntries, "/parent/child/jcr:content", new String[] { "sling:alias" }, false, false);
        
        assertEquals(1, aliasMap.size()); 
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertEquals(1, aliasMapEntry.size());  
        
        // re-add the node and test /parent/child
        method.invoke(mapEntries, "/parent/child/jcr:content");
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("aliasJcrContent"));
        assertEquals("child", aliasMapEntry.get("aliasJcrContent"));
        assertEquals(2, aliasMapEntry.size()); 
        
        method1.invoke(mapEntries, "/parent/child", new String[] { "sling:alias" }, false, false);
        
        assertEquals(1, aliasMap.size()); 
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertEquals(1, aliasMapEntry.size());  
        
        // re-add the node and test node removal
        method.invoke(mapEntries, "/parent/child");
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("aliasJcrContent"));
        assertEquals("child", aliasMapEntry.get("aliasJcrContent"));
        assertEquals(2, aliasMapEntry.size()); 
        
        when(resourceResolver.getResource("/parent/child/jcr:content")).thenReturn(null);
        when(result.getChild("jcr:content")).thenReturn(null);
        method1.invoke(mapEntries, "/parent/child/jcr:content", new String[] { "sling:alias" }, true, false);
        
        assertEquals(1, aliasMap.size()); 
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertEquals(1, aliasMapEntry.size());  
        
        // re-add the node and test node removal for  /parent/child
        when(resourceResolver.getResource("/parent/child/jcr:content")).thenReturn(jcrContentResult);
        when(result.getChild("jcr:content")).thenReturn(jcrContentResult);
        method.invoke(mapEntries, "/parent/child/jcr:content");
        
        
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("aliasJcrContent"));
        assertEquals("child", aliasMapEntry.get("aliasJcrContent"));
        assertEquals(2, aliasMapEntry.size()); 
        
        method1.invoke(mapEntries, "/parent/child", new String[] { "sling:alias" }, true, false);
        
        assertEquals(0, aliasMap.size()); 
        aliasMapEntry = mapEntries.getAliasMap("/parent");
        assertNull(aliasMapEntry);
    }
}

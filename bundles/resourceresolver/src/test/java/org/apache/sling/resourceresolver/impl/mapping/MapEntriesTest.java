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

import java.io.File;
import java.io.IOException;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.resourceresolver.impl.mapping.MapConfigurationProvider.VanityPathConfig;
import org.junit.After;
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
    
    File vanityBloomFilterFile;

    @Mock
    private MapConfigurationProvider resourceResolverFactory;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private EventAdmin eventAdmin;

    @SuppressWarnings("deprecation")
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
        vanityBloomFilterFile = new File("src/main/resourcesvanityBloomFilter.txt");
        when(bundleContext.getDataFile("vanityBloomFilter.txt")).thenReturn(vanityBloomFilterFile);
        when(resourceResolverFactory.getAdministrativeResourceResolver(null)).thenReturn(resourceResolver);
        when(resourceResolverFactory.isVanityPathEnabled()).thenReturn(true);
        when(resourceResolverFactory.getVanityPathConfig()).thenReturn(configs);
        when(resourceResolverFactory.isOptimizeAliasResolutionEnabled()).thenReturn(true);
        when(resourceResolver.findResources(anyString(), eq("sql"))).thenReturn(
                Collections.<Resource> emptySet().iterator());

        mapEntries = new MapEntries(resourceResolverFactory, bundleContext, eventAdmin);
        Field field0 = MapEntries.class.getDeclaredField("mapRoot");
        field0.setAccessible(true);  
        field0.set(mapEntries, MapEntries.DEFAULT_MAP_ROOT);
        
        Field field1 = MapEntries.class.getDeclaredField("maxCachedVanityPathEntries");
        field1.setAccessible(true);  
        field1.set(mapEntries, -1);
        
        Field field2 = MapEntries.class.getDeclaredField("maxCachedVanityPathEntriesStartup");
        field2.setAccessible(true);  
        field2.set(mapEntries, true);
        
    }
    
    @After
    public void tearDown() throws Exception {
        vanityBloomFilterFile.delete();
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
        mapEntries.initializeVanityPaths();

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
        @SuppressWarnings("unchecked")
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
    public void test_vanity_path_registration_include_exclude() throws IOException {
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
        mapEntries.initializeVanityPaths();

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
    
    @SuppressWarnings("unchecked")
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
        
        Field vanityCounter = MapEntries.class.getDeclaredField("vanityCounter");
        vanityCounter.setAccessible(true);  
        AtomicLong counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(2, counter.longValue());        
        
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
        
        counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(4, counter.longValue());        

        vanityTargets = (Map<String, List<String>>) field.get(mapEntries);
        assertEquals(2, vanityTargets.size());
        
        assertNull(vanityTargets.get("/vanityPathOnJcrContent/jcr:content"));
        assertNotNull(vanityTargets.get("/vanityPathOnJcrContent"));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void test_doAddVanity_1() throws Exception {
        Field field1 = MapEntries.class.getDeclaredField("maxCachedVanityPathEntries");
        field1.setAccessible(true);  
        field1.set(mapEntries, 10);
        
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
        
        Field vanityCounter = MapEntries.class.getDeclaredField("vanityCounter");
        vanityCounter.setAccessible(true);  
        AtomicLong counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(2, counter.longValue());        
        
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
        
        counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(4, counter.longValue());

        vanityTargets = (Map<String, List<String>>) field.get(mapEntries);
        assertEquals(2, vanityTargets.size());
        
        assertNull(vanityTargets.get("/vanityPathOnJcrContent/jcr:content"));
        assertNotNull(vanityTargets.get("/vanityPathOnJcrContent"));
    }
    
    
    @SuppressWarnings("unchecked")
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
    
    @SuppressWarnings("unchecked")
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
        
        Field vanityCounter = MapEntries.class.getDeclaredField("vanityCounter");
        vanityCounter.setAccessible(true);  
        AtomicLong counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(2, counter.longValue());  
        assertEquals(2, resolveMapsMap.size());
        assertEquals(1, vanityTargets.size());
        assertNotNull(resolveMapsMap.get("/target/justVanityPath"));
        assertEquals(1, vanityTargets.get("/justVanityPath").size());
        assertEquals("/target/justVanityPath", vanityTargets.get("/justVanityPath").get(0));
        
        //remove vanity path
        method1.invoke(mapEntries, "/justVanityPath");
        
        counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(0, counter.longValue());  
        
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
    
    @SuppressWarnings("unchecked")
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
        Field field0 = MapEntries.class.getDeclaredField("mapRoot");
        field0.setAccessible(true);  
        field0.set(mapEntries, MapEntries.DEFAULT_MAP_ROOT);
        
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
        Field field0 = MapEntries.class.getDeclaredField("mapRoot");
        field0.setAccessible(true);  
        field0.set(mapEntries, MapEntries.DEFAULT_MAP_ROOT);
        
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
        Field field0 = MapEntries.class.getDeclaredField("mapRoot");
        field0.setAccessible(true);  
        field0.set(mapEntries, MapEntries.DEFAULT_MAP_ROOT);
        
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
    
    @SuppressWarnings("unchecked")
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
    
    @SuppressWarnings("unchecked")
    @Test
    public void test_doAddAlias2() throws Exception {
        Method method = MapEntries.class.getDeclaredMethod("doAddAlias", String.class);
        method.setAccessible(true);
        
        Field field0 = MapEntries.class.getDeclaredField("aliasMap");
        field0.setAccessible(true);   
        
        Map<String, Map<String, String>> aliasMap = ( Map<String, Map<String, String>>) field0.get(mapEntries);
        assertEquals(0, aliasMap.size());        
        
        Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/");
        
        final Resource result = mock(Resource.class);
        when(resourceResolver.getResource("/parent")).thenReturn(result);
        when(result.getParent()).thenReturn(parent);
        when(result.getPath()).thenReturn("/parent");
        when(result.getName()).thenReturn("parent");
        when(result.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "alias"));
        
        method.invoke(mapEntries, "/parent");
        
        Map<String, String> aliasMapEntry = mapEntries.getAliasMap("/");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("alias"));
        assertEquals("parent", aliasMapEntry.get("alias"));
        
        assertEquals(1, aliasMap.size()); 
        
        //test_that_duplicate_alias_doesnt_replace_first_alias
        final Resource secondResult = mock(Resource.class);
        when(resourceResolver.getResource("/parent2")).thenReturn(secondResult);
        when(secondResult.getParent()).thenReturn(parent);
        when(secondResult.getPath()).thenReturn("/parent2");
        when(secondResult.getName()).thenReturn("parent2");
        when(secondResult.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "alias"));
        
        method.invoke(mapEntries, "/parent2");
        
        aliasMapEntry = mapEntries.getAliasMap("/");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("alias"));
        assertEquals("parent", aliasMapEntry.get("alias"));
        
        assertEquals(1, aliasMap.size()); 
        
        //testing jcr:content node
        final Resource jcrContentResult = mock(Resource.class);
        when(resourceResolver.getResource("/parent/jcr:content")).thenReturn(jcrContentResult);
        when(jcrContentResult.getParent()).thenReturn(result);
        when(jcrContentResult.getPath()).thenReturn("/parent/jcr:content");
        when(jcrContentResult.getName()).thenReturn("jcr:content");
        when(jcrContentResult.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "aliasJcrContent"));
        
        method.invoke(mapEntries, "/parent/jcr:content");
        
        aliasMapEntry = mapEntries.getAliasMap("/");
        assertNotNull(aliasMapEntry);
        assertEquals(2, aliasMapEntry.size()); 
        assertTrue(aliasMapEntry.containsKey("aliasJcrContent"));
        assertEquals("parent", aliasMapEntry.get("aliasJcrContent"));
        
        assertEquals(1, aliasMap.size()); 
    }
    
    
    @SuppressWarnings("unchecked")
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
    
    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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
    
    @SuppressWarnings("unchecked")
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
    
    @SuppressWarnings("unchecked")
    @Test
    public void test_doRemoveAlias4() throws Exception {       
        Method method = MapEntries.class.getDeclaredMethod("doAddAlias", String.class);
        method.setAccessible(true);
        
        Method method1 = MapEntries.class.getDeclaredMethod("doRemoveAttributes", String.class, String[].class, boolean.class, boolean.class);
        method1.setAccessible(true);
        
        Field field0 = MapEntries.class.getDeclaredField("aliasMap");
        field0.setAccessible(true);   
        
        Map<String, Map<String, String>> aliasMap = ( Map<String, Map<String, String>>) field0.get(mapEntries);
        assertEquals(0, aliasMap.size()); 
        
        Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/");
        
        final Resource result = mock(Resource.class);
        when(resourceResolver.getResource("/parent")).thenReturn(result);
        when(result.getParent()).thenReturn(parent);
        when(result.getPath()).thenReturn("/parent");
        when(result.getName()).thenReturn("parent");
        when(result.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "alias"));
        
        method.invoke(mapEntries, "/parent");
        
        Map<String, String> aliasMapEntry = mapEntries.getAliasMap("/");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("alias"));
        assertEquals("parent", aliasMapEntry.get("alias"));
        
        assertEquals(1, aliasMap.size()); 
        
        method1.invoke(mapEntries, "/parent", new String[] { "sling:alias" }, false, false);
        
        aliasMapEntry = mapEntries.getAliasMap("/");
        assertNull(aliasMapEntry);
        
        assertEquals(0, aliasMap.size()); 
        
        //re-add node and test nodeDeletion true
        method.invoke(mapEntries, "/parent");
        
        aliasMapEntry = mapEntries.getAliasMap("/");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("alias"));
        assertEquals("parent", aliasMapEntry.get("alias"));
        
        assertEquals(1, aliasMap.size()); 
        
        when(resourceResolver.getResource("/parent")).thenReturn(null);
        method1.invoke(mapEntries, "/parent", new String[] { "sling:alias" }, true, false);
        
        aliasMapEntry = mapEntries.getAliasMap("/");
        assertNull(aliasMapEntry);
        
        assertEquals(0, aliasMap.size()); 
    } 
       
    @SuppressWarnings("unchecked")
    @Test
    public void test_doRemoveAlias5() throws Exception { 
        Method method = MapEntries.class.getDeclaredMethod("doAddAlias", String.class);
        method.setAccessible(true);
        
        Method method1 = MapEntries.class.getDeclaredMethod("doRemoveAttributes", String.class, String[].class, boolean.class, boolean.class);
        method1.setAccessible(true);
        
        Field field0 = MapEntries.class.getDeclaredField("aliasMap");
        field0.setAccessible(true);   
        
        Map<String, Map<String, String>> aliasMap = ( Map<String, Map<String, String>>) field0.get(mapEntries);
        assertEquals(0, aliasMap.size()); 
        
        Resource parent = mock(Resource.class);
        when(parent.getPath()).thenReturn("/");
        
        final Resource result = mock(Resource.class);
        when(resourceResolver.getResource("/parent")).thenReturn(result);
        when(result.getParent()).thenReturn(parent);
        when(result.getPath()).thenReturn("/parent");
        when(result.getName()).thenReturn("parent");
        when(result.adaptTo(ValueMap.class)).thenReturn(buildValueMap());
        
        //testing jcr:content node removal
        final Resource jcrContentResult = mock(Resource.class);
        when(resourceResolver.getResource("/parent/jcr:content")).thenReturn(jcrContentResult);
        when(jcrContentResult.getParent()).thenReturn(result);
        when(jcrContentResult.getPath()).thenReturn("/parent/jcr:content");
        when(jcrContentResult.getName()).thenReturn("jcr:content");
        when(jcrContentResult.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:alias", "aliasJcrContent"));
        when(result.getChild("jcr:content")).thenReturn(jcrContentResult);
        
        method.invoke(mapEntries, "/parent/jcr:content");
        
        Map<String, String> aliasMapEntry = mapEntries.getAliasMap("/");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("aliasJcrContent"));
        assertEquals("parent", aliasMapEntry.get("aliasJcrContent"));
        
        assertEquals(1, aliasMap.size());        
        
        method1.invoke(mapEntries, "/parent/jcr:content", new String[] { "sling:alias" }, false, false);
        
        aliasMapEntry = mapEntries.getAliasMap("/");
        assertNull(aliasMapEntry);
        
        assertEquals(0, aliasMap.size()); 
        
        //re-add node and test nodeDeletion true       
        method.invoke(mapEntries, "/parent/jcr:content");
        
        aliasMapEntry = mapEntries.getAliasMap("/");
        assertNotNull(aliasMapEntry);
        assertTrue(aliasMapEntry.containsKey("aliasJcrContent"));
        assertEquals("parent", aliasMapEntry.get("aliasJcrContent"));
        
        assertEquals(1, aliasMap.size());        
        when(resourceResolver.getResource("/parent/jcr:content")).thenReturn(null);
        when(result.getChild("jcr:content")).thenReturn(null);
        method1.invoke(mapEntries, "/parent/jcr:content", new String[] { "sling:alias" }, true, false);
        
        aliasMapEntry = mapEntries.getAliasMap("/");
        assertNull(aliasMapEntry);
        
        assertEquals(0, aliasMap.size());
    }
    
    @Test
    public void test_isValidVanityPath() throws Exception {
        Method method = MapEntries.class.getDeclaredMethod("isValidVanityPath", Resource.class);
        method.setAccessible(true);
        
        final Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn("/jcr:system/node");
        
        assertFalse((Boolean)method.invoke(mapEntries, resource));
        
        when(resource.getPath()).thenReturn("/justVanityPath");
        assertFalse((Boolean)method.invoke(mapEntries, resource));
        
        when(resource.adaptTo(ValueMap.class)).thenReturn(mock(ValueMap.class));
        assertTrue((Boolean)method.invoke(mapEntries, resource));
    }
    
    @Test
    //SLING-4847
    public void test_doNodeAdded1() throws Exception { 
        Method method = MapEntries.class.getDeclaredMethod("doNodeAdded", String.class, boolean.class);
        method.setAccessible(true);
        Boolean resfreshed = (Boolean ) method.invoke(mapEntries, "/node", true);
        assertTrue(resfreshed.booleanValue());
    }
    
    @Test
    //SLING-4891
    public void test_getVanityPaths_1() throws Exception {
        
        Field field1 = MapEntries.class.getDeclaredField("maxCachedVanityPathEntries");
        field1.setAccessible(true);  
        field1.set(mapEntries, 0);
        
        Method method = MapEntries.class.getDeclaredMethod("getVanityPaths", String.class);
        method.setAccessible(true);
        method.invoke(mapEntries, "/notExisting");   
        
        Field vanityCounter = MapEntries.class.getDeclaredField("vanityCounter");
        vanityCounter.setAccessible(true);  
        AtomicLong counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(0, counter.longValue());        
    }
    
    @Test
    //SLING-4891
    public void test_getVanityPaths_2() throws Exception { 
        
        final Resource justVanityPath = mock(Resource.class, "justVanityPath");
        when(resourceResolver.getResource("/justVanityPath")).thenReturn(justVanityPath);
        when(justVanityPath.getPath()).thenReturn("/justVanityPath");                 
        when(justVanityPath.getName()).thenReturn("justVanityPath");
        when(justVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath"));
        
        
        when(resourceResolver.findResources(anyString(), eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

            public Iterator<Resource> answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].toString().contains("sling:vanityPath")) {
                    return Collections.singleton(justVanityPath).iterator();
                } else {
                    return Collections.<Resource> emptySet().iterator();
                }
            }
        });
        
        Field field1 = MapEntries.class.getDeclaredField("maxCachedVanityPathEntries");
        field1.setAccessible(true);  
        field1.set(mapEntries, 0);
        
        Method method = MapEntries.class.getDeclaredMethod("getVanityPaths", String.class);
        method.setAccessible(true);
        method.invoke(mapEntries, "/target/justVanityPath");   
        
        Field vanityCounter = MapEntries.class.getDeclaredField("vanityCounter");
        vanityCounter.setAccessible(true);  
        AtomicLong counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(2, counter.longValue());        
        
        final Resource justVanityPath2 = mock(Resource.class, "justVanityPath2");
        when(resourceResolver.getResource("/justVanityPath2")).thenReturn(justVanityPath2);
        when(justVanityPath2.getPath()).thenReturn("/justVanityPath2");                 
        when(justVanityPath2.getName()).thenReturn("justVanityPath2");
        when(justVanityPath2.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath","sling:vanityOrder", 100));
        
        when(resourceResolver.findResources(anyString(), eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

            public Iterator<Resource> answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].toString().contains("sling:vanityPath")) {
                    return Collections.singleton(justVanityPath).iterator();
                } else {
                    return Collections.<Resource> emptySet().iterator();
                }
            }
        });
        
        method.invoke(mapEntries, "/target/justVanityPath");
  
        counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(4, counter.longValue());  
    }
    
    @Test
    //SLING-4891
    public void test_getVanityPaths_3() throws Exception { 
        
        final Resource justVanityPath = mock(Resource.class, "justVanityPath");
        when(resourceResolver.getResource("/justVanityPath")).thenReturn(justVanityPath);
        when(justVanityPath.getPath()).thenReturn("/justVanityPath");                 
        when(justVanityPath.getName()).thenReturn("justVanityPath");
        when(justVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath"));
        
        
        when(resourceResolver.findResources(anyString(), eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

            public Iterator<Resource> answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].toString().contains("sling:vanityPath")) {
                    return Collections.singleton(justVanityPath).iterator();
                } else {
                    return Collections.<Resource> emptySet().iterator();
                }
            }
        });
        
        Field field1 = MapEntries.class.getDeclaredField("maxCachedVanityPathEntries");
        field1.setAccessible(true);  
        field1.set(mapEntries, 0);
        
        Field field2 = MapEntries.class.getDeclaredField("maxCachedVanityPathEntriesStartup");
        field2.setAccessible(true);  
        field2.set(mapEntries, false);
        
        Method method = MapEntries.class.getDeclaredMethod("getVanityPaths", String.class);
        method.setAccessible(true);
        method.invoke(mapEntries, "/target/justVanityPath");   
        
        Field vanityCounter = MapEntries.class.getDeclaredField("vanityCounter");
        vanityCounter.setAccessible(true);  
        AtomicLong counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(0, counter.longValue());        
    }
    
    @Test
    //SLING-4891
    public void test_getVanityPaths_4() throws Exception { 
        
        final Resource badVanityPath = mock(Resource.class, "badVanityPath");
        when(badVanityPath.getPath()).thenReturn("/badVanityPath");
        when(badVanityPath.getName()).thenReturn("badVanityPath");
        when(badVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/content/mypage/en-us-{132"));
        
        
        when(resourceResolver.findResources(anyString(), eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

            public Iterator<Resource> answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].toString().contains("sling:vanityPath")) {
                    return Collections.singleton(badVanityPath).iterator();
                } else {
                    return Collections.<Resource> emptySet().iterator();
                }
            }
        });
        
        Field field1 = MapEntries.class.getDeclaredField("maxCachedVanityPathEntries");
        field1.setAccessible(true);  
        field1.set(mapEntries, 0);
        
        Field field2 = MapEntries.class.getDeclaredField("maxCachedVanityPathEntriesStartup");
        field2.setAccessible(true);  
        field2.set(mapEntries, true);
        
        Method method = MapEntries.class.getDeclaredMethod("getVanityPaths", String.class);
        method.setAccessible(true);
        method.invoke(mapEntries, "/content/mypage/en-us-{132");   
        
        Field vanityCounter = MapEntries.class.getDeclaredField("vanityCounter");
        vanityCounter.setAccessible(true);  
        AtomicLong counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(0, counter.longValue());        
    }
    
    @Test
    //SLING-4891
    public void test_getVanityPaths_5() throws Exception { 
        
        final Resource justVanityPath = mock(Resource.class, "justVanityPath");
        when(resourceResolver.getResource("/justVanityPath")).thenReturn(justVanityPath);
        when(justVanityPath.getPath()).thenReturn("/justVanityPath");                 
        when(justVanityPath.getName()).thenReturn("justVanityPath");
        when(justVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath"));       
        
        when(resourceResolver.findResources(anyString(), eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

            public Iterator<Resource> answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].toString().contains("sling:vanityPath")) {
                    return Collections.singleton(justVanityPath).iterator();
                } else {
                    return Collections.<Resource> emptySet().iterator();
                }
            }
        });
        
        Field field1 = MapEntries.class.getDeclaredField("maxCachedVanityPathEntries");
        field1.setAccessible(true);  
        field1.set(mapEntries, 2);
        
        Field field2 = MapEntries.class.getDeclaredField("maxCachedVanityPathEntriesStartup");
        field2.setAccessible(true);  
        field2.set(mapEntries, false);
        
        Method method = MapEntries.class.getDeclaredMethod("getVanityPaths", String.class);
        method.setAccessible(true);
        method.invoke(mapEntries, "/target/justVanityPath");   
        
        Field vanityCounter = MapEntries.class.getDeclaredField("vanityCounter");
        vanityCounter.setAccessible(true);  
        AtomicLong counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(2, counter.longValue());        
        
        final Resource justVanityPath2 = mock(Resource.class, "justVanityPath2");
        when(resourceResolver.getResource("/justVanityPath2")).thenReturn(justVanityPath2);
        when(justVanityPath2.getPath()).thenReturn("/justVanityPath2");                 
        when(justVanityPath2.getName()).thenReturn("justVanityPath2");
        when(justVanityPath2.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath","sling:vanityOrder", 100));
        
        when(resourceResolver.findResources(anyString(), eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

            public Iterator<Resource> answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].toString().contains("sling:vanityPath")) {
                    return Collections.singleton(justVanityPath).iterator();
                } else {
                    return Collections.<Resource> emptySet().iterator();
                }
            }
        });
        
        method.invoke(mapEntries, "/target/justVanityPath");
  
        counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(2, counter.longValue());  
    }
    
    @Test
    //SLING-4891
    public void test_loadVanityPaths() throws Exception {
        Field field1 = MapEntries.class.getDeclaredField("maxCachedVanityPathEntries");
        field1.setAccessible(true);  
        field1.set(mapEntries, 2);
        
        final Resource justVanityPath = mock(Resource.class, "justVanityPath");
        when(resourceResolver.getResource("/justVanityPath")).thenReturn(justVanityPath);
        when(justVanityPath.getPath()).thenReturn("/justVanityPath");                 
        when(justVanityPath.getName()).thenReturn("justVanityPath");
        when(justVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath"));       
        
        when(resourceResolver.findResources(anyString(), eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

            public Iterator<Resource> answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].toString().contains("sling:vanityPath")) {
                    return Collections.singleton(justVanityPath).iterator();
                } else {
                    return Collections.<Resource> emptySet().iterator();
                }
            }
        });        
        
        Method method = MapEntries.class.getDeclaredMethod("loadVanityPaths", boolean.class);
        method.setAccessible(true);
        method.invoke(mapEntries, false);   
        
        Field vanityCounter = MapEntries.class.getDeclaredField("vanityCounter");
        vanityCounter.setAccessible(true);  
        AtomicLong counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(2, counter.longValue()); 
    }
    
    @Test
    //SLING-4891
    public void test_loadVanityPaths_1() throws Exception {
        
        final Resource justVanityPath = mock(Resource.class, "justVanityPath");
        when(resourceResolver.getResource("/justVanityPath")).thenReturn(justVanityPath);
        when(justVanityPath.getPath()).thenReturn("/justVanityPath");                 
        when(justVanityPath.getName()).thenReturn("justVanityPath");
        when(justVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath"));       
        
        when(resourceResolver.findResources(anyString(), eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

            public Iterator<Resource> answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].toString().contains("sling:vanityPath")) {
                    return Collections.singleton(justVanityPath).iterator();
                } else {
                    return Collections.<Resource> emptySet().iterator();
                }
            }
        });        
        
        Method method = MapEntries.class.getDeclaredMethod("loadVanityPaths", boolean.class);
        method.setAccessible(true);
        method.invoke(mapEntries, false);   
        
        Field vanityCounter = MapEntries.class.getDeclaredField("vanityCounter");
        vanityCounter.setAccessible(true);  
        AtomicLong counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(2, counter.longValue()); 
    }
    
    @Test
    //SLING-4891
    public void test_getMapEntryList() throws Exception {

        List<MapEntry> entries = mapEntries.getResolveMaps();
        assertEquals(0, entries.size());


        final Resource justVanityPath = mock(Resource.class,
                "justVanityPath");

        when(resourceResolver.getResource("/justVanityPath")).thenReturn(justVanityPath);

        when(justVanityPath.getPath()).thenReturn("/justVanityPath");

        when(justVanityPath.getName()).thenReturn("justVanityPath");

        when(justVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath",
                "/target/justVanityPath"));

        when(resourceResolver.findResources(anyString(),
                eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

                    public Iterator<Resource> answer(InvocationOnMock invocation)
                            throws Throwable {
                        if
                        (invocation.getArguments()[0].toString().contains("sling:vanityPath")) {
                            return Collections.singleton(justVanityPath).iterator();
                        } else {
                            return Collections.<Resource> emptySet().iterator();
                        }
                    }
                });

        Method method =
                MapEntries.class.getDeclaredMethod("getMapEntryList",String.class);
        method.setAccessible(true);
        method.invoke(mapEntries, "/target/justVanityPath");

        entries = mapEntries.getResolveMaps();
        assertEquals(2, entries.size());

        Field vanityCounter =
                MapEntries.class.getDeclaredField("vanityCounter");
        vanityCounter.setAccessible(true);
        AtomicLong counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(2, counter.longValue());

        method.invoke(mapEntries, "/target/justVanityPath");

        entries = mapEntries.getResolveMaps();
        assertEquals(2, entries.size());

        counter = (AtomicLong) vanityCounter.get(mapEntries);
        assertEquals(2, counter.longValue());
    }
    
    @Test
    //SLING-4883
    public void test_concutrrent_getResolveMapsIterator() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        
        final Resource justVanityPath = mock(Resource.class, "justVanityPath");
        when(resourceResolver.getResource("/justVanityPath")).thenReturn(justVanityPath);
        when(justVanityPath.getPath()).thenReturn("/justVanityPath");                 
        when(justVanityPath.getName()).thenReturn("justVanityPath");
        when(justVanityPath.adaptTo(ValueMap.class)).thenReturn(buildValueMap("sling:vanityPath", "/target/justVanityPath"));
        
        
        when(resourceResolver.findResources(anyString(), eq("sql"))).thenAnswer(new Answer<Iterator<Resource>>() {

            public Iterator<Resource> answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getArguments()[0].toString().contains("sling:vanityPath")) {
                    return Collections.singleton(justVanityPath).iterator();
                } else {
                    return Collections.<Resource> emptySet().iterator();
                }
            }
        });
        
        
        Field field1 = MapEntries.class.getDeclaredField("maxCachedVanityPathEntries");
        field1.setAccessible(true);  
        field1.set(mapEntries, 2);
        
        ArrayList<DataFuture> list = new ArrayList<DataFuture>();
        for (int i =0;i<10;i++) {
            list.add(createDataFuture(pool, mapEntries));
 
        }
 
       for (DataFuture df : list) {
           df.future.get();           
        }
  
    }
    
    // -------------------------- private methods ----------
    private DataFuture createDataFuture(ExecutorService pool, final MapEntries mapEntries) {

        Future<Iterator<?>> future = pool.submit(new Callable<Iterator<?>>() {
            @Override
            public Iterator<MapEntry> call() throws Exception {
                return mapEntries.getResolveMapsIterator("http/localhost.8080/target/justVanityPath");                     
            }
        });
        return new DataFuture(future);
    }    
    
    // -------------------------- inner classes ------------

    private static class DataFuture {
        public Future<Iterator<?>> future;

        public DataFuture(Future<Iterator<?>> future) {
            super();
            this.future = future;
        }
    }    
}

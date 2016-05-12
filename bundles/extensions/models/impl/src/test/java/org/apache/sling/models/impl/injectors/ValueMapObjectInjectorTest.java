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
package org.apache.sling.models.impl.injectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.injectorspecific.ValueMapObject;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ValueMapObjectInjectorTest {

    private static final String LINK_JCR_PROP = "linkPath";
    private static final String LINK_JCR_PROP_VAL = "/path/to/page";

    private static final String LINK_JCR_PROP_DATE = "eventDate";
    private static final Date LINK_JCR_PROP_DATE_VAL = new Date();
    
    private ValueMapObjectInjector injector = new ValueMapObjectInjector();

    @Mock
    private ValueMap valueMap;
    
    @Mock
    private AnnotatedElement element;
    
    @Mock
    private DisposalCallbackRegistry registry;
    
    @Mock
    private SlingHttpServletRequest request;
    
    @Mock
    private Resource resource;

    Type testTypeListOfLink;

    
    @Before
    public void setUp() throws Exception {
        doReturn(resource).when(request).getResource();
        doReturn(valueMap).when(resource).adaptTo(ValueMap.class);
        doReturn(true).when(element).isAnnotationPresent(ValueMapObject.class);
        
        testTypeListOfLink = new Object() { public List<Link> testField; }.getClass().getField("testField").getGenericType();
        
    }
    
    @Test
    public void testScalarInjectionStringSource() {

        doReturn(LINK_JCR_PROP_VAL).when(valueMap).get(LINK_JCR_PROP);

        Object result = injector.getValue(request, LINK_JCR_PROP, Link.class, element, registry);
        assertEquals(Link.class, result.getClass());
        assertEquals(LINK_JCR_PROP_VAL, result.toString());
    }
    
    @Test
    public void testScalarInjectionStringSourceButNoAnnoation() {

        doReturn(LINK_JCR_PROP_VAL).when(valueMap).get(LINK_JCR_PROP);
        doReturn(false).when(element).isAnnotationPresent(ValueMapObject.class);

        Object result = injector.getValue(request, LINK_JCR_PROP, Link.class, element, registry);
        assertEquals(null, result);
    }
    
    @Test
    public void testScalarInjectionDateSource() {
        doReturn(LINK_JCR_PROP_DATE_VAL).when(valueMap).get(LINK_JCR_PROP_DATE);
        
        Object result = injector.getValue(request, LINK_JCR_PROP_DATE, EventDate.class, element, registry);
        assertEquals(EventDate.class, result.getClass());
        assertEquals(LINK_JCR_PROP_DATE_VAL, ((EventDate) result).getDate());
    }
    
    @Test
    public void testArrayInjectionStringSource() {

        doReturn(LINK_JCR_PROP_VAL).when(valueMap).get(LINK_JCR_PROP);

        Object result = injector.getValue(request, LINK_JCR_PROP, Link[].class, element, registry);
        assertEquals(Link[].class, result.getClass());
        Link[] resultArr = (Link[]) result;
        assertEquals(1, resultArr.length);
        assertEquals(LINK_JCR_PROP_VAL, resultArr[0].toString());
    }
    
    @Test
    public void testArrayInjectionStringArrSource() {

        String path1 = "/path1";
        String path2 = "/path2";
        doReturn(new String[]{path1, path2}).when(valueMap).get(LINK_JCR_PROP);

        Object result = injector.getValue(request, LINK_JCR_PROP, Link[].class, element, registry);
        assertEquals(Link[].class, result.getClass());
        Link[] resultArr = (Link[]) result;
        assertEquals(2, resultArr.length);
        assertEquals(path1, resultArr[0].toString());
        assertEquals(path2, resultArr[1].toString());
    }
    

    @Test
    public void testListInjectionStringSource() {
        
        doReturn(LINK_JCR_PROP_VAL).when(valueMap).get(LINK_JCR_PROP);

        Object result = injector.getValue(request, LINK_JCR_PROP, testTypeListOfLink, element, registry);
        assertTrue(List.class.isInstance(result)); 
        List resultList = (List) result;
        assertEquals(1, resultList.size());
        assertEquals(LINK_JCR_PROP_VAL, resultList.get(0).toString());
    }
    

    @Test
    public void testListInjectionStringArrSource() {
        
        String path1 = "/path1";
        String path2 = "/path2";
        doReturn(new String[]{path1, path2}).when(valueMap).get(LINK_JCR_PROP);
        
        Object result = injector.getValue(request, LINK_JCR_PROP, testTypeListOfLink, element, registry);
        assertTrue(List.class.isInstance(result)); 
        List resultList = (List) result;
        assertEquals(2, resultList.size());
        assertEquals(path1, resultList.get(0).toString());
        assertEquals(path2, resultList.get(1).toString());
    }
    
    @Test
    public void testScalarInjectionStringArrSource() {
        
        String path1 = "/path1";
        String path2 = "/path2";
        doReturn(new String[]{path1, path2}).when(valueMap).get(LINK_JCR_PROP);
        
        Object result = injector.getValue(request, LINK_JCR_PROP, Paths.class, element, registry);
        assertEquals(Paths.class, result.getClass());
        assertEquals(path1+","+path2, result.toString());
    }
    
    // example 1: Custom type for string
    public static class Link {
        final String link;

        public Link(String link) {
            this.link = link;
        }
        
        public String toString() {
            return link;
        }
        
    }
    
    // example 2: Custom type for date
    public static class EventDate {
        final Date date;

        public EventDate(Date date) {
            this.date = date;
        }
        
        public Date getDate() {
            return date;
        }
        
    }
    
    
    // example 3: Custom type for taking an array as constructor arg
    public static class Paths {
        final String[] paths;

        public Paths(String[] paths) {
            this.paths = paths;
        }
        
        public String toString() {
            return StringUtils.join(paths, ",");
        }
        
    }

}

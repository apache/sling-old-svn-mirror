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
package org.apache.sling.commons.json.sling;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JsonObjectCreatorTest {

    @Mock
    private Resource resource;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private ResourceResolver childResourceResolver;

    private Map<String, Object> props;
    private static final String RESOURCE_NAME = "testResource";
    private static final String PATH = "/" + RESOURCE_NAME;

    private static final Object SAME = new Object();
    private static final int NCHILDREN = 3;

    @Before
    public void setup() {
        props = new HashMap<String, Object>();
        props.put("defaultProperty", "defaultValue");

        final List<Resource> empty = new ArrayList<Resource>();
        when(childResourceResolver.listChildren(any(Resource.class))).thenReturn(empty.iterator());

        final List<Resource> children = new ArrayList<Resource>();
        for(int i=0; i < NCHILDREN; i++) {
            final Map<String, Object> childProps = new HashMap<String, Object>();
            final String id = "child" + i;
            childProps.put("id", id);
            final Resource r = Mockito.mock(Resource.class);
            when(r.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(childProps));
            when(r.getResourceResolver()).thenReturn(childResourceResolver);
            when(r.getPath()).thenReturn(PATH + "/" + id);
            children.add(r);
        }

        when(resourceResolver.listChildren(any(Resource.class))).thenReturn(children.iterator());
        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(resource.getPath()).thenReturn(PATH);
    }

    private void assertGet(Object data) throws JSONException {
        assertGet(data, SAME);
    }

    private void assertGet(Object data, Object expected) throws JSONException {
        final String key = UUID.randomUUID().toString();
        props.clear();
        props.put(key, data);
        when(resource.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(props));
        final JSONObject j = JsonObjectCreator.create(resource, 1);

        final String getKey = data instanceof InputStream ? ":"  + key : key;
        final Object toExpect = expected == SAME ? data : expected;
        if(toExpect instanceof String) {
            assertEquals(toExpect, j.get(getKey).toString());
        } else {
            assertEquals(toExpect, j.get(getKey));
        }
    }

    @Test
    public void testSimpleTypes() throws JSONException {
        assertGet("bar");
        assertGet(true);
        assertGet(123);
        assertGet(456.78);
        assertGet(System.currentTimeMillis());
    }

    @Test
    public void testStringValue() throws JSONException {
        final String value = "the string";
        when(resource.adaptTo(String.class)).thenReturn(value);
        final JSONObject j = JsonObjectCreator.create(resource, 1);
        assertEquals(value, j.get(RESOURCE_NAME));
    }

    @Test
    public void testStringArray() throws JSONException {
        final String [] values = { "A", "B" };
        when(resource.adaptTo(String[].class)).thenReturn(values);
        final JSONObject j = JsonObjectCreator.create(resource, 1);
        assertEquals("A", j.getJSONArray(RESOURCE_NAME).get(0));
        assertEquals("B", j.getJSONArray(RESOURCE_NAME).get(1));
    }

    @Test
    public void testArray() throws JSONException {
        final String [] values = { "C", "D" };
        final JSONArray a = new JSONArray().put("C").put("D");
        assertGet(values, a.toString());
    }

    @Test
    public void testEmptyArray() throws JSONException {
        final Long [] values = {};
        assertGet(values, "[]");
    }

    @Test
    public void testCalendar() throws JSONException {
        final Calendar nowCalendar = Calendar.getInstance();
        final String ECMA_DATE_FORMAT = "EEE MMM dd yyyy HH:mm:ss 'GMT'Z";
        final String nowString = new SimpleDateFormat(ECMA_DATE_FORMAT, JsonObjectCreator.DATE_FORMAT_LOCALE).format(nowCalendar.getTime());
        assertGet(nowCalendar, nowString);
    }

    @Test
    public void testStream() throws JSONException {
        final byte [] bytes = "Hello there".getBytes();
        final InputStream stream = new ByteArrayInputStream(bytes);
        // TODO not sure why we don't get the actual length here
        assertGet(stream, -1L);
    }

    @Test
    public void testChildren() throws JSONException {
        when(resource.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(props));
        final JSONObject j = JsonObjectCreator.create(resource, 2);
        for(int i=0 ; i < NCHILDREN; i++) {
            final String id = "child" + i;
            assertEquals(id, j.getJSONObject(id).get("id"));
        }
    }

}

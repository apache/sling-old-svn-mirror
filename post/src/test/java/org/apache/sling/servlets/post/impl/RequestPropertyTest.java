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
package org.apache.sling.servlets.post.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import junitx.util.PrivateAccessor;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.servlets.post.HtmlResponse;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;
import org.apache.sling.servlets.post.impl.operations.ModifyOperation;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This is primary a series of tests of the hasValues(), providesValues(), and getStringValues() methods of
 * RequestProperty. It uses the collectContent() method of ModifyOperation to make the test cases more readable.
 */
@RunWith(JMock.class)
public class RequestPropertyTest {

    private Mockery context = new JUnit4Mockery();

    @Test
    public void testSingleValue() throws Throwable {
        Map<String, RequestProperty> props = collectContent(p("./param", "true"));

        assertEquals(1, props.size());
        assertTrue(props.get("/test/path/param").hasValues());
        assertTrue(props.get("/test/path/param").providesValue());
        assertEquals(1, props.get("/test/path/param").getStringValues().length);
        assertEquals("true", props.get("/test/path/param").getStringValues()[0]);
    }

    @Test
    public void testSingleValueWithBlank() throws Throwable {
        Map<String, RequestProperty> props = collectContent(p("./param", ""));

        assertEquals(1, props.size());
        RequestProperty prop = props.get("/test/path/param");
        assertTrue(prop.hasValues());
        assertFalse(prop.providesValue());
        assertEquals(1, prop.getStringValues().length);
        assertEquals("", prop.getStringValues()[0]);
    }

    @Test
    public void testNullSingleValueWithDefaultToIgnore() throws Throwable {
        Map<String, RequestProperty> props = collectContent(p("./param@DefaultValue", ":ignore"));

        assertEquals(1, props.size());
        RequestProperty prop = props.get("/test/path/param");
        assertFalse(prop.hasValues());
    }

    @Test
    public void testSingleValueWithDefaultToIgnore() throws Throwable {
        Map<String, RequestProperty> props = collectContent(p("./param", ""), p("./param@DefaultValue", ":ignore"));

        assertEquals(1, props.size());
        RequestProperty prop = props.get("/test/path/param");
        assertTrue(prop.hasValues());
        assertFalse(prop.providesValue());
        assertEquals(0, prop.getStringValues().length);
    }

    @Test
    public void testSingleValueWithDefaultToNull() throws Throwable {
        Map<String, RequestProperty> props = collectContent(p("./param", ""), p("./param@DefaultValue", ":null"));

        assertEquals(1, props.size());
        RequestProperty prop = props.get("/test/path/param");
        assertTrue(prop.hasValues());
        assertTrue(prop.providesValue());
        assertNull(prop.getStringValues());
    }

    @Test
    public void testSingleValueIgnoringBlanks() throws Throwable {
        Map<String, RequestProperty> props = collectContent(p("./param", ""), p("./param@IgnoreBlanks", "true"));

        assertEquals(1, props.size());
        RequestProperty prop = props.get("/test/path/param");
        assertFalse(prop.hasValues());
    }

    @Test
    public void testMultiValue() throws Throwable {
        Map<String, RequestProperty> props = collectContent(p("./param", "true", "false"));

        assertEquals(1, props.size());
        RequestProperty prop = props.get("/test/path/param");
        assertTrue(prop.hasValues());
        assertTrue(prop.providesValue());
        assertEquals(2, prop.getStringValues().length);
        assertEquals("true", prop.getStringValues()[0]);
        assertEquals("false", prop.getStringValues()[1]);
    }

    @Test
    public void testMultiValueWithBlank() throws Throwable {
        Map<String, RequestProperty> props = collectContent(p("./param", "true", ""));

        assertEquals(1, props.size());
        RequestProperty prop = props.get("/test/path/param");
        assertTrue(prop.hasValues());
        assertTrue(prop.providesValue());
        assertEquals(2, prop.getStringValues().length);
        assertEquals("true", prop.getStringValues()[0]);
        assertEquals("", prop.getStringValues()[1]);
    }

    @Test
    public void testMultiValueWithBlanks() throws Throwable {
        Map<String, RequestProperty> props = collectContent(p("./param", "true", "", ""));

        assertEquals(1, props.size());
        RequestProperty prop = props.get("/test/path/param");
        assertTrue(prop.hasValues());
        assertTrue(prop.providesValue());
        assertEquals(3, prop.getStringValues().length);
        assertEquals("true", prop.getStringValues()[0]);
        assertEquals("", prop.getStringValues()[1]);
        assertEquals("", prop.getStringValues()[2]);
    }

    @Test
    public void testMultiValueWithAllBlanks() throws Throwable {
        Map<String, RequestProperty> props = collectContent(p("./param", "", "", ""));

        assertEquals(1, props.size());
        RequestProperty prop = props.get("/test/path/param");
        assertTrue(prop.hasValues());
        assertFalse(prop.providesValue());
        assertEquals(3, prop.getStringValues().length);
        assertEquals("", prop.getStringValues()[0]);
        assertEquals("", prop.getStringValues()[1]);
        assertEquals("", prop.getStringValues()[2]);
    }

    @Test
    public void testMultiValueWithBlankIgnoringBlanks() throws Throwable {
        Map<String, RequestProperty> props = collectContent(p("./param", "true", ""));

        assertEquals(1, props.size());
        RequestProperty prop = props.get("/test/path/param");
        assertTrue(prop.hasValues());
        assertTrue(prop.providesValue());
        assertEquals(2, prop.getStringValues().length);
        assertEquals("true", prop.getStringValues()[0]);
        assertEquals("", prop.getStringValues()[1]);
    }

    @Test
    public void testMultiValueWithBlanksIgnoringBlanks() throws Throwable {
        Map<String, RequestProperty> props = collectContent(p("./param", "true", "", ""), p("./param@IgnoreBlanks", "true"));

        assertEquals(1, props.size());
        RequestProperty prop = props.get("/test/path/param");
        assertTrue(prop.hasValues());
        assertTrue(prop.providesValue());
        assertEquals(1, prop.getStringValues().length);
        assertEquals("true", prop.getStringValues()[0]);
    }

    @Test
    public void testMultiValueWithAllBlanksIgnoringBlanks() throws Throwable {
        Map<String, RequestProperty> props = collectContent(p("./param", "", "", ""), p("./param@IgnoreBlanks", "true"));

        assertEquals(1, props.size());
        RequestProperty prop = props.get("/test/path/param");
        assertFalse(prop.hasValues());
    }

    private static final Class[] COLLECT_CLASSES = new Class[] { SlingHttpServletRequest.class, PostResponse.class };

    private class Param {
        String key;
        String[] value;
    }

    private Param p(String key, String... value) {
        Param kv = new Param();
        kv.key = key;
        kv.value = value;
        return kv;
    }

    @SuppressWarnings("unchecked")
    private Map<String, RequestProperty> collectContent(Param... kvs) throws Throwable {
        final List<Map.Entry<String, RequestParameter>> params = new ArrayList<Map.Entry<String, RequestParameter>>();
        for (int i = 0; i < kvs.length; i++) {
            final Param kv = kvs[i];
            final RequestParameter[] param = new RequestParameter[kv.value.length];
            for (int j = 0; j < kv.value.length; j++) {
                final String strValue = kv.value[j];
                final RequestParameter aparam = context.mock(RequestParameter.class, "requestParameter" + i + "#" + j);
                context.checking(new Expectations() {
                    {
                        allowing(aparam).getString();
                        will(returnValue(strValue));
                    }
                });
                param[j] = aparam;
            }
            final Map.Entry<String, RequestParameter> entry = context.mock(Map.Entry.class, "entry" + i);
            context.checking(new Expectations() {
                {
                    allowing(entry).getKey();
                    will(returnValue(kv.key));
                    allowing(entry).getValue();
                    will(returnValue(param));

                }
            });
            params.add(entry);
        }

        final Set set = context.mock(Set.class);
        context.checking(new Expectations() {
            {
                one(set).iterator();
                will(returnValue(params.iterator()));
            }
        });

        final RequestParameterMap map = context.mock(RequestParameterMap.class);
        context.checking(new Expectations() {
            {
                one(map).entrySet();
                will(returnValue(set));

            }
        });

        final SlingHttpServletRequest request = context.mock(SlingHttpServletRequest.class);
        context.checking(new Expectations() {
            {
                Vector names = new Vector();
                names.add("./param");

                one(request).getParameterNames();
                will(returnValue(names.elements()));
                one(request).getRequestParameterMap();
                will(returnValue(map));

            }
        });
        final HtmlResponse response = new HtmlResponse();
        response.setPath("/test/path");

        Map<String, RequestProperty> props = (Map<String, RequestProperty>) PrivateAccessor.invoke(
            new ModifyOperation(), "collectContent", COLLECT_CLASSES,
            new Object[] { request, response });
        return props;
    }
}

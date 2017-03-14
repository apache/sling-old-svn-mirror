/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.contentparser.impl;

import static org.apache.sling.jcr.contentparser.ParserOptions.DEFAULT_PRIMARY_TYPE;
import static org.apache.sling.jcr.contentparser.impl.ParserHelper.JCR_PRIMARYTYPE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.sling.jcr.contentparser.ParseException;
import org.apache.sling.jcr.contentparser.ParserOptions;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ParserHelperTest {

    @Test
    public void testEnsureDefaultPrimaryType() {
        Map<String,Object> content = new HashMap<>();
        content.put("prop1", "value1");

        ParserHelper underTest = new ParserHelper(new ParserOptions());
        underTest.ensureDefaultPrimaryType(content);
        
        assertEquals(ImmutableMap.<String,Object>of("prop1", "value1", JCR_PRIMARYTYPE, DEFAULT_PRIMARY_TYPE), content);
    }

    @Test
    public void testEnsureDefaultPrimaryType_Disabled() {
        Map<String,Object> content = new HashMap<>();
        content.put("prop1", "value1");

        ParserHelper underTest = new ParserHelper(new ParserOptions().defaultPrimaryType(null));
        underTest.ensureDefaultPrimaryType(content);
        
        assertEquals(ImmutableMap.<String,Object>of("prop1", "value1"), content);
    }

    @Test
    public void testEnsureDefaultPrimaryType_AlreadySet() {
        Map<String,Object> content = new HashMap<>();
        content.put("prop1", "value1");
        content.put(JCR_PRIMARYTYPE, "type1");

        ParserHelper underTest = new ParserHelper(new ParserOptions());
        underTest.ensureDefaultPrimaryType(content);
        
        assertEquals(ImmutableMap.<String,Object>of("prop1", "value1", JCR_PRIMARYTYPE, "type1"), content);
    }

    @Test
    public void testTryParseCalendar() {
        ParserHelper underTest = new ParserHelper(new ParserOptions().detectCalendarValues(true));
        
        Calendar value = underTest.tryParseCalendar("Tue Apr 22 2014 15:11:24 GMT+0200");
        assertNotNull(value);

        value.setTimeZone(TimeZone.getTimeZone("GMT+2"));

        assertEquals(2014, value.get(Calendar.YEAR));
        assertEquals(4, value.get(Calendar.MONTH) + 1);
        assertEquals(22, value.get(Calendar.DAY_OF_MONTH));

        assertEquals(15, value.get(Calendar.HOUR_OF_DAY));
        assertEquals(11, value.get(Calendar.MINUTE));
        assertEquals(24, value.get(Calendar.SECOND));
    }

    @Test
    public void testTryParseCalendar_Invalid() {
        ParserHelper underTest = new ParserHelper(new ParserOptions().detectCalendarValues(true));
        
        Calendar value = underTest.tryParseCalendar("hello world");
        assertNull(value);

        value = underTest.tryParseCalendar("");
        assertNull(value);

        value = underTest.tryParseCalendar(null);
        assertNull(value);
    }

    @Test
    public void testTryParseCalendar_Disabled() {
        ParserHelper underTest = new ParserHelper(new ParserOptions());
        
        Calendar value = underTest.tryParseCalendar("Tue Apr 22 2014 15:11:24 GMT+0200");
        assertNull(value);
    }

    @Test
    public void testIgnoreProperty() {
        ParserHelper underTest = new ParserHelper(new ParserOptions().ignorePropertyNames(ImmutableSet.of("prop1", "jcr:prop2")));
        
        assertTrue(underTest.ignoreProperty("prop1"));
        assertTrue(underTest.ignoreProperty("jcr:prop2"));
        assertFalse(underTest.ignoreProperty("prop3"));
    }

    @Test
    public void testIgnoreProperty_Disabled() {
        ParserHelper underTest = new ParserHelper(new ParserOptions());
        
        assertFalse(underTest.ignoreProperty("prop1"));
        assertFalse(underTest.ignoreProperty("jcr:prop2"));
        assertFalse(underTest.ignoreProperty("prop3"));
    }

    @Test
    public void testIgnoreResource() {
        ParserHelper underTest = new ParserHelper(new ParserOptions().ignoreResourceNames(ImmutableSet.of("node1", "jcr:node2")));
        
        assertTrue(underTest.ignoreResource("node1"));
        assertTrue(underTest.ignoreResource("jcr:node2"));
        assertFalse(underTest.ignoreResource("node3"));
    }

    @Test
    public void testIgnoreResource_Disabled() {
        ParserHelper underTest = new ParserHelper(new ParserOptions());
        
        assertFalse(underTest.ignoreResource("node1"));
        assertFalse(underTest.ignoreResource("jcr:node2"));
        assertFalse(underTest.ignoreResource("node3"));
    }

    @Test
    public void testCleanupPropertyName() {
        ParserHelper underTest = new ParserHelper(new ParserOptions());

        assertEquals("prop1", underTest.cleanupPropertyName("jcr:reference:prop1"));
        assertEquals("prop2", underTest.cleanupPropertyName("jcr:path:prop2"));
        assertEquals("jcr:xyz:prop3", underTest.cleanupPropertyName("jcr:xyz:prop3"));
    }

    @Test
    public void testCleanupPropertyName_Disabled() {
        ParserHelper underTest = new ParserHelper(new ParserOptions().removePropertyNamePrefixes(null));

        assertEquals("jcr:reference:prop1", underTest.cleanupPropertyName("jcr:reference:prop1"));
        assertEquals("jcr:path:prop2", underTest.cleanupPropertyName("jcr:path:prop2"));
        assertEquals("jcr:xyz:prop3", underTest.cleanupPropertyName("jcr:xyz:prop3"));
    }

    @Test
    public void testConvertSingleTypeArray() {
        ParserHelper underTest = new ParserHelper(new ParserOptions());

        assertArrayEquals(new Object[0], (Object[])underTest.convertSingleTypeArray(new Object[0]));
        assertArrayEquals(new String[] {"value1","value2"}, (String[])underTest.convertSingleTypeArray(new Object[] {"value1","value2"}));
        assertArrayEquals(new Long[] {1L,2L}, (Long[])underTest.convertSingleTypeArray(new Object[] {1L,2L}));
    }

    @Test(expected=ParseException.class)
    public void testConvertSingleTypeArray_WithNull() {
        ParserHelper underTest = new ParserHelper(new ParserOptions());
        underTest.convertSingleTypeArray(new Object[] {"value1",null});
    }

    @Test(expected=ParseException.class)
    public void testConvertSingleTypeArray_Map() {
        ParserHelper underTest = new ParserHelper(new ParserOptions());
        underTest.convertSingleTypeArray(new Object[] {ImmutableMap.<String,Object>of("prop1", "value1")});
    }

    @Test(expected=ParseException.class)
    public void testConvertSingleTypeArray_MixedType() {
        ParserHelper underTest = new ParserHelper(new ParserOptions());
        underTest.convertSingleTypeArray(new Object[] {"value1",1L});
    }

}

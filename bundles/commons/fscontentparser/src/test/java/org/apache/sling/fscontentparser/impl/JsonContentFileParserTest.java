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
package org.apache.sling.fscontentparser.impl;

import static org.apache.sling.fscontentparser.impl.TestUtils.getDeep;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import org.apache.sling.fscontentparser.ContentFileType;
import org.apache.sling.fscontentparser.ContentFileParser;
import org.apache.sling.fscontentparser.ContentFileParserFactory;
import org.apache.sling.fscontentparser.ParseException;
import org.apache.sling.fscontentparser.ParserOptions;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class JsonContentFileParserTest {

    private File file;

    @Before
    public void setUp() {
        file = new File("src/test/resources/content-test/content.json");
    }

    @Test
    public void testPageJcrPrimaryType() throws Exception {
        ContentFileParser underTest = ContentFileParserFactory.create(ContentFileType.JSON);
        Map<String, Object> content = underTest.parse(file);

        assertEquals("app:Page", content.get("jcr:primaryType"));
    }

    @Test
    public void testDataTypes() throws Exception {
        ContentFileParser underTest = ContentFileParserFactory.create(ContentFileType.JSON);
        Map<String, Object> content = underTest.parse(file);

        Map<String, Object> props = getDeep(content, "toolbar/profiles/jcr:content");
        assertEquals(true, props.get("hideInNav"));

        assertEquals(1234567890123L, props.get("longProp"));
        assertEquals(1.2345d, (Double) props.get("decimalProp"), 0.00001d);
        assertEquals(true, props.get("booleanProp"));

        assertArrayEquals(new Long[] { 1234567890123L, 55L }, (Long[]) props.get("longPropMulti"));
        assertArrayEquals(new Double[] { 1.2345d, 1.1d }, (Double[]) props.get("decimalPropMulti"));
        assertArrayEquals(new Boolean[] { true, false }, (Boolean[]) props.get("booleanPropMulti"));
    }

    @Test
    public void testContentProperties() throws Exception {
        ContentFileParser underTest = ContentFileParserFactory.create(ContentFileType.JSON);
        Map<String, Object> content = underTest.parse(file);

        Map<String, Object> props = getDeep(content, "jcr:content/header");
        assertEquals("/content/dam/sample/header.png", props.get("imageReference"));
    }

    @Test
    public void testCalendar() throws Exception {
        ContentFileParser underTest = ContentFileParserFactory.create(ContentFileType.JSON,
                new ParserOptions().detectCalendarValues(true));
        Map<String, Object> content = underTest.parse(file);

        Map<String, Object> props = getDeep(content, "jcr:content");

        Calendar calendar = (Calendar) props.get("app:lastModified");
        assertNotNull(calendar);

        calendar.setTimeZone(TimeZone.getTimeZone("GMT+2"));

        assertEquals(2014, calendar.get(Calendar.YEAR));
        assertEquals(4, calendar.get(Calendar.MONTH) + 1);
        assertEquals(22, calendar.get(Calendar.DAY_OF_MONTH));

        assertEquals(15, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(11, calendar.get(Calendar.MINUTE));
        assertEquals(24, calendar.get(Calendar.SECOND));
    }

    @Test
    public void testUTF8Chars() throws Exception {
        ContentFileParser underTest = ContentFileParserFactory.create(ContentFileType.JSON);
        Map<String, Object> content = underTest.parse(file);

        Map<String, Object> props = getDeep(content, "jcr:content");

        assertEquals("äöüß€", props.get("utf8Property"));
    }

    @Test(expected = ParseException.class)
    public void testParseInvalidJson() throws Exception {
        file = new File("src/test/resources/invalid-test/invalid.json");
        ContentFileParser underTest = ContentFileParserFactory.create(ContentFileType.JSON);
        Map<String, Object> content = underTest.parse(file);
        assertNull(content);
    }

    @Test(expected = ParseException.class)
    public void testParseInvalidJsonWithObjectList() throws Exception {
        file = new File("src/test/resources/invalid-test/contentWithObjectList.json");
        ContentFileParser underTest = ContentFileParserFactory.create(ContentFileType.JSON);
        Map<String, Object> content = underTest.parse(file);
        assertNull(content);
    }

    @Test
    public void testIgnoreResourcesProperties() throws Exception {
        ContentFileParser underTest = ContentFileParserFactory.create(ContentFileType.JSON,
                new ParserOptions().ignoreResourceNames(ImmutableSet.of("header", "newslist"))
                        .ignorePropertyNames(ImmutableSet.of("jcr:title")));
        Map<String, Object> content = underTest.parse(file);
        Map<String, Object> props = getDeep(content, "jcr:content");

        assertEquals("Sample Homepage", props.get("pageTitle"));
        assertNull(props.get("jcr:title"));

        assertNull(props.get("header"));
        assertNull(props.get("newslist"));
        assertNotNull(props.get("lead"));

        assertEquals("abc", props.get("refpro1"));
        assertEquals("def", props.get("pathprop1"));
    }

}

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

import static org.apache.sling.jcr.contentparser.impl.TestUtils.parse;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import org.apache.sling.jcr.contentparser.ContentParser;
import org.apache.sling.jcr.contentparser.ContentParserFactory;
import org.apache.sling.jcr.contentparser.ContentType;
import org.apache.sling.jcr.contentparser.ParseException;
import org.apache.sling.jcr.contentparser.ParserOptions;
import org.apache.sling.jcr.contentparser.impl.mapsupport.ContentElement;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class JsonContentParserTest {

    private File file;

    @Before
    public void setUp() {
        file = new File("src/test/resources/content-test/content.json");
    }

    @Test
    public void testPageJcrPrimaryType() throws Exception {
        ContentParser underTest = ContentParserFactory.create(ContentType.JSON);
        ContentElement content = parse(underTest, file);

        assertEquals("app:Page", content.getProperties().get("jcr:primaryType"));
    }

    @Test
    public void testDataTypes() throws Exception {
        ContentParser underTest = ContentParserFactory.create(ContentType.JSON);
        ContentElement content = parse(underTest, file);

        Map<String, Object> props = content.getChild("toolbar/profiles/jcr:content").getProperties();
        assertEquals(true, props.get("hideInNav"));

        assertEquals(1234567890123L, props.get("longProp"));
        assertEquals(new BigDecimal("1.2345"), props.get("decimalProp"));
        assertEquals(true, props.get("booleanProp"));

        assertArrayEquals(new Long[] { 1234567890123L, 55L }, (Long[]) props.get("longPropMulti"));
        assertArrayEquals(new BigDecimal[] { new BigDecimal("1.2345"), new BigDecimal("1.1") }, (BigDecimal[]) props.get("decimalPropMulti"));
        assertArrayEquals(new Boolean[] { true, false }, (Boolean[]) props.get("booleanPropMulti"));
    }

    @Test
    public void testContentProperties() throws Exception {
        ContentParser underTest = ContentParserFactory.create(ContentType.JSON);
        ContentElement content = parse(underTest, file);

        Map<String, Object> props = content.getChild("jcr:content/header").getProperties();
        assertEquals("/content/dam/sample/header.png", props.get("imageReference"));
    }

    @Test
    public void testCalendar() throws Exception {
        ContentParser underTest = ContentParserFactory.create(ContentType.JSON,
                new ParserOptions().detectCalendarValues(true));
        ContentElement content = parse(underTest, file);

        Map<String, Object> props = content.getChild("jcr:content").getProperties();

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
        ContentParser underTest = ContentParserFactory.create(ContentType.JSON);
        ContentElement content = parse(underTest, file);

        Map<String, Object> props = content.getChild("jcr:content").getProperties();

        assertEquals("äöüß€", props.get("utf8Property"));
    }

    @Test(expected = ParseException.class)
    public void testParseInvalidJson() throws Exception {
        file = new File("src/test/resources/invalid-test/invalid.json");
        ContentParser underTest = ContentParserFactory.create(ContentType.JSON);
        ContentElement content = parse(underTest, file);
        assertNull(content);
    }

    @Test(expected = ParseException.class)
    public void testParseInvalidJsonWithObjectList() throws Exception {
        file = new File("src/test/resources/invalid-test/contentWithObjectList.json");
        ContentParser underTest = ContentParserFactory.create(ContentType.JSON);
        ContentElement content = parse(underTest, file);
        assertNull(content);
    }

    @Test
    public void testIgnoreResourcesProperties() throws Exception {
        ContentParser underTest = ContentParserFactory.create(ContentType.JSON,
                new ParserOptions().ignoreResourceNames(ImmutableSet.of("header", "newslist"))
                        .ignorePropertyNames(ImmutableSet.of("jcr:title")));
        ContentElement content = parse(underTest, file);
        ContentElement child = content.getChild("jcr:content");

        assertEquals("Sample Homepage", child.getProperties().get("pageTitle"));
        assertNull(child.getProperties().get("jcr:title"));

        assertNull(child.getChildren().get("header"));
        assertNull(child.getChildren().get("newslist"));
        assertNotNull(child.getChildren().get("lead"));

        assertEquals("abc", child.getProperties().get("refpro1"));
        assertEquals("def", child.getProperties().get("pathprop1"));
    }

    @Test
    public void testGetChild() throws Exception {
        ContentParser underTest = ContentParserFactory.create(ContentType.JSON);
        ContentElement content = parse(underTest, file);
        assertNull(content.getName());
        
        ContentElement deepChild = content.getChild("jcr:content/par/image/file/jcr:content");
        assertEquals("jcr:content", deepChild.getName());
        assertEquals("nt:resource", deepChild.getProperties().get("jcr:primaryType"));

        ContentElement invalidChild = content.getChild("non/existing/path");
        assertNull(invalidChild);

        invalidChild = content.getChild("/jcr:content");
        assertNull(invalidChild);
    }

}

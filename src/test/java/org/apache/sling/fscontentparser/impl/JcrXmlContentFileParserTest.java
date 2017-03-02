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

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.fscontentparser.ContentFileType;
import org.apache.sling.fscontentparser.ContentFileParser;
import org.apache.sling.fscontentparser.ContentFileParserFactory;
import org.apache.sling.fscontentparser.ParseException;
import org.apache.sling.fscontentparser.ParserOptions;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class JcrXmlContentFileParserTest {

    private File file;

    @Before
    public void setUp() throws Exception {
        file = new File("src/test/resources/content-test/content.jcr.xml");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testParseJcrXml() throws Exception {
        ContentFileParser underTest = ContentFileParserFactory.create(ContentFileType.JCR_XML);
        Map<String,Object> content = underTest.parse(file);
        assertNotNull(content);
        assertEquals("app:Page", content.get("jcr:primaryType"));
        assertEquals("app:PageContent", ((Map<String,Object>)content.get("jcr:content")).get("jcr:primaryType"));
    }

    @Test(expected=ParseException.class)
    public void testParseInvalidJcrXml() throws Exception {
        file = new File("src/test/resources/invalid-test/invalid.jcr.xml");
        ContentFileParser underTest = ContentFileParserFactory.create(ContentFileType.JCR_XML);
        underTest.parse(file);
    }

    @Test
    public void testDataTypes() throws Exception {
        ContentFileParser underTest = ContentFileParserFactory.create(ContentFileType.JCR_XML);
        Map<String,Object> content = underTest.parse(file);
        Map<String,Object> props = getDeep(content, "jcr:content");
        
        assertEquals("en", props.get("jcr:title"));
        assertEquals(true, props.get("includeAside"));
        assertEquals((Long)1234567890123L, props.get("longProp"));
        assertEquals((Double)1.2345d, (Double)props.get("decimalProp"), 0.00001d);
        
        assertArrayEquals(new String[] { "aa", "bb", "cc" }, (String[])props.get("stringPropMulti"));
        assertArrayEquals(new Long[] { 1234567890123L, 55L }, (Long[])props.get("longPropMulti"));
        
        Calendar calendar = (Calendar)props.get("dateProp");
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+2"));
        assertEquals(2014, calendar.get(Calendar.YEAR));
        assertEquals(9, calendar.get(Calendar.MONTH) + 1);
        assertEquals(19, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(21, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(20, calendar.get(Calendar.MINUTE));
        assertEquals(26, calendar.get(Calendar.SECOND));
        assertEquals(812, calendar.get(Calendar.MILLISECOND));
    }

    @Test
    public void testDecodeName() {
        assertEquals("jcr:title", JcrXmlContentFileParser.decodeName("jcr:" + ISO9075.encode("title")));
        assertEquals("sling:123", JcrXmlContentFileParser.decodeName("sling:" + ISO9075.encode("123")));
    }

    @Test
    public void testIgnoreResourcesProperties() throws Exception {
        ContentFileParser underTest = ContentFileParserFactory.create(ContentFileType.JCR_XML, new ParserOptions()
                .ignoreResourceNames(ImmutableSet.of("teaserbar", "aside"))
                .ignorePropertyNames(ImmutableSet.of("longProp", "jcr:title")));
        Map<String,Object> content = underTest.parse(file);
        Map<String,Object> props = getDeep(content, "jcr:content");
        
        assertEquals("HOME", props.get("navTitle"));
        assertNull(props.get("jcr:title"));
        assertNull(props.get("longProp"));
        
        assertNull(props.get("teaserbar"));
        assertNull(props.get("aside"));
        assertNotNull(props.get("content"));
    }

}

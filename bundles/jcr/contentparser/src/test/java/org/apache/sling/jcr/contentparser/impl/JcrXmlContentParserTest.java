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

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.jcr.contentparser.ContentParser;
import org.apache.sling.jcr.contentparser.ContentParserFactory;
import org.apache.sling.jcr.contentparser.ContentType;
import org.apache.sling.jcr.contentparser.ParseException;
import org.apache.sling.jcr.contentparser.ParserOptions;
import org.apache.sling.jcr.contentparser.impl.mapsupport.ContentElement;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class JcrXmlContentParserTest {

    private File file;

    @Before
    public void setUp() throws Exception {
        file = new File("src/test/resources/content-test/content.jcr.xml");
    }

    @Test
    public void testParseJcrXml() throws Exception {
        ContentParser underTest = ContentParserFactory.create(ContentType.JCR_XML);
        ContentElement content = parse(underTest, file);
        assertNotNull(content);
        assertEquals("app:Page", content.getProperties().get("jcr:primaryType"));
        assertEquals("app:PageContent", content.getChild("jcr:content").getProperties().get("jcr:primaryType"));
    }

    @Test(expected=ParseException.class)
    public void testParseInvalidJcrXml() throws Exception {
        file = new File("src/test/resources/invalid-test/invalid.jcr.xml");
        ContentParser underTest = ContentParserFactory.create(ContentType.JCR_XML);
        parse(underTest, file);
    }

    @Test
    public void testDataTypes() throws Exception {
        ContentParser underTest = ContentParserFactory.create(ContentType.JCR_XML);
        ContentElement content = parse(underTest, file);
        Map<String,Object> props = content.getChild("jcr:content").getProperties();
        
        assertEquals("en", props.get("jcr:title"));
        assertEquals(true, props.get("includeAside"));
        assertEquals((Long)1234567890123L, props.get("longProp"));
        assertEquals(new BigDecimal("1.2345"), props.get("decimalProp"));
        
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
        assertEquals("jcr:title", JcrXmlContentParser.decodeName("jcr:" + ISO9075.encode("title")));
        assertEquals("sling:123", JcrXmlContentParser.decodeName("sling:" + ISO9075.encode("123")));
    }

    @Test
    public void testIgnoreResourcesProperties() throws Exception {
        ContentParser underTest = ContentParserFactory.create(ContentType.JCR_XML, new ParserOptions()
                .ignoreResourceNames(ImmutableSet.of("teaserbar", "aside"))
                .ignorePropertyNames(ImmutableSet.of("longProp", "jcr:title")));
        ContentElement content = parse(underTest, file);
        ContentElement child = content.getChild("jcr:content");
        
        assertEquals("HOME", child.getProperties().get("navTitle"));
        assertNull(child.getProperties().get("jcr:title"));
        assertNull(child.getProperties().get("longProp"));
        
        assertNull(child.getChildren().get("teaserbar"));
        assertNull(child.getChildren().get("aside"));
        assertNotNull(child.getChildren().get("content"));
    }

    @Test
    public void testGetChild() throws Exception {
        ContentParser underTest = ContentParserFactory.create(ContentType.JCR_XML);
        ContentElement content = parse(underTest, file);
        assertNull(content.getName());
        
        ContentElement deepChild = content.getChild("jcr:content/teaserbar/teaserbaritem");
        assertEquals("teaserbaritem", deepChild.getName());
        assertEquals("samples/sample-app/components/content/teaserbar/teaserbarItem", deepChild.getProperties().get("sling:resourceType"));

        ContentElement invalidChild = content.getChild("non/existing/path");
        assertNull(invalidChild);

        invalidChild = content.getChild("/jcr:content");
        assertNull(invalidChild);
    }

    @Test
    public void testSameNamePropertyAndSubResource() throws Exception {
        ContentParser underTest = ContentParserFactory.create(ContentType.JCR_XML);
        ContentElement content = parse(underTest, file);
        ContentElement child = content.getChild("jcr:content/teaserbar");
        // teaserbaritem is a direct property as well as a sub resource
        assertEquals("test", child.getProperties().get("teaserbaritem"));
        assertNotNull(child.getChildren().get("teaserbaritem"));
    }

}

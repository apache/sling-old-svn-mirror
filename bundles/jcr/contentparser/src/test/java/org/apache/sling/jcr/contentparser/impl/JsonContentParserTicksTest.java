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
import static org.junit.Assert.assertEquals;

import java.util.EnumSet;
import java.util.Map;

import org.apache.sling.jcr.contentparser.ContentParser;
import org.apache.sling.jcr.contentparser.ContentParserFactory;
import org.apache.sling.jcr.contentparser.ContentType;
import org.apache.sling.jcr.contentparser.JsonParserFeature;
import org.apache.sling.jcr.contentparser.ParseException;
import org.apache.sling.jcr.contentparser.ParserOptions;
import org.apache.sling.jcr.contentparser.impl.mapsupport.ContentElement;
import org.junit.Before;
import org.junit.Test;

public class JsonContentParserTicksTest {
    
    private ContentParser underTest;
    
    @Before
    public void setUp() {
        underTest = ContentParserFactory.create(ContentType.JSON,
                new ParserOptions().jsonParserFeatures(JsonParserFeature.QUOTE_TICK, JsonParserFeature.COMMENTS));
    }

    @Test
    public void testJsonWithTicks() throws Exception {
        ContentElement content = parse(underTest, "{'prop1':'value1','prop2':123,'obj':{'prop3':'value2'}}");

        Map<String, Object> props = content.getProperties();
        assertEquals("value1", props.get("prop1"));
        assertEquals(123L, props.get("prop2"));
        assertEquals("value2", content.getChild("obj").getProperties().get("prop3"));
    }

    @Test
    public void testJsonWithTicksMixed() throws Exception {
        ContentElement content = parse(underTest, "{\"prop1\":'value1','prop2':123,'obj':{'prop3':\"value2\"}}");

        Map<String, Object> props = content.getProperties();
        assertEquals("value1", props.get("prop1"));
        assertEquals(123L, props.get("prop2"));
        assertEquals("value2", content.getChild("obj").getProperties().get("prop3"));
    }

    @Test
    public void testJsonWithTicksMixedWithComment() throws Exception {
        ContentElement content = parse(underTest, "{/*a'b\"c*/\"prop1\":'value1','prop2':123,'obj':{'prop3':\"value2\"}}");

        Map<String, Object> props = content.getProperties();
        assertEquals("value1", props.get("prop1"));
        assertEquals(123L, props.get("prop2"));
        assertEquals("value2", content.getChild("obj").getProperties().get("prop3"));
    }

    @Test
    public void testTicksDoubleQuotesInDoubleQuotes() throws Exception {
        ContentElement content = parse(underTest, "{\"prop1\":\"'\\\"\'\\\"\"}");

        Map<String, Object> props = content.getProperties();
        assertEquals("'\"'\"", props.get("prop1"));
    }

    @Test
    public void testTicksDoubleQuotesInTicks() throws Exception {
        ContentElement content = parse(underTest, "{'prop1':'\\'\\\"\\\'\\\"'}");

        Map<String, Object> props = content.getProperties();
        assertEquals("'\"'\"", props.get("prop1"));
    }

    @Test
    public void testWithUtf8Escaped() throws Exception {
        ContentElement content = parse(underTest, "{\"prop1\":\"\\u03A9\\u03A6\\u00A5\"}");

        Map<String, Object> props = content.getProperties();
        assertEquals("\u03A9\u03A6\u00A5", props.get("prop1"));
    }

    @Test
    public void testWithTicksUtf8Escaped() throws Exception {
        ContentElement content = parse(underTest, "{'prop1':'\\u03A9\\u03A6\\u00A5'}");

        Map<String, Object> props = content.getProperties();
        assertEquals("\u03A9\u03A6\u00A5", props.get("prop1"));
    }

    @Test(expected = ParseException.class)
    public void testFailsWihtoutFeatureEnabled() throws Exception {
        underTest = ContentParserFactory.create(ContentType.JSON,
                new ParserOptions().jsonParserFeatures(EnumSet.noneOf(JsonParserFeature.class)));
        parse(underTest, "{'prop1':'value1','prop2':123,'obj':{'prop3':'value2'}}");
    }

}

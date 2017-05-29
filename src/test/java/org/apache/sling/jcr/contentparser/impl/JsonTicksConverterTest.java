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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import static org.apache.sling.jcr.contentparser.impl.JsonTicksConverter.*;

public class JsonTicksConverterTest {

    @Test
    public void testNoConvert() {
        assertEquals("{\"p\":\"v\"}", tickToDoubleQuote("{\"p\":\"v\"}"));
    }

    @Test
    public void testTickToQuote() {
        assertEquals("{\"p\":\"v\"}", tickToDoubleQuote("{'p':\"v\"}"));
    }

    @Test
    public void testTickToQuoteMixed() {
        assertEquals("{\"p\":\"v\"}", tickToDoubleQuote("{'p':\"v\"}"));
        assertEquals("{\"p\":\"v\"}", tickToDoubleQuote("{\"p\":'v'}"));
    }

    @Test
    public void testTickToQuoteMixedWithComment() {
        assertEquals("{ /* abc */ \"p\":\"v\"}", tickToDoubleQuote("{ /* abc */ 'p':\"v\"}"));
        assertEquals("{ /* ab'c */ \"p\":\"v\"}", tickToDoubleQuote("{ /* ab'c */ 'p':\"v\"}"));
        assertEquals("{ /* ab'c' */ \"p\":\"v\"}", tickToDoubleQuote("{ /* ab'c' */ 'p':\"v\"}"));
        assertEquals("{ /* ab\"c */ \"p\":\"v\"}", tickToDoubleQuote("{ /* ab\"c */ 'p':\"v\"}"));
        assertEquals("{ /* ab\"c\" */ \"p\":\"v\"}", tickToDoubleQuote("{ /* ab\"c\" */ 'p':\"v\"}"));
        assertEquals("{ /* ab'c\" */ \"p\":\"v\"}", tickToDoubleQuote("{ /* ab'c\" */ 'p':\"v\"}"));
        assertEquals("{/*ab'c\"*/\"p\":\"v\"/*ab'c\"*/}", tickToDoubleQuote("{/*ab'c\"*/'p':\"v\"/*ab'c\"*/}"));
    }
    
    @Test
    public void testTicksDoubleQuotesInDoubleQuotes() {
        assertEquals("{\"p\":\"'\\\"'\\\"\"}", tickToDoubleQuote("{\"p\":\"'\\\"'\\\"\"}"));
    }

    @Test
    public void testTicksDoubleQuotesInTicks() {
        assertEquals("{\"p\":\"'\\\"'\\\"\"}", tickToDoubleQuote("{\"p\":'\\'\\\"\\'\\\"'}"));
        assertEquals("{\"p\":\"'\\\"'\\\"\"}", tickToDoubleQuote("{\"p\":'\\'\"\\'\"'}"));
    }

    @Test
    public void testTickToQuoteWithUtf8Escaped() {
        assertEquals("{\"p\":\"\\u03A9\\u03A6\\u00A5\"}", tickToDoubleQuote("{'p':\"\\u03A9\\u03A6\\u00A5\"}"));
    }

    @Test
    public void testTickToQuoteWithDoubleBackslash() {
        assertEquals("{\"p\":\"aa\\\\bb\"}", tickToDoubleQuote("{'p':\"aa\\\\bb\"}"));
    }

}

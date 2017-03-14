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

import static org.apache.sling.jcr.contentparser.impl.JcrXmlValueConverter.parseValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.UUID;

import org.junit.Test;

public class JcrXmlValueConverterTest {
    
    private static final String NAME = "prop1";

    @Test
    public void testNull() {
        assertNull(parseValue(NAME, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid() {
        parseValue(NAME, "{InvalidType}xyz");
    }

    @Test
    public void testString() {
        assertEquals("myString", parseValue(NAME, "myString"));
        assertEquals("prop", "myString [ ] { } \\ ,", parseValue(NAME, "myString [ ] { } \\\\ ,"));
        assertEquals("{myString}", parseValue(NAME, "\\{myString}"));
        assertEquals("aaa{myString}", parseValue(NAME, "aaa{myString}"));
        assertEquals("[myString]", parseValue(NAME, "\\[myString]"));
        assertEquals("aaa[myString]", parseValue(NAME, "aaa[myString]"));
    }

    @Test
    public void testStringArray() {
        assertArrayEquals(new Object[] { "myString1", "myString2" }, (Object[]) parseValue(NAME, "[myString1,myString2]"));
        assertArrayEquals(new Object[] { "myString1,[]\\äöüß€", "myString2", "myString3 [ ] { } \\ ,", "", "[myString5]", "{myString6}" },
                (Object[]) parseValue(NAME, "[myString1\\,[]\\\\äöüß€,myString2,myString3 [ ] { } \\\\ \\,,,[myString5],{myString6}]"));
    }

    @Test
    public void testBoolean() {
        assertEquals(true, parseValue(NAME, "{Boolean}true"));
        assertEquals(false, parseValue(NAME, "{Boolean}false"));
    }

    @Test
    public void testBooleanArray() {
        assertArrayEquals(new Object[] { true, false }, (Object[]) parseValue(NAME, "{Boolean}[true,false]"));
    }

    @Test
    public void testLong() {
        assertEquals(1L, parseValue(NAME, "{Long}1"));
        assertEquals(10000000000L, parseValue(NAME, "{Long}10000000000"));
    }

    @Test
    public void testLongArray() {
        assertArrayEquals(new Object[] { 1L, 2L }, (Object[]) parseValue(NAME, "{Long}[1,2]"));
        assertArrayEquals(new Object[] { 10000000000L, 20000000000L }, (Object[]) parseValue(NAME, "{Long}[10000000000,20000000000]"));
    }

    @Test
    public void testDouble() {
        assertEquals(new BigDecimal("1.234"), parseValue(NAME, "{Decimal}1.234"));
    }

    @Test
    public void testDoubleArray() {
        assertArrayEquals(new Object[] { new BigDecimal("1.234"), new BigDecimal("2.345") }, (Object[]) parseValue(NAME, "{Decimal}[1.234,2.345]"));
    }

    @Test
    public void testCalendar() {
        Calendar value = (Calendar)parseValue(NAME, "{Date}2010-09-05T15:10:20.000Z");
        assertEquals(2010, value.get(Calendar.YEAR));
        assertEquals(8, value.get(Calendar.MONTH));
        assertEquals(5, value.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testStringArrayRepPrivileges() {
        assertArrayEquals(new Object[] { "rep:write", "crx:replicate", "jcr:read" }, (Object[]) parseValue(NAME, "{Name}[rep:write,crx:replicate,jcr:read]"));
    }

    @Test
    public void testReference() {
        UUID uuid = UUID.randomUUID();
        UUID value = (UUID)parseValue(NAME, "{Reference}" + uuid.toString());
        assertEquals(uuid, value);
    }

    @Test
    public void testURI() {
        URI value = (URI)parseValue(NAME, "{URI}http://www.jodelkaiser.de/");
        assertEquals("http://www.jodelkaiser.de/", value.toString());
    }

}

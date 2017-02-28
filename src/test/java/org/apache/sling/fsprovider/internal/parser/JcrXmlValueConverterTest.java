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
package org.apache.sling.fsprovider.internal.parser;

import static org.apache.sling.fsprovider.internal.parser.JcrXmlValueConverter.parseValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;

import org.junit.Test;

public class JcrXmlValueConverterTest {

    @Test
    public void testNull() {
        assertNull(parseValue(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid() {
        parseValue("{InvalidType}xyz");
    }

    @Test
    public void testString() {
        assertEquals("myString", parseValue("myString"));
        assertEquals("prop", "myString [ ] { } \\ ,", parseValue("myString [ ] { } \\\\ ,"));
        assertEquals("{myString}", parseValue("\\{myString}"));
        assertEquals("aaa{myString}", parseValue("aaa{myString}"));
        assertEquals("[myString]", parseValue("\\[myString]"));
        assertEquals("aaa[myString]", parseValue("aaa[myString]"));
    }

    @Test
    public void testStringArray() {
        assertArrayEquals(new Object[] { "myString1", "myString2" }, (Object[]) parseValue("[myString1,myString2]"));
        assertArrayEquals(new Object[] { "myString1,[]\\äöüß€", "myString2", "myString3 [ ] { } \\ ,", "", "[myString5]", "{myString6}" },
                (Object[]) parseValue("[myString1\\,[]\\\\äöüß€,myString2,myString3 [ ] { } \\\\ \\,,,[myString5],{myString6}]"));
    }

    @Test
    public void testBoolean() {
        assertEquals(true, parseValue("{Boolean}true"));
        assertEquals(false, parseValue("{Boolean}false"));
    }

    @Test
    public void testBooleanArray() {
        assertArrayEquals(new Object[] { true, false }, (Object[]) parseValue("{Boolean}[true,false]"));
    }

    @Test
    public void testLong() {
        assertEquals(1L, parseValue("{Long}1"));
        assertEquals(10000000000L, parseValue("{Long}10000000000"));
    }

    @Test
    public void testLongArray() {
        assertArrayEquals(new Object[] { 1L, 2L }, (Object[]) parseValue("{Long}[1,2]"));
        assertArrayEquals(new Object[] { 10000000000L, 20000000000L }, (Object[]) parseValue("{Long}[10000000000,20000000000]"));
    }

    @Test
    public void testDouble() {
        assertEquals(1.234d, parseValue("{Decimal}1.234"));
    }

    @Test
    public void testDoubleArray() {
        assertArrayEquals(new Object[] { 1.234d, 2.345d }, (Object[]) parseValue("{Decimal}[1.234,2.345]"));
    }

    @Test
    public void testCalendar() {
        Calendar value = (Calendar)parseValue("{Date}2010-09-05T15:10:20.000Z");
        assertEquals(2010, value.get(Calendar.YEAR));
        assertEquals(8, value.get(Calendar.MONTH));
        assertEquals(5, value.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testStringArrayRepPrivileges() {
        assertArrayEquals(new Object[] { "rep:write", "crx:replicate", "jcr:read" }, (Object[]) parseValue("{Name}[rep:write,crx:replicate,jcr:read]"));
    }

}

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
package org.apache.sling.engine.impl.parameters;

import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

public class ContainerRequestParameterTest extends TestCase {

    private static final String LATIN1 = "ISO-8859-1";

    private static final String UTF8 = "UTF-8";

    public void testChangeEncodingAscii() throws UnsupportedEncodingException {
        testInternal("value", LATIN1, UTF8);
    }

    public void testChangeEncodingLatin1() throws UnsupportedEncodingException {
        // latin small letter o/a/u with diaresis
        // encoded LATIN-1 String of UTF-8 encoding
        testInternal("\u00c3\u00b6\u00c3\u00a4\u00c3\u00bc", UTF8, LATIN1);
    }

    public void testChangeEncodingUpper() throws UnsupportedEncodingException {
        // runic letter e, katakana letter pa, halfwidth katakana letter no
        // encoded LATIN-1 String of UTF-8 encoding
        testInternal("\u00e1\u009b\u0082\u00e3\u0083\u0091\u00ef\u00be\u0089", LATIN1, UTF8);
    }

    private void testInternal(String value, String baseEncoding,
            String targetEncoding) throws UnsupportedEncodingException {
        ContainerRequestParameter par = new ContainerRequestParameter("name", value,
            baseEncoding);

        assertEquals(baseEncoding, par.getEncoding());
        assertEquals(value, par.getString());
        assertEquals("byte[] value mismatch", value.getBytes(baseEncoding),
            par.get());

        par.setEncoding(targetEncoding);

        assertEquals(targetEncoding, par.getEncoding());
        assertEquals(new String(value.getBytes(baseEncoding), targetEncoding),
            par.getString());
        assertEquals("byte[] value mismatch", value.getBytes(baseEncoding),
            par.get());
    }

    private void assertEquals(String message, byte[] expected, byte[] actual) {
        assertEquals(message, expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            assertEquals(message, expected[i], actual[i]);
        }
    }
}

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
package org.apache.sling.servlets.get.impl.helpers;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Random;

import org.apache.sling.servlets.get.impl.helpers.StreamRendererServlet;

import junit.framework.TestCase;

public class StreamRendererServletTest extends TestCase {

    public void testCopyRange() {
        assertCopyRange(1234);
        assertCopyRange(4321);
    }

    private void assertCopyRange(long seed) {
        Random random = new Random(seed);

        // generate some random test data
        byte[] expected = new byte[random.nextInt(10000)]; // >> IO_BUFFER_SIZE
        random.nextBytes(expected);

        // Check some simple cases ...
        assertCopyRange(expected, 0, 0);
        assertCopyRange(expected, 0, 1);
        assertCopyRange(expected, 0, expected.length);

        // ... and a few randomly selected ones
        int n = random.nextInt(100);
        for (int i = 0; i < n; i++) {
            int a = random.nextInt(expected.length);
            int b = random.nextInt(expected.length);
            if (a > b) {
                int x = a;
                a = b;
                b = x;
            }
            assertCopyRange(expected, a, b);
        }
    }

    private void assertCopyRange(byte[] expected, int a, int b) {
        assertCopyRange(expected, new ByteArrayInputStream(expected), a, b);
        // with BufferedInputStream
        assertCopyRange(expected, new BufferedInputStream(new ByteArrayInputStream(expected)), a, b);
        // without available()
        assertCopyRange(expected, new ByteArrayInputStream(expected) {
            @Override
            public synchronized int available() {
                return 0;
            }
        }, a, b);
        // with BufferedInputStream and without available()
        assertCopyRange(expected, new BufferedInputStream(new ByteArrayInputStream(expected) {
            @Override
            public synchronized int available() {
                return 0;
            }
        }), a, b);
    }

    private void assertCopyRange(
            byte[] expected, InputStream input, int a, int b) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        StreamRendererServlet.staticCopyRange(
                new ByteArrayInputStream(expected), output, a, b);

        byte[] actual = output.toByteArray();
        assertEquals(b - a, actual.length);
        for (int i = a; i < b; i++) {
            assertEquals(expected[i], actual[i - a]);
        }
    }
}

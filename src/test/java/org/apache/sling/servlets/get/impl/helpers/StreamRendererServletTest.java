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

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.junit.Test;

public class StreamRendererServletTest {

    @Test
    public void testCopyRange() throws IOException {
        runTests(1234);
        runTests(4321);
    }

    @Test
    public void testResultingLength() throws IOException {
        final ByteArrayInputStream in = new ByteArrayInputStream("12345678".getBytes());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamRendererServlet.staticCopyRange(in, out, 2, 4);
        final String result = out.toString();
        assertEquals(2, result.length());
        assertEquals("34", result);
    }
    
    private void runTests(int randomSeed) throws IOException {
        final Random random = new Random(randomSeed);
        assertCopyRange(random, StreamRendererServlet.IO_BUFFER_SIZE * 2 + 42);
        assertCopyRange(random, StreamRendererServlet.IO_BUFFER_SIZE * 3);
        assertCopyRange(random, StreamRendererServlet.IO_BUFFER_SIZE);
        assertCopyRange(random, StreamRendererServlet.IO_BUFFER_SIZE - 1);
        assertCopyRange(random, random.nextInt(StreamRendererServlet.IO_BUFFER_SIZE));
        assertCopyRange(random, 42);
        assertCopyRange(random, 1);
    }

    private void assertCopyRange(Random random, int bufferSize) throws IOException {

        // generate some random test data
        final byte[] expected = new byte[bufferSize];
        random.nextBytes(expected);

        // Check some simple cases ...
        assertCopyRange(expected, 0, 0);
        assertCopyRange(expected, 0, 1);
        assertCopyRange(expected, 0, expected.length);

        // ... and a few randomly selected ones
        final int n = random.nextInt(100);
        for (int i = 0; i < n; i++) {
            final int a = random.nextInt(expected.length);
            final int b = random.nextInt(expected.length);
            assertCopyRange(expected, Math.min(a, b), Math.max(a, b));
        }
    }

    private void assertCopyRange(byte[] expected, int a, int b) throws IOException {
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
        // with an input stream that does not return everything in read()
        assertCopyRange(expected, new ByteArrayInputStream(expected) {
            @Override
            public synchronized int read(byte[] b, int off, int len) {
                // allow maximum of 10
                if (len > 10) {
                    len = 10;
                }
                return super.read(b, off, len);
            }
        }, a, b);
    }

    private void assertCopyRange(
            byte[] expected, InputStream input, int a, int b) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        StreamRendererServlet.staticCopyRange(input, output, a, b);

        byte[] actual = output.toByteArray();
        assertEquals(b - a, actual.length);
        for (int i = a; i < b; i++) {
            assertEquals(expected[i], actual[i - a]);
        }
    }
}

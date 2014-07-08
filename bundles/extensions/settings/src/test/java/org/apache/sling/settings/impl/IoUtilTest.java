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
package org.apache.sling.settings.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IoUtilTest {

    private static final String ISO_88591 = "ISO-8859-1";

    private static final int NB_BYTES = 1024 * 1024;    // ~ 1 MB

    private static final int MAX_NB_BYTES_PER_READ = 1024 * 128;    // ~ 100 KB

    private byte[] reference = null;

    @Before
    public void before() throws IOException {
        reference = RandomStringUtils.randomAscii(NB_BYTES).getBytes(ISO_88591);
    }

    @After
    public void after() throws IOException {
        if (reference != null ) {
            reference = null;
        }
    }

    @Test
    public void testFillFromLongInputStream()
            throws IOException {
        testFillFromLongInputStream(
                new ByteArrayInputStream(reference));
    }

    @Test
    public void testFillFromLongSlowInputStream()
            throws IOException {
        testFillFromLongInputStream(
                new SlowInputStream(
                        new ByteArrayInputStream(reference), MAX_NB_BYTES_PER_READ));
    }

    @Test
    public void testFillTooShortInputStream()
            throws IOException {
        testFillTooShortInputStream(
                new ByteArrayInputStream(reference));
    }

    @Test
    public void testFillTooShortSlowInputStream()
            throws IOException {
        testFillTooShortInputStream(
                new SlowInputStream(
                        new ByteArrayInputStream(reference), MAX_NB_BYTES_PER_READ));
    }

    @Test
    public void testFillFromTooLongInputStream()
            throws IOException {
        testFillFromTooLongInputStream(
                new ByteArrayInputStream(reference));
    }

    @Test
    public void testFillFromTooLongSlowInputStream()
            throws IOException {
        testFillFromTooLongInputStream(
                new SlowInputStream(
                        new ByteArrayInputStream(reference), MAX_NB_BYTES_PER_READ));
    }

    @Test
    public void testFillFromEmptyInputStream()
            throws IOException {
        testFillFromEmptyInputStream(new ByteArrayInputStream(new byte[0]));
    }

    @Test
    public void testFillFromEmptySlowInputStream()
            throws IOException {
        testFillFromEmptyInputStream(
                new SlowInputStream(
                        new ByteArrayInputStream(new byte[0]), MAX_NB_BYTES_PER_READ));
    }

    private void testFillFromLongInputStream(InputStream is)
            throws IOException {
        byte[] buffer = new byte[reference.length];
        int read = IoUtil.fill(is, buffer);
        Assert.assertEquals(reference.length, read);
        Assert.assertArrayEquals(reference, buffer);
    }

    private void testFillTooShortInputStream(InputStream is)
            throws IOException {
        byte[] buffer = new byte[reference.length + 1];
        int read = IoUtil.fill(is, buffer);
        Assert.assertEquals(reference.length, read);
    }

    private void testFillFromTooLongInputStream(InputStream is)
            throws IOException {
        byte[] buffer = new byte[reference.length - 1];
        int read = IoUtil.fill(is, buffer);
        Assert.assertEquals(buffer.length, read);
    }

    private void testFillFromEmptyInputStream(InputStream is)
            throws IOException {
        Assert.assertEquals(0, IoUtil.fill(is, new byte[0]));
        Assert.assertEquals(0, IoUtil.fill(is, new byte[1]));
    }

    /**
     * Wrap a {@link InputStream} instance in order to emulate a slow input.
     * The slow input is emulated by limiting the maximum number of bytes
     * read for each batch read operation.
     */
    private static class SlowInputStream extends InputStream {

        private final InputStream is;

        private final int maxPerRead;

        SlowInputStream(InputStream is, int maxPerRead) {
            this.is = is;
            this.maxPerRead = maxPerRead;
        }

        @Override
        public int read() throws IOException {
            return is.read();
        }

        public int read(byte b[], int off, int len) throws IOException {
            return is.read(b, off, Math.min(maxPerRead, len));
        }
    }
}

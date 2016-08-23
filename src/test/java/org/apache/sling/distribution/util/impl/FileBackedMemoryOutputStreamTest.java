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
package org.apache.sling.distribution.util.impl;

import static org.apache.commons.io.IOUtils.copy;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.sling.distribution.util.impl.FileBackedMemoryOutputStream.MemoryUnit;
import org.junit.Test;

/**
 * Tests for {@link org.apache.sling.distribution.util.impl.FileBackedMemoryOutputStream}
 */
public class FileBackedMemoryOutputStreamTest {

    @Test
    public void justKeepDataInMemory() throws IOException {
        FileBackedMemoryOutputStream output = new FileBackedMemoryOutputStream(10,
                                                                               MemoryUnit.BYTES,
                                                                               false,
                                                                               new File("/tmp"),
                                                                               "FileBackedMemoryOutputStreamTest.justKeepDataInMemory",
                                                                               ".tmp");
        byte[] data = newDataArray(2);

        output.write(data);
        output.close();

        assertEquals(2, output.size());
        assertNull(output.getFile());

        verifyWrittenData(data, output);
    }

    @Test
    public void backedToFile() throws IOException {
        FileBackedMemoryOutputStream output = new FileBackedMemoryOutputStream(10,
                                                                               MemoryUnit.BYTES,
                                                                               false,
                                                                               new File("/tmp"),
                                                                               "FileBackedMemoryOutputStreamTest.backedToFile",
                                                                               ".tmp");
        byte[] data = newDataArray(100);

        output.write(data);
        output.close();

        assertEquals(100, output.size());
        assertNotNull(output.getFile());
        assertTrue(output.getFile().exists());
        assertEquals(90, output.getFile().length());

        verifyWrittenData(data, output);

        output.clean();
        assertFalse(output.getFile().exists());
    }

    @Test
    public void multiBackedToFileTest() throws IOException {

        List<byte[]> datum = Arrays.asList(newDataArray(0), newDataArray(1), newDataArray(9),
                newDataArray(10), newDataArray(11), newDataArray(100), newDataArray(1000));

        for (byte[] data : datum) {
            FileBackedMemoryOutputStream output = new FileBackedMemoryOutputStream(10,
                    MemoryUnit.BYTES,false, new File("/tmp"),
                    "FileBackedMemoryOutputStreamTest.multiTest-" + data.length, ".tmp");
            output.write(data);
            output.close();
            assertEquals(data.length, output.size());
            verifyWrittenData(data, output);
        }
    }

    private byte[] newDataArray(int size) {
        byte[] data = new byte[size];
        byte b = (byte) (new Random().nextInt() & 0xff);
        Arrays.fill(data, b);
        return data;
    }

    private void verifyWrittenData(byte[] expecteds, FileBackedMemoryOutputStream writtenData) throws IOException {
        InputStream input = writtenData.openWrittenDataInputStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        byte[] actuals = output.toByteArray();
        assertArrayEquals(expecteds, actuals);
    }

}

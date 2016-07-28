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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class FileBackedMemoryOutputStreamTest {

    @Test
    public void justKeepDataInMemory() throws IOException {
        FileBackedMemoryOutputStream output = new FileBackedMemoryOutputStream(10,
                                                                               new File("/tmp"),
                                                                               "FileBackedMemoryOutputStreamTest.justKeepDataInMemory",
                                                                               ".tmp");
        byte[] data = newDataArray(2);

        output.write(data);
        output.close();

        assertEquals(2, output.size());
        assertNull(output.getFile());
    }

    @Test
    public void backedToFile() throws IOException {
        FileBackedMemoryOutputStream output = new FileBackedMemoryOutputStream(2,
                                                                               new File("/tmp"),
                                                                               "FileBackedMemoryOutputStreamTest.backedToFile",
                                                                               ".tmp");
        byte[] data = newDataArray(100);

        output.write(data);
        output.close();

        assertEquals(100, output.size());
        assertNotNull(output.getFile());
        assertTrue(output.getFile().exists());

        output.clean();
        assertFalse(output.getFile().exists());
    }

    private byte[] newDataArray(int size) {
        byte[] data = new byte[size];
        byte b = (byte) (new Random().nextInt() & 0xff);
        Arrays.fill(data, b);
        return data;
    }

}

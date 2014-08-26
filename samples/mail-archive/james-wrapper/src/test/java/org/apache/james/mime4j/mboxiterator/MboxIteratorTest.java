/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.mime4j.mboxiterator;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.*;

/**
 * Tests for {@link MboxIterator}.
 */
public class MboxIteratorTest {

    @Rule
    public final TestName name = new TestName();
    public static final String MBOX_PATH = "src/test/resources/test-1/mbox.rlug";
    private final int DEFAULT_MESSAGE_SIZE = 10 * 1024;
    // number of chars oin our largest test message
    private static final int CHARS_IN_MAX_MSG = 3500;
    private static final int MORE_THAN_FILE_SIZE = 13291;

    /**
     * Test of iterator method, of class MboxIterator.
     */
    @Test
    public void testIterator() throws FileNotFoundException, IOException {
        System.out.println("Executing " + name.getMethodName());
        iterateWithMaxMessage(DEFAULT_MESSAGE_SIZE);
    }

    /**
     * Test of iterator method, of class MboxIterator.
     */
    @Test
    public void testIteratorLoop() throws FileNotFoundException, IOException {
        System.out.println("Executing " + name.getMethodName());
        for (int i = CHARS_IN_MAX_MSG; i < MORE_THAN_FILE_SIZE; i++) {
            System.out.println("Runinng iteration " + (i - CHARS_IN_MAX_MSG) + "  with message size " + i);
            iterateWithMaxMessage(i);
        }
    }

    private void iterateWithMaxMessage(int maxMessageSize) throws IOException {
        int count = 0;
        for (CharBufferWrapper msg : MboxIterator.fromFile(MBOX_PATH).maxMessageSize(maxMessageSize).build()) {
            String message = fileToString(new File(MBOX_PATH + "-" + count));
            //MboxIterator.printCharBuffer(msg);
            Assert.assertEquals("String sizes match for file " + count, message.length(), msg.toString().length());
            Assert.assertEquals("Missmatch with file " + count, message, msg.toString());
            count++;
        }
    }

    private static String fileToString(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            sb.append((char) ch);
        }
        reader.close();
        return sb.toString();
    }

}

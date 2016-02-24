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
package org.apache.sling.bgservlets.impl.nodestream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.sling.commons.testing.jcr.RepositoryTestBase;

public class NodeStreamTest extends RepositoryTestBase {
    
    public static final String ASCII_DATA = "0123456789abcdefgjijklmnoprqstuvwxyz";
    public static final byte [] BINARY_DATA = getBinaryData();
    public static final String NAME_PREFIX = "testNode";
    public static final int BIG_FACTOR = 15;
    public static final byte [] BIG_DATA = bigData(BINARY_DATA, BIG_FACTOR);
    private int counter;
    
    private void assertStream(InputStream expected, InputStream actual) {
        int offset = 0;
        try {
            while(true) {
                final int exp = expected.read();
                if(exp == -1) {
                    assertEquals("Expecting end of actual stream at offset " + offset, -1, actual.read());
                    break;
                } else {
                    final int act = actual.read();
                    assertEquals("Expecting same data at offset " + offset, exp, act);
                }
                offset++;
            }
        } catch(Exception e) {
            fail("Exception at offset " + offset + ": " + e);
        }
    }
    
    public void testAsciiWriteAndRead() throws Exception {
        final Node testNode = getTestRootNode().addNode(NAME_PREFIX + counter++);
        testNode.getSession().save();
        final NodeOutputStream nos = new NodeOutputStream(testNode);
        nos.write(ASCII_DATA.getBytes());
        nos.close();
        final NodeInputStream nis = new NodeInputStream(testNode);
        assertStream(new ByteArrayInputStream(ASCII_DATA.getBytes()), nis);
    }
    
    public void testBinaryWriteAndRead() throws Exception {
        final Node testNode = getTestRootNode().addNode(NAME_PREFIX + counter++);
        testNode.getSession().save();
        final NodeOutputStream nos = new NodeOutputStream(testNode);
        nos.write(BINARY_DATA);
        nos.close();
        final NodeInputStream nis = new NodeInputStream(testNode);
        assertStream(new ByteArrayInputStream(BINARY_DATA), nis);
    }
    
    public void testBigBinaryWriteAndRead() throws Exception {
        final Node testNode = getTestRootNode().addNode(NAME_PREFIX + counter++);
        testNode.getSession().save();
        final NodeOutputStream nos = new NodeOutputStream(testNode);
        nos.write(BIG_DATA);
        nos.close();
        
        assertFalse("Expecting no pending changes in testNode session", testNode.getSession().hasPendingChanges());
        
        final NodeInputStream nis = new NodeInputStream(testNode);
        assertStream(new ByteArrayInputStream(BIG_DATA), nis);
    }
    
    public void testMultipleBinaryWrites() throws Exception {
        final Node testNode = getTestRootNode().addNode(NAME_PREFIX + counter++);
        testNode.getSession().save();
        final NodeOutputStream nos = new NodeOutputStream(testNode);
        for(int i=0; i < BIG_FACTOR; i++) {
            nos.write(BINARY_DATA);
        }
        nos.close();
        
        assertFalse("Expecting no pending changes in testNode session", testNode.getSession().hasPendingChanges());
        
        // Stream must be stored in a hierarchy under testNode, to
        // avoid limitations if flush() is called many times
        final int childCount = getChildCount(testNode);
        final int expect = 10;
        assertTrue("Expecting > " + expect + " child nodes under testNode, got " + childCount, childCount > expect);

        final NodeInputStream nis = new NodeInputStream(testNode);
        assertStream(new ByteArrayInputStream(BIG_DATA), nis);
    }
    
    private int getChildCount(Node n) throws RepositoryException {
        int result = 0;
        final NodeIterator it = n.getNodes();
        while(it.hasNext()) {
            result++;
            final Node kid = it.nextNode();
            result += getChildCount(kid);
        }
        return result;
    }
    
    public void testWriteWithOffset() throws Exception {
        final Node testNode = getTestRootNode().addNode(NAME_PREFIX + counter++);
        testNode.getSession().save();

        final NodeOutputStream nos = new NodeOutputStream(testNode);
        int offset = 0;
        int step = 1271;
        while(offset < BIG_DATA.length && step > 0) {
            step = Math.min(step, BIG_DATA.length - offset);
            nos.write(BIG_DATA, offset, step);
            offset += step;
        }
        nos.close();
        
        final NodeInputStream nis = new NodeInputStream(testNode);
        assertStream(new ByteArrayInputStream(BIG_DATA), nis);
    }
    
    public void testChunkedRead() throws Exception {
        final Node testNode = getTestRootNode().addNode(NAME_PREFIX + counter++);
        testNode.getSession().save();
        
        final NodeOutputStream nos = new NodeOutputStream(testNode);
        try {
            nos.write(BIG_DATA);
        } finally {
            nos.close();
        }
     
        final ByteArrayOutputStream actual = new ByteArrayOutputStream(BIG_DATA.length);
        final byte [] buffer = new byte[7432];
        final NodeInputStream nis = new NodeInputStream(testNode);
        try {
            int count = 0;
            while((count = nis.read(buffer, 0, buffer.length)) > 0) {
                actual.write(buffer, 0, count);
            }
        } finally {
            nis.close();
        }
        
        assertStream(new ByteArrayInputStream(BIG_DATA), new ByteArrayInputStream(actual.toByteArray()));
    }
    
    private static byte[] bigData(byte [] data, int multiplier) {
        final byte [] result = new byte[data.length * multiplier];
        int destPos = 0;
        for(int i=0; i < multiplier; i++) {
            System.arraycopy(data, 0, result, destPos, data.length);
            destPos += data.length;
        }
        return result;
    }
    
    private static byte [] getBinaryData() {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final Random random = new Random();
        for(int i=0;i  < 66000; i++) {
            os.write(random.nextInt());
        }
        return os.toByteArray();
    }
}

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
package org.apache.sling.commons.log.slf4j;

import java.io.File;
import java.io.IOException;

import org.apache.sling.commons.log.slf4j.SlingLogFileWriter;

import junit.framework.TestCase;

public class SlingLogFileWriterTest extends TestCase {

    private String base;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        File baseFile = new File("target/" + System.currentTimeMillis() + "/" + getClass().getSimpleName());
        baseFile.getParentFile().mkdirs();
        base = baseFile.getAbsolutePath();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testNoRotateSize() throws IOException {
        
        SlingLogFileWriter slfw = new SlingLogFileWriter(base, -1, 10);
        
        // only base file should exist with size 0 (for now)
        File test = new File(base);
        assertTrue(test.exists() && test.length() == 0);
        
        File test0 = new File(base + ".0");
        assertFalse(test0.exists());
        File testn1 = new File(base + ".-1");
        assertFalse(testn1.exists());
        
        // write some bytes and ensure size
        slfw.write("012345");
        slfw.writeln();
        assertTrue(test.exists() && test.length() > 0);
        assertFalse(test0.exists());
        assertFalse(testn1.exists());
        
        // write some more, ensuring rotation does happen
        slfw.write("012345");
        slfw.writeln();
        assertTrue(test.exists() && test.length() == 0);
        assertFalse(test0.exists());
        assertFalse(testn1.exists());
    }

    public void testRotate0Size() throws IOException {
        
        SlingLogFileWriter slfw = new SlingLogFileWriter(base, 0, 10);
        
        // only base file should exist with size 0 (for now)
        File test = new File(base);
        assertTrue(test.exists() && test.length() == 0);
        
        File test0 = new File(base + ".0");
        assertFalse(test0.exists());
        File test1 = new File(base + ".1");
        assertFalse(test1.exists());
        File testn1 = new File(base + ".-1");
        assertFalse(testn1.exists());
        
        // write some bytes and ensure size
        slfw.write("012345");
        slfw.writeln();
        assertTrue(test.exists() && test.length() > 0);
        assertFalse(test0.exists());
        assertFalse(testn1.exists());
        
        // write some more, ensuring rotation does happen
        slfw.write("012345");
        slfw.writeln();
        assertTrue(test.exists() && test.length() == 0);
        assertTrue(test0.exists());
        assertFalse(test1.exists());
        assertFalse(testn1.exists());
    }
    
    public void testRotate1Size() throws IOException {

        SlingLogFileWriter slfw = new SlingLogFileWriter(base, 1, 10);
        
        // only base file should exist with size 0 (for now)
        File test = new File(base);
        assertTrue(test.exists() && test.length() == 0);
        
        File test0 = new File(base + ".0");
        assertFalse(test0.exists());
        File test1 = new File(base + ".1");
        assertFalse(test1.exists());
        File testn1 = new File(base + ".-1");
        assertFalse(testn1.exists());
        
        // write some bytes and ensure size
        slfw.write("012345");
        slfw.writeln();
        assertTrue(test.exists() && test.length() > 0);
        assertFalse(test0.exists());
        assertFalse(testn1.exists());
        
        // write some more, ensuring rotation does happen
        slfw.write("012345");
        slfw.writeln();
        assertTrue(test.exists() && test.length() == 0);
        assertTrue(test0.exists());
        assertFalse(test1.exists());
        assertFalse(testn1.exists());
        
        // write bytes to rotate in onw fell swoop
        slfw.write("0123456789 - more");
        slfw.writeln();
        assertTrue(test.exists() && test.length() == 0);
        assertTrue(test0.exists());
        assertTrue(test1.exists());
        assertFalse(testn1.exists());
    }
    
    public void testMaxSizeConversion() {
        assertEquals(1, SlingLogFileWriter.convertMaxSizeSpec("1"));
        
        // kilo
        assertEquals(1*1024, SlingLogFileWriter.convertMaxSizeSpec("1K"));
        assertEquals(1*1024, SlingLogFileWriter.convertMaxSizeSpec("1k"));
        assertEquals(1*1024, SlingLogFileWriter.convertMaxSizeSpec("1KB"));
        assertEquals(1*1024, SlingLogFileWriter.convertMaxSizeSpec("1kb"));
        
        // mega
        assertEquals(1*1024*1024, SlingLogFileWriter.convertMaxSizeSpec("1M"));
        assertEquals(1*1024*1024, SlingLogFileWriter.convertMaxSizeSpec("1m"));
        assertEquals(1*1024*1024, SlingLogFileWriter.convertMaxSizeSpec("1MB"));
        assertEquals(1*1024*1024, SlingLogFileWriter.convertMaxSizeSpec("1mb"));
        
        // giga
        assertEquals(1*1024*1024*1024, SlingLogFileWriter.convertMaxSizeSpec("1G"));
        assertEquals(1*1024*1024*1024, SlingLogFileWriter.convertMaxSizeSpec("1g"));
        assertEquals(1*1024*1024*1024, SlingLogFileWriter.convertMaxSizeSpec("1GB"));
        assertEquals(1*1024*1024*1024, SlingLogFileWriter.convertMaxSizeSpec("1gb"));
    }
}

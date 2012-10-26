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
package org.apache.sling.commons.log.internal.slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

public class SlingLogWriterTest extends AbstractSlingLogTest {

    private long july21;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        july21 = DateFormat.getDateInstance(DateFormat.LONG,
            Locale.US).parse("July 21, 2009").getTime();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test_size_no_rotation() throws IOException {
        final String base = getBase();
        SlingLoggerWriter slfw = createLogWriter(base, -1, 10);

        // only base file should exist with size 0 (for now)
        File test = new File(base);
        assertTrue(test.exists());
        assertEquals(0, test.length());

        File test0 = new File(base + ".0");
        assertFalse(test0.exists());
        File testn1 = new File(base + ".-1");
        assertFalse(testn1.exists());

        // write some bytes and ensure size
        slfw.write("012345");
        slfw.writeln();
        slfw.checkRotate();
        assertTrue(test.exists());
        assertTrue(test.length() > 0);
        assertFalse(test0.exists());
        assertFalse(testn1.exists());

        // write some more, ensuring rotation does happen
        slfw.write("012345");
        slfw.writeln();
        slfw.checkRotate();
        assertTrue(test.exists());
        assertEquals(0, test.length());
        assertFalse(test0.exists());
        assertFalse(testn1.exists());
    }

    public void test_size_rotation_1() throws IOException {
        final String base = getBase();
        SlingLoggerWriter slfw = createLogWriter(base, 1, 10);

        // only base file should exist with size 0 (for now)
        File test = new File(base);
        assertTrue(test.exists());
        assertEquals(0, test.length());

        File test0 = new File(base + ".0");
        assertFalse(test0.exists());
        File test1 = new File(base + ".1");
        assertFalse(test1.exists());
        File testn1 = new File(base + ".-1");
        assertFalse(testn1.exists());

        // write some bytes and ensure size
        slfw.write("012345");
        slfw.writeln();
        slfw.checkRotate();
        assertTrue(test.exists());
        assertTrue(test.length() > 0);
        assertFalse(test0.exists());
        assertFalse(testn1.exists());

        // write some more, ensuring rotation does happen
        slfw.write("012345");
        slfw.writeln();
        slfw.checkRotate();
        assertTrue(test.exists());
        assertEquals(0, test.length());
        assertTrue(test0.exists());
        assertFalse(test1.exists());
        assertFalse(testn1.exists());
    }

    public void test_size_rotation_2() throws IOException {
        final String base = getBase();
        SlingLoggerWriter slfw = createLogWriter(base, 2, 10);

        // only base file should exist with size 0 (for now)
        File test = new File(base);
        assertTrue(test.exists());
        assertTrue(test.length() == 0);

        File test0 = new File(base + ".0");
        assertFalse(test0.exists());
        File test1 = new File(base + ".1");
        assertFalse(test1.exists());
        File testn1 = new File(base + ".-1");
        assertFalse(testn1.exists());

        // write some bytes and ensure size
        slfw.write("012345");
        slfw.writeln();
        slfw.checkRotate();
        assertTrue(test.exists());
        assertTrue(test.length() > 0);
        assertFalse(test0.exists());
        assertFalse(testn1.exists());

        // write some more, ensuring rotation does happen
        slfw.write("012345");
        slfw.writeln();
        slfw.checkRotate();
        assertTrue(test.exists());
        assertEquals(0, test.length());
        assertTrue(test0.exists());
        assertFalse(test1.exists());
        assertFalse(testn1.exists());

        // write bytes to rotate in one fell swoop
        slfw.write("0123456789 - more");
        slfw.writeln();
        slfw.checkRotate();
        assertTrue(test.exists());
        assertEquals(0, test.length());
        assertTrue(test0.exists());
        assertTrue(test1.exists());
        assertFalse(testn1.exists());
    }

    public void test_daily_rotation() throws IOException {
        final String base = getBase();
        final String limit = "'.'yyyy-MM-dd";
        SlingLoggerWriter slfw = createLogWriter(base, -1, limit);
        setNow(slfw, july21);

        // only base file should exist with size 0 (for now)
        final File test = new File(base);
        assertTrue(test.exists());
        assertEquals(0, test.length());

        final File test0 = new File(base + ".2009-07-21");
        assertFalse(test0.exists());
        final File testn1 = new File(base + ".2009-07-23");
        assertFalse(testn1.exists());

        // write some bytes and ensure size
        slfw.write("012345");
        slfw.writeln();

        slfw.checkRotate();
        assertTrue(test.exists());
        assertTrue(test.length() > 0);
        assertFalse(test0.exists());
        assertFalse(testn1.exists());

        // simulate July 23rd
        setNow(slfw, july21 + 24*60*60*1000L);
        forceRotate(slfw);
        // setLastModified fails under Windows if file is still open in logger
        slfw.close();
        test.setLastModified(july21);
        slfw = createLogWriter(base, -1, limit);

        // rotate the file now
        slfw.checkRotate();
        assertTrue(test.exists());
        assertEquals(0, test.length());
        assertTrue(test0.exists());
        assertFalse(testn1.exists());
    }

    public void test_something() throws ParseException {
    }

    public void test_createFileRotator() {
        assertSize(1, "1");

        // kilo
        assertSize(1 * 1024, "1K");
        assertSize(1 * 1024, "1k");
        assertSize(1 * 1024, "1KB");
        assertSize(1 * 1024, "1kb");

        // mega
        assertSize(1 * 1024 * 1024, "1M");
        assertSize(1 * 1024 * 1024, "1m");
        assertSize(1 * 1024 * 1024, "1MB");
        assertSize(1 * 1024 * 1024, "1mb");

        // giga
        assertSize(1 * 1024 * 1024 * 1024, "1G");
        assertSize(1 * 1024 * 1024 * 1024, "1g");
        assertSize(1 * 1024 * 1024 * 1024, "1GB");
        assertSize(1 * 1024 * 1024 * 1024, "1gb");

        // some time stuff testing
        assertTime("'.'yyyy-MM-dd", null);
        assertTime("'.'yyyy-MM-dd", "");
        assertTime("'.'yyyy-MM-dd-mm", "'.'yyyy-MM-dd-mm");
    }
    
    public void test_create_denied_parent() throws IOException {
        File baseFile = getBaseFile();
        File protectedParent = new File(baseFile,"protected");
        protectedParent.mkdirs();
        File loggingParent = new File(protectedParent,"logging");
        try {
            // these methods are JDK 1.6 and later so we have introspect to invoke
            File.class.getMethod("setWritable", boolean.class).invoke(protectedParent, false);
            File.class.getMethod("setExecutable", boolean.class).invoke(protectedParent, false);
        } catch ( Exception e ) {
            e.printStackTrace();
            if ( System.getProperty("java.version").startsWith("1.5") ) {
                return; // cant perform this test on JDKs before 1.5
            }
        }
        try{
            assertFalse(protectedParent.canWrite());
            SlingLoggerWriter writer = createLogWriter(loggingParent.getAbsolutePath(), -1, 10);
            assertNotNull(writer);
            assertNull(writer.getFile());
            assertNull(writer.getPath());
            writer.append("Testing Stdout");
        } finally {
            try {
                // these methods are JDK 1.6 and later so we have introspect to invoke
                File.class.getMethod("setWritable", boolean.class).invoke(loggingParent, true);
                File.class.getMethod("setExecutable", boolean.class).invoke(protectedParent, true);
            } catch ( Exception e ) {
                // no need.
            }
        }
    }

    public void test_create_denied() throws IOException {
        File baseFile = getBaseFile();
        File protectedParent = new File(baseFile,"protected");
        File loggingParent = new File(protectedParent,"logging");
        loggingParent.mkdirs();
        try {
            // these methods are JDK 1.6 and later so we have introspect to invoke
            File.class.getMethod("setWritable", boolean.class).invoke(loggingParent, false);
            File.class.getMethod("setExecutable", boolean.class).invoke(protectedParent, false);
        } catch ( Exception e ) {
            e.printStackTrace();
            if ( System.getProperty("java.version").startsWith("1.5") ) {
                return; // cant perform this test on JDKs before 1.5
            }
        }
        try {
            assertFalse(loggingParent.canWrite());
            SlingLoggerWriter writer = createLogWriter(loggingParent.getAbsolutePath(), -1, 10);
            assertNotNull(writer);
            assertNull(writer.getFile());
            assertNull(writer.getPath());
            writer.append("Testing Stdout");
        } finally {
            try {
                // these methods are JDK 1.6 and later so we have introspect to invoke
                File.class.getMethod("setWritable", boolean.class).invoke(loggingParent, true);
                File.class.getMethod("setExecutable", boolean.class).invoke(protectedParent, true);
            } catch ( Exception e ) {
                // no need.
            }
        }
    }

    private SlingLoggerWriter createLogWriter(String file, int numFiles,
            long size) throws IOException {
        return createLogWriter(file, numFiles, String.valueOf(size));
    }

    private SlingLoggerWriter createLogWriter(String file, int numFiles,
            String limit) throws IOException {
        SlingLoggerWriter slw = new SlingLoggerWriter(getClass().getName());
        slw.configure(file, numFiles, limit);
        return slw;
    }

    private void assertSize(final long expected, final String config) {
        final FileRotator checker = SlingLoggerWriter.createFileRotator(-1,
            config);
        assertTrue("Size checker expeced",
            checker instanceof SizeLimitedFileRotator);
        assertEquals(expected, ((SizeLimitedFileRotator) checker).getMaxSize());
    }

    private void assertTime(final String expected, final String config) {
        final FileRotator checker = SlingLoggerWriter.createFileRotator(-1,
            config);
        assertTrue("Size checker expeced",
            checker instanceof ScheduledFileRotator);
        assertEquals(expected,
            ((ScheduledFileRotator) checker).getDatePattern());
    }

    private void setNow(SlingLoggerWriter writer, long now) {
        try {
            Field f = writer.getClass().getDeclaredField("fileRotator");
            f.setAccessible(true);
            ScheduledFileRotator sfr = (ScheduledFileRotator) f.get(writer);

            // set the "now" time to the indicated time
            f = sfr.getClass().getDeclaredField("now");
            f.setAccessible(true);
            ((Date) f.get(sfr)).setTime(now);

            // clear scheduled filename to force its reasteblishment
            f = sfr.getClass().getDeclaredField("scheduledFilename");
            f.setAccessible(true);
            f.set(sfr, null);
        } catch (Throwable t) {
            fail("Cannot set now on ScheduledFileRotator: " + t);
        }
    }

    private void forceRotate(SlingLoggerWriter writer) {
        try {
            Field f = writer.getClass().getDeclaredField("fileRotator");
            f.setAccessible(true);
            ScheduledFileRotator sfr = (ScheduledFileRotator) f.get(writer);
            f = sfr.getClass().getDeclaredField("nextCheck");
            f.setAccessible(true);
            f.setLong(sfr, -1);
        } catch (Throwable t) {
            fail("Cannot set now on ScheduledFileRotator: " + t);
        }
    }
}

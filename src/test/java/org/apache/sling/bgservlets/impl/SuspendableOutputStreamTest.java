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
package org.apache.sling.bgservlets.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.sling.bgservlets.JobStatus;
import org.junit.Test;

public class SuspendableOutputStreamTest {
    public final static String TEST = "0123456789abcdefghijklmnopqrstuvwxyz";

    static class WriterThread extends Thread {
        private final OutputStream os;
        private final byte[] toWrite = "TEST".getBytes();
        private Exception runException;
        final static int WRITE_DELAY = 50;

        WriterThread(OutputStream os) {
            this.os = os;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    os.write(toWrite);
                    Thread.sleep(WRITE_DELAY);
                }
            } catch (Exception e) {
                runException = e;
            }
        }
    }

    @Test
    public void testNoSuspend() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final SuspendableOutputStream f = new SuspendableOutputStream(bos);
        f.write(TEST.getBytes());
        f.flush();
        assertEquals("String should be fully written", TEST, bos.toString());
    }

    @Test
    public void testStop() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final SuspendableOutputStream f = new SuspendableOutputStream(bos);
        assertEquals("Expecting NEW state first", JobStatus.State.NEW, f
                .getState());
        f.requestStateChange(JobStatus.State.RUNNING);
        f.write(TEST.getBytes());
        f.flush();

        f.requestStateChange(JobStatus.State.STOPPED);
        assertEquals("Expecting STOP_REQUESTED state before write",
                JobStatus.State.STOP_REQUESTED, f.getState());
        try {
            f.write("nothing".getBytes());
            fail("Expected StreamStoppedException when writing to STOPPED stream");
        } catch (SuspendableOutputStream.StreamStoppedException asExpected) {
        }

        assertEquals("Expecting STOPPED state after write",
                JobStatus.State.STOPPED, f.getState());
    }

    @Test
    public void testSuspend() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final SuspendableOutputStream f = new SuspendableOutputStream(bos);
        f.requestStateChange(JobStatus.State.RUNNING);
        final WriterThread t = new WriterThread(f);
        t.setDaemon(true);
        t.start();

        final long delay = WriterThread.WRITE_DELAY * 3;
        Thread.sleep(delay);
        assertTrue("Expecting data to be written by WriterThread",
                bos.size() > 0);

        f.requestStateChange(JobStatus.State.SUSPENDED);
        Thread.sleep(delay);
        assertEquals("Expecting SUSPEND state after a few writes",
                JobStatus.State.SUSPENDED, f.getState());

        final int count = bos.size();
        Thread.sleep(delay);
        assertEquals("Expecting no writes in SUSPEND state", count, bos.size());

        f.requestStateChange(JobStatus.State.RUNNING);
        Thread.sleep(delay);
        assertEquals("Expecting RUNNING state", JobStatus.State.RUNNING, f
                .getState());
        assertTrue("Expecting data to be written after resuming",
                bos.size() > count);

        f.close();
        Thread.sleep(delay);
        assertFalse("Expecting WriterThread to end after closing stream", t
                .isAlive());
        assertNotNull("Expecting non-null Exception in WriterThread",
                t.runException);
        assertTrue("Expecting IOException in WriterThread",
                t.runException instanceof IOException);
    }

    @Test
    public void testSuspendThenStop() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final SuspendableOutputStream f = new SuspendableOutputStream(bos);
        assertEquals("Expecting NEW state first", JobStatus.State.NEW, f
                .getState());
        final WriterThread t = new WriterThread(f);
        t.setDaemon(true);
        t.start();

        f.requestStateChange(JobStatus.State.SUSPENDED);

        final long delay = WriterThread.WRITE_DELAY * 3;
        Thread.sleep(delay);
        assertEquals("Expecting SUSPEND state after a few writes",
                JobStatus.State.SUSPENDED, f.getState());

        f.requestStateChange(JobStatus.State.STOPPED);
        assertEquals("Expecting STOP_REQUESTED state before write",
                JobStatus.State.STOP_REQUESTED, f.getState());
        try {
            f.write("nothing".getBytes());
            fail("Expected StreamStoppedException when writing to STOPPED stream");
        } catch (SuspendableOutputStream.StreamStoppedException asExpected) {
        }

        assertEquals("Expecting STOPPED state after write",
                JobStatus.State.STOPPED, f.getState());
        f.close();
    }

    @Test
    public void testDone() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final SuspendableOutputStream f = new SuspendableOutputStream(bos);
        f.requestStateChange(JobStatus.State.DONE);
        assertEquals("Expecting DONE state (1)", JobStatus.State.DONE, f
                .getState());
        f.requestStateChange(JobStatus.State.SUSPENDED);
        assertEquals("Expecting DONE state (2)", JobStatus.State.DONE, f
                .getState());
        f.requestStateChange(JobStatus.State.STOPPED);
        assertEquals("Expecting DONE state (3)", JobStatus.State.DONE, f
                .getState());
    }
    
    @Test
    public void testCreationToRunningState() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final SuspendableOutputStream f = new SuspendableOutputStream(bos);
        assertEquals("Expecting NEW state initially", JobStatus.State.NEW, f.getState());
        f.requestStateChange(JobStatus.State.QUEUED);
        assertEquals("Expecting QUEUED state as requests", JobStatus.State.QUEUED, f.getState());
        f.requestStateChange(JobStatus.State.RUNNING);
        assertEquals("Expecting RUNNING state as requested", JobStatus.State.RUNNING, f.getState());
    }
}

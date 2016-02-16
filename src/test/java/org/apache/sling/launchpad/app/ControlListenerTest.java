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
package org.apache.sling.launchpad.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.sling.launchpad.base.shared.SharedConstants;

import junit.framework.TestCase;

public class ControlListenerTest extends TestCase {

    private static String SLING1 = "target/sling.test/1";

    private static String SLING2 = "target/sling.test/2";

    private static String CTL = "conf/controlport";

    private final File ctlFile1 = new File(SLING1, CTL);

    private final File ctlFile2 = new File(SLING2, CTL);

    @Override
    protected void tearDown() throws Exception {
        ctlFile1.delete();
        ctlFile2.delete();

        super.tearDown();
    }

    public void test_start_status_stop() {
        int port = getPort();
        MyMain main = new MyMain(SLING1);

        ControlListener cl = new ControlListener(main, String.valueOf(port));
        TestCase.assertTrue(cl.listen());
        delay(); // wait for sever to start

        TestCase.assertTrue(ctlFile1.canRead());

        TestCase.assertEquals(0, new ControlListener(main, null).statusServer());

        TestCase.assertEquals(0, new ControlListener(main, null).shutdownServer());

        TestCase.assertTrue(main.stopCalled);

        delay();
        TestCase.assertFalse(ctlFile1.exists());
    }

    public void test_start_status_long_stop() {
        int port = getPort();
        MyMain2 main = new MyMain2(SLING1);

        ControlListener cl = new ControlListener(main, String.valueOf(port));
        TestCase.assertTrue(cl.listen());
        delay(); // wait for sever to start

        TestCase.assertTrue(ctlFile1.canRead());

        TestCase.assertEquals(0, new ControlListener(main, null).statusServer());

        TestCase.assertEquals(0, new ControlListener(main, null).shutdownServer());

        TestCase.assertTrue(main.stopCalled);

        TestCase.assertEquals(0, new ControlListener(main, null).statusServer());

        Thread t1 = new MonitorDeadlockingThread(false);
        Thread t2 = new MonitorDeadlockingThread(true);
        t1.start();
        t2.start();

        Thread t3 = new SynchronizerDeadlockingThread(false);
        Thread t4 = new SynchronizerDeadlockingThread(true);
        t3.start();
        t4.start();

        try {
            Thread.sleep(200);
        } catch (InterruptedException ie) {
        }

        TestCase.assertEquals(0, new ControlListener(main, null).dumpThreads());

        TestCase.assertEquals(0, new ControlListener(main, null).shutdownServer());

        // make sure it will shutdown now
        t1.interrupt();
        t2.interrupt();
        t3.interrupt();
        t4.interrupt();
        main.trigger();

        delay();
        TestCase.assertFalse(ctlFile1.exists());
    }

    static class MonitorDeadlockingThread extends Thread {

        private static final Object l1 = new Object();
        private static final Object l2 = new Object();
        private final boolean flag;

        MonitorDeadlockingThread(boolean flag) {
            this.flag = flag;
            this.setDaemon(true);
        }

        @Override
        public void run() {
            if (this.flag) {
                synchronized (l1) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                    }
                    synchronized(l2) {
                        System.out.println(Thread.currentThread().getName() + ": locked l2");
                    }
                }
            } else {
                synchronized(l2) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                    }
                    synchronized (l1) {
                        System.out.println(Thread.currentThread().getName() + ": locked l1");
                    }
                }
            }
        }
    }

    static class SynchronizerDeadlockingThread extends Thread {

        private static final ReentrantLock l1 = new ReentrantLock();
        private static final ReentrantLock l2 = new ReentrantLock();
        private final boolean flag;

        SynchronizerDeadlockingThread(boolean flag) {
            this.flag = flag;
            this.setDaemon(true);
        }

        @Override
        public void run() {
            if (this.flag) {
                try {
                    l1.lock();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                    }
                    l2.lock();
                } finally {
                    l2.unlock();
                    l1.unlock();
                }
            } else {
                try {
                    l2.lock();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                    }
                    l1.lock();
                } finally {
                    l1.unlock();
                    l2.unlock();
                }
            }
        }
    }

    public void test_parallel_start_status_stop() {
        int port1 = getPort();
        MyMain main1 = new MyMain(SLING1);
        ControlListener server1 = new ControlListener(main1, String.valueOf(port1));
        TestCase.assertTrue(server1.listen());
        delay(); // wait for sever to start

        int port2 = getPort();
        MyMain main2 = new MyMain(SLING2);
        ControlListener server2 = new ControlListener(main2, String.valueOf(port2));
        TestCase.assertTrue(server2.listen());
        delay(); // wait for sever to start

        TestCase.assertTrue(ctlFile1.canRead());
        TestCase.assertTrue(ctlFile2.canRead());

        TestCase.assertEquals(0, new ControlListener(main1, null).statusServer());
        TestCase.assertEquals(0, new ControlListener(main1, null).shutdownServer());
        delay(); // wait for sever to start

        TestCase.assertEquals(0, new ControlListener(main2, null).statusServer());
        TestCase.assertEquals(0, new ControlListener(main2, null).shutdownServer());
        delay(); // wait for sever to start

        TestCase.assertTrue(main1.stopCalled);
        TestCase.assertTrue(main2.stopCalled);

        delay();
        TestCase.assertFalse(ctlFile1.exists());
        TestCase.assertFalse(ctlFile2.exists());
    }

    public void test_no_start_in_same_sling_home() {
        int port1 = getPort();
        MyMain main1 = new MyMain(SLING1);
        ControlListener server1 = new ControlListener(main1, String.valueOf(port1));
        TestCase.assertTrue(server1.listen());
        delay(); // wait for sever to start

        int port2 = getPort();
        MyMain main2 = new MyMain(SLING1);
        ControlListener server2 = new ControlListener(main2, String.valueOf(port2));
        TestCase.assertFalse(server2.listen());
        delay(); // wait for sever to start

        TestCase.assertTrue(ctlFile1.canRead());
        TestCase.assertFalse(ctlFile2.canRead());

        TestCase.assertEquals(0, new ControlListener(main1, null).statusServer());
        TestCase.assertEquals(0, new ControlListener(main1, null).shutdownServer());

        delay();

        TestCase.assertTrue(main1.stopCalled);

        delay();
        TestCase.assertFalse(ctlFile1.exists());

        // retry cl2
        TestCase.assertTrue(server2.listen());
        delay(); // wait for sever to start

        TestCase.assertEquals(0, new ControlListener(main2, null).statusServer());
        TestCase.assertEquals(0, new ControlListener(main2, null).shutdownServer());

        delay();

        TestCase.assertTrue(main2.stopCalled);

        delay();
        TestCase.assertFalse(ctlFile2.exists());
    }

    public void test_no_status() throws IOException {
        int port = getPort();
        MyMain main = new MyMain(SLING1);

        TestCase.assertFalse(ctlFile1.exists());
        ctlFile1.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(ctlFile1);
        PrintWriter pw = new PrintWriter(out);
        pw.println("127.0.0.1:" + port);
        pw.println("password");
        pw.close();

        TestCase.assertTrue(new File(SLING1, CTL).canRead());

        TestCase.assertEquals(3, new ControlListener(main, null).statusServer());

        TestCase.assertEquals(3, new ControlListener(main, null).shutdownServer());

        delay();

        TestCase.assertFalse(main.stopCalled);

        TestCase.assertTrue(ctlFile1.exists());
    }

    public void test_no_configuration_for_status() {
        int port = getPort();
        MyMain main = new MyMain(SLING1);

        TestCase.assertFalse(ctlFile1.exists());

        TestCase.assertEquals(4, new ControlListener(main, String.valueOf(port)).statusServer());

        TestCase.assertEquals(4, new ControlListener(main, String.valueOf(port)).shutdownServer());

        delay();

        TestCase.assertFalse(main.stopCalled);

        TestCase.assertFalse(ctlFile1.exists());
    }

    public void test_no_key_for_status() throws IOException {
        int port = getPort();
        MyMain main = new MyMain(SLING1);

        TestCase.assertFalse(ctlFile1.exists());
        ctlFile1.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(ctlFile1);
        PrintWriter pw = new PrintWriter(out);
        pw.println("127.0.0.1:" + port);
        pw.close();

        TestCase.assertTrue(new File(SLING1, CTL).canRead());

        TestCase.assertEquals(4, new ControlListener(main, null).statusServer());

        TestCase.assertEquals(4, new ControlListener(main, null).shutdownServer());

        delay();

        TestCase.assertFalse(main.stopCalled);

        TestCase.assertTrue(ctlFile1.exists());
    }

    private int getPort() {
        ServerSocket s = null;
        try {
            s = new ServerSocket(0);
            return s.getLocalPort();
        } catch (IOException ignore) {
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException i2) {
                }
            }
        }

        TestCase.fail("Cannot acquire port");
        return 0; // compiler satisfaction
    }

    private void delay() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException ie) {
        }
    }

    private static class MyMain extends Main {

        boolean stopCalled;

        MyMain(final String slingHome) {
            super(new HashMap<String, String>() {
                {
                    put(SharedConstants.SLING_HOME, slingHome);
                }
            });
        }

        @Override
        protected void doStop() {
            this.stopCalled = true;
        }

        @Override
        void terminateVM(int status) {
            // not really
        }
    }

    private static class MyMain2 extends MyMain {

        private final Object trigger = new Object();

        public MyMain2(final String slingHome) {
            super(slingHome);
        }

        @Override
        protected void doStop() {
            super.doStop();

            // wait for trigger to continue simlutating a blocked
            // shutdown -- at most 10 seconds, though
            synchronized (this.trigger) {
                try {
                    this.trigger.wait(10 * 1000L);
                } catch (InterruptedException e) {
                    // ignore and just continue
                }
            }
        }

        void trigger() {
            synchronized (this.trigger) {
                this.trigger.notifyAll();
            }
        }
    }
}

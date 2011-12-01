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
package org.apache.sling.launchpad.base.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.felix.framework.Felix;
import org.apache.sling.launchpad.base.shared.Loader;
import org.apache.sling.launchpad.base.shared.Notifiable;
import org.osgi.framework.BundleException;


public class SlingFelix extends Felix {

    private final Notifiable notifiable;

    private Thread notifierThread;

    public SlingFelix(Notifiable notifiable, Map<?, ?> props) throws Exception {
        super(props);
        this.notifiable = notifiable;
    }

    @Override
    public void update() throws BundleException {
        update(null);
    }

    @Override
    public void update(InputStream is) throws BundleException {
        // get the update file and make sure, the stream is closed
        try {
            startNotifier(true, is);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                }
            }
        }

        // just stop the framework now
        super.stop();
    }

    @Override
    public void stop() throws BundleException {
        startNotifier(false, null);
        super.stop();
    }

    public void stop(int status) throws BundleException {
        startNotifier(false, null);
        super.stop(status);
    }

    private synchronized void startNotifier(boolean restart, InputStream ins) {
        if (notifierThread == null) {
            notifierThread = new Thread(new Notifier(restart, ins),
                "Sling Notifier");
            notifierThread.setDaemon(false);
            notifierThread.start();
        }
    }

    private class Notifier implements Runnable {

        private final boolean restart;

        private final File updateFile;

        private Notifier(boolean restart, InputStream ins) {
            this.restart = restart;

            if (ins != null) {
                File tmpFile;
                try {
                    tmpFile = File.createTempFile("slingupdate", ".jar");
                    Loader.spool(ins, tmpFile);
                } catch (IOException ioe) {
                    // TOOD: log
                    tmpFile = null;
                }
                this.updateFile = tmpFile;
            } else {
                this.updateFile = null;
            }
        }

        public void run() {

            try {
                SlingFelix.this.waitForStop(0);
            } catch (InterruptedException ie) {
                // TODO: log
            }

            if (restart) {
                SlingFelix.this.notifiable.updated(updateFile);
            } else {
                SlingFelix.this.notifiable.stopped();
            }
        }
    }
}

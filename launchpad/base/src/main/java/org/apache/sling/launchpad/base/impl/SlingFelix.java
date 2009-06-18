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

    private Notifier notifierThread;

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
        // get the update file
        startNotifier(true, is);

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
            notifierThread = new Notifier(restart, ins);
            notifierThread.setDaemon(false);
            notifierThread.start();
        }
    }

    private class Notifier extends Thread {

        private final boolean restart;

        private final File updateFile;

        private Notifier(boolean restart, InputStream ins) {
            super("Sling Notifier");
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
                updateFile = tmpFile;
            } else {
                updateFile = null;
            }
        }

        @Override
        public void run() {

            try {
                waitForStop(0);
            } catch (InterruptedException ie) {
                // TODO: log
            }

            if (restart) {
                notifiable.updated(updateFile);
            } else {
                notifiable.stopped();
            }
        }
    }
}

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
package org.apache.sling.fsprovider.internal;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.sling.api.SlingConstants;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a monitor for the file system
 * that periodically checks for changes.
 */
public class FileMonitor extends TimerTask {

    /** The logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Timer timer = new Timer();
    private boolean stop = false;
    private boolean stopped = true;

    private final Monitorable root;

    private final FsResourceProvider provider;

    /**
     * Creates a new instance of this class.
     * @param provider The resource provider.
     * @param interval The interval between executions of the task, in milliseconds.
     */
    public FileMonitor(final FsResourceProvider provider, final long interval) {
        this.provider = provider;
        this.root = new Monitorable(this.provider.getProviderRoot(), this.provider.getRootFile());
        createStatus(this.root);
        logger.debug("Starting file monitor for {} with an interval of {}ms", this.root.file, interval);
        timer.schedule(this, 0, interval);
    }

    /**
     * Stop periodically executing this task. If the task is currently executing it
     * will never be run again after the current execution, otherwise it will simply
     * never run (again).
     */
    void stop() {
        synchronized (timer) {
            if (!stop) {
                stop = true;
                cancel();
                timer.cancel();
            }

            boolean interrupted = false;
            while (!stopped) {
                try {
                    timer.wait();
                }
                catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        logger.debug("Stopped file monitor for {}", this.root.file);
    }

    /**
     * @see java.util.TimerTask#run()
     */
    public void run() {
        synchronized (timer) {
            stopped = false;
            if (stop) {
                stopped = true;
                timer.notifyAll();
                return;
            }
        }
        synchronized ( this ) {
            try {
                // if we don't have an event admin, we just skip the check
                final EventAdmin localEA = this.provider.getEventAdmin();
                if ( localEA != null ) {
                    this.check(this.root, localEA);
                }
            } catch (Exception e) {
                // ignore this
            }
        }
        synchronized (timer) {
            stopped = true;
            timer.notifyAll();
        }
    }

    /**
     * Check the monitorable
     * @param monitorable The monitorable to check
     * @param localEA The event admin
     */
    private void check(final Monitorable monitorable, final EventAdmin localEA) {
        logger.debug("Checking {}", monitorable.file);
        // if the file is non existing, check if it has been readded
        if ( monitorable.status instanceof NonExistingStatus ) {
            if ( monitorable.file.exists() ) {
                // new file and reset status
                createStatus(monitorable);
                sendEvents(monitorable,
                           SlingConstants.TOPIC_RESOURCE_ADDED,
                           localEA);
            }
        } else {
            // check if the file has been removed
            if ( !monitorable.file.exists() ) {
                // removed file and update status
                sendEvents(monitorable,
                           SlingConstants.TOPIC_RESOURCE_REMOVED,
                           localEA);
                monitorable.status = NonExistingStatus.SINGLETON;
            } else {
                // check for changes
                final FileStatus fs = (FileStatus)monitorable.status;
                boolean changed = false;
                if ( fs.lastModified < monitorable.file.lastModified() ) {
                    fs.lastModified = monitorable.file.lastModified();
                    // changed
                    sendEvents(monitorable,
                               SlingConstants.TOPIC_RESOURCE_CHANGED,
                               localEA);
                    changed = true;
                }
                if ( fs instanceof DirStatus ) {
                    // directory
                    final DirStatus ds = (DirStatus)fs;
                    for(int i=0; i<ds.children.length; i++) {
                        check(ds.children[i], localEA);
                    }
                    // if the dir changed we have to update
                    if ( changed ) {
                        // and now update
                        final File[] files = monitorable.file.listFiles();
                        if (files != null) {
                            final Monitorable[] children = new Monitorable[files.length];
                            for (int i = 0; i < files.length; i++) {
                                // search in old list
                                for (int m = 0; m < ds.children.length; m++) {
                                    if (ds.children[m].file.equals(files[i])) {
                                        children[i] = ds.children[m];
                                        break;
                                    }
                                }
                                if (children[i] == null) {
                                    children[i] = new Monitorable(
                                        monitorable.path + '/'
                                            + files[i].getName(), files[i]);
                                    children[i].status = NonExistingStatus.SINGLETON;
                                    check(children[i], localEA);
                                }
                            }
                            ds.children = children;
                        } else {
                            ds.children = new Monitorable[0];
                        }
                    }
                }
            }
        }
    }

    /**
     * Send the event async via the event admin.
     */
    private void sendEvents(final Monitorable monitorable, final String topic, final EventAdmin localEA) {
        if ( logger.isDebugEnabled() ) {
            logger.debug("Detected change for resource {} : {}", monitorable.path, topic);
        }

        final Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(SlingConstants.PROPERTY_PATH, monitorable.path);
        final String type = monitorable.status instanceof FileStatus ?
                FsResource.RESOURCE_TYPE_FILE : FsResource.RESOURCE_TYPE_FOLDER;
        properties.put(SlingConstants.PROPERTY_RESOURCE_TYPE, type);
        localEA.postEvent(new org.osgi.service.event.Event(topic, properties));
    }

    /**
     * Create a status object for the monitorable
     */
    private static void createStatus(final Monitorable monitorable) {
        if ( !monitorable.file.exists() ) {
            monitorable.status = NonExistingStatus.SINGLETON;
        } else if ( monitorable.file.isFile() ) {
            monitorable.status = new FileStatus(monitorable.file);
        } else {
            monitorable.status = new DirStatus(monitorable.file, monitorable.path);
        }
    }

    /** The monitorable to hold the resource path, the file and the status. */
    private static final class Monitorable {
        public final String path;
        public final File   file;
        public Object status;

        public Monitorable(final String path, final File file) {
            this.path = path;
            this.file = file;
        }
    }

    /** Status for files. */
    private static class FileStatus {
        public long lastModified;
        public FileStatus(final File file) {
            this.lastModified = file.lastModified();
        }
    }

    /** Status for directories. */
    private static final class DirStatus extends FileStatus {
        public Monitorable[] children;

        public DirStatus(final File dir, final String path) {
            super(dir);
            final File[] files = dir.listFiles();
            if (files != null) {
                this.children = new Monitorable[files.length];
                for (int i = 0; i < files.length; i++) {
                    this.children[i] = new Monitorable(path + '/'
                        + files[i].getName(), files[i]);
                    FileMonitor.createStatus(this.children[i]);
                }
            } else {
                this.children = new Monitorable[0];
            }
        }
    }

    /** Status for non existing files. */
    private static final class NonExistingStatus {
        public static NonExistingStatus SINGLETON = new NonExistingStatus();
    }
}
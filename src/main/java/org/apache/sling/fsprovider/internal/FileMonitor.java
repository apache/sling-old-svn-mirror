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

import static org.apache.jackrabbit.vault.util.Constants.ROOT_DIR;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.util.PlatformNameFormat;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.fsprovider.internal.mapper.ContentFile;
import org.apache.sling.fsprovider.internal.parser.ContentFileCache;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ObserverConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a monitor for the file system
 * that periodically checks for changes.
 */
public final class FileMonitor extends TimerTask {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Timer timer = new Timer();
    private boolean stop = false;
    private boolean stopped = true;

    private final Monitorable root;

    private final FsResourceProvider provider;
    private final FsMode fsMode;
    private final ContentFileExtensions contentFileExtensions;
    private final ContentFileCache contentFileCache;

    /**
     * Creates a new instance of this class.
     * @param provider The resource provider.
     * @param interval The interval between executions of the task, in milliseconds.
     */
    public FileMonitor(final FsResourceProvider provider, final long interval, FsMode fsMode,
            final ContentFileExtensions contentFileExtensions, final ContentFileCache contentFileCache) {
        this.provider = provider;
        this.fsMode = fsMode;
        this.contentFileExtensions = contentFileExtensions;
        this.contentFileCache = contentFileCache;
        
        File rootFile = this.provider.getRootFile();
        if (fsMode == FsMode.FILEVAULT_XML) {
            rootFile = new File(this.provider.getRootFile(), ROOT_DIR + PlatformNameFormat.getPlatformPath(this.provider.getProviderRoot()));
        }
        this.root = new Monitorable(this.provider.getProviderRoot(), rootFile, null);
        
        createStatus(this.root, contentFileExtensions, contentFileCache);
        log.debug("Starting file monitor for {} with an interval of {}ms", this.root.file, interval);
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
        log.debug("Stopped file monitor for {}", this.root.file);
    }

    /**
     * @see java.util.TimerTask#run()
     */
    @Override
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
                // if we don't have an observation reporter, we just skip the check
                final ObservationReporter reporter = this.provider.getObservationReporter();
                if ( reporter != null ) {
                    this.check(this.root, reporter);
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
     * @param reporter The ObservationReporter
     */
    private void check(final Monitorable monitorable, final ObservationReporter reporter) {
        log.trace("Checking {}", monitorable.file);
        // if the file is non existing, check if it has been readded
        if ( monitorable.status instanceof NonExistingStatus ) {
            if ( monitorable.file.exists() ) {
                // new file and reset status
                createStatus(monitorable, contentFileExtensions, contentFileCache);
                sendEvents(monitorable, ChangeType.ADDED, reporter);
                final FileStatus fs = (FileStatus)monitorable.status;
                if ( fs instanceof DirStatus ) {
                    final DirStatus ds = (DirStatus)fs;
                    // remove monitorables for new folder and update folder children to send events for directory contents
                    ds.children = new Monitorable[0];
                    checkDirStatusChildren(monitorable, reporter);
                }
            }
        } else {
            // check if the file has been removed
            if ( !monitorable.file.exists() ) {
                // removed file and update status
                sendEvents(monitorable, ChangeType.REMOVED, reporter);
                monitorable.status = NonExistingStatus.SINGLETON;
                contentFileCache.remove(monitorable.path);
            } else {
                // check for changes
                final FileStatus fs = (FileStatus)monitorable.status;
                boolean changed = false;
                if ( fs.lastModified < monitorable.file.lastModified() ) {
                    fs.lastModified = monitorable.file.lastModified();
                    // changed
                    sendEvents(monitorable, ChangeType.CHANGED, reporter);
                    changed = true;
                    contentFileCache.remove(monitorable.path);
                }
                if ( fs instanceof DirStatus ) {
                    // directory
                    final DirStatus ds = (DirStatus)fs;
                    for(int i=0; i<ds.children.length; i++) {
                        check(ds.children[i], reporter);
                    }
                    // if the dir changed we have to update
                    if ( changed ) {
                        // and now update
                        checkDirStatusChildren(monitorable, reporter);
                    }
                }
            }
        }
    }
    
    private void checkDirStatusChildren(final Monitorable dirMonitorable, final ObservationReporter reporter) {
        final DirStatus ds = (DirStatus)dirMonitorable.status;
        final File[] files = dirMonitorable.file.listFiles();
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
                    children[i] = new Monitorable(dirMonitorable.path + '/' + files[i].getName(), files[i],
                            contentFileExtensions.getSuffix(files[i]));
                    children[i].status = NonExistingStatus.SINGLETON;
                    check(children[i], reporter);
                }
            }
            ds.children = children;
        } else {
            ds.children = new Monitorable[0];
        }
    }

    /**
     * Send the event async via the event admin.
     */
    private void sendEvents(final Monitorable monitorable, final ChangeType changeType, final ObservationReporter reporter) {
        if (log.isDebugEnabled()) {
            log.debug("Detected change for resource {} : {}", transformPath(monitorable.path), changeType);
        }

        List<ResourceChange> changes = null;
        for (final ObserverConfiguration config : reporter.getObserverConfigurations()) {
            if (config.matches(transformPath(monitorable.path))) {
                if (changes == null) {
                    changes = collectResourceChanges(monitorable, changeType);
                }
                if (log.isTraceEnabled()) {
                    for (ResourceChange change : changes) {
                        log.debug("Send change for resource {}: {} to {}", change.getPath(), change.getType(), config);
                    }
                }
            }
        }
        if (changes != null) {
            reporter.reportChanges(changes, false);
        }
    }
    
    /**
     * Transform path for resource event.
     * @param path Path
     * @return Transformed path
     */
    private String transformPath(String path) {
        if (fsMode == FsMode.FILEVAULT_XML) {
            return PlatformNameFormat.getRepositoryPath(path);
        }
        else {
            return path;
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<ResourceChange> collectResourceChanges(final Monitorable monitorable, final ChangeType changeType) {
        List<ResourceChange> changes = new ArrayList<>();
        if (monitorable.status instanceof ContentFileStatus) {
            ContentFile contentFile = ((ContentFileStatus)monitorable.status).contentFile;
            if (changeType == ChangeType.CHANGED) {
                Map<String,Object> content = (Map<String,Object>)contentFile.getContent();
                // we cannot easily report the diff of resource changes between two content files
                // so we simulate a removal of the toplevel node and then add all nodes contained in the current content file again.
                changes.add(buildContentResourceChange(ChangeType.REMOVED,  transformPath(monitorable.path)));
                addContentResourceChanges(changes, ChangeType.ADDED, content, transformPath(monitorable.path));
            }
            else {
                addContentResourceChanges(changes, changeType, (Map<String,Object>)contentFile.getContent(), transformPath(monitorable.path));
            }
        }
        else {
            changes.add(buildContentResourceChange(changeType, transformPath(monitorable.path)));
        }
        return changes;
    }
    @SuppressWarnings("unchecked")
    private void addContentResourceChanges(final List<ResourceChange> changes, final ChangeType changeType,
            final Map<String,Object> content, final String path) {
        changes.add(buildContentResourceChange(changeType,  path));
        if (content != null) {
            for (Map.Entry<String,Object> entry : content.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    String childPath = path + "/" + entry.getKey();
                    addContentResourceChanges(changes, changeType, (Map<String,Object>)entry.getValue(), childPath);
                }
            }
        }
    }
    private ResourceChange buildContentResourceChange(final ChangeType changeType, final String path) {
        return new ResourceChange(changeType, path, false, null, null, null);
    }

    /**
     * Create a status object for the monitorable
     */
    private static void createStatus(final Monitorable monitorable, ContentFileExtensions contentFileExtensions, ContentFileCache contentFileCache) {
        if ( !monitorable.file.exists() ) {
            monitorable.status = NonExistingStatus.SINGLETON;
        } else if ( monitorable.file.isFile() ) {
            if (contentFileExtensions.matchesSuffix(monitorable.file)) {
                monitorable.status = new ContentFileStatus(monitorable.file,
                        new ContentFile(monitorable.file, monitorable.path, null, contentFileCache));
            }
            else {
                monitorable.status = new FileStatus(monitorable.file);
            }
        } else {
            monitorable.status = new DirStatus(monitorable.file, monitorable.path, contentFileExtensions, contentFileCache);
        }
    }

    /** The monitorable to hold the resource path, the file and the status. */
    private static final class Monitorable {
        public final String path;
        public final File file;
        public Object status;
        public Monitorable(final String path, final File file, String contentFileSuffix) {
            this.file = file;
            if (contentFileSuffix != null) {
                this.path = StringUtils.substringBeforeLast(path, contentFileSuffix);
            }
            else {
                this.path = path;
            }
        }
    }

    /** Status for files. */
    private static class FileStatus {
        public long lastModified;
        public FileStatus(final File file) {
            this.lastModified = file.lastModified();
        }
    }
    
    /** Status for content files */
    private static class ContentFileStatus extends FileStatus {
        public final ContentFile contentFile;
        public ContentFileStatus(final File file, final ContentFile contentFile) {
            super(file);
            this.contentFile = contentFile;
        }
    }
    
    /** Status for directories. */
    private static final class DirStatus extends FileStatus {
        public Monitorable[] children;

        public DirStatus(final File dir, final String path,
                final ContentFileExtensions contentFileExtensions, final ContentFileCache contentFileCache) {
            super(dir);
            final File[] files = dir.listFiles();
            if (files != null) {
                this.children = new Monitorable[files.length];
                for (int i = 0; i < files.length; i++) {
                    this.children[i] = new Monitorable(path + '/' + files[i].getName(), files[i],
                            contentFileExtensions.getSuffix(files[i]));
                    FileMonitor.createStatus(this.children[i], contentFileExtensions, contentFileCache);
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
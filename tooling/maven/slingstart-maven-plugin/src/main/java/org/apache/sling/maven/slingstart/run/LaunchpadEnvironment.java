/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.maven.slingstart.run;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.codehaus.plexus.util.FileUtils;

/**
 * Common settings for all launchpad instances.
 */
public class LaunchpadEnvironment {

    /** The work directory created by starting launchpad. */
    public static final String WORK_DIR_NAME = "sling";

    private final File launchpadJar;
    private final boolean cleanWorkingDirectory;
    private final boolean shutdownOnExit;
    private final int readyTimeOutSec;
    private final String debug;

    public LaunchpadEnvironment(final File launchpadJar,
                                final boolean cleanWorkingDirectory,
                                final boolean shutdownOnExit,
                                final int readyTimeOutSec,
                                final String debug) {
        this.launchpadJar = launchpadJar;
        this.cleanWorkingDirectory = cleanWorkingDirectory;
        this.shutdownOnExit = shutdownOnExit;
        this.readyTimeOutSec = readyTimeOutSec;
        this.debug = debug;
    }

    public boolean isShutdownOnExit() {
        return this.shutdownOnExit;
    }

    public int getReadyTimeOutSec() {
        return this.readyTimeOutSec;
    }

    /**
     * Check if the launchpad folder exists.
     */
    private void ensureFolderExists(final File folder) {
        if (!folder.exists()) {
            folder.mkdirs();
        }
        if (this.cleanWorkingDirectory) {
            final File work = new File(folder, WORK_DIR_NAME);
            org.apache.commons.io.FileUtils.deleteQuietly(work);
        }
    }

    private File installLaunchpad(final File folder) throws IOException {
        if (this.launchpadJar.getParentFile().getAbsolutePath().equals(folder.getAbsolutePath())) {
            return this.launchpadJar;
        }
        try {
            FileUtils.copyFileToDirectory(this.launchpadJar, folder);
            return new File(folder, this.launchpadJar.getName());
        } catch (final IOException ioe) {
            throw new IOException("Unable to copy " + this.launchpadJar + " to " + folder, ioe);
        }
    }

    private void installLauncher(final File folder) throws IOException {
        final File binDir = new File(folder, "bin");
        copyResource("org/apache/sling/maven/slingstart/launcher/Launcher.class", binDir);
        copyResource("org/apache/sling/maven/slingstart/launcher/LauncherMBean.class", binDir);
        copyResource("org/apache/sling/maven/slingstart/launcher/Main.class", binDir);
    }

    /**
     * Prepare a new instance.
     * @param folder The target folder for the instance
     * @return The launchpad jar
     * @throws IOException if an error occurs.
     */
    public File prepare(final File folder) throws IOException {
        this.ensureFolderExists(folder);

        // copy launchpadJar
        final File launchpad = this.installLaunchpad(folder);

        // install launcher
        this.installLauncher(folder);

        return launchpad;
    }

    private void copyResource(final String resource,
            final File dir)
    throws IOException {
        final int lastSlash = resource.lastIndexOf('/');
        final File baseDir;
        if ( lastSlash > 0 ) {
            final String filePath = resource.substring(0, lastSlash).replace('/', File.separatorChar);
            baseDir = new File(dir, filePath);
        } else {
            baseDir = dir;
        }
        baseDir.mkdirs();
        final File file = new File(baseDir, resource.substring(lastSlash + 1));
        final InputStream is = LaunchpadEnvironment.class.getClassLoader().getResourceAsStream(resource);
        if ( is == null ) {
            throw new IOException("Resource not found: " + resource);
        }
        final FileOutputStream fos = new FileOutputStream(file);
        final byte[] buffer = new byte[2048];
        int l;
        try {
            while ( (l = is.read(buffer)) > 0 ) {
                fos.write(buffer, 0, l);
            }
        } finally {
            if ( fos != null ) {
                fos.close();
            }
            if ( is != null ) {
                is.close();
            }
        }
    }

    /**
     * 
     * @return the global debug parameter for all Sling instances. Set through {@link StartMojo#debug}.
     */
    public String getDebug() {
        return debug;
    }

}

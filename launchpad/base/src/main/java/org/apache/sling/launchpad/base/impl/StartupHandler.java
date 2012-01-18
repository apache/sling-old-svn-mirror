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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.felix.framework.Logger;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.osgi.framework.BundleContext;

/**
 * The <code>StartupHandler</code> tries to detect the startup mode:
 * It distinguishes between an initial startup (INSTALL), an update (UPDATE)
 * and a restart without a change (RESTART).
 * @since 2.4.0
 */
class StartupHandler {

    public enum StartupMode {
        INSTALL,
        UPDATE,
        RESTART
    }

    /** The data file which works as a marker to detect the first startup. */
    private static final String DATA_FILE = "bootstrapinstaller.ser";

    /**
     * The {@link Logger} use for logging messages during installation and
     * startup.
     */
    private final Logger logger;

    /** The bundle context. */
    private final BundleContext bundleContext;

    private final StartupMode mode;

    private final File startupDir;

    private final long selfStamp;

    StartupHandler(final BundleContext bundleContext,
            final Logger logger,
            final File slingStartupDir)
    throws IOException {
        this.logger = logger;
        this.bundleContext = bundleContext;
        this.startupDir = slingStartupDir;
        this.selfStamp = this.getSelfTimestamp();
        this.mode = detectMode();
        this.logger.log(Logger.LOG_INFO, "Starting in mode " + this.mode);
    }

    public StartupMode getMode() {
        return this.mode;
    }

    public void finished() {
        this.markInstalled();
    }

    private StartupMode detectMode() {
        final File dataFile = this.bundleContext.getDataFile(DATA_FILE);
        if (dataFile != null && dataFile.exists()) {

            FileInputStream fis = null;
            try {
                if (this.selfStamp > 0) {

                    fis = new FileInputStream(dataFile);
                    byte[] bytes = new byte[20];
                    int len = fis.read(bytes);
                    String value = new String(bytes, 0, len);

                    long storedStamp = Long.parseLong(value);

                    logger.log(Logger.LOG_INFO, String.format("Stored timestamp: %s", storedStamp));

                    return (storedStamp >= this.selfStamp ? StartupMode.RESTART : StartupMode.UPDATE);
                }

            } catch (final NumberFormatException nfe) {
                // probably still the old value, fallback to assume not
                // installed
                return StartupMode.RESTART;

            } catch (final IOException ioe) {
                logger.log(Logger.LOG_ERROR,
                    "IOException during reading of installed flag.", ioe);

            } finally {
                if (fis != null) {
                    try { fis.close(); } catch (IOException ignore) {}
                }
            }
        }
        // not installed yet - fallback
        return StartupMode.INSTALL;
    }

    private long getTimeStampOfClass(final Class<?> clazz)
    throws IOException {
        long selfStamp = -1;
        final ClassLoader loader = clazz.getClassLoader();
        if (loader instanceof URLClassLoader) {
            final URLClassLoader urlLoader = (URLClassLoader) loader;
            final URL[] urls = urlLoader.getURLs();
            if (urls.length > 0) {
                final URL url = urls[0];
                logger.log(Logger.LOG_INFO, String.format("Using timestamp from %s.", url));
                selfStamp = urls[0].openConnection().getLastModified();
            }
        }
        return selfStamp;
    }

    /**
     * Returns the time stamp of JAR file from which this class has been loaded
     * or -1 if the timestamp cannot be resolved.
     * <p>
     * This method assumes that the ClassLoader of this class is an
     * URLClassLoader and that the first URL entry of this class loader is the
     * JAR providing this class. This is in fact true as the URLClassLoader has
     * been created by the launcher from the launcher JAR file.
     *
     * @return The last modification time stamp of the launcher JAR file or -1
     *         if the class loader of this class is not an URLClassLoader or the
     *         class loader has no URL entries. Both situations are not really
     *         expected.
     * @throws IOException If an error occurrs reading accessing the last
     *             modification time stampe.
     */
    private long getSelfTimestamp() throws IOException {

        // the timestamp of the launcher jar and the bootstrap jar
        long selfStamp = this.getTimeStampOfClass(this.getClass());
        long bootStamp = this.getTimeStampOfClass(LaunchpadContentProvider.class);
        if ( bootStamp > selfStamp ) {
            selfStamp = bootStamp;
        }

        // check whether any bundle is younger than the launcher jar
        final File[] directories = this.startupDir.listFiles(DirectoryUtil.DIRECTORY_FILTER);
        for (final File levelDir : directories) {

            // iterate through all files in the startlevel dir
            final File[] jarFiles = levelDir.listFiles(DirectoryUtil.BUNDLE_FILE_FILTER);
            for (final File bundleJar : jarFiles) {
                if (bundleJar.lastModified() > selfStamp) {
                    logger.log(Logger.LOG_INFO, String.format("Using timestamp from %s.", bundleJar));
                    selfStamp = bundleJar.lastModified();
                }
            }
        }

        logger.log(Logger.LOG_INFO, String.format("Final self timestamp: %s.", selfStamp));

        // return the final stamp (may be -1 if launcher jar cannot be checked
        // and there are no bundle jar files)
        return selfStamp;
    }

    private void markInstalled() {
        final File dataFile = this.bundleContext.getDataFile(DATA_FILE);
        try {
            final FileOutputStream fos = new FileOutputStream(dataFile);
            try {
                fos.write(String.valueOf(this.selfStamp).getBytes());
            } finally {
                try { fos.close(); } catch (final IOException ignore) {}
            }
        } catch (final IOException ioe) {
            logger.log(Logger.LOG_ERROR,
                "IOException during writing of installed flag.", ioe);
        }
    }
}

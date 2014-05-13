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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import org.apache.felix.framework.Logger;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.apache.sling.launchpad.api.StartupMode;
import org.apache.sling.launchpad.base.shared.SharedConstants;
import org.osgi.framework.Constants;

/**
 * The <code>StartupHandler</code> tries to detect the startup mode:
 * It distinguishes between an initial startup (INSTALL), an update (UPDATE)
 * and a restart without a change (RESTART).
 * @since 2.4.0
 */
public class StartupManager {

    /** The data file which works as a marker to detect the first startup. */
    private static final String DATA_FILE = "launchpad-timestamp.txt";

    /** The old data file. */
    private static final String OLD_DATA_FILE = "bundle0" + File.separatorChar + "bootstrapinstaller.ser";

    /** Name of the mode override property. */
    private static final String OVERRIDE_PROP = "org.apache.sling.launchpad.startupmode";

    /**
     * The {@link Logger} use for logging messages during installation and
     * startup.
     */
    private final Logger logger;

    private final StartupMode mode;

    private final File startupDir;

    private final File confDir;

    private final long targetStartLevel;

    private final boolean incrementalStartupEnabled;

    StartupManager(final Map<String, String> properties,
                   final Logger logger) {
        this.logger = logger;
        this.startupDir = DirectoryUtil.getStartupDir(properties);
        this.confDir = DirectoryUtil.getConfigDir(properties);
        // check for override property
        final String overrideMode = System.getProperty(OVERRIDE_PROP, properties.get(OVERRIDE_PROP));
        if ( overrideMode != null ) {
            this.mode = StartupMode.valueOf(overrideMode.toUpperCase());
            this.logger.log(Logger.LOG_INFO, "Override property set. Starting in mode " + this.mode);
        } else {
            this.mode = detectMode(properties.get(Constants.FRAMEWORK_STORAGE));
            this.logger.log(Logger.LOG_INFO, "Detected startup mode. Starting in mode " + this.mode);
        }

        this.targetStartLevel = Long.valueOf(properties.get(Constants.FRAMEWORK_BEGINNING_STARTLEVEL));

        this.incrementalStartupEnabled = Boolean.valueOf(properties.get(SharedConstants.SLING_INSTALL_INCREMENTAL_START));

        // if this is not a restart, reduce start level
        if ( this.mode != StartupMode.RESTART && this.incrementalStartupEnabled ) {
            final String startLevel = properties.get(SharedConstants.SLING_INSTALL_STARTLEVEL);
            properties.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, startLevel != null ? startLevel : "10");
        }
    }

    /**
     * Return the startup mode
     * @return The startup mode
     */
    public StartupMode getMode() {
        return this.mode;
    }

    /**
     * Is the incremental startup enabled?
     */
    public boolean isIncrementalStartupEnabled() {
        return this.incrementalStartupEnabled;
    }

    /**
     * Return the target start level.
     * @return Target start level
     */
    public long getTargetStartLevel() {
        return this.targetStartLevel;
    }

    /**
     * Detect the startup mode by comparing time stamps
     */
    private StartupMode detectMode(final String osgiStorageDir) {
        final File dataFile = new File(this.confDir, DATA_FILE);
        if (dataFile.exists()) {

            FileReader fis = null;
            try {
                final long selfStamp = this.getSelfTimestamp();
                if (selfStamp > 0) {

                    fis = new FileReader(dataFile);
                    final char[] txt = new char[128];
                    final int len = fis.read(txt);
                    final String value = new String(txt, 0, len);

                    final long storedStamp = Long.parseLong(value);

                    logger.log(Logger.LOG_INFO, String.format("Stored startup timestamp: %s", storedStamp));

                    return (storedStamp >= selfStamp ? StartupMode.RESTART : StartupMode.UPDATE);
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
        } else {
            // check for old data file
            // this is a little bit hacky as we have to directly look into
            // the Apache Felix bundle cache. However, as we know that older
            // versions did use Felix this is fine.
            final File felixDir = new File(osgiStorageDir);
            final File oldFile = new File(felixDir, OLD_DATA_FILE);
            if ( oldFile.exists() ) {
                // this is an update - remove old file
                oldFile.delete();
                return StartupMode.UPDATE;
            }
        }
        // not installed yet - fallback
        return StartupMode.INSTALL;
    }

    /**
     * Get the time stamp of a class through its url classloader (if possible)
     */
    long getTimeStampOfClass(final Class<?> clazz, final long selfStamp) {
        long timeStamp = selfStamp;
        final ClassLoader loader = clazz.getClassLoader();
        if (loader instanceof URLClassLoader) {
            final URLClassLoader urlLoader = (URLClassLoader) loader;
            final URL[] urls = urlLoader.getURLs();
            if (urls.length > 0) {
                final URL url = urls[0];
                try {
                    final long stamp = urls[0].openConnection().getLastModified();
                    if ( stamp > selfStamp ) {
                        logger.log(Logger.LOG_INFO, String.format("Newer timestamp for %s from %s : %s", clazz.getName(), url, selfStamp));
                        timeStamp = stamp;
                    }
                } catch (final IOException ignore) {}
            }
        }
        return timeStamp;
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
     * @throws IOException If an error occurs reading accessing the last
     *             modification time stamp.
     */
    private long getSelfTimestamp() {

        // the time stamp of the launcher jar and the bootstrap jar
        long selfStamp = this.getTimeStampOfClass(this.getClass(), -1);
        selfStamp = this.getTimeStampOfClass(LaunchpadContentProvider.class, selfStamp);

        // check whether any bundle is younger than the launcher jar
        final File[] directories = this.startupDir.listFiles(DirectoryUtil.DIRECTORY_FILTER);
        if ( directories != null ) {
            for (final File levelDir : directories) {

                // iterate through all files in the startlevel dir
                final File[] jarFiles = levelDir.listFiles(DirectoryUtil.BUNDLE_FILE_FILTER);
                if ( jarFiles != null ) {
                    for (final File bundleJar : jarFiles) {
                        if (bundleJar.lastModified() > selfStamp) {
                            selfStamp = bundleJar.lastModified();
                            logger.log(Logger.LOG_INFO, String.format("Newer timestamp from %s : %s", bundleJar, selfStamp));
                        }
                    }
                }
            }
        }

        logger.log(Logger.LOG_INFO, String.format("Final self timestamp: %s.", selfStamp));

        // return the final stamp (may be -1 if launcher jar cannot be checked
        // and there are no bundle jar files)
        return selfStamp;
    }

    /**
     * Set the finished installation marker.
     */
    public void markInstalled() {
        final File dataFile = new File(this.confDir, DATA_FILE);
        try {
            this.confDir.mkdirs();
            final FileWriter fos = new FileWriter(dataFile);
            try {
                fos.write(String.valueOf(System.currentTimeMillis()));
            } finally {
                try { fos.close(); } catch (final IOException ignore) {}
            }
        } catch (final IOException ioe) {
            logger.log(Logger.LOG_ERROR,
                "IOException during writing of installed flag.", ioe);
        }
    }
}

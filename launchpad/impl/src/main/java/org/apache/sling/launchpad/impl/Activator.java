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
package org.apache.sling.launchpad.impl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.sling.launchpad.api.StartupMode;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    /** The data file which works as a marker to detect the first startup. */
    private static final String DATA_FILE = "launchpad-timestamp.txt";

    /** Name of the mode override property. */
    private static final String OVERRIDE_PROP = "org.apache.sling.launchpad.startupmode";

    /**
     * The name of the configuration property defining if the startup level
     * is increased incrementally for installs and updates.
     * If enabled the framework starts with the start level defined by
     * {@link Constants#FRAMEWORK_BEGINNING_STARTLEVEL}
     * and the startup manager increases the start level one by one until
     * the initial framework start level is reached.
     * The default value is false.
     */
    private static final String SLING_INSTALL_INCREMENTAL_START = "sling.framework.startup.incremental";

    /**
     * The name of the configuration property defining the final start level
     * The framework starts with the start level defined by
     * {@link Constants#FRAMEWORK_BEGINNING_STARTLEVEL}
     * and the startup manager increases the start level until
     * this start level is reached.
     * Default value is 30.
     */
    private static final String SLING_INSTALL_STARTLEVEL = "sling.framework.startup.startlevel";

    private String getProperty(final BundleContext context, final String name, final String defaultValue) {
        String value = context.getProperty(name);
        if ( value == null ) {
            value = System.getProperty(name);
            if ( value == null ) {
                value = defaultValue;
            }
        }
        return value;
    }

    private int getIntProperty(final Logger logger, final BundleContext context, final String name, final int defaultValue) {
        String value = this.getProperty(context, name, null);
        if ( value != null ) {
            try {
                return Integer.valueOf(value);
            } catch (final NumberFormatException nfe) {
                logger.warn("Unable to parse integer value {} for property {}. Ignoring and using default {}",
                        new Object[] {value, name, defaultValue});
            }
        }
        return defaultValue;
    }

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        final Logger logger = LoggerFactory.getLogger("org.apache.sling.launchpad.startup");

        final File confDir = new File(context.getProperty(Constants.FRAMEWORK_STORAGE));

        // check for override property
        final String overrideMode = this.getProperty(context, OVERRIDE_PROP, null);
        final StartupMode mode;
        if ( overrideMode != null ) {
            mode = StartupMode.valueOf(overrideMode.toUpperCase());
            logger.info("Startup override property set. Starting in mode {}", mode);
        } else {
            mode = this.detectMode(logger, confDir);
            logger.info("Detected startup mode. Starting in mode {}",  mode);
        }

        final int targetFrameworkStartLevel = this.getIntProperty(logger, context, Constants.FRAMEWORK_BEGINNING_STARTLEVEL, 1);
        final int targetSlingStartLevel = this.getIntProperty(logger, context, SLING_INSTALL_STARTLEVEL, targetFrameworkStartLevel);

        final int targetStartLevel;
        if (targetSlingStartLevel >= targetFrameworkStartLevel ) {
            targetStartLevel = targetSlingStartLevel;
        } else {
            targetStartLevel = targetFrameworkStartLevel;
            logger.warn("Ignoring target Sling framework start level as it is lower than the framework beginning start level: {} - {}",
                    targetSlingStartLevel, targetFrameworkStartLevel);
        }
        final boolean incrementalStartupEnabled = Boolean.valueOf(this.getProperty(context, SLING_INSTALL_INCREMENTAL_START, "false"));

        new DefaultStartupHandler(context, logger, mode, targetStartLevel, incrementalStartupEnabled);

        this.markInstalled(logger, confDir);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) throws Exception {
        // nothing to do
    }

    /**
     * Detect the startup mode by comparing time stamps
     */
    private StartupMode detectMode(final Logger logger, final File confDir) {
        final File dataFile = new File(confDir, DATA_FILE);
        if (dataFile.exists()) {

            FileReader fis = null;
            try {
                final long selfStamp = dataFile.lastModified();
                if (selfStamp > 0) {

                    fis = new FileReader(dataFile);
                    final char[] txt = new char[128];
                    final int len = fis.read(txt);
                    final String value = new String(txt, 0, len);

                    final long storedStamp = Long.parseLong(value);

                    logger.info("Stored startup timestamp: {}", storedStamp);

                    return (storedStamp >= selfStamp ? StartupMode.RESTART : StartupMode.UPDATE);
                }

            } catch (final NumberFormatException nfe) {
                // fallback to assume restart
                return StartupMode.RESTART;

            } catch (final IOException ioe) {
                logger.error("IOException during reading of installed flag.", ioe);

            } finally {
                if (fis != null) {
                    try { fis.close(); } catch (IOException ignore) {}
                }
            }
        }
        // not installed yet - fallback
        return StartupMode.INSTALL;
    }

    /**
     * Set the finished installation marker.
     */
    private void markInstalled(final Logger logger, final File confDir) {
        final File dataFile = new File(confDir, DATA_FILE);
        try {
            confDir.mkdirs();
            final FileWriter fos = new FileWriter(dataFile);
            try {
                fos.write(String.valueOf(System.currentTimeMillis()));
            } finally {
                try { fos.close(); } catch (final IOException ignore) {}
            }
        } catch (final IOException ioe) {
            logger.error( "IOException during writing of installed flag.", ioe);
        }
    }
}

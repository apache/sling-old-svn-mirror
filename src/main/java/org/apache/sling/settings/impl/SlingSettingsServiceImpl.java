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
package org.apache.sling.settings.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the basic implementation of the sling settings service.
 */
public class SlingSettingsServiceImpl
    implements SlingSettingsService {

    /** The logger */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The sling instance id. */
    private String slingId;

    /** The sling home */
    private String slingHome;

    /** The sling home url */
    private URL slingHomeUrl;

    private Set<String> runModes;

    /** The name of the data file holding the sling id. */
    private static final String DATA_FILE = "sling.id.file";

    /**
     * Create the service and search the Sling home urls and
     * get/create a sling id.
     * Setup run modes
     * @param context The bundle context
     */
    public SlingSettingsServiceImpl(final BundleContext context) {
        this.setupSlingHome(context);
        this.setupSlingId(context);
        this.setupRunModes(context);

    }

    /**
     * Get sling home and sling home url
     */
    private void setupSlingHome(final BundleContext context) {
        this.slingHome = context.getProperty(SLING_HOME);
        final String url = context.getProperty(SLING_HOME_URL);
        if ( url != null ) {
            try {
                this.slingHomeUrl = new URL(url);
            } catch (MalformedURLException e) {
                logger.error("Sling home url is not a url: {}", url);
            }
        }
    }

    /**
     * Get / create sling id
     */
    private void setupSlingId(final BundleContext context) {
        // try to read the id from the id file first
        final File idFile = context.getDataFile(DATA_FILE);
        if ( idFile == null ) {
            // the osgi framework does not support storing something in the file system
            throw new RuntimeException("Unable to read from bundle data file.");
        }
        this.slingId = this.readSlingId(idFile);

        // no sling id yet or failure to read file: create an id and store
        if (slingId == null) {
            slingId = UUID.randomUUID().toString();
            this.writeSlingId(idFile, this.slingId);
        }
    }

    /**
     * Set up run modes.
     */
    private void setupRunModes(final BundleContext context) {
        final String prop = context.getProperty(RUN_MODES_PROPERTY);
        if (prop == null || prop.trim().length() == 0) {
            this.runModes = Collections.emptySet();
        } else {
            final Set<String> modesSet = new HashSet<String>();
            final String[] modes = prop.split(",");
            for(int i=0; i < modes.length; i++) {
                modesSet.add(modes[i].trim());
            }
            // make the set unmodifiable and synced
            // we propably don't need a synced set as it is read only
            this.runModes = Collections.synchronizedSet(Collections.unmodifiableSet(modesSet));
            logger.info("Active run modes {}", this.runModes);
        }
    }


    /**
     * Read the id from a file.
     */
    private String readSlingId(final File idFile) {
        if (idFile.exists() && idFile.length() >= 36) {
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(idFile);
                final byte[] rawBytes = new byte[36];
                if (fin.read(rawBytes) == 36) {
                    final String rawString = new String(rawBytes, "ISO-8859-1");

                    // roundtrip to ensure correct format of UUID value
                    final String id = UUID.fromString(rawString).toString();
                    logger.debug("Got Sling ID {} from file {}", id, idFile);

                    return id;
                }
            } catch (Throwable t) {
                logger.error("Failed reading UUID from id file " + idFile
                        + ", creating new id", t);
            } finally {
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
        return null;
    }

    /**
     * Write the sling id file.
     */
    private void writeSlingId(final File idFile, final String id) {
        idFile.delete();
        idFile.getParentFile().mkdirs();
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(idFile);
            fout.write(slingId.getBytes("ISO-8859-1"));
            fout.flush();
        } catch (Throwable t) {
            logger.error("Failed writing UUID to id file " + idFile, t);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    /**
     * @see org.apache.sling.settings.SlingSettingsService#getAbsolutePathWithinSlingHome(String)
     */
    public String getAbsolutePathWithinSlingHome(final String relativePath) {
        return new File(slingHome, relativePath).getAbsolutePath();
    }

    /**
     * @see org.apache.sling.settings.SlingSettingsService#getSlingId()
     */
    public String getSlingId() {
        return this.slingId;
    }

    /**
     * @see org.apache.sling.settings.SlingSettingsService#getSlingHome()
     */
    public URL getSlingHome() {
        return this.slingHomeUrl;
    }

    /**
     * @see org.apache.sling.settings.SlingSettingsService#getSlingHomePath()
     */
    public String getSlingHomePath() {
        return this.slingHome;
    }

    /**
     * @see org.apache.sling.settings.SlingSettingsService#getRunModes()
     */
    public Set<String> getRunModes() {
        return this.runModes;
    }
}

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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

    /** The name of the data file holding install run mode options */
    private static final String OPTIONS_FILE = "sling.options.file";

    /**
     * Create the service and search the Sling home urls and
     * get/create a sling id.
     * Setup run modes
     * @param context The bundle context
     */
    public SlingSettingsServiceImpl(final BundleContext context) {
        this.setupSlingHome(context);
        final boolean isInstall = this.setupSlingId(context);
        
        // Detect if upgrading from a previous version (where OPTIONS_FILE did not exist),
        // as in terms of run modes this needs to be handled like an install
        final File options = context.getDataFile(OPTIONS_FILE);
        final boolean isUpgrade = !isInstall && !options.exists();
                
        logger.info("isInstall={}, isUpgrade={}", isInstall, isUpgrade);
        this.setupRunModes(context, isInstall, isUpgrade);

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
    private boolean setupSlingId(final BundleContext context) {
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
            return true;
        }
        return false;
    }

    private static final class Options implements Serializable {
        private static final long serialVersionUID = 1L;
        String[] modes;
        String   selected;
    }

    private List<Options> handleOptions(final Set<String> modesSet, final String propOptions) {
        if ( propOptions != null && propOptions.trim().length() > 0 ) {
            final List<Options> optionsList = new ArrayList<Options>();

            final String[] options = propOptions.trim().split("\\|");
            for(final String opt : options) {
                String selected = null;
                final String[] modes = opt.trim().split(",");
                for(int i=0; i<modes.length; i++) {
                    modes[i] = modes[i].trim();
                    if ( selected != null ) {
                        modesSet.remove(modes[i]);
                    } else {
                        if ( modesSet.contains(modes[i]) ) {
                            selected = modes[i];
                        }
                    }
                }
                if ( selected == null ) {
                    selected = modes[0];
                    modesSet.add(modes[0]);
                }
                final Options o = new Options();
                o.selected = selected;
                o.modes = modes;
                optionsList.add(o);
            }
            return optionsList;
        }
        return null;
    }

    /**
     * Set up run modes.
     */
    @SuppressWarnings("unchecked")
    private void setupRunModes(final BundleContext context,
            final boolean isInstall, final boolean isUpgrade) {
        final Set<String> modesSet = new HashSet<String>();

        // check configuration property first
        final String prop = context.getProperty(RUN_MODES_PROPERTY);
        if (prop != null && prop.trim().length() > 0) {
            final String[] modes = prop.split(",");
            for(int i=0; i < modes.length; i++) {
                modesSet.add(modes[i].trim());
            }
        }

        // now options
        this.handleOptions(modesSet, context.getProperty(RUN_MODE_OPTIONS));
        // now install options
        if ( isInstall || isUpgrade) {
            final List<Options> optionsList = this.handleOptions(modesSet, context.getProperty(RUN_MODE_INSTALL_OPTIONS));
            if ( optionsList != null ) {
                final File file = context.getDataFile(OPTIONS_FILE);
                FileOutputStream fos = null;
                ObjectOutputStream oos = null;
                try {
                    fos = new FileOutputStream(file);
                    oos = new ObjectOutputStream(fos);
                    oos.writeObject(optionsList);
                } catch ( final IOException ioe ) {
                    throw new RuntimeException("Unable to write to options data file.", ioe);
                } finally {
                    if ( oos != null ) {
                        try { oos.close(); } catch ( final IOException ignore) {}
                    }
                    if ( fos != null ) {
                        try { fos.close(); } catch ( final IOException ignore) {}
                    }
                }
            }
        } else {
            final File file = context.getDataFile(OPTIONS_FILE);
            if ( file.exists() ) {
                List<Options> optionsList = null;
                FileInputStream fis = null;
                ObjectInputStream ois = null;
                try {
                    fis = new FileInputStream(file);
                    ois = new ObjectInputStream(fis);

                    optionsList = (List<Options>) ois.readObject();
                } catch ( final IOException ioe ) {
                    throw new RuntimeException("Unable to read from options data file.", ioe);
                } catch (ClassNotFoundException cnfe) {
                    throw new RuntimeException("Unable to read from options data file.", cnfe);
                } finally {
                    if ( ois != null ) {
                        try { ois.close(); } catch ( final IOException ignore) {}
                    }
                    if ( fis != null ) {
                        try { fis.close(); } catch ( final IOException ignore) {}
                    }
                }
                if ( optionsList != null ) {
                    for(final Options o : optionsList) {
                        for(final String m : o.modes) {
                            modesSet.remove(m);
                        }
                        modesSet.add(o.selected);
                    }
                }
            }
        }

        // make the set unmodifiable and synced
        // we probably don't need a synced set as it is read only
        this.runModes = Collections.synchronizedSet(Collections.unmodifiableSet(modesSet));
        if ( this.runModes.size() > 0 ) {
            logger.info("Active run modes: {}", this.runModes);
        } else {
            logger.info("No run modes active");
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
            } catch (final Throwable t) {
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
        } catch (final Throwable t) {
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

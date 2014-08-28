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

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.sling.launchpad.api.StartupHandler;
import org.apache.sling.launchpad.api.StartupMode;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the basic implementation of the sling settings service.
 */
public class SlingSettingsServiceImpl
    implements SlingSettingsService {

    /** Property containing the sling name. */
    private static final String SLING_NAME = "sling.name";

    /** Property containing the sling description. */
    private static final String SLING_DESCRIPTION = "sling.description";

    /** The logger */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The sling instance id. */
    private String slingId;

    /** The sling home */
    private String slingHome;

    /** The sling home url */
    private URL slingHomeUrl;

    /** The set of run modes .*/
    private Set<String> runModes;

    /** The name of the data file holding the sling id. */
    private static final String ID_FILE = "sling.id.file";

    /** The name of the data file holding install run mode options */
    private static final String OPTIONS_FILE = "sling.options.file";

    /** The length in bytes of a sling identifier */
    private static final int SLING_ID_LENGTH = 36;

    /** The properties for name, description. */
    private final Map<String, String> slingProps = new HashMap<String, String>();

    /**
     * Create the service and search the Sling home urls and
     * get/create a sling id.
     * Setup run modes
     * @param context The bundle context
     */
    public SlingSettingsServiceImpl(final BundleContext context,
            final StartupHandler handler) {
        this.setupSlingProps(context);
        this.setupSlingHome(context);
        this.setupSlingId(context);

        final StartupMode mode = handler.getMode();
        logger.debug("Settings: Using startup mode : {}", mode);

        this.setupRunModes(context, mode);

    }

    /**
     * Get sling home and sling home URL
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
        final File idFile = context.getDataFile(ID_FILE);
        if ( idFile == null ) {
            // the osgi framework does not support storing something in the file system
            throw new RuntimeException("Unable to read from bundle data file.");
        }
        this.slingId = this.readSlingId(idFile, SLING_ID_LENGTH);

        // no sling id yet or failure to read file: create an id and store
        if (slingId == null) {
            slingId = UUID.randomUUID().toString();
            this.writeSlingId(idFile, this.slingId);
        }
    }

    /**
     * Get / create sling id
     */
    private void setupSlingProps(final BundleContext context) {
        synchronized ( this.slingProps ) {
            if ( this.slingProps.get(SLING_NAME) == null && context.getProperty(SLING_NAME) != null ) {
                this.slingProps.put(SLING_NAME, context.getProperty(SLING_NAME));
            }
            if ( this.slingProps.get(SLING_DESCRIPTION) == null && context.getProperty(SLING_DESCRIPTION) != null ) {
                this.slingProps.put(SLING_DESCRIPTION, context.getProperty(SLING_DESCRIPTION));
            }
        }
    }

    static final class Options implements Serializable {
        private static final long serialVersionUID = 1L;
        String[] modes;
        String   selected;
    }

    private List<Options> handleOptions(final Set<String> modesSet, final String propOptions) {
        final List<Options> optionsList = new ArrayList<Options>();
        if ( propOptions != null && propOptions.trim().length() > 0 ) {

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
        }
        return optionsList;
    }

    /**
     * Set up run modes.
     */
    private void setupRunModes(final BundleContext context,
            final StartupMode startupMode) {
        final Set<String> modesSet = new HashSet<String>();

        // check configuration property first
        final String prop = context.getProperty(RUN_MODES_PROPERTY);
        if (prop != null && prop.trim().length() > 0) {
            final String[] modes = prop.split(",");
            for(int i=0; i < modes.length; i++) {
                modesSet.add(modes[i].trim());
            }
        }

        //  handle configured options
        this.handleOptions(modesSet, context.getProperty(RUN_MODE_OPTIONS));

        // handle configured install options
        if ( startupMode != StartupMode.INSTALL ) {
            // read persisted options if restart or update
            final List<Options> storedOptions = readOptions(context);
            if ( storedOptions != null ) {
                for(final Options o : storedOptions) {
                    for(final String m : o.modes) {
                        modesSet.remove(m);
                    }
                    modesSet.add(o.selected);
                }
            }
        }

        // now install options
        if ( startupMode != StartupMode.RESTART ) {
            // process new install options if install or update
            final List<Options> optionsList = this.handleOptions(modesSet, context.getProperty(RUN_MODE_INSTALL_OPTIONS));
            // and always save new install options
            writeOptions(context, optionsList);
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


    private List<Options> readOptions(final BundleContext context) {
        List<Options> optionsList = null;
        final File file = context.getDataFile(OPTIONS_FILE);
        if ( file.exists() ) {
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
        }
        return optionsList;
    }

    void writeOptions(final BundleContext context, final List<Options> optionsList) {
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

    /**
     * Read the id from a file.
     */
    String readSlingId(final File idFile, int maxLength) {
        if (idFile.exists() && idFile.length() >= maxLength) {
            DataInputStream dis = null;
            try {
                final byte[] rawBytes = new byte[maxLength];
                dis = new DataInputStream(new FileInputStream(idFile));
                dis.readFully(rawBytes);
                final String rawString = new String(rawBytes, "ISO-8859-1");

                // roundtrip to ensure correct format of UUID value
                final String id = UUID.fromString(rawString).toString();
                logger.debug("Got Sling ID {} from file {}", id, idFile);

                return id;
            } catch (final Throwable t) {
                logger.error("Failed reading UUID from id file " + idFile
                        + ", creating new id", t);
            } finally {
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException ignore){}
                }
            }
        }
        return null;
    }

    /**
     * Write the sling id file.
     */
    void writeSlingId(final File idFile, final String id) {
        idFile.delete();
        idFile.getParentFile().mkdirs();
        DataOutputStream dos = null;
        try {
            final byte[] rawBytes = id.getBytes("ISO-8859-1");
            dos = new DataOutputStream(new FileOutputStream(idFile));
            dos.write(rawBytes, 0, rawBytes.length);
            dos.flush();
        } catch (final Throwable t) {
            logger.error("Failed writing UUID to id file " + idFile, t);
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException ignore) {}
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

    /**
     * @see org.apache.sling.settings.SlingSettingsService#getSlingName()
     */
    public String getSlingName() {
        synchronized ( this.slingProps ) {
            String name = this.slingProps.get(SLING_NAME);
            if ( name == null ) {
                name = "Instance " + this.slingId; // default
            }
            return name;
        }
    }

    /**
     * @see org.apache.sling.settings.SlingSettingsService#getSlingDescription()
     */
    public String getSlingDescription() {
        synchronized ( this.slingProps ) {
            String desc = this.slingProps.get(SLING_DESCRIPTION);
            if ( desc == null ) {
                desc = "Instance with id " + this.slingId + " and run modes " + this.getRunModes(); // default
            }
            return desc;
        }
    }

    /**
     * Update the configuration of this service
     */
    public void update(final Dictionary<String, Object> properties) {
        if ( properties != null ) {
            synchronized ( this.slingProps ) {
                if ( properties.get(SLING_NAME) != null ) {
                    this.slingProps.put(SLING_NAME, properties.get(SLING_NAME).toString());
                }
                if ( properties.get(SLING_DESCRIPTION) != null ) {
                    this.slingProps.put(SLING_DESCRIPTION, properties.get(SLING_DESCRIPTION).toString());
                }
            }
        }
    }
}

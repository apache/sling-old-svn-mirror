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
package org.apache.sling.engine.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.engine.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

/**
 * This is the basic implementation of the sling settings service.
 */
public class SlingSettingsServiceImpl
    implements SlingSettingsService, org.apache.sling.api.services.SlingSettingsService {

    /** The logger */
    private org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The sling instance id. */
    private String slingId;

    /** The sling home */
    private final String slingHome;

    /** The sling home url */
    private URL slingHomeUrl;

    public SlingSettingsServiceImpl(final BundleContext context) {
        this.slingHome = context.getProperty(SlingConstants.SLING_HOME);
        final String url = context.getProperty(SlingConstants.SLING_HOME_URL);
        if ( url != null ) {
            try {
                this.slingHomeUrl = new URL(url);
            } catch (MalformedURLException e) {
                logger.error("Sling home url is not a url: {}", url);
            }
        }
        // try to read the id from the id file first
        File idFile = context.getDataFile("sling.id.file");
        if ( idFile == null ) {
            // the osgi framework does not support storing something in the file system
            throw new RuntimeException("Unable to read from bundle data file.");
        }
        if (idFile.exists() && idFile.length() >= 36) {
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(idFile);
                byte[] rawBytes = new byte[36];
                if (fin.read(rawBytes) == 36) {
                    String rawString = new String(rawBytes, "ISO-8859-1");

                    // roundtrip to ensure correct format of UUID value
                    slingId = UUID.fromString(rawString).toString();
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

        // no sling id yet or failure to read file: create an id and store
        if (slingId == null) {
            slingId = UUID.randomUUID().toString();

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
    }

    /**
     * @see org.apache.sling.api.services.SlingSettingsService#getSlingId()
     */
    public String getSlingId() {
        return this.slingId;
    }

    /**
     * @see org.apache.sling.api.services.SlingSettingsService#getSlingHome()
     */
    public URL getSlingHome() {
        return this.slingHomeUrl;
    }

    /**
     * @see org.apache.sling.api.services.SlingSettingsService#getSlingHomePath()
     */
    public String getSlingHomePath() {
        return this.slingHome;
    }
}

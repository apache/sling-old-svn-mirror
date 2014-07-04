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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.apache.sling.launchpad.api.StartupHandler;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

public class SlingSettingsServiceImplTest {

    private static final String SLING_ID_FILE_NAME = "sling.id.file";

    private static final String OPTIONS_FILE_NAME = "sling.options.file";

    private static final String SLING_ID = "097bae9b-bf60-45a2-ad8c-ccdd374dd9b0";

    @Test
    public void testGetSlingId() throws Exception {
        Assert.assertEquals(36, SLING_ID.length());
        setGetSlignId(SLING_ID);
    }

    private void setGetSlignId(String id)
            throws IOException {
        File slingIdFile = null, optionsFile = null;
        try {
            slingIdFile = File.createTempFile(SLING_ID_FILE_NAME, "");
            optionsFile = File.createTempFile(OPTIONS_FILE_NAME, "");
            SlingSettingsServiceImpl settings = getSlingSettings(slingIdFile, optionsFile);
            settings.writeSlingId(slingIdFile, id);
            String slingId = settings.readSlingId(slingIdFile);
            Assert.assertNotNull(slingId);
            Assert.assertEquals(SLING_ID, slingId);
        } finally {
            if (slingIdFile != null ) {
                slingIdFile.delete();
            }
            if (optionsFile != null) {
                optionsFile.delete();
            }
        }
    }

    private SlingSettingsServiceImpl getSlingSettings(File slingIdFile, File optionsFile) throws IOException {
        BundleContext context = Mockito.mock(BundleContext.class);
        Mockito.when(context.getDataFile("sling.id.file"))
                .thenReturn(slingIdFile);
        Mockito.when(context.getDataFile("sling.options.file"))
                .thenReturn(optionsFile);
        StartupHandler handler = Mockito.mock(StartupHandler.class);
        // write options
        List<SlingSettingsServiceImpl.Options> options = new ArrayList<SlingSettingsServiceImpl.Options>();
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(optionsFile);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(options);
        } catch ( final IOException ioe ) {
            throw new RuntimeException("Unable to write to options data file.", ioe);
        } finally {
            if ( oos != null ) {
                try {
                    oos.close();
                } catch (IOException ignore) {
                    // ...
                }
            }
            if ( fos != null ) {
                try {
                    fos.close();
                } catch (IOException ignore) {
                    // ...
                }
            }
        }
        return new SlingSettingsServiceImpl(context, handler);
    }
}

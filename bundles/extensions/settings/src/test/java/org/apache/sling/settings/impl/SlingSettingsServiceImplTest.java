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

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.apache.sling.launchpad.api.StartupHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

public class SlingSettingsServiceImplTest {

    private static final String SLING_ID_FILE_NAME = "sling.id.file";

    private static final String OPTIONS_FILE_NAME = "sling.options.file";

    private static final String SLING_ID = "097bae9b-bf60-45a2-ad8c-ccdd374dd9b0";

    private File slingIdFile = null;

    private File optionsFile = null;

    @Before
    public void before() throws IOException {
        slingIdFile = File.createTempFile(
                SLING_ID_FILE_NAME, "");
        optionsFile = File.createTempFile(
                OPTIONS_FILE_NAME, "");
    }

    @After
    public void after() throws IOException {
        if (slingIdFile != null ) {
            slingIdFile.delete();
            slingIdFile = null;
        }
        if (optionsFile != null) {
            optionsFile.delete();
            optionsFile = null;
        }
    }

    @Test
    public void testGenerateSlingId()
            throws IOException {
        String slingId =  readSlingId(slingIdFile, optionsFile, SLING_ID.length());
        Assert.assertNotNull(slingId);
    }

    @Test
    public void testGetSlingId()
            throws IOException {
        writeSlingId(slingIdFile, optionsFile, SLING_ID);
        String generated =  readSlingId(slingIdFile, optionsFile, SLING_ID.length());
        Assert.assertNotNull(generated);
        Assert.assertEquals(SLING_ID, generated);
        String slingId = readSlingId(slingIdFile, optionsFile, SLING_ID.length());
        Assert.assertNotNull(slingId);
        Assert.assertEquals(generated, slingId);
    }

    @Test
    public void testGetLongSlingIdFromTooLargeData()
            throws IOException {
        String data = SLING_ID + RandomStringUtils.randomAscii(1024 * 1024); // 1MB long random String
        writeSlingId(slingIdFile, optionsFile, data);
        String slingId =  readSlingId(slingIdFile, optionsFile, SLING_ID.length());
        Assert.assertNotNull(slingId);
        Assert.assertEquals(SLING_ID, slingId);
    }

    @Test
    public void testGetSlingIdFromTooShortData()
            throws IOException {
        String data = RandomStringUtils.randomAscii(8); // 8 byte long string
        writeSlingId(slingIdFile, optionsFile, data);
        String slingId =  readSlingId(slingIdFile, optionsFile, SLING_ID.length());
        Assert.assertNotNull(slingId);
        Assert.assertNotEquals(SLING_ID, slingId);
    }

    private String readSlingId(File slingIdFile, File optionsFile, int maxLength)
            throws IOException {
        SlingSettingsServiceImpl settings = getSlingSettings(slingIdFile, optionsFile);
        return settings.readSlingId(slingIdFile, maxLength);
    }

    private void writeSlingId(File slingIdFile, File optionsFile, String slingId)
            throws IOException {
        SlingSettingsServiceImpl settings = getSlingSettings(slingIdFile, optionsFile);
        settings.writeSlingId(slingIdFile, slingId);
    }

    private SlingSettingsServiceImpl getSlingSettings(File slingIdFile, File optionsFile)
            throws IOException {
        BundleContext context = Mockito.mock(BundleContext.class);
        Mockito.when(context.getDataFile(SLING_ID_FILE_NAME))
                .thenReturn(slingIdFile);
        Mockito.when(context.getDataFile(OPTIONS_FILE_NAME))
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

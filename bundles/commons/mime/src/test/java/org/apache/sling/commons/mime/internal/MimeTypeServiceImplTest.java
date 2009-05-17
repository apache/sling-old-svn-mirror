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
package org.apache.sling.commons.mime.internal;

import java.io.IOException;
import java.io.InputStream;

import org.apache.sling.commons.mime.MimeTypeProvider;
import org.apache.sling.commons.mime.internal.MimeTypeServiceImpl;

import junit.framework.TestCase;

/**
 * The <code>MimeTypeServiceImplTest</code> TODO
 */
public class MimeTypeServiceImplTest extends TestCase {

    private static final String IMAGE_GIF = "image/gif";

    private static final String GIF = "gif";

    private static final String UNMAPPED_GIF = "unmapped_gif";

    private static final String LOG = "log";

    private static final String TXT = "txt";

    private static final String APT = "apt";

    private static final String TEXT_PLAIN = "text/plain";

    private MimeTypeServiceImpl service;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.service = new MimeTypeServiceImpl();
    }

    @Override
    protected void tearDown() throws Exception {
        this.service = null;

        super.tearDown();
    }

    public void testNoMapping() throws Exception {
        assertNull(this.service.getMimeType("file." + TXT));
        assertNull(this.service.getMimeType(TXT));
        assertNull(this.service.getExtension(TEXT_PLAIN));
    }

    public void testTxtMapping() throws Exception {

        this.service.registerMimeType(TEXT_PLAIN, TXT, LOG, APT);

        final String [] exts = { TXT, LOG, APT };
        for(String ext : exts) {
            assertEquals("Extension " + ext + " (1)", TEXT_PLAIN, this.service.getMimeType("file." + ext));
            assertEquals("Extension " + ext + " (2)", TEXT_PLAIN, this.service.getMimeType(ext));
        }

        assertEquals(TEXT_PLAIN,
            this.service.getMimeType(("file." + TXT).toUpperCase()));
        assertEquals(TEXT_PLAIN, this.service.getMimeType((TXT).toUpperCase()));
        assertEquals(TEXT_PLAIN,
            this.service.getMimeType(("file." + LOG).toUpperCase()));
        assertEquals(TEXT_PLAIN, this.service.getMimeType((LOG).toUpperCase()));

        assertNull(this.service.getMimeType(GIF));

        assertEquals(TXT, this.service.getExtension(TEXT_PLAIN));
    }

    public void testFileLoading() throws Exception {
        loadMimeTypes(MimeTypeServiceImpl.CORE_MIME_TYPES);
        loadMimeTypes(MimeTypeServiceImpl.MIME_TYPES);

        final String [] exts = { TXT, LOG, APT };
        for(String ext : exts) {
            assertEquals("Extension " + ext + " (1)", TEXT_PLAIN, this.service.getMimeType("file." + ext));
            assertEquals("Extension " + ext + " (2)", TEXT_PLAIN, this.service.getMimeType(ext));
        }

        assertEquals(TEXT_PLAIN,
            this.service.getMimeType(("file." + TXT).toUpperCase()));
        assertEquals(TEXT_PLAIN, this.service.getMimeType((TXT).toUpperCase()));
        assertEquals(TEXT_PLAIN,
            this.service.getMimeType(("file." + LOG).toUpperCase()));
        assertEquals(TEXT_PLAIN, this.service.getMimeType((LOG).toUpperCase()));

        assertEquals(IMAGE_GIF, this.service.getMimeType(GIF));
        assertNull(this.service.getMimeType(UNMAPPED_GIF));

        assertEquals(TXT, this.service.getExtension(TEXT_PLAIN));
    }

    public void testProvider() throws Exception {
        MimeTypeProvider mtp = this.createMimeTypeProvider(IMAGE_GIF, GIF);

        assertNull(this.service.getMimeType(GIF));
        assertNull(this.service.getExtension(IMAGE_GIF));

        this.service.bindMimeTypeProvider(mtp);

        assertEquals(IMAGE_GIF, this.service.getMimeType(GIF));
        assertEquals(GIF, this.service.getExtension(IMAGE_GIF));

        this.service.unbindMimeTypeProvider(mtp);

        assertNull(this.service.getMimeType(GIF));
        assertNull(this.service.getExtension(IMAGE_GIF));
    }

    public void testProvider2() throws Exception {
        MimeTypeProvider mtp = this.createMimeTypeProvider(IMAGE_GIF, GIF);

        this.service.registerMimeType(IMAGE_GIF, UNMAPPED_GIF);

        assertEquals(IMAGE_GIF, this.service.getMimeType(UNMAPPED_GIF));
        assertEquals(UNMAPPED_GIF, this.service.getExtension(IMAGE_GIF));

        assertNull(this.service.getMimeType(GIF));

        this.service.bindMimeTypeProvider(mtp);

        assertEquals(IMAGE_GIF, this.service.getMimeType(UNMAPPED_GIF));
        assertEquals(UNMAPPED_GIF, this.service.getExtension(IMAGE_GIF));

        assertEquals(IMAGE_GIF, this.service.getMimeType(GIF));

        this.service.unbindMimeTypeProvider(mtp);

        assertEquals(IMAGE_GIF, this.service.getMimeType(UNMAPPED_GIF));
        assertEquals(UNMAPPED_GIF, this.service.getExtension(IMAGE_GIF));

        assertNull(this.service.getMimeType(GIF));
    }

    private MimeTypeProvider createMimeTypeProvider(final String type, final String ext) {
        return new MimeTypeProvider() {
            public String getMimeType(String name) {
                if (name == null) {
                    return null;
                } else if (name.toLowerCase().endsWith(ext)) {
                    return type;
                } else {
                    return null;
                }
            }

            public String getExtension(String mimeType) {
                if (mimeType == null) {
                    return null;
                } else if (mimeType.toLowerCase().equals(type)) {
                    return ext;
                } else {
                    return null;
                }
            }
        };

    }
    
    private void loadMimeTypes(String path) throws IOException {
        InputStream ins = this.getClass().getResourceAsStream(path);
        assertNotNull(ins);

        try {
            this.service.registerMimeType(ins);
        } finally {
            try {
                ins.close();
            } catch (IOException ignore) {
            }
        }

    }
}

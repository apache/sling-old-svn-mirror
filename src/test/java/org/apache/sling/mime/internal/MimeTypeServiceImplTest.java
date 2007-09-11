/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.mime.internal;

import java.io.IOException;
import java.io.InputStream;

import org.apache.sling.mime.MimeTypeProvider;

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

    private static final String TEXT_PLAIN = "text/plain";

    private MimeTypeServiceImpl service;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        service = new MimeTypeServiceImpl();
    }

    @Override
    protected void tearDown() throws Exception {
        service = null;

        super.tearDown();
    }

    public void testNoMapping() throws Exception {
        assertNull(service.getMimeType("file." + TXT));
        assertNull(service.getMimeType(TXT));
        assertNull(service.getExtension(TEXT_PLAIN));
    }

    public void testTxtMapping() throws Exception {

        service.registerMimeType(TEXT_PLAIN, TXT, LOG);

        assertEquals(TEXT_PLAIN, service.getMimeType("file." + TXT));
        assertEquals(TEXT_PLAIN, service.getMimeType(TXT));
        assertEquals(TEXT_PLAIN, service.getMimeType("file." + LOG));
        assertEquals(TEXT_PLAIN, service.getMimeType(LOG));

        assertEquals(TEXT_PLAIN,
            service.getMimeType(("file." + TXT).toUpperCase()));
        assertEquals(TEXT_PLAIN, service.getMimeType((TXT).toUpperCase()));
        assertEquals(TEXT_PLAIN,
            service.getMimeType(("file." + LOG).toUpperCase()));
        assertEquals(TEXT_PLAIN, service.getMimeType((LOG).toUpperCase()));

        assertNull(service.getMimeType(GIF));

        assertEquals(TXT, service.getExtension(TEXT_PLAIN));
    }

    public void testFileLoading() throws Exception {
        InputStream ins = getClass().getResourceAsStream("/META-INF/mime.types");
        assertNotNull(ins);

        try {
            service.registerMimeType(ins);

            assertEquals(TEXT_PLAIN, service.getMimeType("file." + TXT));
            assertEquals(TEXT_PLAIN, service.getMimeType(TXT));
            assertEquals(TEXT_PLAIN, service.getMimeType("file." + LOG));
            assertEquals(TEXT_PLAIN, service.getMimeType(LOG));

            assertEquals(TEXT_PLAIN,
                service.getMimeType(("file." + TXT).toUpperCase()));
            assertEquals(TEXT_PLAIN, service.getMimeType((TXT).toUpperCase()));
            assertEquals(TEXT_PLAIN,
                service.getMimeType(("file." + LOG).toUpperCase()));
            assertEquals(TEXT_PLAIN, service.getMimeType((LOG).toUpperCase()));

            assertEquals(IMAGE_GIF, service.getMimeType(GIF));
            assertNull(service.getMimeType(UNMAPPED_GIF));

            assertEquals(TXT, service.getExtension(TEXT_PLAIN));

        } finally {
            try {
                ins.close();
            } catch (IOException ignore) {
            }
        }
    }

    public void testProvider() throws Exception {
        MimeTypeProvider mtp = createMimeTypeProvider(IMAGE_GIF, GIF);
        
        assertNull(service.getMimeType(GIF));
        assertNull(service.getExtension(IMAGE_GIF));
        
        service.bindMimeTypeProvider(mtp);
        
        assertEquals(IMAGE_GIF, service.getMimeType(GIF));
        assertEquals(GIF, service.getExtension(IMAGE_GIF));
        
        service.unbindMimeTypeProvider(mtp);
        
        assertNull(service.getMimeType(GIF));
        assertNull(service.getExtension(IMAGE_GIF));
    }
    
    public void testProvider2() throws Exception {
        MimeTypeProvider mtp = createMimeTypeProvider(IMAGE_GIF, GIF);

        service.registerMimeType(IMAGE_GIF, UNMAPPED_GIF);
        
        assertEquals(IMAGE_GIF, service.getMimeType(UNMAPPED_GIF));
        assertEquals(UNMAPPED_GIF, service.getExtension(IMAGE_GIF));
        
        assertNull(service.getMimeType(GIF));

        service.bindMimeTypeProvider(mtp);

        assertEquals(IMAGE_GIF, service.getMimeType(UNMAPPED_GIF));
        assertEquals(UNMAPPED_GIF, service.getExtension(IMAGE_GIF));

        assertEquals(IMAGE_GIF, service.getMimeType(GIF));

        service.unbindMimeTypeProvider(mtp);

        assertEquals(IMAGE_GIF, service.getMimeType(UNMAPPED_GIF));
        assertEquals(UNMAPPED_GIF, service.getExtension(IMAGE_GIF));

        assertNull(service.getMimeType(GIF));
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
}

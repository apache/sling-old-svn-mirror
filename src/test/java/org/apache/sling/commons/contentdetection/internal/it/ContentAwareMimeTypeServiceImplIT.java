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

package org.apache.sling.commons.contentdetection.internal.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.sling.commons.contentdetection.ContentAwareMimeTypeService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class ContentAwareMimeTypeServiceImplIT {

    @Inject
    private ContentAwareMimeTypeService contentAwaremimeTypeService;
    
    class NonMarkableStream extends BufferedInputStream {
        NonMarkableStream(InputStream is) {
            super(is);
        }

        @Override
        public synchronized void mark(int readlimit) {
        }

        @Override
        public synchronized void reset() throws IOException {
        }

        @Override
        public boolean markSupported() {
            return false;
        }
    };
    
    abstract class AssertDetect {
        void assertDetection(String expectedType, boolean expectSameContent) throws IOException {
            final String filename = "this-is-actually-a-wav-file.mp3";
            final String path = "/" + filename;
            final InputStream s = wrapStream(getClass().getResourceAsStream(path));
            assertNotNull("Expecting stream to be found:" + filename, s);
            InputStream originalStream = null;
            try {
                assertEquals(expectedType, contentAwaremimeTypeService.getMimeType(filename, s));
                originalStream = getClass().getResourceAsStream(path);
                assertNotNull("Expecting stream to be found:" + filename, originalStream);
                if(expectSameContent) {
                    assertTrue("Expecting content to be unchanged", IOUtils.contentEquals(s, originalStream));
                } else {
                    assertFalse("Expecting content to have changed", IOUtils.contentEquals(s, originalStream));
                }
            } finally {
                IOUtils.closeQuietly(s);
                IOUtils.closeQuietly(originalStream);
            }
        }
        
        abstract InputStream wrapStream(InputStream toWrap);
    }

    @Test
    public void detectFromExtension(){
        String mimeTypeName = "test.mp3";
        String mimeType = "audio/mpeg";
        assertEquals(mimeType, contentAwaremimeTypeService.getMimeType(mimeTypeName));
    }

    @Test
    public void detectFromContent() throws IOException{
        new AssertDetect() {
            @Override
            InputStream wrapStream(InputStream toWrap) {
                return new BufferedInputStream(toWrap);
            }
        }.assertDetection("audio/x-wav", true);
    }
    
    @Test
    public void detectFromContentWithNonMarkableStream() throws IOException{
        // Interestingly, with a non-markable stream  the detector falls back to
        // filename detection but still touches the content stream
        new AssertDetect() {
            @Override
            InputStream wrapStream(InputStream toWrap) {
                return new NonMarkableStream(toWrap);
            }
        }.assertDetection("audio/mpeg", false);
    }
    
    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return U.paxConfig();
    }
}
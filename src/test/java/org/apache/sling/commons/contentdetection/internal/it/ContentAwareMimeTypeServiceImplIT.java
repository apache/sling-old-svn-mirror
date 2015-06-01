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

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.apache.sling.commons.contentdetection.ContentAwareMimeTypeService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class ContentAwareMimeTypeServiceImplIT {

    @Inject
    private ContentAwareMimeTypeService contentAwaremimeTypeService;

    @Test
    public void detectFromExtension(){
        String mimeTypeName = "test.mp3";
        String mimeType = "audio/mpeg";
        assertEquals(mimeType, contentAwaremimeTypeService.getMimeType(mimeTypeName));
    }

    @Test
    public void detectFromContent() throws IOException{
        final String filename = "this-is-actually-a-wav-file.mp3";
        final InputStream s = getClass().getResourceAsStream("/" + filename);
        assertNotNull("Expecting stream to be found:" + filename, s);
        try {
            assertEquals("audio/x-wav", contentAwaremimeTypeService.getMimeType(filename, s));
        } finally {
            if(s != null) {
                s.close();
            }
        }
    }

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return U.paxConfig();
    }
}
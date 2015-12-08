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
package org.apache.sling.testing.samples.bundlewit;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.testing.samples.bundlewit.api.ResourceMimeTypeDetector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Server-side test of the ResourceMimeTypeDetector
 *  provided by this bundle */
public class ResourceMimeTypeDetectorIT {

    private ResourceMimeTypeDetector detector;
    
    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "BWIT_Teleporter");

    @Before
    public void setup() {
        detector = teleporter.getService(ResourceMimeTypeDetector.class); 
    }
    
    @Test
    public void textHtml() throws IOException {
        assertEquals("text/html", detector.getMimeType(new MockResource("/someResource.html")));
    }
    
    @Test
    public void nullMimeType() throws IOException {
        assertEquals(null, detector.getMimeType(new MockResource("/someResourceWithNoExtension")));
    }
}
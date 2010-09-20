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
package org.apache.sling.installer.provider.jcr.impl;

import org.apache.sling.installer.provider.jcr.impl.FileNodeConverter;

import junit.framework.TestCase;

public class FileNodeConverterTest extends TestCase {
    private final FileNodeConverter fc = new FileNodeConverter();
    
    public void testAcceptedFilenames() {
        final String [] filenames = {
                "a.cfg",
                "a.jar",
                "a.properties",
                "longername.cfg",
                "someproperties.properties",
                "longername-with_various.things-And-CASES-9123456789.cfg",
                "abc.cfg",
                "a.b.c.cfg",
                "1-2-3.cfg"
        };
        
        for(String f : filenames) {
            assertTrue("Filename must be accepted: '" + f + "'", fc.acceptNodeName(f));
        }
    }
    
    public void testRejectedFilenames() {
        final String [] filenames = {
                "a.js",
                "a.config",
                "a.sh",
                "a.bat",
                ".DS_Store",
                ".DS_Store.cfg",
                ".something.cfg",
                "noextension",
        };
        
        for(String f : filenames) {
            assertFalse("Filename must be rejected: '" + f + "'", fc.acceptNodeName(f));
        }
    }
}

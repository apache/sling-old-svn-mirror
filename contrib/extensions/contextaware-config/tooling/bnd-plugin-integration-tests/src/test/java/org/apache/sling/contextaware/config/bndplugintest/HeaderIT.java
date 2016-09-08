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
package org.apache.sling.contextaware.config.bndplugintest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Manifest;

import org.junit.Test;

public class HeaderIT {

    private static final String CONFIGURATION_CLASSES_HEADER = "Sling-ContextAware-Configuration-Classes";
    
    @Test
    public void testBundleHeader() throws Exception {
        
        try (InputStream is = new FileInputStream("target/classes/META-INF/MANIFEST.MF")) {
            Manifest manifest = new Manifest(is);
            String classesHeader = manifest.getMainAttributes().getValue(CONFIGURATION_CLASSES_HEADER);
            
            Set<String> classNames = new HashSet<>(Arrays.asList(classesHeader.split(",")));
            
            assertTrue(classNames.contains("org.apache.sling.contextaware.config.bndplugintest.MetadataSimpleConfig"));
            assertTrue(classNames.contains("org.apache.sling.contextaware.config.bndplugintest.NestedConfig"));
            assertTrue(classNames.contains("org.apache.sling.contextaware.config.bndplugintest.SimpleConfig"));
            assertFalse(classNames.contains("org.apache.sling.contextaware.config.bndplugintest.WithoutAnnotationConfig"));
        }
        
    }
    
}

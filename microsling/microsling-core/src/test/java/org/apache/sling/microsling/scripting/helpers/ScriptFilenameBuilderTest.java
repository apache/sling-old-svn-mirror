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
package org.apache.sling.microsling.scripting.helpers;

import junit.framework.TestCase;

/** Test the ScriptFilenameBuilder */
public class ScriptFilenameBuilderTest extends TestCase {
    
    private ScriptFilenameBuilder builder;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        builder = new ScriptFilenameBuilder();
    }

    public void testNull() {
        assertEquals("NO_METHOD.NO_EXT",builder.buildScriptFilename(null,null,null));
    }
    
    public void testSimpleGet() {
        assertEquals("html.js",builder.buildScriptFilename("GET","html","js"));
    }
    
    public void testGetAndNulls() {
        assertEquals("GET.NO_EXT",builder.buildScriptFilename("GET",null,null));
    }
    
    public void testSimpleHead() {
        assertEquals("HEAD.js",builder.buildScriptFilename("HEAD","text/html","js"));
    }
    
    public void testSimplePost() {
        assertEquals("POST.js",builder.buildScriptFilename("POST","html","js"));
    }
    
    public void testSimplePut() {
        assertEquals("PUT.js",builder.buildScriptFilename("PUT","html","js"));
    }
    
    public void testSimpleDelete() {
        assertEquals("DELETE.js",builder.buildScriptFilename("DELETE","html","js"));
    }
    
    public void testGetTextMimeType() {
        assertEquals("txt.js",builder.buildScriptFilename("GET","txt","js"));
    }
    
    public void testGetWhateverMimeType() {
        assertEquals("whatever.js",builder.buildScriptFilename("GET","whatever","js"));
    }
    
    public void testGetNoSlashMimeType() {
        assertEquals("bar.js",builder.buildScriptFilename("GET","bar","js"));
    }
    
    public void testCaseCleanupOne() {
        assertEquals("POST.js",builder.buildScriptFilename("posT","html","jS"));
    }
    
    public void testCaseCleanupTwo() {
        assertEquals("html.js",builder.buildScriptFilename("get","HTML","JS"));
    }
}

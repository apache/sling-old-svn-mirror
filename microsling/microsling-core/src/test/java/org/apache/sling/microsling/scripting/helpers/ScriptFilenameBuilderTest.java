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
        assertEquals("NO_METHOD.NO_EXT",builder.buildScriptFilename(null,null,null,null));
    }
    
    public void testSimpleGet() {
        assertEquals("html.js",builder.buildScriptFilename("GET",null,"text/html","js"));
    }
    
    public void testGetAndNulls() {
        assertEquals("NO_CONTENT_TYPE.NO_EXT",builder.buildScriptFilename("GET",null,null,null));
    }
    
    public void testSimpleHead() {
        assertEquals("HEAD.js",builder.buildScriptFilename("HEAD",null,"text/html","js"));
    }
    
    public void testGetOneSelector() {
        assertEquals("print/html.js",builder.buildScriptFilename("GET","print","text/html","js"));
    }
    
    public void testGetTwoSelectors() {
        assertEquals("print/a4/xml.vlt",builder.buildScriptFilename("GET","print.a4","text/xml","vlt"));
    }
    
    public void testSimplePost() {
        assertEquals("POST.js",builder.buildScriptFilename("POST",null,"text/html","js"));
    }
    
    public void testSimplePut() {
        assertEquals("PUT.js",builder.buildScriptFilename("PUT",null,"text/html","js"));
    }
    
    public void testSimpleDelete() {
        assertEquals("DELETE.js",builder.buildScriptFilename("DELETE",null,"text/html","js"));
    }
    
    public void testGetTextMimeType() {
        assertEquals("plain.js",builder.buildScriptFilename("GET",null,"text/plain","js"));
    }
    
    public void testGetWhateverMimeType() {
        assertEquals("foo.js",builder.buildScriptFilename("GET",null,"whatever/Foo","js"));
    }
    
    public void testGetNoSlashMimeType() {
        assertEquals("bar.js",builder.buildScriptFilename("GET",null,"bar","js"));
    }
    
    public void testCaseCleanupOne() {
        assertEquals("POST.js",builder.buildScriptFilename("posT",null,"text/html","jS"));
    }
    
    public void testCaseCleanupTwo() {
        assertEquals("html.js",builder.buildScriptFilename("get",null,"text/HTML","JS"));
    }
    
    public void testCaseCleanupThree() {
        assertEquals("print/a4/html.js",builder.buildScriptFilename("get","PRInT.A4","text/HTML","JS"));
    }
}

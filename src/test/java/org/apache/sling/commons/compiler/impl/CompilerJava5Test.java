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
package org.apache.sling.commons.compiler.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;

import junit.framework.TestCase;

import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.compiler.CompilationResult;
import org.apache.sling.commons.compiler.CompilationUnit;
import org.apache.sling.commons.compiler.Options;

/**
 * Test case for java 5 support
 */
public class CompilerJava5Test extends TestCase
        implements ClassLoaderWriter {

    public void testJava5Support() throws Exception {
        String sourceFile = "Java5Test";

        CompilationUnit unit = createCompileUnit(sourceFile);
        final Options options = new Options();
        options.put(Options.KEY_SOURCE_VERSION, Options.VERSION_1_5);
        options.put(Options.KEY_CLASS_LOADER_WRITER, this);
        options.put(Options.KEY_CLASS_LOADER, this.getClass().getClassLoader());

        final CompilationResult result = new EclipseJavaCompiler().compile(new CompilationUnit[]{unit}, options);
        assertNotNull(result);
        assertNull(result.getErrors());
    }

    //--------------------------------------------------------< misc. helpers >

    private CompilationUnit createCompileUnit(final String sourceFile) throws Exception {
        return new CompilationUnit() {

            /**
             * @see org.apache.sling.commons.compiler.CompilationUnit#getMainClassName()
             */
            public String getMainClassName() {
                return "org.apache.sling.commons.compiler.test." + sourceFile;
            }

            /**
             * @see org.apache.sling.commons.compiler.CompilationUnit#getSource()
             */
            public Reader getSource() throws IOException {
                InputStream in = getClass().getClassLoader().getResourceAsStream(sourceFile);
                return new InputStreamReader(in, "UTF-8");
            }

            /**
             * @see org.apache.sling.commons.compiler.CompilationUnit#getLastModified()
             */
            public long getLastModified() {
                return 0;
            }
        };
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#delete(java.lang.String)
     */
    public boolean delete(String path) {
        return false;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getInputStream(java.lang.String)
     */
    public InputStream getInputStream(String path) throws IOException {
        return null;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getLastModified(java.lang.String)
     */
    public long getLastModified(String path) {
        return -1;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getOutputStream(java.lang.String)
     */
    public OutputStream getOutputStream(String path) {
        return new ByteArrayOutputStream();
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#rename(java.lang.String, java.lang.String)
     */
    public boolean rename(String oldPath, String newPath) {
        return false;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getClassLoader()
     */
    public ClassLoader getClassLoader() {
        return null;
    }
}

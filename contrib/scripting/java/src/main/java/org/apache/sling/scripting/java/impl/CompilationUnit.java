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
package org.apache.sling.scripting.java.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;


public class CompilationUnit
    implements org.apache.sling.commons.compiler.CompilationUnitWithSource {

    private final SlingIOProvider ioProvider;
    private final String className;
    private final String sourceFile;

    public CompilationUnit(final String sourceFile,
                           final String className,
                           final SlingIOProvider ioProvider) {
        this.className = className;
        this.sourceFile = sourceFile;
        this.ioProvider = ioProvider;
    }

    /**
     * @see org.apache.sling.commons.compiler.CompilationUnit#getMainClassName()
     */
    public String getMainClassName() {
        return this.className;
    }

    /**
     * @see org.apache.sling.commons.compiler.CompilationUnit#getSource()
     */
    public Reader getSource() throws IOException {
        InputStream fr = null;
        try {
            fr = ioProvider.getInputStream(this.sourceFile);
            return new InputStreamReader(fr, ioProvider.getOptions().getJavaEncoding());
        } catch (IOException e) {
            if ( fr != null ) {
                try { fr.close(); } catch (IOException ignore) {}
            }
            throw e;
        }
    }

    /**
     * @see org.apache.sling.commons.compiler.CompilationUnit#getLastModified()
     */
    public long getLastModified() {
        return this.ioProvider.lastModified(this.sourceFile);
    }


    /**
     * @see org.apache.sling.commons.compiler.CompilationUnitWithSource#getLastModified()
     */
    public String getFileName() {
        final int idx = this.sourceFile.lastIndexOf('/');
        if (idx == -1) {
            return this.sourceFile;
        } else {
            return this.sourceFile.substring(idx + 1);
        }
    }
}

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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.commons.compiler.ClassWriter;
import org.apache.sling.commons.compiler.CompileUnit;
import org.apache.sling.commons.compiler.CompilerEnvironment;
import org.apache.sling.commons.compiler.ErrorHandler;


public class CompilationUnit
    implements CompileUnit, CompilerEnvironment, ErrorHandler, ClassWriter {

    private final SlingIOProvider ioProvider;
    private final String className;
    private final String sourceFile;

    /** The list of compile errors - this is created lazily. */
    private List<CompilerError> errors;

    public CompilationUnit(String sourceFile,
                           String className,
                           SlingIOProvider ioProvider) {
        this.className = className;
        this.sourceFile = sourceFile;
        this.ioProvider = ioProvider;
    }

    /**
     * @see org.apache.sling.commons.compiler.CompileUnit#getSourceFileName()
     */
    public String getSourceFileName() {
        return getMainTypeName() + ".java";
    }

    /**
     * @see org.apache.sling.commons.compiler.CompileUnit#getSourceFileContents()
     */
    public char[] getSourceFileContents() {
        char[] result = null;
        InputStream fr = null;
        try {
            fr = ioProvider.getInputStream(this.sourceFile);
            final Reader reader = new BufferedReader(new InputStreamReader(fr, ioProvider.getOptions().getJavaEncoding()));
            try {
                char[] chars = new char[8192];
                StringBuilder buf = new StringBuilder();
                int count;
                while ((count = reader.read(chars, 0, chars.length)) > 0) {
                    buf.append(chars, 0, count);
                }
                result = new char[buf.length()];
                buf.getChars(0, result.length, result, 0);
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            this.onError(e.getMessage(), this.sourceFile, 0, 0);
        }
        return result;
    }

    /**
     * @see org.apache.sling.commons.compiler.CompileUnit#getMainTypeName()
     */
    public String getMainTypeName() {
        int dot = className.lastIndexOf('.');
        if (dot > 0) {
            return className.substring(dot + 1);
        }
        return className;
    }

    /**
     * @see org.apache.sling.commons.compiler.CompilerEnvironment#isPackage(java.lang.String)
     */
    public boolean isPackage(String result) {
        if (result.equals(this.className)) {
            return false;
        }
        String resourceName = result.replace('.', '/') + ".class";
        if ( resourceName.startsWith("/") ) {
            resourceName = resourceName.substring(1);
        }
        final InputStream is = this.ioProvider.getClassLoader().getResourceAsStream(resourceName);
        if ( is != null ) {
            try {
                is.close();
            } catch (IOException ignore) {}
        }
        return is == null;
    }

    /**
     * @see org.apache.sling.commons.compiler.CompilerEnvironment#findClass(java.lang.String)
     */
    public byte[] findClass(String name) throws Exception {
        final String resourceName = name.replace('.', '/') + ".class";
        final InputStream is = this.ioProvider.getClassLoader().getResourceAsStream(resourceName);
        if (is != null) {
            try {
                byte[] buf = new byte[8192];
                ByteArrayOutputStream baos = new ByteArrayOutputStream(buf.length);
                int count;
                while ((count = is.read(buf, 0, buf.length)) > 0) {
                    baos.write(buf, 0, count);
                }
                baos.flush();
                return baos.toByteArray();
            } finally {
                try {
                    is.close();
                } catch (IOException ignore) {}
            }
        }
        return null;
    }

    /**
     * @see org.apache.sling.commons.compiler.CompilerEnvironment#cleanup()
     */
    public void cleanup() {
        // EMPTY
    }

    /**
     * @see org.apache.sling.commons.compiler.ErrorHandler#onError(java.lang.String, java.lang.String, int, int)
     */
    public void onError(String msg, String sourceFile, int line, int position) {
        if ( errors == null ) {
            errors = new ArrayList<CompilerError>();
        }
        errors.add(new CompilerError(sourceFile, line, position, msg));
    }

    /**
     * @see org.apache.sling.commons.compiler.ErrorHandler#onWarning(java.lang.String, java.lang.String, int, int)
     */
    public void onWarning(String msg, String sourceFile, int line, int position) {
        // we ignore warnings
    }

    /**
     * @see org.apache.sling.commons.compiler.ClassWriter#write(java.lang.String, byte[])
     */
    public void write(String name, byte[] data) throws Exception {
        final OutputStream os = this.ioProvider.getOutputStream('/' + name.replace('.', '/') + ".class");
        os.write(data);
        os.close();
    }

    /** Return the list of errors. */
    public List<CompilerError> getErrors() throws IOException {
        return errors;
    }
}

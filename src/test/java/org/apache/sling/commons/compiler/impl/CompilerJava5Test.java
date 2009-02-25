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
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.sling.commons.compiler.ClassWriter;
import org.apache.sling.commons.compiler.CompileUnit;
import org.apache.sling.commons.compiler.CompilerEnvironment;
import org.apache.sling.commons.compiler.ErrorHandler;
import org.apache.sling.commons.compiler.Options;

import junit.framework.TestCase;

/**
 * Test case for java 5 support
 */
public class CompilerJava5Test extends TestCase
        implements CompilerEnvironment, ErrorHandler, ClassWriter {

    public void testJava5Support() throws Exception {
        String sourceFile = "Java5Test";

        CompileUnit unit = createCompileUnit(sourceFile);
        new EclipseJavaCompiler().compile(new CompileUnit[]{unit}, this, this, this, new Options(Options.VERSION_1_5, true));
    }

    //---------------------------------------------------------< ErrorHandler >

    public void onError(String msg, String sourceFile, int line, int position) {
        System.out.println("Error in " + sourceFile + ", line " + line + ", pos. " + position + ": " + msg);
    }

    public void onWarning(String msg, String sourceFile, int line, int position) {
        System.out.println("Warning in " + sourceFile + ", line " + line + ", pos. " + position + ": " + msg);
    }

    //--------------------------------------------------< CompilerEnvironment >

    public byte[] findClass(String className) throws Exception {
        System.out.println("findClass('" + className + "')");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = cl.getResourceAsStream(className.replace('.', '/') + ".class");
        if (in == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(0x7fff);

        try {
            byte[] buffer = new byte[0x1000];
            int read = 0;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            //out.close();
            in.close();
        }

        return out.toByteArray();
    }

    public char[] findSource(String className) throws Exception {
        System.out.println("findSource('" + className + "')");
        return new char[0];
    }

    public boolean isPackage(String packageName) {
        System.out.println("isPackage('" + packageName + "')");

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = cl.getResourceAsStream(packageName.replace('.', '/') + ".class");
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignore) {
            }
            return false;
        }
        return true;
    }

    public void cleanup() {
    }

    //----------------------------------------------------------< ClassWriter >

    public void write(String className, byte[] data) throws Exception {
        System.out.println("compiled class " + className + ", " + data.length + " bytes");
    }

    //--------------------------------------------------------< misc. helpers >

    private CompileUnit createCompileUnit(final String sourceFile) throws Exception {
        final char[] chars = readTextResource(sourceFile);

        return new CompileUnit() {

            public String getSourceFileName() {
                return sourceFile;
            }

            public char[] getSourceFileContents() {
                return chars;
            }

            public String getMainTypeName() {
                String className;
                int pos = sourceFile.lastIndexOf(".java");
                if (pos != -1) {
                    className = sourceFile.substring(0, pos).trim();
                } else {
                    className = sourceFile.trim();
                }
                pos = className.lastIndexOf('/');
                return (pos == -1) ? className : className.substring(pos);
            }
        };
    }

    private char[] readTextResource(String resourcePath) throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("resource not found");
        }
        Reader reader = new InputStreamReader(in);
        CharArrayWriter writer = new CharArrayWriter(0x7fff);
        try {
            char[] buffer = new char[0x1000];
            int read = 0;
            while ((read = reader.read(buffer)) > 0) {
                writer.write(buffer, 0, read);
            }
            return writer.toCharArray();
        } finally {
            //writer.close();
            reader.close();
        }
    }
}

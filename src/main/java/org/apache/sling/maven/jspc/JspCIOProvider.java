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
package org.apache.sling.maven.jspc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.compiler.JavaCompiler;
import org.apache.sling.scripting.jsp.jasper.IOProvider;

/**
 * Implements the IOProvider for the JSPC plugin
 */
public class JspCIOProvider implements IOProvider {

    private final ClassLoader loader;

    private final JavaCompiler compiler;

    private final ClassLoaderWriter writer;

    JspCIOProvider(ClassLoader loader, JavaCompiler compiler, ClassLoaderWriter writer) {
        this.loader = loader;
        this.compiler = compiler;
        this.writer = writer;
    }

    private File getFile(String fileName) {
        // TODO: sanity check to not write above project directory?
        return new File(fileName);
    }

    @Override
    public OutputStream getOutputStream(String fileName) throws IOException {
        return FileUtils.openOutputStream(getFile(fileName));
    }

    @Override
    public InputStream getInputStream(String fileName) throws FileNotFoundException, IOException {
        return FileUtils.openInputStream(getFile(fileName));
    }

    @Override
    public boolean delete(String fileName) {
        return getFile(fileName).delete();
    }

    @Override
    public boolean rename(String oldFileName, String newFileName) {
        return getFile(oldFileName).renameTo(getFile(newFileName));
    }

    @Override
    public boolean mkdirs(String path) {
        return getFile(path).mkdirs();
    }

    @Override
    public long lastModified(String fileName) {
        return getFile(fileName).lastModified();
    }

    @Override
    public ClassLoader getClassLoader() {
        return loader;
    }

    @Override
    public JavaCompiler getJavaCompiler() {
        return compiler;
    }

    @Override
    public ClassLoaderWriter getClassLoaderWriter() {
        return writer;
    }
}
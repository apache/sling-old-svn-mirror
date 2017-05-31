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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.sling.commons.classloader.ClassLoaderWriter;

/**
 * Implements the class loader writer
 */
public class JspCClassLoaderWriter implements ClassLoaderWriter {

    private final ClassLoader loader;

    private final File rootDirectory;

    JspCClassLoaderWriter(ClassLoader loader, File rootDirectory) {
        this.loader = loader;
        this.rootDirectory = rootDirectory;
    }

    private File getFile(String fileName) {
        return new File(rootDirectory, fileName);
    }

    @Override
    public OutputStream getOutputStream(String fileName) {
        try {
            return FileUtils.openOutputStream(getFile(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getInputStream(String fileName) throws IOException {
        return FileUtils.openInputStream(getFile(fileName));
    }

    @Override
    public long getLastModified(String fileName) {
        File file = getFile(fileName);
        if (file.exists()) {
            return file.lastModified();
        }
        return -1L;
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
    public ClassLoader getClassLoader() {
        return loader;
    }
}
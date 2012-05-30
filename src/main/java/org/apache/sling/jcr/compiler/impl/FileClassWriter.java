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
package org.apache.sling.jcr.compiler.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.sling.commons.classloader.ClassLoaderWriter;

class FileClassWriter implements ClassLoaderWriter {

    private final File outputDir;

    FileClassWriter(File outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getOutputStream(java.lang.String)
     */
    public OutputStream getOutputStream(final String path) {
        final String fileName;
        if ( path.startsWith("/") ) {
            fileName = path.substring(1);
        } else {
            fileName = path;
        }
        File classFile = new File(outputDir, fileName.replace('/', File.separatorChar));
        if (!classFile.getParentFile().exists()) {
            classFile.getParentFile().mkdirs();
        }
        try {
            return new FileOutputStream(classFile);
        } catch (IOException ioe) {
            return null;
        }
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#delete(java.lang.String)
     */
    public boolean delete(String path) {
        // we don't need to implement this one
        return false;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getInputStream(java.lang.String)
     */
    public InputStream getInputStream(String path) throws IOException {
        // we don't need to implement this one
        return null;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getLastModified(java.lang.String)
     */
    public long getLastModified(String path) {
        // we don't need to implement this one
        return 0;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#rename(java.lang.String, java.lang.String)
     */
    public boolean rename(String oldPath, String newPath) {
        // we don't need to implement this one
        return false;
    }

    /**
     * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getClassLoader()
     */
    public ClassLoader getClassLoader() {
        // we don't need to implement this one
        return null;
    }
}

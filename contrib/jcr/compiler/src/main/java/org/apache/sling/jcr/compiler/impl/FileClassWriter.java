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

import org.apache.sling.commons.compiler.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FileClassWriter implements ClassWriter {
    
    /** Logger instance */
    private static final Logger log = LoggerFactory.getLogger(FileClassWriter.class);

    private File outputDir;
    
    FileClassWriter(File outputDir) {
        this.outputDir = outputDir;
    }

    /* (non-Javadoc)
     * @see org.apache.sling.commons.compiler.ClassWriter#write(java.lang.String, byte[])
     */
    public void write(String className, byte[] data) throws Exception {
        File classFile = new File(outputDir, className.replace('.', '/') + ".class");
        if (!classFile.getParentFile().exists()) {
            classFile.getParentFile().mkdirs();
        }
        FileOutputStream out = new FileOutputStream(classFile);
        boolean succeeded = false;
        try {
            out.write(data);
            succeeded = true;
        } catch (IOException e) {
            log.error("Failed to persist " + className + " at path " + classFile.getPath(), e);
            // re-throw
            throw e;
        } finally {
            try {
                out.close();
            } catch (IOException ignore) {
            }
            if (!succeeded) {
                classFile.delete();
            }
        }
    }
}

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

import java.io.ByteArrayInputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.commons.compiler.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JcrClassWriter implements ClassWriter {
    
    /** Logger instance */
    private static final Logger log = LoggerFactory.getLogger(JcrClassWriter.class);

    private Node outputFolder;
    
    JcrClassWriter(Node outputFolder) {
        this.outputFolder = outputFolder;
    }

    /* (non-Javadoc)
     * @see org.apache.sling.commons.compiler.ClassWriter#write(java.lang.String, byte[])
     */
    public void write(String className, byte[] data) throws Exception {
        synchronized (outputFolder.getSession()) {
            boolean succeeded = false;
            try {
                Node folder = outputFolder;
                String[] names = className.split("\\.");
                for (int i = 0; i < names.length - 1; i++) {
                    if (folder.hasNode(names[i])) {
                        folder = folder.getNode(names[i]);
                    } else {
                        folder = folder.addNode(names[i], "nt:folder");
                    }
                }
                String classFileName = names[names.length - 1] + ".class";
                if (folder.hasNode(classFileName)) {
                    folder.getNode(classFileName).remove();
                }
                Node file = folder.addNode(classFileName, "nt:file");
                Node content = file.addNode("jcr:content", "nt:resource");

                content.setProperty("jcr:mimeType", "application/octet-stream");
                content.setProperty("jcr:data", new ByteArrayInputStream(data));
                content.setProperty("jcr:lastModified", Calendar.getInstance());
                succeeded = true;
            } catch (RepositoryException e) {
                String p = outputFolder.getPath() + "/" + className.replace('.', '/') + ".class";
                log.error("Failed to persist " + className + " at path " + p, e);
                // re-throw
                throw e;
            } finally {
                if (succeeded) {
                    outputFolder.save();
                } else {
                    outputFolder.refresh(false);
                }
            }
        }
    }
}

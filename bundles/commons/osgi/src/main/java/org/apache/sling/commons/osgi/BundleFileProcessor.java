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
package org.apache.sling.commons.osgi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * The <code>BundleFileProcessor</code> can transform a bundle Manifest
 * by creating a modified copy of the bundle file. 
 * @since 2.4
 */
public abstract class BundleFileProcessor {
    
    private final File input;
    private final File outputFolder;
    
    public BundleFileProcessor(File input, File outputFolder) {
        this.input = input;
        this.outputFolder = outputFolder;
    }
    
    /** 
     * Process the bundle Manifest. Can return the original
     * one if no changes are needed.
     * @param originalManifest The manifest to process
     * @return The processed manifest.
     */
    protected abstract Manifest processManifest(Manifest originalManifest);

    /** 
     * Return the filename to use for the newly created bundle file 
     * @param inputJarManifest The manifest
     * @return The filename
     */
    protected abstract String getTargetFilename(Manifest inputJarManifest);
    
    /**
     * Creates a new OSGi Bundle from a given bundle, processing its manifest
     * using the processManifest method.
     * @return The new bundle file
     * @throws IOException If something goes wrong reading or writing.
     */
    public File process() throws IOException {
        JarInputStream jis = null;
        try {
            jis = new JarInputStream(new FileInputStream(input));
            Manifest oldMF = jis.getManifest();
            Manifest newMF = processManifest(oldMF);
            File newBundle = new File(outputFolder, getTargetFilename(oldMF));

            JarOutputStream jos = null;
            try {
                jos = new JarOutputStream(new FileOutputStream(newBundle), newMF);
                JarEntry je = null;
                while ((je = jis.getNextJarEntry()) != null) {
                    try {
                        jos.putNextEntry(je);
                        if (!je.isDirectory())
                            pumpStream(jis, jos);
                    } finally {
                        jos.closeEntry();
                        jis.closeEntry();;
                    }
                }
            } finally {
                if(jos != null) {
                    jos.close();
                }
            }
            return newBundle;
        } finally {
            if(jis != null) {
                jis.close();
            }
        }
    }

    static void pumpStream(InputStream is, OutputStream os) throws IOException {
        byte[] bytes = new byte[65536];

        int length = 0;
        int offset = 0;

        while ((length = is.read(bytes, offset, bytes.length - offset)) != -1) {
            offset += length;

            if (offset == bytes.length) {
                os.write(bytes, 0, bytes.length);
                offset = 0;
            }
        }
        if (offset != 0) {
            os.write(bytes, 0, offset);
        }
    }
}

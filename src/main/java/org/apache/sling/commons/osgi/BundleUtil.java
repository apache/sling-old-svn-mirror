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
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * The <code>BundleUtil</code> is a utility class providing some
 * useful utility methods for bundle handling.
 * @since 2.4
 */
public class BundleUtil {
    /**
     * Creates a new OSGi Bundle from a given bundle with the only difference that the
     * symbolic name is changed. The original symbolic name is recorded in the Manifest
     * using the {@code X-Original-Bundle-SymbolicName} header.
     * @param bundleFile The original bundle file. This file will not be modified.
     * @param newBSN The new Bundle-SymbolicName
     * @param tempDir The temporary directory to use. This is where the new bundle will be
     * written. This directory must exist.
     * @return The new bundle with the altered Symbolic Name.
     * @throws IOException If something goes wrong reading or writing.
     */
    public static File renameBSN(File bundleFile, String newBSN, File tempDir) throws IOException {
        try (JarInputStream jis = new JarInputStream(new FileInputStream(bundleFile))) {
            Manifest inputMF = jis.getManifest();

            Attributes inputAttrs = inputMF.getMainAttributes();
            String bver = inputAttrs.getValue("Bundle-Version");
            String orgBSN = inputAttrs.getValue("Bundle-SymbolicName");
            if (bver == null)
                bver = "0.0.0";

            File newBundle = new File(tempDir, newBSN + "-" + bver + ".jar");

            Manifest newMF = new Manifest(inputMF);
            Attributes outputAttrs = newMF.getMainAttributes();
            outputAttrs.putValue("Bundle-SymbolicName", newBSN);
            outputAttrs.putValue("X-Original-Bundle-SymbolicName", orgBSN);

            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(newBundle), newMF)) {
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
            }

            return newBundle;
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

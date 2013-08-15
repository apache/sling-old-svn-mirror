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
package org.apache.sling.tooling.support.install.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prototype for installing/updating a bundle from a directory
 */
@Component
@Service(value = Servlet.class)
@Property(name="alias", value="/system/sling/tooling/install")
public class InstallServlet extends HttpServlet {

    private static final long serialVersionUID = -8820366266126231409L;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String DIR = "dir";

    private BundleContext bundleContext;

    @Activate
    protected void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        final String dirPath = req.getParameter(DIR);
        if ( dirPath == null ) {
            logger.error("No dir parameter specified : {}", req.getParameterMap());
            resp.setStatus(500);
            return;
        }
        final File dir = new File(dirPath);
        if ( dir.exists() && dir.isDirectory() ) {
            logger.info("Checking dir {} for bundle install", dir);
            final File manifestFile = new File(dir, JarFile.MANIFEST_NAME);
            if ( manifestFile.exists() ) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(manifestFile);
                    final Manifest mf = new Manifest(fis);

                    final String symbolicName = mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
                    if ( symbolicName != null ) {
                        // search bundle
                        Bundle found = null;
                        for(final Bundle b : this.bundleContext.getBundles() ) {
                            if ( symbolicName.equals(b.getSymbolicName()) ) {
                                found = b;
                                break;
                            }
                        }

                        final File tempFile = File.createTempFile(dir.getName(), "bundle");
                        try {
                            createJar(dir, tempFile, mf);

                            final InputStream in = new FileInputStream(tempFile);
                            try {
                                if ( found != null ) {
                                    // update
                                    found.update(in);
                                } else {
                                    // install
                                    final Bundle b = bundleContext.installBundle(dir.getAbsolutePath(), in);
                                    b.start();
                                }
                                resp.setStatus(200);
                                return;
                            } catch ( final BundleException be ) {
                                logger.info("Unable to install/update bundle from dir " + dir, be);
                            }
                        } finally {
                            tempFile.delete();
                        }
                    } else {
                        logger.info("Manifest in {} does not have a symbolic name", dir);
                    }
                } finally {
                    if ( fis != null ) {
                        fis.close();
                    }
                }
            } else {
                logger.info("Dir {} does not have a manifest", dir);
            }
        } else {
            logger.info("Dir {} does not exist", dir);
        }
        resp.setStatus(500);
    }

    private static void createJar(final File sourceDir, final File jarFile, final Manifest mf)
    throws IOException {
        final JarOutputStream zos = new JarOutputStream(new FileOutputStream(jarFile));
        try {
            zos.setLevel(Deflater.NO_COMPRESSION);
            // manifest first
            final ZipEntry anEntry = new ZipEntry(JarFile.MANIFEST_NAME);
            zos.putNextEntry(anEntry);
            mf.write(zos);
            zos.closeEntry();
            zipDir(sourceDir, zos, "");
        } finally {
            try {
                zos.close();
            } catch ( final IOException ignore ) {
                // ignore
            }
        }
    }

    public static void zipDir(final File sourceDir, final ZipOutputStream zos, final String path)
    throws IOException {
        final byte[] readBuffer = new byte[8192];
        int bytesIn = 0;

        for(final File f : sourceDir.listFiles()) {
            if (f.isDirectory()) {
                final String prefix = path + f.getName() + "/";
                zos.putNextEntry(new ZipEntry(prefix));
                zipDir(f, zos, prefix);
            } else {
                final String entry = path + f.getName();
                if ( !JarFile.MANIFEST_NAME.equals(entry) ) {
                    final FileInputStream fis = new FileInputStream(f);
                    try {
                        final ZipEntry anEntry = new ZipEntry(entry);
                        zos.putNextEntry(anEntry);
                        while ( (bytesIn = fis.read(readBuffer)) != -1) {
                            zos.write(readBuffer, 0, bytesIn);
                        }
                    } finally {
                        fis.close();
                    }
                }
            }
        }
    }
}

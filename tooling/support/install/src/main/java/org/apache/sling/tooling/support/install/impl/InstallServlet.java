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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

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

    private static final int UPLOAD_IN_MEMORY_SIZE_THRESHOLD = 512 * 1024 * 1024;

    private BundleContext bundleContext;

    @Reference
    private PackageAdmin packageAdmin;

    @Activate
    protected void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        final String dirPath = req.getParameter(DIR);

        boolean isMultipart = ServletFileUpload.isMultipartContent(req);

        if (dirPath == null && !isMultipart) {
            logger.error("No dir parameter specified : {} and no multipart content found", req.getParameterMap());
            resp.setStatus(500);
            InstallationResult result = new InstallationResult(false, "No dir parameter specified: "
                    + req.getParameterMap() + " and no multipart content found");
            result.render(resp.getWriter());
            return;
        }

        if (isMultipart) {
            installBasedOnUploadedJar(req, resp);
        } else {
            installBasedOnDirectory(resp, new File(dirPath));
        }
    }

    private void installBasedOnUploadedJar(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        InstallationResult result = null;

        try {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            // try to hold even largish bundles in memory to potentially improve performance
            factory.setSizeThreshold(UPLOAD_IN_MEMORY_SIZE_THRESHOLD);

            ServletFileUpload upload = new ServletFileUpload();
            upload.setFileItemFactory(factory);

            @SuppressWarnings("unchecked")
            List<FileItem> items = upload.parseRequest(req);
            if (items.size() != 1) {
                logAndWriteError("Found " + items.size() + " items to process, but only updating 1 bundle is supported", resp);
                return;
            }

            FileItem item = items.get(0);

            JarInputStream jar = null;
            InputStream rawInput = null;
            try {
                jar = new JarInputStream(item.getInputStream());
                Manifest manifest = jar.getManifest();
                if (manifest == null) {
                    logAndWriteError("Uploaded jar file does not contain a manifest", resp);
                    return;
                }

                final String symbolicName = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
                if (symbolicName == null) {
                    logAndWriteError("Manifest does not have a " + Constants.BUNDLE_SYMBOLICNAME, resp);
                    return;
                }

                // the JarInputStream is used only for validation, we need a fresh input stream for updating
                rawInput = item.getInputStream();

                Bundle found = getBundle(symbolicName);
                try {
                    installOrUpdateBundle(found, rawInput, null);

                    result = new InstallationResult(true, null);
                    resp.setStatus(200);
                    result.render(resp.getWriter());
                    return;
                } catch (BundleException e) {
                    logAndWriteError("Unable to install/update bundle " + symbolicName, e, resp);
                    return;
                }
            } finally {
                IOUtils.closeQuietly(jar);
                IOUtils.closeQuietly(rawInput);
            }

        } catch (FileUploadException e) {
            logAndWriteError("Failed parsing uploaded bundle", e, resp);
            return;
        }
    }

    private void logAndWriteError(String message, HttpServletResponse resp) throws IOException {
        logger.info(message);
        resp.setStatus(500);
        new InstallationResult(false, message).render(resp.getWriter());
    }

    private void logAndWriteError(String message, Exception e, HttpServletResponse resp) throws IOException {
        logger.info(message, e);
        resp.setStatus(500);
        new InstallationResult(false, message + " : " + e.getMessage()).render(resp.getWriter());
    }

    private void installBasedOnDirectory(HttpServletResponse resp, final File dir) throws FileNotFoundException,
            IOException {

        InstallationResult result = null;

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
                        Bundle found = getBundle(symbolicName);

                        final File tempFile = File.createTempFile(dir.getName(), "bundle");
                        try {
                            createJar(dir, tempFile, mf);

                            final InputStream in = new FileInputStream(tempFile);
                            try {
                                String location = dir.getAbsolutePath();

                                installOrUpdateBundle(found, in, location);
                                result = new InstallationResult(true, null);
                                resp.setStatus(200);
                                result.render(resp.getWriter());
                                return;
                            } catch ( final BundleException be ) {
                                logAndWriteError("Unable to install/update bundle from dir " + dir, be, resp);
                            }
                        } finally {
                            tempFile.delete();
                        }
                    } else {
                        logAndWriteError("Manifest in " + dir + " does not have a symbolic name", resp);
                    }
                } finally {
                    IOUtils.closeQuietly(fis);
                }
            } else {
                result = new InstallationResult(false, "Dir " + dir + " does not have a manifest");
                logAndWriteError("Dir " + dir + " does have a manifest", resp);
            }
        } else {
            result = new InstallationResult(false, "Dir " + dir + " does not exist");
            logAndWriteError("Dir " + dir + " does not exist", resp);
        }
    }

    private void installOrUpdateBundle(Bundle bundle, final InputStream in, String location) throws BundleException {
        if (bundle != null) {
            // update
            bundle.update(in);
        } else {
            // install
            final Bundle b = bundleContext.installBundle(location, in);
            b.start();
        }

        // take into account added/removed packages for updated bundles and newly satisfied optional package imports
        // for new installed bundles
        packageAdmin.refreshPackages(new Bundle[] { bundle });
    }

    private Bundle getBundle(final String symbolicName) {
        Bundle found = null;
        for (final Bundle b : this.bundleContext.getBundles()) {
            if (symbolicName.equals(b.getSymbolicName())) {
                found = b;
                break;
            }
        }
        return found;
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

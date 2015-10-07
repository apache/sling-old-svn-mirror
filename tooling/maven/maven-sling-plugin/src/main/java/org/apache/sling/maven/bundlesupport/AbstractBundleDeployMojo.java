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

package org.apache.sling.maven.bundlesupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.Deflater;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.FilePartSource;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

abstract class AbstractBundleDeployMojo extends AbstractBundlePostMojo {

    /**
     * The URL to the OSGi Bundle repository to which the bundle is posted, e.g.
     * <code>http://obr.sample.com</code>
     */
    @Parameter(required = true, property="obr")
    private String obr;

    /**
     * Returns the path and name of the jar file containing the bundle to be
     * uploaded. This method always returns a non-<code>null</code> name but
     * throws a <code>MojoExecutionException</code> if the name is not known.
     * 
     * @return The name of the file to be uploaded, this is never
     *         <code>null</code>.
     * @throws MojoExecutionException If the name of the file is not known
     *             because it might not have been configured.
     */
    protected abstract String getJarFileName() throws MojoExecutionException;

    /**
     * Optionally fixes up the version of the bundle given in the jar File. If
     * no version fixup is required the <code>jarFile</code> may just be
     * returned.
     * 
     * @param jarFile The file whose bundle version should be fixed
     * @return The file containing the fixed version or <code>jarFile</code>
     *         if the version was not fixed.
     * @throws MojoExecutionException May be thrown in case of any problems
     */
    protected abstract File fixBundleVersion(File jarFile)
            throws MojoExecutionException;

    /**
     * Execute this Mojo
     */
    public void execute() throws MojoExecutionException {
        // only upload if packaging as an osgi-bundle
        File jarFile = new File(getJarFileName());
        String bundleName = getBundleSymbolicName(jarFile);
        if (bundleName == null) {
            this.getLog().info(
                jarFile + " is not an OSGi Bundle, not uploading");
            return;
        }

        // optionally fix up the bundle version
        jarFile = fixBundleVersion(jarFile);

        getLog().info(
            "Deploying Bundle " + bundleName + "(" + jarFile + ") to " + obr);
        this.post(this.obr, jarFile);
    }

    private void post(String targetURL, File file)
            throws MojoExecutionException {
        
        PostMethod filePost = new PostMethod(targetURL);
        try {
            Part[] parts = {
                new FilePart(file.getName(), new FilePartSource(file.getName(),
                    file)), new StringPart("_noredir_", "_noredir_") };
            filePost.setRequestEntity(new MultipartRequestEntity(parts,
                filePost.getParams()));
            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(
                5000);
            int status = client.executeMethod(filePost);
            if (status == HttpStatus.SC_OK) {
                getLog().info("Bundle deployed");
            } else {
                String msg = "Deployment failed, cause: "
                    + HttpStatus.getStatusText(status);
                if (failOnError) {
                    throw new MojoExecutionException(msg);
                } else {
                    getLog().error(msg);
                }
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Deployment on " + targetURL
                + " failed, cause: " + ex.getMessage(), ex);
        } finally {
            filePost.releaseConnection();
        }
    }

    /**
     * Change the version in jar
     * 
     * @param newVersion
     * @param file
     * @return
     * @throws MojoExecutionException
     */
    protected File changeVersion(File file, String oldVersion, String newVersion)
            throws MojoExecutionException {
        String fileName = file.getName();
        int pos = fileName.indexOf(oldVersion);
        fileName = fileName.substring(0, pos) + newVersion
            + fileName.substring(pos + oldVersion.length());

        JarInputStream jis = null;
        JarOutputStream jos;
        OutputStream out = null;
        try {
            // now create a temporary file and update the version
            final JarFile sourceJar = new JarFile(file);
            final Manifest manifest = sourceJar.getManifest();
            manifest.getMainAttributes().putValue("Bundle-Version", newVersion);

            jis = new JarInputStream(new FileInputStream(file));
            final File destJar = new File(file.getParentFile(), fileName);
            out = new FileOutputStream(destJar);
            jos = new JarOutputStream(out, manifest);

            jos.setMethod(JarOutputStream.DEFLATED);
            jos.setLevel(Deflater.BEST_COMPRESSION);

            JarEntry entryIn = jis.getNextJarEntry();
            while (entryIn != null) {
                JarEntry entryOut = new JarEntry(entryIn.getName());
                entryOut.setTime(entryIn.getTime());
                entryOut.setComment(entryIn.getComment());
                jos.putNextEntry(entryOut);
                if (!entryIn.isDirectory()) {
                    IOUtils.copy(jis, jos);
                }
                jos.closeEntry();
                jis.closeEntry();
                entryIn = jis.getNextJarEntry();
            }

            // close the JAR file now to force writing
            jos.close();
            return destJar;
        } catch (IOException ioe) {
            throw new MojoExecutionException(
                "Unable to update version in jar file.", ioe);
        } finally {
            IOUtils.closeQuietly(jis);
            IOUtils.closeQuietly(out);
        }

    }
}
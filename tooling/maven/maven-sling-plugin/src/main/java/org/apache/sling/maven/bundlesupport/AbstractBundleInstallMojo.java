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
package org.apache.sling.maven.bundlesupport;

import static org.apache.sling.maven.bundlesupport.JsonSupport.JSON_MIME_TYPE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.FilePartSource;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.sling.maven.bundlesupport.fsresource.SlingInitialContentMounter;

abstract class AbstractBundleInstallMojo extends AbstractBundlePostMojo {

    /**
     * If a PUT via WebDAV should be used instead of the standard POST to the
     * Felix Web Console. In the <code>uninstall</code> goal, a HTTP DELETE will be
     * used.
     * 
     * @deprecated Use {@link #deploymentMethod} instead.
     */
    @Parameter(property="sling.usePut", defaultValue = "false")
    protected boolean usePut;

    /**
     * Possible methodologies for deploying (installing and uninstalling)
     * bundles from the remote server.
     * Use camel-case values because those are used when you configure the plugin (and uppercase with separators "_" just looks ugly in that context)
     */
    enum BundleDeploymentMethod {
        /** Via POST to Felix Web Console */
        WebConsole,

        /** Via WebDAV */
        WebDAV,

        /** Via POST to Sling directly */
        SlingPostServlet;
    }

    /**
     * Bundle deployment method. One of the following three values are allowed
     * <ol>
     *  <li><strong>WebConsole</strong>, uses the <a href="http://felix.apache.org/documentation/subprojects/apache-felix-web-console/web-console-restful-api.html#post-requests">
     *  Felix Web Console REST API</a> for deployment (HTTP POST). This is the default. 
     *  Make sure that {@link #slingUrl} points to the Felix Web Console in that case.</li>
     *  <li><strong>WebDAV</strong>, uses <a href="https://sling.apache.org/documentation/development/repository-based-development.html">
     *  WebDAV</a> for deployment (HTTP PUT). Make sure that {@link #slingUrl} points to the entry path of 
     *  the Sling WebDAV bundle (usually below regular Sling root URL). Issues a HTTP Delete for the uninstall goal.
     *  <li><strong>SlingPostServlet</strong>, uses the
     *  <a href="https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html">Sling Post Servlet</a> for deployment (HTTP POST).
     *  Make sure that {@link #slingUrl} points a path which is handled by the Sling POST Servlet (usually below regular Sling root URL).</li>
     * </ol>
     * 
     * This has precedence over the deprecated parameter {@link #usePut}.
     */
    @Parameter(property="sling.deploy.method", required = false)
    protected BundleDeploymentMethod deploymentMethod;

    /**
     * The content type / mime type used for WebDAV or Sling POST deployment.
     */
    @Parameter(property="sling.mimeType", defaultValue = "application/java-archive", required = true)
    protected String mimeType;

    /**
     * The start level to set on the installed bundle. If the bundle is already installed and therefore is only 
     * updated this parameter is ignored. The parameter is also ignored if the running Sling instance has no 
     * StartLevel service (which is unusual actually). Only applies when POSTing to Felix Web Console.
     */
    @Parameter(property="sling.bundle.startlevel", defaultValue = "20", required = true)
    private String bundleStartLevel;

    /**
     * Whether to start the uploaded bundle or not. Only applies when POSTing
     * to Felix Web Console
     */
    @Parameter(property="sling.bundle.start", defaultValue = "true", required = true)
    private boolean bundleStart;

    /**
     * Whether to refresh the packages after installing the uploaded bundle.
     * Only applies when POSTing to Felix Web Console
     */
    @Parameter(property="sling.refreshPackages", defaultValue = "true", required = true)
    private boolean refreshPackages;

    /**
     * Whether to add the mapping for the
     * <a href="https://sling.apache.org/documentation/bundles/accessing-filesystem-resources-extensions-fsresource.html">Apache Sling File System Resource Provider</a>.
     */
    @Parameter(property="sling.mountByFS", defaultValue = "false", required = true)
    private boolean mountByFS;

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    public AbstractBundleInstallMojo() {
        super();
    }

    protected abstract String getBundleFileName() throws MojoExecutionException;

    /**
     * Returns the URL with the filename appended to it.
     * @param targetURL the original requested targetURL to append fileName to
     * @param fileName the name of the file to append to the targetURL.
     */
    protected String getURLWithFilename(String targetURL, String fileName) {
        return targetURL + (targetURL.endsWith("/") ? "" : "/") + fileName;
    }

    @Override
    public void execute() throws MojoExecutionException {

        // get the file to upload
        String bundleFileName = getBundleFileName();

        // only upload if packaging as an osgi-bundle
        File bundleFile = new File(bundleFileName);

        if(!bundleFile.exists()) {
            getLog().info(bundleFile + " does not exist, no uploading");
            return;
        }

        String bundleName = getBundleSymbolicName(bundleFile);
        if (bundleName == null) {
            getLog().info(bundleFile + " is not an OSGi Bundle, not uploading");
            return;
        }

        String targetURL = getTargetURL();

        BundleDeploymentMethod deploymentMethod = getDeploymentMethod();
        getLog().info(
            "Installing Bundle " + bundleName + "(" + bundleFile + ") to "
                + targetURL + " via " + deploymentMethod);


        switch (deploymentMethod) {
        case SlingPostServlet:
            postToSling(targetURL, bundleFile);
            break;
        case WebConsole:
            postToFelix(targetURL, bundleFile);
            break;
        case WebDAV:
            putViaWebDav(targetURL, bundleFile);
            break;
        // sanity check to make sure it gets handled in some fashion
        default:
            throw new MojoExecutionException("Unrecognized BundleDeployMethod " + deploymentMethod);
        }

        if ( mountByFS ) {
            configure(getConsoleTargetURL(), bundleFile);
        }
    }
    
    protected void configure(final String targetURL, final File file) throws MojoExecutionException {
        new SlingInitialContentMounter(getLog(), getHttpClient(), project).mount(targetURL, file);
    }

    /**
     * Retrieve the bundle deployment method matching the configuration.
     * @return bundle deployment method matching the plugin configuration.
     */
    protected BundleDeploymentMethod getDeploymentMethod() throws MojoExecutionException {
        if (this.deploymentMethod == null) {
            if (usePut) {
                getLog().warn("Using deprecated configuration parameter 'usePut=true', please instead use the new parameter 'deploymentMethod=WebDAV'!");
                return BundleDeploymentMethod.WebDAV;
            } else {
                return BundleDeploymentMethod.WebConsole;
            }
        } else {
            return deploymentMethod;
        }
    }

    /**
     * Install the bundle via POST to the Felix Web Console
     * @param targetURL the URL to the Felix Web Console Bundles listing
     * @param file the file to POST
     * @throws MojoExecutionException
     * @see <a href="http://felix.apache.org/documentation/subprojects/apache-felix-web-console/web-console-restful-api.html#post-requests">Webconsole RESTful API</a>
     * @see <a href="https://github.com/apache/felix/blob/trunk/webconsole/src/main/java/org/apache/felix/webconsole/internal/core/BundlesServlet.java">BundlesServlet@Github</a>
     */
    protected void postToFelix(String targetURL, File file)
            throws MojoExecutionException {

        // append pseudo path after root URL to not get redirected for nothing
        final PostMethod filePost = new PostMethod(targetURL + "/install");

        try {
            // set referrer
            filePost.setRequestHeader("referer", "about:blank");

            List<Part> partList = new ArrayList<Part>();
            partList.add(new StringPart("action", "install"));
            partList.add(new StringPart("_noredir_", "_noredir_"));
            partList.add(new FilePart("bundlefile", new FilePartSource(
                file.getName(), file)));
            partList.add(new StringPart("bundlestartlevel", bundleStartLevel));

            if (bundleStart) {
                partList.add(new StringPart("bundlestart", "start"));
            }

            if (refreshPackages) {
                partList.add(new StringPart("refreshPackages", "true"));
            }

            Part[] parts = partList.toArray(new Part[partList.size()]);

            filePost.setRequestEntity(new MultipartRequestEntity(parts,
                filePost.getParams()));

            int status = getHttpClient().executeMethod(filePost);
            if (status == HttpStatus.SC_OK) {
                getLog().info("Bundle installed");
            } else {
                String msg = "Installation failed, cause: "
                    + HttpStatus.getStatusText(status);
                if (failOnError) {
                    throw new MojoExecutionException(msg);
                } else {
                    getLog().error(msg);
                }
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Installation on " + targetURL
                + " failed, cause: " + ex.getMessage(), ex);
        } finally {
            filePost.releaseConnection();
        }
    }

    /**
     * Perform the operation via POST to SlingPostServlet
     * @param targetURL the URL of the Sling instance to post the file to.
     * @param file the file being interacted with the POST to Sling.
     * @throws MojoExecutionException
     */
    protected void postToSling(String targetURL, File file) throws MojoExecutionException {

        /* truncate off trailing slash as this has special behaviorisms in
         * the SlingPostServlet around created node name conventions */
        if (targetURL.endsWith("/")) {
            targetURL = targetURL.substring(0, targetURL.length()-1);
        }
        // append pseudo path after root URL to not get redirected for nothing
        final PostMethod filePost = new PostMethod(targetURL);

        try {

            Part[] parts = new Part[2];
            // Add content type to force the configured mimeType value
            parts[0] = new FilePart("*", new FilePartSource(
                file.getName(), file), mimeType, null);
            // Add TypeHint to have jar be uploaded as file (not as resource)
            parts[1] = new StringPart("*@TypeHint", "nt:file");

            /* Request JSON response from Sling instead of standard HTML, to
             * reduce the payload size (if the PostServlet supports it). */
            filePost.setRequestHeader("Accept", JSON_MIME_TYPE);
            filePost.setRequestEntity(new MultipartRequestEntity(parts,
                filePost.getParams()));

            int status = getHttpClient().executeMethod(filePost);
            // SlingPostServlet may return 200 or 201 on creation, accept both
            if (status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED) {
                getLog().info("Bundle installed");
            } else {
                String msg = "Installation failed, cause: "
                    + HttpStatus.getStatusText(status);
                if (failOnError) {
                    throw new MojoExecutionException(msg);
                } else {
                    getLog().error(msg);
                }
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Installation on " + targetURL
                + " failed, cause: " + ex.getMessage(), ex);
        } finally {
            filePost.releaseConnection();
        }
    }

    /**
     * Puts the file via PUT (leveraging WebDAV). Creates the intermediate folders as well.
     * @param targetURL
     * @param file
     * @throws MojoExecutionException
     * @see <a href="https://tools.ietf.org/html/rfc4918#section-9.7.1">RFC 4918</a>
     */
    protected void putViaWebDav(String targetURL, File file) throws MojoExecutionException {

        boolean success = false;
        int status;

        try {
            status = performPut(targetURL, file);
            if (status >= 200 && status < 300) {
                success = true;
            } else if (status == HttpStatus.SC_CONFLICT) {

                getLog().debug("Bundle not installed due missing parent folders. Attempting to create parent structure.");
                createIntermediaryPaths(targetURL);

                getLog().debug("Re-attempting bundle install after creating parent folders.");
                status = performPut(targetURL, file);
                if (status >= 200 && status < 300) {
                    success = true;
                }
            }

            if (!success) {
                String msg = "Installation failed, cause: "
                    + HttpStatus.getStatusText(status);
                if (failOnError) {
                    throw new MojoExecutionException(msg);
                } else {
                    getLog().error(msg);
                }
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Installation on " + targetURL
                + " failed, cause: " + ex.getMessage(), ex);
        }
    }

    private int performPut(String targetURL, File file) throws HttpException, IOException {
        PutMethod filePut = new PutMethod(getURLWithFilename(targetURL, file.getName()));
        try {
            filePut.setRequestEntity(new FileRequestEntity(file, mimeType));
            return getHttpClient().executeMethod(filePut);
        } finally {
            filePut.releaseConnection();
        }
    }

    private int performHead(String uri) throws HttpException, IOException {
        HeadMethod head = new HeadMethod(uri);
        try {
            return getHttpClient().executeMethod(head);
        } finally {
            head.releaseConnection();
        }
    }

    private int performMkCol(String uri) throws IOException {
        MkColMethod mkCol = new MkColMethod(uri);
        try {
            return getHttpClient().executeMethod(mkCol);
        } finally {
            mkCol.releaseConnection();
        }
    }

    private void createIntermediaryPaths(String targetURL) throws HttpException, IOException, MojoExecutionException {
        // extract all intermediate URIs (longest one first)
        List<String> intermediateUris = IntermediateUrisExtractor.extractIntermediateUris(targetURL);

        // 1. go up to the node in the repository which exists already (HEAD request towards the root node)
        String existingIntermediateUri = null;
        // go through all intermediate URIs (longest first)
        for (String intermediateUri : intermediateUris) {
            // until one is existing
            int result = performHead(intermediateUri) ;
            if (result == HttpStatus.SC_OK) {
                existingIntermediateUri = intermediateUri;
                break;
            } else if (result != HttpStatus.SC_NOT_FOUND) {
                throw new MojoExecutionException("Failed getting intermediate path at " + intermediateUri + "."
                        + " Reason: " + HttpStatus.getStatusText(result));
            }
        }

        if (existingIntermediateUri == null) {
            throw new MojoExecutionException(
                    "Could not find any intermediate path up until the root of " + targetURL + ".");
        }

        // 2. now create from that level on each intermediate node individually towards the target path
        int startOfInexistingIntermediateUri = intermediateUris.indexOf(existingIntermediateUri);
        if (startOfInexistingIntermediateUri == -1) {
            throw new IllegalStateException(
                    "Could not find intermediate uri " + existingIntermediateUri + " in the list");
        }

        for (int index = startOfInexistingIntermediateUri - 1; index >= 0; index--) {
            // use MKCOL to create the intermediate paths
            String intermediateUri = intermediateUris.get(index);
            int result = performMkCol(intermediateUri);
            if (result == HttpStatus.SC_CREATED || result == HttpStatus.SC_OK) {
                getLog().debug("Intermediate path at " + intermediateUri + " successfully created");
                continue;
            } else {
                throw new MojoExecutionException("Failed creating intermediate path at '" + intermediateUri + "'."
                        + " Reason: " + HttpStatus.getStatusText(result));
            }
        }
    }

}

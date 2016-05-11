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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.FilePartSource;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.ManifestHeader;
import org.apache.sling.commons.osgi.ManifestHeader.Entry;
import org.codehaus.plexus.util.StringUtils;

abstract class AbstractBundleInstallMojo extends AbstractBundlePostMojo {

    /** Header containing the sling initial content information. */
    private static final String HEADER_INITIAL_CONTENT = "Sling-Initial-Content";
    /** The fs resource provider factory. */
    private static final String FS_FACTORY = "org.apache.sling.fsprovider.internal.FsResourceProvider";
    /** Mime type for json response. */
    protected static final String JSON_MIME_TYPE = "application/json";
    /** Http header for content type. */
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * The URL of the running Sling instance. The default is only useful for <strong>WebConsole</strong> deployment (see {@link #deploymentMethod}).
     */
    @Parameter(property="sling.url", defaultValue="http://localhost:8080/system/console", required = true)
    protected String slingUrl;

    /**
     * An optional url suffix which will be appended to the <code>sling.url</code>
     * for use as the real target url. This allows to configure different target URLs
     * in each POM, while using the same common <code>sling.url</code> in a parent
     * POM (eg. <code>sling.url=http://localhost:8080</code> and
     * <code>sling.urlSuffix=/project/specific/path</code>). This is typically used
     * in conjunction with WebDAV or SlingPostServlet deployment methods.
     */
    @Parameter(property="sling.urlSuffix")
    protected String slingUrlSuffix;

    /**
     * If a PUT via WebDAV should be used instead of the standard POST to the
     * Felix Web Console. In the <code>uninstall</code> goal, a HTTP DELETE will be
     * used.
     * 
     * @deprecated Use {@link #deployMethod} instead.
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
     *  <li><strong>SlingPostServlet</strong>, uses the <a href="">Sling Post Servlet</a> for deployment (HTTP POST).
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
     * The user name to authenticate at the running Sling instance.
     */
    @Parameter(property="sling.user", defaultValue = "admin", required = true)
    private String user;

    /**
     * The password to authenticate at the running Sling instance.
     */
    @Parameter(property="sling.password", defaultValue = "admin", required = true)
    private String password;

    /**
     * The startlevel for the uploaded bundle. Only applies when POSTing to
     * Felix Web Console.
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
     * Whether to add the mapping for the fs provider
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
     * Returns the combination of <code>sling.url</code> and
     * <code>sling.urlSuffix</code>.
     */
    protected String getTargetURL() {
        String targetURL = slingUrl;
        if (slingUrlSuffix != null) {
            targetURL += slingUrlSuffix;
        }
        return targetURL;
    }

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
            configure(targetURL, bundleFile);
        }
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
     * Helper method to throw a meaningful exception for an outdated felix
     * web console.
     * @throws MojoExecutionException
     */
    protected void throwWebConsoleTooOldException()
    throws MojoExecutionException {
        throw new MojoExecutionException("The Apache Felix Web Console is too old to mount " +
                "the initial content through file system provider configs. " +
                "Either upgrade the web console or disable this feature.");
    }

    /**
     * Get the http client
     */
    protected HttpClient getHttpClient() {
        final HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(
            5000);

        // authentication stuff
        client.getParams().setAuthenticationPreemptive(true);
        Credentials defaultcreds = new UsernamePasswordCredentials(user,
            password);
        client.getState().setCredentials(AuthScope.ANY, defaultcreds);

        return client;
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

    /**
     * Add configurations to a running OSGi instance for initial content.
     * @param targetURL The web console base url
     * @param file The artifact (bundle)
     * @throws MojoExecutionException
     */
    protected void configure(String targetURL, File file)
    throws MojoExecutionException {
        // first, let's get the manifest and see if initial content is configured
        ManifestHeader header = null;
        try {
            final Manifest mf = this.getManifest(file);
            final String value = mf.getMainAttributes().getValue(HEADER_INITIAL_CONTENT);
            if ( value == null ) {
                getLog().debug("Bundle has no initial content - no file system provider config created.");
                return;
            }
            header = ManifestHeader.parse(value);
            if ( header == null || header.getEntries().length == 0 ) {
                getLog().warn("Unable to parse header or header is empty: " + value);
                return;
            }
        } catch (IOException ioe) {
            throw new MojoExecutionException("Unable to read manifest from file " + file, ioe);
        }
        // setup http client
        final HttpClient client = getHttpClient();

        getLog().info("Trying to configure file system provider...");
        // quick check if resources are configured
        final List resources = project.getResources();
        if ( resources == null || resources.size() == 0 ) {
            throw new MojoExecutionException("No resources configured for this project.");
        }
        // now get current configurations
        final Map oldConfigs = this.getCurrentFileProviderConfigs(targetURL, client);

        final Entry[] entries = header.getEntries();
        for(final Entry entry : entries) {
            String path = entry.getValue();
            if ( path != null && !path.endsWith("/") ) {
                path += "/";
            }
            // check if we should ignore this
            final String ignoreValue = entry.getDirectiveValue("maven:mount");
            if ( ignoreValue != null && ignoreValue.equalsIgnoreCase("false") ) {
                getLog().debug("Ignoring " + path);
                continue;
            }
            String installPath = entry.getDirectiveValue("path");
            if ( installPath == null ) {
                installPath = "/";
            }
            // search the path in the resources (usually this should be the first resource
            // entry but this might be reconfigured
            File dir = null;
            final Iterator i = resources.iterator();
            while ( dir == null && i.hasNext() ) {
                final Resource rsrc = (Resource)i.next();
                String child = path;
                // if resource mapping defines a target path: remove target path from checked resource path
                String targetPath = rsrc.getTargetPath();
                if ( targetPath != null && !targetPath.endsWith("/") ) {
                    targetPath = targetPath + "/";
                }
                if ( targetPath != null && path.startsWith(targetPath) ) {
                    child = child.substring(targetPath.length());
                }
                dir = new File(rsrc.getDirectory(), child);
                if ( !dir.exists() ) {
                    dir = null;
                }
            }
            if ( dir == null ) {
                throw new MojoExecutionException("No resource entry found containing " + path);
            }
            // check for root mapping - which we don't support atm
            if ( "/".equals(installPath) ) {
                throw new MojoExecutionException("Mapping to root path not supported by fs provider at the moment. Please adapt your initial content configuration.");
            }
            getLog().info("Mapping " + dir + " to " + installPath);

            // check if this is already configured
            boolean found = false;
            final Iterator entryIterator = oldConfigs.entrySet().iterator();
            while ( !found && entryIterator.hasNext() ) {
                final Map.Entry current = (Map.Entry) entryIterator.next();
                final String[] value = (String[])current.getValue();
                getLog().debug("Comparing " + dir.getAbsolutePath() + " with " + value[0] + " (" + value[1] + ")");
                if ( dir.getAbsolutePath().equals(value[0]) ) {
                    if ( installPath.equals(value[1]) ) {
                        getLog().debug("Using existing configuration for " + dir + " and " + installPath);
                        found = true;
                    } else {
                        // remove old config
                        getLog().debug("Removing old configuration for " + value[0] + " and " + value[1]);
                        removeConfiguration(client, targetURL, current.getKey().toString());
                    }
                    entryIterator.remove();
                }
            }
            if ( !found ) {
                getLog().debug("Adding new configuration for " + dir + " and " + installPath);
                addConfiguration(client, targetURL, dir.getAbsolutePath(), installPath);
            }
        }
        // finally remove old configs
        final Iterator entryIterator = oldConfigs.entrySet().iterator();
        while ( entryIterator.hasNext() ) {
            final Map.Entry current = (Map.Entry) entryIterator.next();
            final String[] value = (String[])current.getValue();
            getLog().debug("Removing old configuration for " + value[0] + " and " + value[1]);
            // remove old config
            removeConfiguration(client, targetURL, current.getKey().toString());
        }
    }

    protected void removeConfiguration(final HttpClient client, final String targetURL, String pid)
    throws MojoExecutionException {
        final String postUrl = targetURL  + "/configMgr/" + pid;
        final PostMethod post = new PostMethod(postUrl);
        post.addParameter("apply", "true");
        post.addParameter("delete", "true");
        try {
            final int status = client.executeMethod(post);
            // we get a moved temporarily back from the configMgr plugin
            if (status == HttpStatus.SC_MOVED_TEMPORARILY || status == HttpStatus.SC_OK) {
                getLog().debug("Configuration removed.");
            } else {
                getLog().error(
                    "Removing configuration failed, cause: "
                        + HttpStatus.getStatusText(status));
            }
        } catch (HttpException ex) {
            throw new MojoExecutionException("Removing configuration at " + postUrl
                    + " failed, cause: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new MojoExecutionException("Removing configuration at " + postUrl
                    + " failed, cause: " + ex.getMessage(), ex);
        } finally {
            post.releaseConnection();
        }
    }

    /**
     * Add a new configuration for the file system provider
     * @throws MojoExecutionException
     */
    protected void addConfiguration(final HttpClient client, final String targetURL, String dir, String path)
    throws MojoExecutionException {
        final String postUrl = targetURL  + "/configMgr/" + FS_FACTORY;
        final PostMethod post = new PostMethod(postUrl);
        post.addParameter("apply", "true");
        post.addParameter("factoryPid", FS_FACTORY);
        post.addParameter("pid", "[Temporary PID replaced by real PID upon save]");
        post.addParameter("provider.file", dir);
        post.addParameter("provider.roots", path);
        post.addParameter("propertylist", "provider.roots,provider.file");
        try {
            final int status = client.executeMethod(post);
            // we get a moved temporarily back from the configMgr plugin
            if (status == HttpStatus.SC_MOVED_TEMPORARILY || status == HttpStatus.SC_OK) {
                getLog().info("Configuration created.");
            } else {
                getLog().error(
                    "Configuration failed, cause: "
                        + HttpStatus.getStatusText(status));
            }
        } catch (HttpException ex) {
            throw new MojoExecutionException("Configuration on " + postUrl
                    + " failed, cause: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new MojoExecutionException("Configuration on " + postUrl
                    + " failed, cause: " + ex.getMessage(), ex);
        } finally {
            post.releaseConnection();
        }
    }

    /**
     * Return all file provider configs for this project
     * @param targetURL The targetURL of the webconsole
     * @param client The http client
     * @return A map (may be empty) with the pids as keys and a string array
     *         containing the path and the root
     * @throws MojoExecutionException
     */
    protected Map getCurrentFileProviderConfigs(final String targetURL, final HttpClient client)
    throws MojoExecutionException {
        getLog().debug("Getting current file provider configurations.");
        final Map result = new HashMap();
        final String getUrl = targetURL  + "/configMgr/(service.factoryPid=" + FS_FACTORY + ").json";
        final GetMethod get = new GetMethod(getUrl);

        try {
            final int status = client.executeMethod(get);
            if ( status == 200 ) {
                String contentType = get.getResponseHeader(HEADER_CONTENT_TYPE).getValue();
                int pos = contentType.indexOf(';');
                if ( pos != -1 ) {
                    contentType = contentType.substring(0, pos);
                }
                if ( !JSON_MIME_TYPE.equals(contentType) ) {
                    getLog().debug("Response type from web console is not JSON, but " + contentType);
                    throwWebConsoleTooOldException();
                }
                final String jsonText = get.getResponseBodyAsString();
                try {
                    JSONArray array = new JSONArray(jsonText);
                    for(int i=0; i<array.length(); i++) {
                        final JSONObject obj = array.getJSONObject(i);
                        final String pid = obj.getString("pid");
                        final JSONObject properties = obj.getJSONObject("properties");
                        final String path = properties.getJSONObject("provider.file").getString("value");
                        final String roots = properties.getJSONObject("provider.roots").getString("value");
                        if ( path != null && path.startsWith(this.project.getBasedir().getAbsolutePath()) ) {
                            getLog().debug("Found configuration with pid: " + pid + ", path: " + path + ", roots: " + roots);
                            result.put(pid, new String[] {path, roots});
                        }
                    }
                } catch (JSONException ex) {
                    throw new MojoExecutionException("Reading configuration from " + getUrl
                            + " failed, cause: " + ex.getMessage(), ex);
                }
            }
        } catch (HttpException ex) {
            throw new MojoExecutionException("Reading configuration from " + getUrl
                    + " failed, cause: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new MojoExecutionException("Reading configuration from " + getUrl
                    + " failed, cause: " + ex.getMessage(), ex);
        } finally {
            get.releaseConnection();
        }
        return result;
    }

    /**
     * Get the manifest from the File.
     * @param bundleFile The bundle jar
     * @return The manifest.
     * @throws IOException
     */
    protected Manifest getManifest(final File bundleFile) throws IOException {
        JarFile file = null;
        try {
            file = new JarFile(bundleFile);
            return file.getManifest();
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    /**
     * Try to get the version of the web console
     * @return The version or <code>null</code> if version is not detectable.
     */
    protected String checkWebConsoleVersion(final String targetUrl) {
        getLog().debug("Checking web console version....");
        final String bundleUrl = targetUrl + "/bundles/org.apache.felix.webconsole.json";
        final HttpClient client = getHttpClient();
        final GetMethod gm = new GetMethod(bundleUrl);
        // if something goes wrong, we assume an older version!!
        try {
            final int status = client.executeMethod(gm);
            if ( status == 200 ) {
                if ( gm.getResponseContentLength() == 0 ) {
                    getLog().debug("Response has zero length. Assuming older version of web console.");
                    return null;
                }
                final String jsonText = gm.getResponseBodyAsString();
                try {
                    final JSONObject obj = new JSONObject(jsonText);
                    final JSONArray props = obj.getJSONArray("props");
                    for(int i=0; i<props.length(); i++) {
                        final JSONObject property = props.getJSONObject(i);
                        if ( "Version".equals(property.get("key")) ) {
                            final String version = property.getString("value");
                            getLog().debug("Found web console version " + version);
                            return version;
                        }
                    }
                    getLog().debug("Version property not found in response. Assuming older version.");
                    return null;
                } catch (JSONException ex) {
                    getLog().debug("Converting response to JSON failed. Assuming older version: " + ex.getMessage());
                    return null;
                }

            }
            getLog().debug("Status code from web console: " + status);
       } catch (HttpException e) {
            getLog().debug("HttpException: " + e.getMessage());
        } catch (IOException e) {
            getLog().debug("IOException: " + e.getMessage());
        }

        getLog().debug("Unknown version.");
        return null;
    }
}
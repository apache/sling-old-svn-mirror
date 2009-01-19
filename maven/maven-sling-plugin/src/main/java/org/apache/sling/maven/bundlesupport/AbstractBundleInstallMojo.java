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
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.FilePartSource;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.ManifestHeader;
import org.apache.sling.commons.osgi.ManifestHeader.Entry;

abstract class AbstractBundleInstallMojo extends AbstractBundlePostMojo {

    /** Header containing the sling initial content information. */
    private static final String HEADER_INITIAL_CONTENT = "Sling-Initial-Content";
    /** The fs resource provider factory. */
    private static final String FS_FACTORY = "org.apache.sling.fsprovider.FsResourceProvider";
    /** Mime type for json response. */
    private static final String JSON_MIME_TYPE = "application/json";
    /** Http header for content type. */
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * The URL of the running Sling instance.
     *
     * @parameter expression="${sling.url}"
     *            default-value="http://localhost:8080/system/console"
     * @required
     */
    protected String slingUrl;

    /**
     * The user name to authenticate at the running Sling instance.
     *
     * @parameter expression="${sling.user}" default-value="admin"
     * @required
     */
    private String user;

    /**
     * The password to authenticate at the running Sling instance.
     *
     * @parameter expression="${sling.password}" default-value="admin"
     * @required
     */
    private String password;

    /**
     * The startlevel for the uploaded bundle
     *
     * @parameter expression="${sling.bundle.startlevel}" default-value="20"
     * @required
     */
    private String bundleStartLevel;

    /**
     * Whether to start the uploaded bundle or not
     *
     * @parameter expression="${sling.bundle.start}" default-value="true"
     * @required
     */
    private boolean bundleStart;

    /**
     * Whether to refresh the packages after installing the uploaded bundle
     *
     * @parameter expression="${sling.refreshPackages}" default-value="true"
     * @required
     */
    private boolean refreshPackages;

    /**
     * Whether to add the mapping for the fs provider
     *
     * @parameter expression="${sling.mountByFS}" default-value="true"
     * @required
     */
    private boolean mountByFS;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    public AbstractBundleInstallMojo() {
        super();
    }

    protected abstract String getBundleFileName() throws MojoExecutionException;

    public void execute() throws MojoExecutionException {

        // get the file to upload
        String bundleFileName = getBundleFileName();

        // only upload if packaging as an osgi-bundle
        File bundleFile = new File(bundleFileName);
        String bundleName = getBundleSymbolicName(bundleFile);
        if (bundleName == null) {
            getLog().info(bundleFile + " is not an OSGi Bundle, not uploading");
            return;
        }

        getLog().info(
            "Installing Bundle " + bundleName + "(" + bundleFile + ") to "
                + slingUrl);
        post(slingUrl, bundleFile);
        if ( mountByFS ) {
            configure(slingUrl, bundleFile);
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

    protected void post(String targetURL, File file)
            throws MojoExecutionException {

        // append pseudo path after root URL to not get redirected for nothing
        PostMethod filePost = new PostMethod(targetURL + "/install");

        try {

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
                getLog().error(
                    "Installation failed, cause: "
                        + HttpStatus.getStatusText(status));
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Installation on " + targetURL
                + " failed, cause: " + ex.getMessage(), ex);
        } finally {
            filePost.releaseConnection();
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
            final String path = entry.getValue();
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
                dir = new File(rsrc.getDirectory(), path);
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
                        final String path = obj.getJSONObject("provider.file").getString("value");
                        final String roots = obj.getJSONObject("provider.roots").getString("value");
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
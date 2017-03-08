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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.sling.commons.osgi.ManifestHeader;
import org.apache.sling.commons.osgi.ManifestHeader.Entry;

/**
 * Manages OSGi configurations for File System Resource Provider.
 */
class FsMountHelper {
    
    /** Header containing the sling initial content information. */
    private static final String HEADER_INITIAL_CONTENT = "Sling-Initial-Content";
    /** The fs resource provider factory. */
    private static final String FS_FACTORY = "org.apache.sling.fsprovider.internal.FsResourceProvider";
    /** Http header for content type. */
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    
    private final Log log;
    private final HttpClient httpClient;
    private final MavenProject project;

    public FsMountHelper(Log log, HttpClient httpClient, MavenProject project) {
        this.log = log;
        this.httpClient = httpClient;
        this.project = project;
    }

    /**
     * Add configurations to a running OSGi instance for initial content.
     * @param targetURL The web console base url
     * @param file The artifact (bundle)
     * @throws MojoExecutionException
     */
    public void configureInstall(final String targetURL, final File file) throws MojoExecutionException {
        // first, let's get the manifest and see if initial content is configured
        ManifestHeader header = null;
        try {
            final Manifest mf = getManifest(file);
            final String value = mf.getMainAttributes().getValue(HEADER_INITIAL_CONTENT);
            if ( value == null ) {
                log.debug("Bundle has no initial content - no file system provider config created.");
                return;
            }
            header = ManifestHeader.parse(value);
            if ( header == null || header.getEntries().length == 0 ) {
                log.warn("Unable to parse header or header is empty: " + value);
                return;
            }
        } catch (IOException ioe) {
            throw new MojoExecutionException("Unable to read manifest from file " + file, ioe);
        }

        log.info("Trying to configure file system provider...");
        // quick check if resources are configured
        final List resources = project.getResources();
        if ( resources == null || resources.size() == 0 ) {
            throw new MojoExecutionException("No resources configured for this project.");
        }
        // now get current configurations
        final Map<String,String[]> oldConfigs = getCurrentFileProviderConfigs(targetURL);

        final Entry[] entries = header.getEntries();
        for (final Entry entry : entries) {
            String path = entry.getValue();
            if ( path != null && !path.endsWith("/") ) {
                path += "/";
            }
            // check if we should ignore this
            final String ignoreValue = entry.getDirectiveValue("maven:mount");
            if ( ignoreValue != null && ignoreValue.equalsIgnoreCase("false") ) {
                log.debug("Ignoring " + path);
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
            log.info("Mapping " + dir + " to " + installPath);

            // check if this is already configured
            boolean found = false;
            final Iterator<Map.Entry<String,String[]>> entryIterator = oldConfigs.entrySet().iterator();
            while ( !found && entryIterator.hasNext() ) {
                final Map.Entry<String,String[]> current = entryIterator.next();
                final String[] value = current.getValue();
                log.debug("Comparing " + dir.getAbsolutePath() + " with " + value[0] + " (" + value[1] + ")");
                if ( dir.getAbsolutePath().equals(value[0]) ) {
                    if ( installPath.equals(value[1]) ) {
                        log.debug("Using existing configuration for " + dir + " and " + installPath);
                        found = true;
                    }
                    else {
                        // remove old config
                        log.debug("Removing old configuration for " + value[0] + " and " + value[1]);
                        removeConfiguration(targetURL, current.getKey().toString());
                    }
                    entryIterator.remove();
                }
            }
            if ( !found ) {
                log.debug("Adding new configuration for " + dir + " and " + installPath);
                addConfiguration(targetURL, dir.getAbsolutePath(), installPath);
            }
        }
        // finally remove old configs
        final Iterator<Map.Entry<String,String[]>> entryIterator = oldConfigs.entrySet().iterator();
        while ( entryIterator.hasNext() ) {
            final Map.Entry<String,String[]> current = entryIterator.next();
            final String[] value = current.getValue();
            log.debug("Removing old configuration for " + value[0] + " and " + value[1]);
            // remove old config
            removeConfiguration(targetURL, current.getKey().toString());
        }
    }

    /**
     * Remove configurations from a running OSGi instance for initial content.
     * @param targetURL The web console base url
     * @param file The artifact (bundle)
     * @throws MojoExecutionException
     */
    public void configureUninstall(final String targetURL, final File file) throws MojoExecutionException {
        log.info("Removing file system provider configurations...");

        // now get current configurations
        final Map oldConfigs = getCurrentFileProviderConfigs(targetURL);

        final Iterator entryIterator = oldConfigs.entrySet().iterator();
        while ( entryIterator.hasNext() ) {
            final Map.Entry current = (Map.Entry) entryIterator.next();
            final String[] value = (String[])current.getValue();
            log.debug("Removing old configuration for " + value[0] + " and " + value[1]);
            // remove old config
            removeConfiguration(targetURL, current.getKey().toString());
        }
    }
    
    private void removeConfiguration(final String targetURL, final String pid) throws MojoExecutionException {
        final String postUrl = targetURL  + "/configMgr/" + pid;
        final PostMethod post = new PostMethod(postUrl);
        post.addParameter("apply", "true");
        post.addParameter("delete", "true");
        try {
            final int status = httpClient.executeMethod(post);
            // we get a moved temporarily back from the configMgr plugin
            if (status == HttpStatus.SC_MOVED_TEMPORARILY || status == HttpStatus.SC_OK) {
                log.debug("Configuration removed.");
            }
            else {
                log.error("Removing configuration failed, cause: "+ HttpStatus.getStatusText(status));
            }
        }
        catch (HttpException ex) {
            throw new MojoExecutionException("Removing configuration at " + postUrl
                    + " failed, cause: " + ex.getMessage(), ex);
        }
        catch (IOException ex) {
            throw new MojoExecutionException("Removing configuration at " + postUrl
                    + " failed, cause: " + ex.getMessage(), ex);
        }
        finally {
            post.releaseConnection();
        }
    }

    /**
     * Add a new configuration for the file system provider
     * @throws MojoExecutionException
     */
    private void addConfiguration(final String targetURL, String dir, String path) throws MojoExecutionException {
        final String postUrl = targetURL  + "/configMgr/" + FS_FACTORY;
        final PostMethod post = new PostMethod(postUrl);
        post.addParameter("apply", "true");
        post.addParameter("factoryPid", FS_FACTORY);
        post.addParameter("pid", "[Temporary PID replaced by real PID upon save]");
        post.addParameter("provider.file", dir);
        // save property value to both "provider.roots" and "provider.root" because the name has changed between fsresource 1.x and 2.x
        post.addParameter("provider.root", path);
        post.addParameter("provider.roots", path);
        post.addParameter("propertylist", "provider.root,provider.roots,provider.file");
        try {
            final int status = httpClient.executeMethod(post);
            // we get a moved temporarily back from the configMgr plugin
            if (status == HttpStatus.SC_MOVED_TEMPORARILY || status == HttpStatus.SC_OK) {
                log.info("Configuration created.");
            }
            else {
                log.error("Configuration failed, cause: " + HttpStatus.getStatusText(status));
            }
        }
        catch (HttpException ex) {
            throw new MojoExecutionException("Configuration on " + postUrl
                    + " failed, cause: " + ex.getMessage(), ex);
        }
        catch (IOException ex) {
            throw new MojoExecutionException("Configuration on " + postUrl
                    + " failed, cause: " + ex.getMessage(), ex);
        }
        finally {
            post.releaseConnection();
        }
    }

    /**
     * Return all file provider configs for this project
     * @param targetURL The targetURL of the webconsole
     * @return A map (may be empty) with the pids as keys and a string array
     *         containing the path and the root
     * @throws MojoExecutionException
     */
    private Map<String,String[]> getCurrentFileProviderConfigs(final String targetURL) throws MojoExecutionException {
        log.debug("Getting current file provider configurations.");
        final Map<String,String[]> result = new HashMap<>();
        final String getUrl = targetURL  + "/configMgr/(service.factoryPid=" + FS_FACTORY + ").json";
        final GetMethod get = new GetMethod(getUrl);

        try {
            final int status = httpClient.executeMethod(get);
            if ( status == 200 ) {
                String contentType = get.getResponseHeader(HEADER_CONTENT_TYPE).getValue();
                int pos = contentType.indexOf(';');
                if ( pos != -1 ) {
                    contentType = contentType.substring(0, pos);
                }
                if ( !AbstractBundleInstallMojo.JSON_MIME_TYPE.equals(contentType) ) {
                    log.debug("Response type from web console is not JSON, but " + contentType);
                    throwWebConsoleTooOldException();
                }
                final String jsonText;
                try (InputStream jsonResponse = get.getResponseBodyAsStream()) {
                    jsonText = IOUtils.toString(jsonResponse, CharEncoding.UTF_8);
                }
                try {
                    JsonArray array = JsonSupport.parseArray(jsonText);
                    for(int i=0; i<array.size(); i++) {
                        final JsonObject obj = array.getJsonObject(i);
                        final String pid = obj.getString("pid");
                        final JsonObject properties = obj.getJsonObject("properties");
                        final String path = getConfigPropertyValue(properties, "provider.file");
                        String roots = getConfigPropertyValue(properties, "provider.roots");
                        if (roots == null) {
                            roots = getConfigPropertyValue(properties, "provider.root");
                        }
                        if ( path != null && path.startsWith(this.project.getBasedir().getAbsolutePath()) && roots != null ) {
                            log.debug("Found configuration with pid: " + pid + ", path: " + path + ", roots: " + roots);
                            result.put(pid, new String[] {path, roots});
                        }
                    }
                } catch (JsonException ex) {
                    throw new MojoExecutionException("Reading configuration from " + getUrl
                            + " failed, cause: " + ex.getMessage(), ex);
                }
            }
        }
        catch (HttpException ex) {
            throw new MojoExecutionException("Reading configuration from " + getUrl
                    + " failed, cause: " + ex.getMessage(), ex);
        }
        catch (IOException ex) {
            throw new MojoExecutionException("Reading configuration from " + getUrl
                    + " failed, cause: " + ex.getMessage(), ex);
        }
        finally {
            get.releaseConnection();
        }
        return result;
    }
    
    private String getConfigPropertyValue(JsonObject obj, String subKey) {
        if (obj.containsKey(subKey)) {
            JsonObject subObj = obj.getJsonObject(subKey);
            if (subObj.containsKey("value")) {
                return subObj.getString("value");
            }
            else if (subObj.containsKey("values")) {
                JsonArray array = subObj.getJsonArray("values");
                if (array.size() > 0) {
                    // use only first property value from array
                    return array.getString(0);
                }
            }
        }
        return null;
    }

    /**
     * Get the manifest from the File.
     * @param bundleFile The bundle jar
     * @return The manifest.
     * @throws IOException
     */
    private Manifest getManifest(final File bundleFile) throws IOException {
        JarFile file = null;
        try {
            file = new JarFile(bundleFile);
            return file.getManifest();
        }
        finally {
            if (file != null) {
                try {
                    file.close();
                }
                catch (IOException ex) {
                    // ignore
                }
            }
        }
    }

    /**
     * Helper method to throw a meaningful exception for an outdated felix
     * web console.
     * @throws MojoExecutionException
     */
    private void throwWebConsoleTooOldException() throws MojoExecutionException {
        throw new MojoExecutionException("The Apache Felix Web Console is too old to mount " +
                "the initial content through file system provider configs. " +
                "Either upgrade the web console or disable this feature.");
    }

}

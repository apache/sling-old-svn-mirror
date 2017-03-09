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
package org.apache.sling.maven.bundlesupport.fsresource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.sling.maven.bundlesupport.JsonSupport;

/**
 * Manages OSGi configurations for File System Resource Provider.
 */
final class FsMountHelper {
    
    /** The fs resource provider factory. */
    private static final String FS_FACTORY = "org.apache.sling.fsprovider.internal.FsResourceProvider";
    /** Http header for content type. */
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    
    private static final String PROPERTY_FSMODE = "provider.fs.mode";
    private static final String PROPERTY_ROOT = "provider.root";
    private static final String PROPERTY_ROOTS = "provider.roots";
    private static final String PROPERTY_PATH = "provider.file";
    private static final String PROPERTY_INITIAL_CONTENT_IMPORT_OPTIONS = "provider.initial.content.import.options";
    private static final String PROPERTY_FILEVAULT_FILTER_XML = "provider.filevault.filterxml.path";
        
    private final Log log;
    private final HttpClient httpClient;
    private final MavenProject project;

    public FsMountHelper(Log log, HttpClient httpClient, MavenProject project) {
        this.log = log;
        this.httpClient = httpClient;
        this.project = project;
    }
    
    /**
     * Adds as set of new configurations and removes old ones.
     */
    public void addConfigurations(final String targetUrl, Collection<FsResourceConfiguration> cfgs) throws MojoExecutionException {
        final Map<String,FsResourceConfiguration> oldConfigs = getCurrentConfigurations(targetUrl);

        for (FsResourceConfiguration cfg : cfgs) {
            log.info("Mapping " + cfg.getContentRootDir() + " to " + cfg.getProviderRootPath());
            
            // check if this is already configured
            boolean found = false;
            final Iterator<Map.Entry<String,FsResourceConfiguration>> entryIterator = oldConfigs.entrySet().iterator();
            while ( !found && entryIterator.hasNext() ) {
                final Map.Entry<String,FsResourceConfiguration> current = entryIterator.next();
                final FsResourceConfiguration oldcfg = current.getValue();
                log.debug("Comparing " + oldcfg.getContentRootDir() + " with " + oldcfg);
                if (StringUtils.equals(oldcfg.getContentRootDir(), oldcfg.getContentRootDir())) {
                    if (cfg.equals(oldcfg)) {
                        log.debug("Using existing configuration for " + cfg.getContentRootDir() + " and " + cfg.getProviderRootPath());
                        found = true;
                    }
                    else {
                        // remove old config
                        log.debug("Removing old configuration for " + oldcfg);
                        removeConfiguration(targetUrl, current.getKey());
                    }
                    entryIterator.remove();
                }
            }
            if ( !found ) {
                log.debug("Adding new configuration for " + cfg.getContentRootDir() + " and " + cfg.getProviderRootPath());
                addConfiguration(targetUrl, cfg);
            }
        }
        
        // finally remove old configs
        removeConfigurations(targetUrl, oldConfigs);
    }
    
    /**
     * Add a new configuration for the file system provider
     */
    private void addConfiguration(final String targetUrl, FsResourceConfiguration cfg) throws MojoExecutionException {
        final String postUrl = targetUrl  + "/configMgr/" + FS_FACTORY;
        final PostMethod post = new PostMethod(postUrl);
        post.addParameter("apply", "true");
        post.addParameter("factoryPid", FS_FACTORY);
        post.addParameter("pid", "[Temporary PID replaced by real PID upon save]");
        Map<String,String> props = toMap(cfg);
        for (Map.Entry<String,String> entry : props.entrySet()) {
            post.addParameter(entry.getKey(), entry.getValue());
        }
        post.addParameter("propertylist", StringUtils.join(props.keySet(), ","));
        try {
            final int status = httpClient.executeMethod(post);
            // we get a moved temporarily back from the configMgr plugin
            if (status == HttpStatus.SC_MOVED_TEMPORARILY || status == HttpStatus.SC_OK) {
                log.info("Configuration created.");
            }
            else {
                log.error("Configuration on " + postUrl + " failed, cause: " + HttpStatus.getStatusText(status));
            }
        }
        catch (HttpException ex) {
            throw new MojoExecutionException("Configuration on " + postUrl + " failed, cause: " + ex.getMessage(), ex);
        }
        catch (IOException ex) {
            throw new MojoExecutionException("Configuration on " + postUrl + " failed, cause: " + ex.getMessage(), ex);
        }
        finally {
            post.releaseConnection();
        }
    }
    
    private Map<String,String> toMap(FsResourceConfiguration cfg) {
        Map<String,String> props = new HashMap<>();
        if (cfg.getFsMode() != null) {
            props.put(PROPERTY_FSMODE, cfg.getFsMode().name());
        }
        if (cfg.getContentRootDir() != null) {
            props.put(PROPERTY_PATH, cfg.getContentRootDir());
        }
        if (cfg.getProviderRootPath() != null) {
            // save property value to both "provider.roots" and "provider.root" because the name has changed between fsresource 1.x and 2.x
            props.put(PROPERTY_ROOT, cfg.getProviderRootPath());
            props.put(PROPERTY_ROOTS, cfg.getProviderRootPath());
        }
        if (cfg.getInitialContentImportOptions() != null) {
            props.put(PROPERTY_INITIAL_CONTENT_IMPORT_OPTIONS, cfg.getInitialContentImportOptions());
        }
        if (cfg.getStringVaultFilterXml() != null) {
            props.put(PROPERTY_FILEVAULT_FILTER_XML, cfg.getStringVaultFilterXml());
        }
        return props;
    }
    
    /**
     * Remove all configurations contained in the given config map.
     */
    public void removeConfigurations(final String targetUrl, Map<String,FsResourceConfiguration> configs) throws MojoExecutionException {
        for (Map.Entry<String,FsResourceConfiguration>  current : configs.entrySet()) {
            log.debug("Removing configuration for " + current.getValue());
            // remove old config
            removeConfiguration(targetUrl, current.getKey());
        }
    }

    /**
     * Remove configuration.
     */
    private void removeConfiguration(final String targetUrl, final String pid) throws MojoExecutionException {
        final String postUrl = targetUrl  + "/configMgr/" + pid;
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
     * Return all file provider configs for this project
     * @param targetUrl The targetUrl of the webconsole
     * @return A map (may be empty) with the pids as keys and the configurations as values
     * @throws MojoExecutionException
     */
    public Map<String,FsResourceConfiguration> getCurrentConfigurations(final String targetUrl) throws MojoExecutionException {
        log.debug("Getting current file provider configurations.");
        final Map<String,FsResourceConfiguration> result = new HashMap<>();
        final String getUrl = targetUrl  + "/configMgr/(service.factoryPid=" + FS_FACTORY + ").json";
        final GetMethod get = new GetMethod(getUrl);

        try {
            final int status = httpClient.executeMethod(get);
            if ( status == 200 ) {
                String contentType = get.getResponseHeader(HEADER_CONTENT_TYPE).getValue();
                int pos = contentType.indexOf(';');
                if ( pos != -1 ) {
                    contentType = contentType.substring(0, pos);
                }
                if ( !JsonSupport.JSON_MIME_TYPE.equals(contentType) ) {
                    log.debug("Response type from web console is not JSON, but " + contentType);
                    throw new MojoExecutionException("The Apache Felix Web Console is too old to mount " +
                            "the initial content through file system provider configs. " +
                            "Either upgrade the web console or disable this feature.");
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
                        final String fsmode = getConfigPropertyValue(properties, PROPERTY_FSMODE);
                        final String path = getConfigPropertyValue(properties, PROPERTY_PATH);
                        final String initialContentImportOptions = getConfigPropertyValue(properties, PROPERTY_INITIAL_CONTENT_IMPORT_OPTIONS);
                        final String fileVaultFilterXml = getConfigPropertyValue(properties, PROPERTY_FILEVAULT_FILTER_XML);
                        String root = getConfigPropertyValue(properties, PROPERTY_ROOTS);
                        if (root == null) {
                            root = getConfigPropertyValue(properties, PROPERTY_ROOT);
                        }
                        if (path != null && path.startsWith(this.project.getBasedir().getAbsolutePath()) && root != null) {
                            FsResourceConfiguration cfg = new FsResourceConfiguration()
                                    .fsMode(fsmode)
                                    .providerRootPath(path)
                                    .contentRootDir(root)
                                    .initialContentImportOptions(initialContentImportOptions)
                                    .fileVaultFilterXml(fileVaultFilterXml);
                            log.debug("Found configuration with pid: " + pid + ", " + cfg);
                            result.put(pid, cfg);
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

}

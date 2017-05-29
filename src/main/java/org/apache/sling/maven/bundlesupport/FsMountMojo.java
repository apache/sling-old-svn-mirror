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
import java.io.IOException;
import java.io.InputStream;

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.repository.RepositorySystem;
import org.apache.sling.maven.bundlesupport.deploy.BundleDeploymentMethod;
import org.apache.sling.maven.bundlesupport.deploy.DeployContext;
import org.apache.sling.maven.bundlesupport.fsresource.FileVaultXmlMounter;
import org.apache.sling.maven.bundlesupport.fsresource.SlingInitialContentMounter;

/**
 * Creates OSGi configurations for the
 * <a href="https://sling.apache.org/documentation/bundles/accessing-filesystem-resources-extensions-fsresource.html">Apache Sling File System Resource Provider</a>.
 */
@Mojo(name = "fsmount", requiresProject = true)
public class FsMountMojo extends AbstractFsMountMojo {
    
    private static final String FS_BUNDLE_GROUP_ID = "org.apache.sling"; 
    private static final String FS_BUNDLE_ARTIFACT_ID = "org.apache.sling.fsresource"; 
    private static final String FS_BUNDLE_SYMBOLIC_NAME = FS_BUNDLE_ARTIFACT_ID; 
    
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
    @Parameter(property="sling.deploy.method", required = true, defaultValue = "WebConsole")
    private BundleDeploymentMethod deploymentMethod;
    
    /**
     * Deploy <code>org.apache.sling.fsresource</code> to Sling instance bundle when it is not deployed already.
     */
    @Parameter(required = true, defaultValue = "true")
    private boolean deployFsResourceBundle;
    
    /**
     * Minimum version of <code>org.apache.sling.fsresource</code> bundle. If an older version is installed this version is deployed. 
     */
    @Parameter(required = true, defaultValue = "2.1.2")
    private String minimumFsResourceVersion;

    @Component
    private RepositorySystem repository;
    @Parameter(property = "localRepository", required = true, readonly = true)
    private ArtifactRepository localRepository;
    @Parameter(property = "project.remoteArtifactRepositories", required = true, readonly = true)
    private java.util.List<ArtifactRepository> remoteRepositories;

    @Override
    protected void configureSlingInitialContent(final String targetUrl, final File bundleFile) throws MojoExecutionException {
        new SlingInitialContentMounter(getLog(), getHttpClient(), project).mount(targetUrl, bundleFile);
    }

    @Override
    protected void configureFileVaultXml(String targetUrl, File jcrRootFile, File filterXmlFile) throws MojoExecutionException {
        new FileVaultXmlMounter(getLog(), getHttpClient(), project).mount(targetUrl, jcrRootFile, filterXmlFile);
    }

    @Override
    protected void ensureBundlesInstalled(String targetUrl) throws MojoExecutionException {
        if (!deployFsResourceBundle) {
            return;
        }
        
        boolean deployRequired = false;       
        String fsBundleVersion = getFsResourceBundleInstalledVersion(targetUrl);
        if (StringUtils.isBlank(fsBundleVersion)) {
            deployRequired = true;
        }
        else {
            DefaultArtifactVersion deployedVersion = new DefaultArtifactVersion(fsBundleVersion);
            DefaultArtifactVersion requiredVersion = new DefaultArtifactVersion(minimumFsResourceVersion);
            deployRequired = (deployedVersion.compareTo(requiredVersion) < 0);
        }
        if (!deployRequired) {
            getLog().info("Bundle " + FS_BUNDLE_SYMBOLIC_NAME + " " + fsBundleVersion + " already installed, skipping deployment.");
            return;
        }
        
        getLog().info("Deploying bundle " + FS_BUNDLE_SYMBOLIC_NAME + " " + minimumFsResourceVersion + " ...");
        
        File file = getArtifactFile(FS_BUNDLE_GROUP_ID, FS_BUNDLE_ARTIFACT_ID, minimumFsResourceVersion, "jar");
        deploymentMethod.execute().deploy(targetUrl, file, FS_BUNDLE_SYMBOLIC_NAME, new DeployContext()
                .log(getLog())
                .httpClient(getHttpClient())
                .failOnError(failOnError));
    }

    /**
     * Get version of fsresource bundle that is installed in the instance.
     * @param targetUrl Target URL
     * @return Version number or null if non installed
     * @throws MojoExecutionException
     */
    public String getFsResourceBundleInstalledVersion(final String targetUrl) throws MojoExecutionException {
        final String getUrl = targetUrl + "/bundles/" + FS_BUNDLE_SYMBOLIC_NAME + ".json";
        final GetMethod get = new GetMethod(getUrl);

        try {
            final int status = getHttpClient().executeMethod(get);
            if ( status == 200 ) {                
                final String jsonText;
                try (InputStream jsonResponse = get.getResponseBodyAsStream()) {
                    jsonText = IOUtils.toString(jsonResponse, CharEncoding.UTF_8);
                }
                try {
                    JsonObject response = JsonSupport.parseObject(jsonText);
                    JsonArray data = response.getJsonArray("data");
                    if (data.size() > 0) {
                        JsonObject bundleData = data.getJsonObject(0);
                        return bundleData.getString("version");
                    }
                    
                } catch (JsonException ex) {
                    throw new MojoExecutionException("Reading bundle data from " + getUrl
                            + " failed, cause: " + ex.getMessage(), ex);
                }
            }
        }
        catch (HttpException ex) {
            throw new MojoExecutionException("Reading bundle data from " + getUrl
                    + " failed, cause: " + ex.getMessage(), ex);
        }
        catch (IOException ex) {
            throw new MojoExecutionException("Reading bundle data from " + getUrl
                    + " failed, cause: " + ex.getMessage(), ex);
        }
        finally {
            get.releaseConnection();
        }
        // no version detected, bundle is not installed
        return null;
    }
   
    private File getArtifactFile(String groupId, String artifactId, String version, String type)
            throws MojoExecutionException {
        Artifact artifactObject = repository.createArtifact(groupId, artifactId, version, type);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifactObject);
        request.setLocalRepository(localRepository);
        request.setRemoteRepositories(remoteRepositories);
        ArtifactResolutionResult result = repository.resolve(request);
        if (result.isSuccess()) {
            return artifactObject.getFile();
        } else {
            throw new MojoExecutionException("Unable to download artifact: " + artifactObject.toString());
        }
    }
   
}
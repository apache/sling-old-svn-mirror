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
import java.util.ArrayList;
import java.util.List;

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
import org.apache.sling.maven.bundlesupport.BundlePrerequisite.Bundle;
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
    
    private static final String BUNDLE_GROUP_ID = "org.apache.sling"; 

    private static final String FS_BUNDLE_ARTIFACT_ID = "org.apache.sling.fsresource"; 
    private static final String FS_BUNDLE_DEFAULT_VERSION = "2.1.2"; 
    private static final String FS_BUNDLE_LEGACY_DEFAULT_VERSION = "1.4.2"; 
    
    private static final String RESOURCE_RESOLVER_BUNDLE_ARTIFACT_ID = "org.apache.sling.resourceresolver"; 
    private static final String RESOURCE_RESOLVER_BUNDLE_MIN_VERSION = "1.5.18"; 

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
    @Parameter(property="sling.deploy.method", required = false, defaultValue = "WebConsole")
    private BundleDeploymentMethod deploymentMethod;
    
    /**
     * Deploy <code>org.apache.sling.fsresource</code> to Sling instance bundle when it is not deployed already.
     */
    @Parameter(required = false, defaultValue = "true")
    private boolean deployFsResourceBundle;
    
    /**
     * Bundles that have to be installed as prerequisites to execute this goal.
     * With multiple entries in the list different bundles with different preconditions can be defined.<br/>
     * <strong>Default value is:</strong>:
     * <pre>
     * &lt;deployFsResourceBundlePrerequisites&gt;
     *   &lt;bundlePrerequisite&gt;
     *     &lt;bundles&gt;
     *       &lt;bundle&gt;
     *         &lt;groupId&gt;org.apache.sling&lt;/groupId&gt;
     *         &lt;artifactId&gt;org.apache.sling.fsresource&lt;/artifactId&gt;
     *         &lt;version&gt;2.1.2&lt;/version&gt;
     *       &lt;/bundle&gt;
     *     &lt;/bundles&gt;
     *     &lt;preconditions&gt;
     *       &lt;bundle&gt;
     *         &lt;groupId&gt;org.apache.sling&lt;/groupId&gt;
     *         &lt;artifactId&gt;org.apache.sling.resourceresolver&lt;/artifactId&gt;
     *         &lt;version&gt;1.5.18&lt;/version&gt;
     *       &lt;/bundle&gt;
     *     &lt;/preconditions&gt;
     *   &lt;/bundlePrerequisite&gt;
     *   &lt;bundlePrerequisite&gt;
     *     &lt;bundles&gt;
     *       &lt;bundle&gt;
     *         &lt;groupId&gt;org.apache.sling&lt;/groupId&gt;
     *         &lt;artifactId&gt;org.apache.sling.fsresource&lt;/artifactId&gt;
     *         &lt;version&gt;1.4.2&lt;/version&gt;
     *       &lt;/bundle&gt;
     *     &lt;/bundles&gt;
     *   &lt;/bundlePrerequisite&gt;
     * &lt;/deployFsResourceBundlePrerequisites&gt;
     * </pre>
     */
    @Parameter(required = false)
    private List<BundlePrerequisite> deployFsResourceBundlePrerequisites;

    @Component
    private RepositorySystem repository;
    @Parameter(property = "localRepository", required = true, readonly = true)
    private ArtifactRepository localRepository;
    @Parameter(property = "project.remoteArtifactRepositories", required = true, readonly = true)
    private java.util.List<ArtifactRepository> remoteRepositories;
    
    public void addDeployFsResourceBundlePrerequisite(BundlePrerequisite item) {
        if (this.deployFsResourceBundlePrerequisites == null) {
            this.deployFsResourceBundlePrerequisites = new ArrayList<>();
        }
        this.deployFsResourceBundlePrerequisites.add(item);
    }

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
        
        if (deployFsResourceBundlePrerequisites == null) {
            BundlePrerequisite latest = new BundlePrerequisite();
            latest.addBundle(new Bundle(BUNDLE_GROUP_ID, FS_BUNDLE_ARTIFACT_ID, FS_BUNDLE_DEFAULT_VERSION));
            latest.addPrecondition(new Bundle(BUNDLE_GROUP_ID, RESOURCE_RESOLVER_BUNDLE_ARTIFACT_ID, RESOURCE_RESOLVER_BUNDLE_MIN_VERSION));
            addDeployFsResourceBundlePrerequisite(latest);
            
            BundlePrerequisite legacy = new BundlePrerequisite();
            legacy.addBundle(new Bundle(BUNDLE_GROUP_ID, FS_BUNDLE_ARTIFACT_ID, FS_BUNDLE_LEGACY_DEFAULT_VERSION));
            addDeployFsResourceBundlePrerequisite(legacy);
        }
        
        for (BundlePrerequisite bundlePrerequisite : deployFsResourceBundlePrerequisites) {
            if (isBundlePrerequisitesPreconditionsMet(bundlePrerequisite, targetUrl)) {
                for (Bundle bundle : bundlePrerequisite.getBundles()) {
                    deployBundle(bundle, targetUrl);
                }
                break;
            }
        }
    }
    
    private void deployBundle(Bundle bundle, String targetUrl) throws MojoExecutionException {
        if (isBundleInstalled(bundle, targetUrl)) {
            getLog().debug("Bundle " + bundle.getSymbolicName() + " " + bundle.getVersion() + " (or higher) already installed.");
            return;
        }
        
        getLog().info("Installing Bundle " + bundle.getSymbolicName() + " " + bundle.getVersion() + " to "
                    + targetUrl + " via " + deploymentMethod);
        
        File file = getArtifactFile(bundle, "jar");
        deploymentMethod.execute().deploy(targetUrl, file, bundle.getSymbolicName(), new DeployContext()
                .log(getLog())
                .httpClient(getHttpClient())
                .failOnError(failOnError));
    }
    
    private boolean isBundlePrerequisitesPreconditionsMet(BundlePrerequisite bundlePrerequisite, String targetUrl) throws MojoExecutionException {
        for (Bundle precondition : bundlePrerequisite.getPreconditions()) {
            if (!isBundleInstalled(precondition, targetUrl)) {
                getLog().debug("Bundle " + precondition.getSymbolicName() + " " + precondition.getVersion() + " (or higher) is not installed.");
                return false;
            }
        }
        return true;
    }
    
    private boolean isBundleInstalled(Bundle bundle, String targetUrl) throws MojoExecutionException {
        String installedVersionString = getBundleInstalledVersion(bundle.getSymbolicName(), targetUrl);
        if (StringUtils.isBlank(installedVersionString)) {
            return false;
        }
        DefaultArtifactVersion installedVersion = new DefaultArtifactVersion(installedVersionString);
        DefaultArtifactVersion requiredVersion = new DefaultArtifactVersion(bundle.getVersion());
        return (installedVersion.compareTo(requiredVersion) >= 0);
    }

    /**
     * Get version of fsresource bundle that is installed in the instance.
     * @param targetUrl Target URL
     * @return Version number or null if non installed
     * @throws MojoExecutionException
     */
    private String getBundleInstalledVersion(final String bundleSymbolicName, final String targetUrl) throws MojoExecutionException {
        final String getUrl = targetUrl + "/bundles/" + bundleSymbolicName + ".json";
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
   
    private File getArtifactFile(Bundle bundle, String type) throws MojoExecutionException {
        Artifact artifactObject = repository.createArtifact(bundle.getGroupId(), bundle.getArtifactId(), bundle.getVersion(), type);
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
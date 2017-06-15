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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.sling.maven.bundlesupport.deploy.BundleDeploymentMethod;
import org.apache.sling.maven.bundlesupport.deploy.DeployContext;
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

        deploymentMethod.execute().deploy(targetURL, bundleFile, bundleName, new DeployContext()
                .log(getLog())
                .httpClient(getHttpClient())
                .failOnError(failOnError)
                .bundleStartLevel(bundleStartLevel)
                .bundleStart(bundleStart)
                .mimeType(mimeType)
                .refreshPackages(refreshPackages));

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

}

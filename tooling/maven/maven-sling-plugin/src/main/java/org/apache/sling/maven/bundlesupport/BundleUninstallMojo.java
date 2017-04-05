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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.sling.maven.bundlesupport.fsresource.SlingInitialContentMounter;

/**
 * Uninstall an OSGi bundle from a running Sling instance.
 * 
 * The plugin places by default an HTTP POST request to <a href="http://felix.apache.org/documentation/subprojects/apache-felix-web-console/web-console-restful-api.html#post-requests">
 * Felix Web Console</a> to uninstall the bundle.
 * It's also possible to use HTTP DELETE leveraging the <a href="http://sling.apache.org/documentation/development/repository-based-development.html">WebDAV bundle from Sling</a>.
 * or the <a href="http://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html">Sling POST servlet</a> to uninstall the bundle. 
 * The chosen method depends on the parameter {@link #deploymentMethod}.
 */
@Mojo(name = "uninstall")
public class BundleUninstallMojo extends AbstractBundleInstallMojo {

    /**
     * The name of the generated JAR file.
     */
    @Parameter(property = "sling.file", defaultValue = "${project.build.directory}/${project.build.finalName}.jar")
    private String bundleFileName;

    @Override
    protected String getBundleFileName() {
        return bundleFileName;
    }

    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException {
        // only upload if packaging as an osgi-bundle
        final File bundleFile = new File(bundleFileName);
        final String bundleName = getBundleSymbolicName(bundleFile);
        if (bundleName == null) {
            getLog().info(bundleFile + " is not an OSGi Bundle, not uploading");
            return;
        }

        String targetURL = getTargetURL();

        BundleDeploymentMethod deployMethod = getDeploymentMethod();
        getLog().info(
            "Unistalling Bundle " + bundleName + " from "
                + targetURL + " via " + deployMethod);

        configure(targetURL, bundleFile);

        switch (deployMethod) {
        case SlingPostServlet:
            postToSling(targetURL, bundleFile);
            break;
        case WebConsole:
            postToFelix(targetURL, bundleName);
            break;
        case WebDAV:
            deleteViaWebDav(targetURL, bundleFile);
            break;
        // sanity check to make sure it gets handled in some fashion
        default:
            throw new MojoExecutionException("Unrecognized BundleDeployMethod " + deployMethod);
        }
    }

    protected void deleteViaWebDav(String targetURL, File file)
        throws MojoExecutionException {

        final DeleteMethod delete = new DeleteMethod(getURLWithFilename(targetURL, file.getName()));

        try {

            int status = getHttpClient().executeMethod(delete);
            if (status >= 200 && status < 300) {
                getLog().info("Bundle uninstalled");
            } else {
                getLog().error(
                    "Uninstall failed, cause: "
                        + HttpStatus.getStatusText(status));
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Uninstall from " + targetURL
                + " failed, cause: " + ex.getMessage(), ex);
        } finally {
            delete.releaseConnection();
        }
    }

    @Override
    protected void postToSling(String targetURL, File file)
        throws MojoExecutionException {
        final PostMethod post = new PostMethod(getURLWithFilename(targetURL, file.getName()));

        try {
            // Add SlingPostServlet operation flag for deleting the content
            Part[] parts = new Part[1];
            parts[0] = new StringPart(":operation", "delete");
            post.setRequestEntity(new MultipartRequestEntity(parts,
                    post.getParams()));

            // Request JSON response from Sling instead of standard HTML
            post.setRequestHeader("Accept", JSON_MIME_TYPE);

            int status = getHttpClient().executeMethod(post);
            if (status == HttpStatus.SC_OK) {
                getLog().info("Bundle uninstalled");
            } else {
                getLog().error(
                    "Uninstall failed, cause: "
                        + HttpStatus.getStatusText(status));
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Uninstall from " + targetURL
                + " failed, cause: " + ex.getMessage(), ex);
        } finally {
            post.releaseConnection();
        }
    }

    protected void postToFelix(String targetURL, String symbolicName)
    throws MojoExecutionException {
        final PostMethod post = new PostMethod(targetURL + "/bundles/" + symbolicName);
        post.addParameter("action", "uninstall");

        try {

            int status = getHttpClient().executeMethod(post);
            if (status == HttpStatus.SC_OK) {
                getLog().info("Bundle uninstalled");
            } else {
                getLog().error(
                    "Uninstall failed, cause: "
                        + HttpStatus.getStatusText(status));
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Uninstall from " + targetURL
                + " failed, cause: " + ex.getMessage(), ex);
        } finally {
            post.releaseConnection();
        }
    }

    @Override
    protected void configure(final String targetURL, final File file) throws MojoExecutionException {
        new SlingInitialContentMounter(getLog(), getHttpClient(), project).unmount(targetURL, file);
    }
    
}
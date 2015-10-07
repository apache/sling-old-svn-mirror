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
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Uninstall an OSGi bundle from a running Sling instance.
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
    public void execute() throws MojoExecutionException {
        // only upload if packaging as an osgi-bundle
        final File bundleFile = new File(bundleFileName);
        final String bundleName = getBundleSymbolicName(bundleFile);
        if (bundleName == null) {
            getLog().info(bundleFile + " is not an OSGi Bundle, not uploading");
            return;
        }

        String targetURL = getTargetURL();
        
        getLog().info(
            "Unistalling Bundle " + bundleName + ") from "
                + targetURL + " via " + (usePut ? "DELETE" : "POST"));
        
        configure(targetURL, bundleFile);
        
        if (usePut) {
            delete(targetURL, bundleFile);
        } else {
            post(targetURL, bundleName);
        }
    }

    protected void delete(String targetURL, File file)
        throws MojoExecutionException {
        
        final DeleteMethod delete = new DeleteMethod(getPutURL(targetURL, file.getName()));

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

    protected void post(String targetURL, String symbolicName)
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

    /**
     * Add configurations to a running OSGi instance for initial content.
     * @param targetURL The web console base url
     * @param file The artifact (bundle)
     * @throws MojoExecutionException
     */
    protected void configure(String targetURL, File file)
    throws MojoExecutionException {
        getLog().info("Removing file system provider configurations...");

        // now get current configurations
        final HttpClient client = this.getHttpClient();
        final Map oldConfigs = this.getCurrentFileProviderConfigs(targetURL, client);


        final Iterator entryIterator = oldConfigs.entrySet().iterator();
        while ( entryIterator.hasNext() ) {
            final Map.Entry current = (Map.Entry) entryIterator.next();
            final String[] value = (String[])current.getValue();
            getLog().debug("Removing old configuration for " + value[0] + " and " + value[1]);
            // remove old config
            removeConfiguration(client, targetURL, current.getKey().toString());
        }
    }

}
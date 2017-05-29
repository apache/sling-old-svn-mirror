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
package org.apache.sling.maven.bundlesupport.deploy.method;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.FilePartSource;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.sling.maven.bundlesupport.deploy.DeployContext;
import org.apache.sling.maven.bundlesupport.deploy.DeployMethod;

public class FelixPostDeployMethod implements DeployMethod {

    @Override
    public void deploy(String targetURL, File file, String bundleSymbolicName, DeployContext context) throws MojoExecutionException {

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
            partList.add(new StringPart("bundlestartlevel", context.getBundleStartLevel()));

            if (context.isBundleStart()) {
                partList.add(new StringPart("bundlestart", "start"));
            }

            if (context.isRefreshPackages()) {
                partList.add(new StringPart("refreshPackages", "true"));
            }

            Part[] parts = partList.toArray(new Part[partList.size()]);

            filePost.setRequestEntity(new MultipartRequestEntity(parts,
                filePost.getParams()));

            int status = context.getHttpClient().executeMethod(filePost);
            if (status == HttpStatus.SC_OK) {
                context.getLog().info("Bundle installed");
            } else {
                String msg = "Installation failed, cause: "
                    + HttpStatus.getStatusText(status);
                if (context.isFailOnError()) {
                    throw new MojoExecutionException(msg);
                } else {
                    context.getLog().error(msg);
                }
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Installation on " + targetURL
                + " failed, cause: " + ex.getMessage(), ex);
        } finally {
            filePost.releaseConnection();
        }
    }

    @Override
    public void undeploy(String targetURL, File file, String bundleSymbolicName, DeployContext context) throws MojoExecutionException {
        final PostMethod post = new PostMethod(targetURL + "/bundles/" + bundleSymbolicName);
        post.addParameter("action", "uninstall");

        try {

            int status = context.getHttpClient().executeMethod(post);
            if (status == HttpStatus.SC_OK) {
                context.getLog().info("Bundle uninstalled");
            } else {
                context.getLog().error(
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

}

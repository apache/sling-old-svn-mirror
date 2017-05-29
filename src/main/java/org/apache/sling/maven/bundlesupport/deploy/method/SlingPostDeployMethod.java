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

import static org.apache.sling.maven.bundlesupport.JsonSupport.JSON_MIME_TYPE;

import java.io.File;

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

public class SlingPostDeployMethod implements DeployMethod {

    @Override
    public void deploy(String targetURL, File file, String bundleSymbolicName, DeployContext context) throws MojoExecutionException {
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
                file.getName(), file), context.getMimeType(), null);
            // Add TypeHint to have jar be uploaded as file (not as resource)
            parts[1] = new StringPart("*@TypeHint", "nt:file");

            /* Request JSON response from Sling instead of standard HTML, to
             * reduce the payload size (if the PostServlet supports it). */
            filePost.setRequestHeader("Accept", JSON_MIME_TYPE);
            filePost.setRequestEntity(new MultipartRequestEntity(parts,
                filePost.getParams()));

            int status = context.getHttpClient().executeMethod(filePost);
            // SlingPostServlet may return 200 or 201 on creation, accept both
            if (status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED) {
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
        final PostMethod post = new PostMethod(getURLWithFilename(targetURL, file.getName()));

        try {
            // Add SlingPostServlet operation flag for deleting the content
            Part[] parts = new Part[1];
            parts[0] = new StringPart(":operation", "delete");
            post.setRequestEntity(new MultipartRequestEntity(parts,
                    post.getParams()));

            // Request JSON response from Sling instead of standard HTML
            post.setRequestHeader("Accept", JSON_MIME_TYPE);

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

    /**
     * Returns the URL with the filename appended to it.
     * @param targetURL the original requested targetURL to append fileName to
     * @param fileName the name of the file to append to the targetURL.
     */
    private String getURLWithFilename(String targetURL, String fileName) {
        return targetURL + (targetURL.endsWith("/") ? "" : "/") + fileName;
    }
    
}

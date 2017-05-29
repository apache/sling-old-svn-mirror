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
import java.io.IOException;
import java.util.List;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.sling.maven.bundlesupport.deploy.DeployContext;
import org.apache.sling.maven.bundlesupport.deploy.DeployMethod;

public class WebDavPutDeployMethod implements DeployMethod {

    @Override
    public void deploy(String targetURL, File file, String bundleSymbolicName, DeployContext context) throws MojoExecutionException {
        boolean success = false;
        int status;

        try {
            status = performPut(targetURL, file, context);
            if (status >= 200 && status < 300) {
                success = true;
            } else if (status == HttpStatus.SC_CONFLICT) {

                context.getLog().debug("Bundle not installed due missing parent folders. Attempting to create parent structure.");
                createIntermediaryPaths(targetURL, context);

                context.getLog().debug("Re-attempting bundle install after creating parent folders.");
                status = performPut(targetURL, file, context);
                if (status >= 200 && status < 300) {
                    success = true;
                }
            }

            if (!success) {
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
        }
    }

    @Override
    public void undeploy(String targetURL, File file, String bundleSymbolicName, DeployContext context) throws MojoExecutionException {
        final DeleteMethod delete = new DeleteMethod(getURLWithFilename(targetURL, file.getName()));

        try {

            int status = context.getHttpClient().executeMethod(delete);
            if (status >= 200 && status < 300) {
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
            delete.releaseConnection();
        }
    }

    private int performPut(String targetURL, File file, DeployContext context) throws HttpException, IOException {
        PutMethod filePut = new PutMethod(getURLWithFilename(targetURL, file.getName()));
        try {
            filePut.setRequestEntity(new FileRequestEntity(file, context.getMimeType()));
            return context.getHttpClient().executeMethod(filePut);
        } finally {
            filePut.releaseConnection();
        }
    }

    private int performHead(String uri, DeployContext context) throws HttpException, IOException {
        HeadMethod head = new HeadMethod(uri);
        try {
            return context.getHttpClient().executeMethod(head);
        } finally {
            head.releaseConnection();
        }
    }

    private int performMkCol(String uri, DeployContext context) throws IOException {
        MkColMethod mkCol = new MkColMethod(uri);
        try {
            return context.getHttpClient().executeMethod(mkCol);
        } finally {
            mkCol.releaseConnection();
        }
    }

    private void createIntermediaryPaths(String targetURL, DeployContext context) throws HttpException, IOException, MojoExecutionException {
        // extract all intermediate URIs (longest one first)
        List<String> intermediateUris = IntermediateUrisExtractor.extractIntermediateUris(targetURL);

        // 1. go up to the node in the repository which exists already (HEAD request towards the root node)
        String existingIntermediateUri = null;
        // go through all intermediate URIs (longest first)
        for (String intermediateUri : intermediateUris) {
            // until one is existing
            int result = performHead(intermediateUri, context) ;
            if (result == HttpStatus.SC_OK) {
                existingIntermediateUri = intermediateUri;
                break;
            } else if (result != HttpStatus.SC_NOT_FOUND) {
                throw new MojoExecutionException("Failed getting intermediate path at " + intermediateUri + "."
                        + " Reason: " + HttpStatus.getStatusText(result));
            }
        }

        if (existingIntermediateUri == null) {
            throw new MojoExecutionException(
                    "Could not find any intermediate path up until the root of " + targetURL + ".");
        }

        // 2. now create from that level on each intermediate node individually towards the target path
        int startOfInexistingIntermediateUri = intermediateUris.indexOf(existingIntermediateUri);
        if (startOfInexistingIntermediateUri == -1) {
            throw new IllegalStateException(
                    "Could not find intermediate uri " + existingIntermediateUri + " in the list");
        }

        for (int index = startOfInexistingIntermediateUri - 1; index >= 0; index--) {
            // use MKCOL to create the intermediate paths
            String intermediateUri = intermediateUris.get(index);
            int result = performMkCol(intermediateUri, context);
            if (result == HttpStatus.SC_CREATED || result == HttpStatus.SC_OK) {
                context.getLog().debug("Intermediate path at " + intermediateUri + " successfully created");
                continue;
            } else {
                throw new MojoExecutionException("Failed creating intermediate path at '" + intermediateUri + "'."
                        + " Reason: " + HttpStatus.getStatusText(result));
            }
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

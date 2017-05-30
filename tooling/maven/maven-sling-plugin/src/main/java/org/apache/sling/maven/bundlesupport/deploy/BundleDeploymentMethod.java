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
package org.apache.sling.maven.bundlesupport.deploy;

import org.apache.sling.maven.bundlesupport.deploy.method.FelixPostDeployMethod;
import org.apache.sling.maven.bundlesupport.deploy.method.SlingPostDeployMethod;
import org.apache.sling.maven.bundlesupport.deploy.method.WebDavPutDeployMethod;

/**
 * Possible methodologies for deploying (installing and uninstalling)
 * bundles from the remote server.
 * Use camel-case values because those are used when you configure the plugin (and uppercase with separators "_" just looks ugly in that context)
 */
public enum BundleDeploymentMethod {
    
    /**
     * Via POST to Felix Web Console
     */
    WebConsole(new FelixPostDeployMethod()),

    /**
     * Via WebDAV
     */
    WebDAV(new WebDavPutDeployMethod()),

    /**
     * Via POST to Sling directly
     */
    SlingPostServlet(new SlingPostDeployMethod());
    
    
    private final DeployMethod deployMethod;

    private BundleDeploymentMethod(DeployMethod deployMethod) {
        this.deployMethod = deployMethod;
    }
    
    public DeployMethod execute() {
        return deployMethod;
    }

}

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
package org.apache.sling.distribution.packaging;

import javax.annotation.CheckForNull;
import java.net.URI;

import org.apache.sling.distribution.communication.DistributionRequestType;

/**
 * Additional information about a package.
 * Additional information is optional and components should expect every piece of it to be null.
 */
public final class DistributionPackageInfo {

    private URI origin;
    private DistributionRequestType requestType;
    private String[] paths;

    /**
     * get the paths covered by the package holding this info
     *
     * @return an array of paths
     */
    @CheckForNull
    public String[] getPaths() {
        return paths;
    }

    /**
     * get the request type associated to the package holding this info
     *
     * @return the request type
     */
    @CheckForNull
    public DistributionRequestType getRequestType() {
        return requestType;
    }

    /**
     * retrieves the origin of the package holding this info
     *
     * @return the package origin
     */
    @CheckForNull
    public URI getOrigin() {
        return origin;
    }

    /**
     * sets the origin of the package.
     *
     * @param origin the originating instance of this package
     */
    public void setOrigin(URI origin) {
        this.origin = origin;
    }

    /**
     * sets the request type for the package holding this info
     *
     * @param requestType the request type that originated this package
     */
    public void setRequestType(DistributionRequestType requestType) {
        this.requestType = requestType;
    }

    /**
     * sets the paths "covered" by the package holding this info
     *
     * @param paths the paths "covered" by this package
     */
    public void setPaths(String[] paths) {
        this.paths = paths;
    }

    /**
     * fills the current info object from the provided one.
     *
     * @param packageInfo package metadata
     */
    public void fillInfo(DistributionPackageInfo packageInfo) {
        if (packageInfo != null) {
            this.setOrigin(packageInfo.getOrigin());
            this.setPaths(packageInfo.getPaths());
            this.setRequestType(packageInfo.getRequestType());
        }
    }
}

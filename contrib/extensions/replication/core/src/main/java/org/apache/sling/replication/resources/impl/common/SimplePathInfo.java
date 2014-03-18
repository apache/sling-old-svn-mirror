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

package org.apache.sling.replication.resources.impl.common;

/**
 * Path info representing a main resource.
 * The requestPath = resourcePath + resourcePathInfo
 * The identified resource can be of three types:
 *      root  - resourcePath = resourceRoot
 *      main  - resourcePath = resourceRoot + mainResourceName
 *      child - resourcePath = resourceRoot + mainResourceName + childResourceName
 */
public class SimplePathInfo {

    String resourcePathInfo;

    // resourcePath = resourceRoot + "/" + mainResourceName + "/" + childResourceName
    private String resourceRoot;
    private String mainResourceName;
    private String childResourceName;

    public SimplePathInfo(String resourcePathInfo, String resourceRoot, String resourceName, String childResourceName) {

        this.resourcePathInfo = resourcePathInfo;
        this.resourceRoot = resourceRoot;
        this.mainResourceName = resourceName;
        this.childResourceName = childResourceName;
    }

    public String getResourcePathInfo() {
        return resourcePathInfo;
    }

    public String getMainResourceName() {
        return mainResourceName;
    }

    public String getChildResourceName() {
        return childResourceName;
    }

    public boolean isRoot() {
        return mainResourceName == null;
    }

    public boolean isMain() {
        return mainResourceName != null && childResourceName == null;
    }

    public boolean isChild() {
        return mainResourceName != null && childResourceName != null;
    }

    public String  getResourcePath() {
        if (isRoot()) {
            return resourceRoot;
        }
        else if (isMain()) {
            return resourceRoot + "/" + mainResourceName;
        }
        else if (isChild()) {
            return resourceRoot + "/" + mainResourceName + "/" + childResourceName;
        }

        return null;
    }


    public static SimplePathInfo parsePathInfo(String resourceRoot, String requestPath) {
        if (!requestPath.startsWith(resourceRoot)) {
            return null;
        }

        String resourceName = null;
        String resourcePathInfo = null;

        if(requestPath.startsWith(resourceRoot + "/")) {
            resourceName = requestPath.substring(resourceRoot.length()+1);
            int idx = resourceName.indexOf(".");
            if (idx >= 0) {
                resourcePathInfo = resourceName.substring(idx);
                resourceName = resourceName.substring(0, idx);
            }
        }
        else {
            int idx = requestPath.indexOf(".");
            if (requestPath.contains(".")) {
                resourcePathInfo = requestPath.substring(idx);
            }
        }

        String childResourceName = null;

        if (resourceName != null) {
            int idx = resourceName.indexOf("/");
            if (idx >= 0) {
                childResourceName = resourceName.substring(idx+1);
                resourceName = resourceName.substring(0, idx);
            }
        }
        return new SimplePathInfo(resourcePathInfo, resourceRoot, resourceName, childResourceName);
    }
}



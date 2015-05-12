/*******************************************************************************
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
 ******************************************************************************/

package org.apache.sling.scripting.sightly.impl.engine.compiled;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;

/**
 * Identifies a Java source file in a JCR repository.
 */
public class SourceIdentifier {

    private final String className;
    private final Resource resource;
    private final String packageName;
    private final String fullyQualifiedName;

    public SourceIdentifier(Resource resource, String classNamePrefix) {
        this.resource = resource;
        this.className = buildClassName(resource, classNamePrefix);
        this.packageName = buildPackageName(resource);
        this.fullyQualifiedName = buildFullyQualifiedName(packageName, className);
    }

    public String getClassName() {
        return className;
    }

    public Resource getResource() {
        return resource;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    private String buildFullyQualifiedName(String packageName, String className) {
        return packageName + "." + className;
    }

    private String buildClassName(Resource resource, String classNamePrefix) {
        String scriptName = ResourceUtil.getName(resource.getPath());
        scriptName = scriptName.substring(0, scriptName.lastIndexOf(getExtension(scriptName)));
        String className = classNamePrefix + scriptName;
        return className.replaceAll("-", "_").replaceAll("\\.", "_");
    }

    private String buildPackageName(Resource resource) {
        return ResourceUtil.getParent(resource.getPath())
                .replaceAll("/", ".")
                .substring(1)
                .replaceAll("-", "_");
    }

    private String getExtension(String scriptName) {
        if (StringUtils.isEmpty(scriptName)) {
            return null;
        }
        int lastDotIndex = scriptName.lastIndexOf('.');
        return scriptName.substring(lastDotIndex);
    }
}

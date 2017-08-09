/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.js.impl;

import java.util.Collections;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.engine.ResourceResolution;

/**
 * Utilities for script evaluation
 */
public class Utils {
    public static final String JS_EXTENSION = "js";

    public static final Bindings EMPTY_BINDINGS = new SimpleBindings(Collections.<String, Object>emptyMap());

    public static SlingScriptHelper getHelper(Bindings bindings) {
        return (SlingScriptHelper) bindings.get(SlingBindings.SLING);
    }

    public static boolean isJsScript(String identifier) {
        String extension = StringUtils.substringAfterLast(identifier, ".");
        return JS_EXTENSION.equalsIgnoreCase(extension);
    }

    public static Resource getScriptResource(Resource caller, String path, Bindings bindings) {
        Resource scriptResource = caller.getChild(path);
        if (scriptResource == null) {
            Resource componentCaller = ResourceResolution
                    .getResourceForRequest(caller.getResourceResolver(), (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST));
            if (isResourceOverlay(caller, componentCaller)) {
                scriptResource = ResourceResolution.getResourceFromSearchPath(componentCaller, path);
            } else {
                scriptResource = ResourceResolution.getResourceFromSearchPath(caller, path);
            }
        }
        if (scriptResource == null) {
            throw new SightlyException("Required script resource could not be located: " + path + ". The caller is " + caller.getPath());
        }
        return scriptResource;
    }

    /**
     * Using the inheritance chain created with the help of {@code sling:resourceSuperType} this method checks if {@code resourceB} inherits
     * from {@code resourceA}. In case {@code resourceA} is a {@code nt:file}, its parent will be used for the inheritance check.
     *
     * @param resourceA the base resource
     * @param resourceB the potentially overlaid resource
     * @return {@code true} if {@code resourceB} overlays {@code resourceB}, {@code false} otherwise
     */
    private static boolean isResourceOverlay(Resource resourceA, Resource resourceB) {
        String resourceBSuperType = resourceB.getResourceSuperType();
        if (StringUtils.isNotEmpty(resourceBSuperType)) {
            ResourceResolver resolver = resourceA.getResourceResolver();
            String parentResourceType = resourceA.getResourceType();
            if ("nt:file".equals(parentResourceType)) {
                parentResourceType = ResourceUtil.getParent(resourceA.getPath());
                if (parentResourceType.equals(resourceB.getPath())) {
                    return true;
                }
            }
            Resource parentB = resolver.getResource(resourceBSuperType);
            while (parentB != null && !"/".equals(parentB.getPath()) && StringUtils.isNotEmpty(resourceBSuperType)) {
                if (parentB.getPath().equals(parentResourceType)) {
                    return true;
                }
                resourceBSuperType = parentB.getResourceSuperType();
                parentB = resolver.getResource(resourceBSuperType);
            }
        }
        return false;
    }

}

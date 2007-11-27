/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.jsp.taglib;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.services.ServiceLocator;

/**
 * This class defines the scripting variables that are created by the
 * <code>DefineObjectsTag</code>.
 */
public class DefineObjectsTEI extends TagExtraInfo {

    /**
     * The name of the tag attribute used to define the name of the
     * RenderRequest scripting variable (value is "requestName").
     */
    public static final String ATTR_REQUEST_NAME = "requestName";

    /**
     * The name of the tag attribute used to define the name of the
     * RenderResponse scripting variable (value is "responseName").
     */
    public static final String ATTR_RESPONSE_NAME = "responseName";

    /**
     * The name of the tag attribute used to define the name of the Resource
     * scripting variable (value is "resourceName").
     */
    public static final String ATTR_RESOURCE_NAME = "resourceName";

    /**
     * The name of the tag attribute used to define the name of the ServiceLocator
     * scripting variable (value is "serviceLocatorName").
     */
    public static final String ATTR_SERVICE_LOCATOR_NAME = "serviceLocatorName";

    /**
     * The name of the tag attribute used to define the name of the
     * ResourceManager scripting variable (value is "resourceManagerName").
     */
    public static final String ATTR_RESOURCE_MANAGER_NAME = "resourceManagerName";

    /**
     * The name of the tag attribute used to define the type of the
     * ResourceManager scripting variable (value is "resourceManagerClass").
     */
    public static final String ATTR_RESOURCE_MANAGER_CLASS = "resourceManagerClass";

    private static final String RENDER_REQUEST_CLASS = SlingHttpServletRequest.class.getName();

    private static final String RENDER_RESPONSE_CLASS = SlingHttpServletResponse.class.getName();

    private static final String RESOURCE_CLASS = Resource.class.getName();

    private static final String SERVICE_LOCATOR_CLASS = ServiceLocator.class.getName();

    /**
     * Returns an Array of <code>VariableInfo</code> objects describing
     * scripting variables.
     *
     * @see javax.servlet.jsp.tagext.TagExtraInfo#getVariableInfo(TagData)
     */
    public VariableInfo[] getVariableInfo(TagData data) {
        String requestName = getValue(data, ATTR_REQUEST_NAME,
            DefineObjectsTag.DEFAULT_REQUEST_NAME);
        String responseName = getValue(data, ATTR_RESPONSE_NAME,
            DefineObjectsTag.DEFAULT_RESPONSE_NAME);
        String resourceName = getValue(data, ATTR_RESOURCE_NAME,
            DefineObjectsTag.DEFAULT_RESOURCE_NAME);
        String resourceManagerName = getValue(data, ATTR_RESOURCE_MANAGER_NAME,
            DefineObjectsTag.DEFAULT_RESOURCE_MANAGER_NAME);
        String resourceManagerClass = getValue(data, ATTR_RESOURCE_MANAGER_CLASS,
            DefineObjectsTag.DEFAULT_RESOURCE_MANAGER_CLASS);
        String serviceLocatorName = getValue(data, ATTR_SERVICE_LOCATOR_NAME,
            DefineObjectsTag.DEFAULT_SERVICE_LOCATOR_NAME);

        return new VariableInfo[] {
            new VariableInfo(requestName, RENDER_REQUEST_CLASS, true,
                VariableInfo.AT_END),
            new VariableInfo(responseName, RENDER_RESPONSE_CLASS, true,
                VariableInfo.AT_END),
            new VariableInfo(resourceName, RESOURCE_CLASS, true,
                VariableInfo.AT_END),
            new VariableInfo(resourceManagerName, resourceManagerClass, true,
                VariableInfo.AT_END),
            new VariableInfo(serviceLocatorName, SERVICE_LOCATOR_CLASS, true,
                VariableInfo.AT_END) };
    }

    private String getValue(TagData data, String name, String defaultValue) {
        Object value = data.getAttribute(name);
        if (value instanceof String) {
            return (String) value;
        }

        return defaultValue;
    }
}

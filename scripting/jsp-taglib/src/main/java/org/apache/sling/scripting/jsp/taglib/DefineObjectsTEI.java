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

import static org.apache.sling.scripting.jsp.taglib.DefineObjectsTag.DEFAULT_MAPPED_OBJECT_NAME;
import static org.apache.sling.scripting.jsp.taglib.DefineObjectsTag.DEFAULT_NODE_NAME;
import static org.apache.sling.scripting.jsp.taglib.DefineObjectsTag.DEFAULT_REQUEST_NAME;
import static org.apache.sling.scripting.jsp.taglib.DefineObjectsTag.DEFAULT_RESOURCE_MANAGER_CLASS;
import static org.apache.sling.scripting.jsp.taglib.DefineObjectsTag.DEFAULT_RESOURCE_MANAGER_NAME;
import static org.apache.sling.scripting.jsp.taglib.DefineObjectsTag.DEFAULT_RESOURCE_NAME;
import static org.apache.sling.scripting.jsp.taglib.DefineObjectsTag.DEFAULT_RESPONSE_NAME;
import static org.apache.sling.scripting.jsp.taglib.DefineObjectsTag.DEFAULT_SERVICE_LOCATOR_NAME;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
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
     * The name of the tag attribute used to define the name of the Node
     * scripting variable (value is "nodeName").
     */
    public static final String ATTR_NODE_NAME = "resourceName";

    /**
     * The name of the tag attribute used to define the name of the mapped
     * object scripting variable (value is "mappedObjectName").
     */
    public static final String ATTR_MAPPED_OBJECT_NAME = "mappedObjectName";

    /**
     * The name of the tag attribute used to define the type of the scripting
     * variable to take for the mapped object (value is "mappedObjectClass").
     */
    public static final String ATTR_MAPPED_OBJECT_CLASS = "mappedObjectClass";

    /**
     * The name of the tag attribute used to define the name of the
     * ServiceLocator scripting variable (value is "serviceLocatorName").
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

    private static final String NODE_CLASS = Node.class.getName();

    private static final String SERVICE_LOCATOR_CLASS = ServiceLocator.class.getName();

    /**
     * Returns an Array of <code>VariableInfo</code> objects describing
     * scripting variables.
     *
     * @see javax.servlet.jsp.tagext.TagExtraInfo#getVariableInfo(TagData)
     */
    public VariableInfo[] getVariableInfo(TagData data) {

        List<VariableInfo> varInfos = new ArrayList<VariableInfo>();

        addVar(varInfos, data, ATTR_REQUEST_NAME, DEFAULT_REQUEST_NAME,
            RENDER_REQUEST_CLASS);
        addVar(varInfos, data, ATTR_RESPONSE_NAME, DEFAULT_RESPONSE_NAME,
            RENDER_RESPONSE_CLASS);

        addVar(varInfos, data, ATTR_RESOURCE_NAME, DEFAULT_RESOURCE_NAME,
            RESOURCE_CLASS);
        addVar(varInfos, data, ATTR_NODE_NAME, DEFAULT_NODE_NAME, NODE_CLASS);

        String mappedObjectClass = getValue(data, ATTR_MAPPED_OBJECT_CLASS,
            null);
        addVar(varInfos, data, ATTR_MAPPED_OBJECT_NAME,
            DEFAULT_MAPPED_OBJECT_NAME, mappedObjectClass);

        String resourceManagerClass = getValue(data,
            ATTR_RESOURCE_MANAGER_CLASS, DEFAULT_RESOURCE_MANAGER_CLASS);
        addVar(varInfos, data, ATTR_RESOURCE_MANAGER_NAME,
            DEFAULT_RESOURCE_MANAGER_NAME, resourceManagerClass);

        addVar(varInfos, data, ATTR_SERVICE_LOCATOR_NAME,
            DEFAULT_SERVICE_LOCATOR_NAME, SERVICE_LOCATOR_CLASS);

        return varInfos.toArray(new VariableInfo[varInfos.size()]);

    }

    private void addVar(List<VariableInfo> varInfos, TagData data,
            String attrName, String defaultValue, String varClass) {
        String value = getValue(data, attrName, defaultValue);

        if (value != null && varClass != null) {
            varInfos.add(new VariableInfo(value, varClass, true,
                VariableInfo.AT_END));
        }
    }

    private String getValue(TagData data, String name, String defaultValue) {
        Object value = data.getAttribute(name);
        if (value instanceof String) {
            return (String) value;
        }

        return defaultValue;
    }
}

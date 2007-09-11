/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.sling.core.ServiceLocator;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.content.ContentManager;

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
     * The name of the tag attribute used to define the name of the Content
     * scripting variable (value is "contentName").
     */
    public static final String ATTR_CONTENT_NAME = "contentName";

    /**
     * The name of the tag attribute used to define the type of the Content
     * scripting variable (value is "contentClass").
     */
    public static final String ATTR_CONTENT_CLASS = "contentClass";

    /**
     * The name of the tag attribute used to define the name of the handle
     * scripting variable (value is "handleName").
     */
    public static final String ATTR_HANDLE_NAME = "handleName";

    /**
     * The name of the tag attribute used to define the name of the ServiceLocator
     * scripting variable (value is "serviceLocatorName").
     */
    public static final String ATTR_SERVICE_LOCATOR_NAME = "serviceLocatorName";

    /**
     * The name of the tag attribute used to define the name of the
     * ContentManager scripting variable (value is "contentManagerName").
     */
    public static final String ATTR_CONTENT_MANAGER_NAME = "contentManagerName";

    private static final String RENDER_REQUEST_CLASS = ComponentRequest.class.getName();

    private static final String RENDER_RESPONSE_CLASS = ComponentResponse.class.getName();

    private static final String STRING_CLASS = "String"; // always imported

    private static final String CONTENT_MANAGER_CLASS = ContentManager.class.getName();

    private static final String SERVICE_LOCATOR_CLASS = ServiceLocator.class.getName();

    /**
     * Returns an Array of <code>VariableInfo</code> objects describing
     * scripting variables.
     *
     * @see javax.servlet.jsp.tagext.TagExtraInfo#getVariableInfo(TagData)
     */
    public VariableInfo[] getVariableInfo(TagData data) {
        String requestName = this.getValue(data, ATTR_REQUEST_NAME,
            DefineObjectsTag.DEFAULT_REQUEST_NAME);
        String responseName = this.getValue(data, ATTR_RESPONSE_NAME,
            DefineObjectsTag.DEFAULT_RESPONSE_NAME);
        String contentName = this.getValue(data, ATTR_CONTENT_NAME,
            DefineObjectsTag.DEFAULT_CONTENT_NAME);
        String contentClassName = this.getValue(data, ATTR_CONTENT_CLASS,
            DefineObjectsTag.DEFAULT_CONTENT_CLASS);
        String handleName = this.getValue(data, ATTR_HANDLE_NAME,
            DefineObjectsTag.DEFUALT_HANDLE_NAME);
        String contentManagerName = this.getValue(data, ATTR_CONTENT_MANAGER_NAME,
            DefineObjectsTag.DEFAULT_CONTENT_MANAGER_NAME);
        String serviceLocatorName = this.getValue(data, ATTR_SERVICE_LOCATOR_NAME,
            DefineObjectsTag.DEFAULT_SERVICE_LOCATOR_NAME);

        return new VariableInfo[] {
            new VariableInfo(requestName, RENDER_REQUEST_CLASS, true,
                VariableInfo.AT_END),
            new VariableInfo(responseName, RENDER_RESPONSE_CLASS, true,
                VariableInfo.AT_END),
            new VariableInfo(contentName, contentClassName, true,
                VariableInfo.AT_END),
            new VariableInfo(handleName, STRING_CLASS, true,
                VariableInfo.AT_END),
            new VariableInfo(contentManagerName, CONTENT_MANAGER_CLASS, true,
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

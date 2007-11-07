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

import javax.servlet.jsp.tagext.TagSupport;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceManager;
import org.apache.sling.scripting.jsp.util.TagUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class DefineObjectsTag extends TagSupport {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(DefineObjectsTag.class);

    private static final long serialVersionUID = -1858674361149195892L;

    /**
     * Default name for the scripting variable referencing the
     * <code>SlingHttpServletRequest</code> object (value is "renderRequest").
     */
    public static final String DEFAULT_REQUEST_NAME = "renderRequest";

    /**
     * Default name for the scripting variable referencing the
     * <code>SlingHttpServletResponse</code> object (value is "renderResponse").
     */
    public static final String DEFAULT_RESPONSE_NAME = "renderResponse";

    /**
     * Default name for the scripting variable referencing the current
     * <code>Resource</code> object (value is "resource").
     */
    public static final String DEFAULT_RESOURCE_NAME = "resource";

    /**
     * Default name for the scripting variable referencing the current
     * <code>ResourceManager</code> (value is "resourceManager").
     */
    public static final String DEFAULT_RESOURCE_MANAGER_NAME = "resourceManager";

    /**
     * Default name of the Java type for the scripting variable referencing the
     * current <code>ResourceManager</code> (value is the fully qualified name
     * of the <code>ResourceManager</code> interface).
     */
    public static final String DEFAULT_RESOURCE_MANAGER_CLASS = ResourceManager.class.getName();

    /**
     * Default name for the scripting variable referencing the current
     * <code>ServiceLocator</code> (value is "serviceLocator").
     */
    public static final String DEFAULT_SERVICE_LOCATOR_NAME = "serviceLocator";

    private String requestName = DEFAULT_REQUEST_NAME;

    private String responseName = DEFAULT_RESPONSE_NAME;

    private String resourceName = DEFAULT_RESOURCE_NAME;

    private String resourceManagerName = DEFAULT_RESOURCE_MANAGER_NAME;

    private String resourceManagerClass = DEFAULT_RESOURCE_MANAGER_CLASS;

    private String serviceLocatorName = DEFAULT_SERVICE_LOCATOR_NAME;

    /**
     * Default constructor.
     */
    public DefineObjectsTag() {
    }

    /**
     * Creates Scripting variables for:
     * <ul>
     * <li><code>SlingHttpServletRequest</code>
     * <li><code>SlingHttpServletResponse</code>
     * <li>current <code>Resource</code>
     * <li>current <code>ResourcManager</code>
     * <li>current <code>ServiceLocator</code>
     * </ul>
     *
     * @return always {@link #EVAL_PAGE}.
     */
    public int doEndTag() {

        SlingHttpServletRequest req = TagUtil.getRequest(pageContext);
        SlingHttpServletResponse res = TagUtil.getResponse(pageContext);
        Resource resource = req.getResource();
        ResourceManager resourceManager = TagUtil.getResourceManager(pageContext);

        pageContext.setAttribute(requestName, req);
        pageContext.setAttribute(responseName, res);
        pageContext.setAttribute(resourceManagerName, resourceManager);
        pageContext.setAttribute(resourceManagerClass, resourceManager.getClass().getName());

        // content may be null
        if (resource != null) {
            pageContext.setAttribute(resourceName, resource);
        } else {
            TagUtil.log(log, pageContext, "RenderRequest has no Content !",
                null);
        }

        pageContext.setAttribute(serviceLocatorName, req.getServiceLocator());

        return EVAL_PAGE;
    }

    // --------------------------< setter methonds >----------------------------

    public void setRequestName(String requestName) {
        this.requestName = requestName;
    }

    public void setResponseName(String responseName) {
        this.responseName = responseName;
    }

    public void setResourceName(String name) {
        this.resourceName = name;
    }

    public void setResourceManagerName(String name) {
        this.resourceManagerName = name;
    }

    public void setResourceManagerClass(String name) {
        this.resourceManagerClass = name;
    }

    public void setServiceLocatorName(String name) {
        this.serviceLocatorName = name;
    }
}

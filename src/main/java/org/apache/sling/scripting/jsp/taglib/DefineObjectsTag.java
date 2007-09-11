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

import javax.servlet.jsp.tagext.TagSupport;

import org.apache.sling.ServiceLocator;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.Content;
import org.apache.sling.content.ContentManager;
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
     * <code>RenderRequest</code> object (value is "renderRequest").
     */
    public static final String DEFAULT_REQUEST_NAME = "renderRequest";

    /**
     * Default name for the scripting variable referencing the
     * <code>RenderResponse</code> object (value is "renderResponse").
     */
    public static final String DEFAULT_RESPONSE_NAME = "renderResponse";

    /**
     * Default name for the scripting variable referencing the current
     * <code>Content</code> object (value is "content").
     */
    public static final String DEFAULT_CONTENT_NAME = "content";

    /**
     * Default name of the Java type for the scripting variable referencing the
     * current <code>Content</code> object (value is
     * "org.apache.sling.components.Content").
     */
    public static final String DEFAULT_CONTENT_CLASS = Content.class.getName();

    /**
     * Default name for the scripting variable referencing the current handle
     * (value is "handle").
     */
    public static final String DEFUALT_HANDLE_NAME = "handle";

    /**
     * Default name for the scripting variable referencing the current
     * <code>ContentManager</code> (value is "contentManager").
     */
    public static final String DEFAULT_CONTENT_MANAGER_NAME = "contentManager";

    /**
     * Default name for the scripting variable referencing the current
     * <code>ServiceLocator</code> (value is "serviceLocator").
     */
    public static final String DEFAULT_SERVICE_LOCATOR_NAME = "serviceLocator";

    private String requestName = DEFAULT_REQUEST_NAME;

    private String responseName = DEFAULT_RESPONSE_NAME;

    private String contentName = DEFAULT_CONTENT_NAME;

    private String contentClass = DEFAULT_CONTENT_CLASS;

    private String handleName = DEFUALT_HANDLE_NAME;

    private String contentManagerName = DEFAULT_CONTENT_MANAGER_NAME;

    private String serviceLocatorName = DEFAULT_SERVICE_LOCATOR_NAME;

    /**
     * Default constructor.
     */
    public DefineObjectsTag() {
    }

    /**
     * Creates Scripting variables for:
     * <ul>
     * <li><code>RenderRequest</code>
     * <li><code>RenderResponse</code>
     * <li>current <code>Content</code>
     * <li>current handle
     * <li>current <code>ContentManager</code>
     * </ul>
     *
     * @return always {@link #EVAL_PAGE}.
     */
    public int doEndTag() {

        ComponentRequest req = TagUtil.getRequest(this.pageContext);
        ComponentResponse res = TagUtil.getResponse(this.pageContext);
        Content content = req.getContent();
        ContentManager contentManager = TagUtil.getContentManager(this.pageContext);

        this.pageContext.setAttribute(this.requestName, req);
        this.pageContext.setAttribute(this.responseName, res);
        this.pageContext.setAttribute(this.contentManagerName, contentManager);

        // content may be null
        if (content != null) {
            this.pageContext.setAttribute(this.contentName, content);
            this.pageContext.setAttribute(this.contentClass, content.getClass().getName());
            this.pageContext.setAttribute(this.handleName, content.getPath());
        } else {
            TagUtil.log(log, this.pageContext, "RenderRequest has no Content !",
                null);
        }

        this.pageContext.setAttribute(this.serviceLocatorName, req.getAttribute(ServiceLocator.REQUEST_ATTRIBUTE_NAME));

        return EVAL_PAGE;
    }

    // --------------------------< setter methonds >----------------------------

    public void setRequestName(String requestName) {
        this.requestName = requestName;
    }

    public void setResponseName(String responseName) {
        this.responseName = responseName;
    }

    public void setContentName(String contentName) {
        this.contentName = contentName;
    }

    public void setContentClass(String contentClass) {
        this.contentClass = contentClass;
    }

    public void setHandleName(String handleName) {
        this.handleName = handleName;
    }

    public void setContentManagerName(String contentManagerName) {
        this.contentManagerName = contentManagerName;
    }

    public void setServiceLocatorName(String name) {
        this.serviceLocatorName = name;
    }
}

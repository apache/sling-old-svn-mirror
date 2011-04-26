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

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;

/**
 */
public class DefineObjectsTag extends TagSupport {

    private static final long serialVersionUID = -1858674361149195892L;

    /**
     * Default name for the scripting variable referencing the
     * <code>SlingHttpServletRequest</code> object (value is "slingRequest").
     */
    public static final String DEFAULT_REQUEST_NAME = "slingRequest";

    /**
     * Default name for the scripting variable referencing the
     * <code>SlingHttpServletResponse</code> object (value is
     * "slingResponse").
     */
    public static final String DEFAULT_RESPONSE_NAME = "slingResponse";

    /**
     * Default name for the scripting variable referencing the current
     * <code>Resource</code> object (value is "resource").
     */
    public static final String DEFAULT_RESOURCE_NAME = "resource";

    /**
     * Default name for the scripting variable referencing the JCR node
     * underlying the current <code>Resource</code> object if it is based on a
     * JCR node (value is "currentNode").
     */
    public static final String DEFAULT_NODE_NAME = "currentNode";

    /**
     * Default name for the scripting variable referencing the
     * <code>javax.script.Bindings</code> object (value is "bindings").
     */
    public static final String DEFAULT_BINDINGS_NAME = "bindings";

    /**
     * Default name for the scripting variable referencing the log
     * <code>org.slf4j.Logger</code> (value is "log").
     */
    public static final String DEFAULT_LOG_NAME = "log";

    /**
     * Default name for the scripting variable referencing the current
     * <code>SlingScriptHelper</code> (value is "sling").
     */
    public static final String DEFAULT_SLING_NAME = "sling";

    /**
     * Default name for the scripting variable referencing the current
     * <code>ResourceResolver</code> (value is "resourceResolver").
     */
    public static final String DEFAULT_RESOURCE_RESOLVER_NAME = "resourceResolver";

    private String requestName = DEFAULT_REQUEST_NAME;

    private String responseName = DEFAULT_RESPONSE_NAME;

    private String resourceName = DEFAULT_RESOURCE_NAME;

    private String nodeName = DEFAULT_NODE_NAME;

    private String slingName = DEFAULT_SLING_NAME;

    private String logName = DEFAULT_LOG_NAME;

    private String bindingsName = DEFAULT_BINDINGS_NAME;

    private String resourceResolverName = DEFAULT_RESOURCE_RESOLVER_NAME;

    static Class<?> JCR_NODE_CLASS;
    static {
        try {
            JCR_NODE_CLASS = DefineObjectsTag.class.getClassLoader().loadClass("javax.jcr.Node");
        } catch (Exception ignore) {
            // we just ignore this
        }
    }

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
     * <li>current <code>Node</code> (if resource is adaptable to a node)
     * <li>current <code>Logger</code>
     * <li>current <code>SlingScriptHelper</code>
     * </ul>
     *
     * @return always {@link #EVAL_PAGE}.
     */
    public int doEndTag() {
        final SlingBindings bindings = (SlingBindings)pageContext.getRequest().getAttribute(SlingBindings.class.getName());
        final SlingScriptHelper scriptHelper = bindings.getSling();

        pageContext.setAttribute(requestName, scriptHelper.getRequest());
        pageContext.setAttribute(responseName, scriptHelper.getResponse());
        final Resource resource = scriptHelper.getRequest().getResource();
        pageContext.setAttribute(resourceName, resource);
        pageContext.setAttribute(resourceResolverName, scriptHelper.getRequest().getResourceResolver());
        pageContext.setAttribute(slingName, scriptHelper);
        pageContext.setAttribute(logName, bindings.getLog());
        pageContext.setAttribute(bindingsName, bindings);
        if ( JCR_NODE_CLASS != null ) {
            final Object node = resource.adaptTo(JCR_NODE_CLASS);
            if (node != null) {
                pageContext.setAttribute(nodeName, node);
            }
        }

        return EVAL_PAGE;
    }

    // --------------------------< setter methonds >----------------------------

    @Override
    public void setPageContext(PageContext pageContext) {
        super.setPageContext(pageContext);
        clear();
    }

    public void setRequestName(String requestName) {
        this.requestName = requestName;
    }

    public void setResponseName(String responseName) {
        this.responseName = responseName;
    }

    public void setResourceName(String name) {
        this.resourceName = name;
    }

    public void setNodeName(String name) {
        this.nodeName = name;
    }

    public void setLogName(String name) {
        this.logName = name;
    }

    public void setSlingName(String name) {
        this.slingName = name;
    }

    public void setResourceResolverName(String name) {
        this.resourceResolverName = name;
    }

    public void setBindingsName(String name) {
        this.bindingsName = name;
    }

    @Override
    public void release() {
        clear();
        super.release();
    }

    private void clear() {
        // reset fields
        requestName = DEFAULT_REQUEST_NAME;
        responseName = DEFAULT_RESPONSE_NAME;
        resourceName = DEFAULT_RESOURCE_NAME;
        nodeName = DEFAULT_NODE_NAME;
        slingName = DEFAULT_SLING_NAME;
        logName = DEFAULT_LOG_NAME;
        bindingsName = DEFAULT_BINDINGS_NAME;
        resourceResolverName = DEFAULT_RESOURCE_RESOLVER_NAME;
    }
}

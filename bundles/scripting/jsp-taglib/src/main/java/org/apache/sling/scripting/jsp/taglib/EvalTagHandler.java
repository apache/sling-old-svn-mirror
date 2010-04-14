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

import javax.servlet.Servlet;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.scripting.jsp.util.JspSlingHttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>EvalTagHandler</code> implements the
 * <code>&lt;sling:eval&gt;</code> custom tag.
 */
public class EvalTagHandler extends TagSupport {

    private static final long serialVersionUID = 7070941156517599283L;

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(EvalTagHandler.class);

    /** resource argument */
    private Resource resource;

    /** script argument */
    private String script;

    /** resource type argument */
    private String resourceType;

    /** ignore resource type hierarchy */
    private boolean ignoreResourceTypeHierarchy = false;

    /** flush argument */
    private boolean flush = false;


    /**
     * Called after the body has been processed.
     *
     * @return whether additional evaluations of the body are desired
     */
    public int doEndTag() throws JspException {
        log.debug("EvalTagHandler doEndTag");

        final SlingBindings bindings = (SlingBindings) pageContext.getRequest().getAttribute(
                SlingBindings.class.getName());
        final SlingScriptHelper scriptHelper = bindings.getSling();
        final ServletResolver servletResolver = scriptHelper.getService(ServletResolver.class);

        final Servlet servlet;
        if ( !this.ignoreResourceTypeHierarchy ) {
            // detecte resource
            final Resource resource;
            if ( this.resource != null ) {
                resource = this.resource;
            } else if ( this.resourceType != null ) {
                resource = new SyntheticResource(bindings.getRequest().getResourceResolver(),
                        bindings.getResource().getPath(), this.resourceType);
            } else {
                resource = bindings.getResource();
            }
            servlet = servletResolver.resolveServlet(resource, this.script);
        } else {
            final ResourceResolver rr = bindings.getRequest().getResourceResolver();
            final String scriptPath;
            if (!script.startsWith("/")) {

                // resolve relative script
                String parentPath = ResourceUtil.getParent(scriptHelper.getScript().getScriptResource().getPath());
                // check if parent resides on search path
                for (String sp: rr.getSearchPath()) {
                    if (parentPath.startsWith(sp)) {
                        parentPath = parentPath.substring(sp.length());
                        break;
                    }
                }
                scriptPath = parentPath + '/' + script;

            } else {

                scriptPath = this.script;
            }
            servlet = servletResolver.resolveServlet(rr, scriptPath);
        }

        if (servlet == null) {
            throw new JspException("Could not find script '" + script + "' referenced in jsp " + scriptHelper.getScript().getScriptResource().getPath());
        }

        try {
            if (flush && !(pageContext.getOut() instanceof BodyContent)) {
                // might throw an IOException of course
                pageContext.getOut().flush();
            }

            // wrap the response to get the correct output order
            SlingHttpServletResponse response = new JspSlingHttpServletResponseWrapper(
                pageContext);

            servlet.service(pageContext.getRequest(), response);

            return EVAL_PAGE;

        } catch (Exception e) {
            log.error("Error while executing script " + script, e);
            throw new JspException("Error while executing script " + script, e);

        }
    }

    /**
     * @see javax.servlet.jsp.tagext.TagSupport#setPageContext(javax.servlet.jsp.PageContext)
     */
    public void setPageContext(final PageContext pageContext) {
        super.setPageContext(pageContext);

        // init local fields, since tag might be reused
        resource = null;
        resourceType = null;
        ignoreResourceTypeHierarchy = false;
        script = null;
        flush = false;
    }

    public void setFlush(boolean flush) {
        this.flush = flush;
    }

    public void setResource(final Resource rsrc) {
        this.resource = rsrc;
    }

    public void setScript(final String script) {
        this.script = script;
    }

    public void setResourceType(final String rsrcType) {
        this.resourceType = rsrcType;
    }

    public void setIgnoreResourceTypeHierarchy(final boolean flag) {
        this.ignoreResourceTypeHierarchy = flag;
    }
}

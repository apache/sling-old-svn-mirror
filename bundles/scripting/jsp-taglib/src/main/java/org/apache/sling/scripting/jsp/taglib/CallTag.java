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
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.request.RequestUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.scripting.jsp.util.JspSlingHttpServletResponseWrapper;
import org.apache.sling.scripting.jsp.util.TagUtil;

/**
 * The <code>CallTag</code> implements the
 * <code>&lt;sling:call&gt;</code> custom tag.
 */
public class CallTag extends TagSupport {

    private static final long serialVersionUID = 5446209582533607741L;

    /**
     * jsp script
     */
    private String script;

    /**
     * flush
     */
    private boolean flush;

    /**
     * ignores the component hierarchy and only respect scripts paths
     */
    private boolean ignoreComponentHierarchy;

    @Override
    public int doEndTag() throws JspException {
        final SlingBindings bindings = (SlingBindings) pageContext.getRequest().getAttribute(
                SlingBindings.class.getName());
        final SlingScriptHelper scriptHelper = bindings.getSling();
        final ServletResolver servletResolver = scriptHelper.getService(ServletResolver.class);

        final RequestProgressTracker tracker = TagUtil.getRequest(pageContext).getRequestProgressTracker();
        String servletName = null;

        final Servlet servlet;
        if (!ignoreComponentHierarchy) {
            final Resource resource = bindings.getResource();
            servlet = servletResolver.resolveServlet(resource, this.script);

            if (servlet != null) {
                servletName = RequestUtil.getServletName(servlet);
                tracker.log("Including script {0} for path={1}, type={2}: {3}", script, resource.getPath(),
                        resource.getResourceType(), servletName);
            }

        } else {
            final ResourceResolver resolver = bindings.getRequest().getResourceResolver();
            final String scriptPath;
            if (!script.startsWith("/")) {

                // resolve relative script
                String parentPath = ResourceUtil.getParent(scriptHelper.getScript().getScriptResource().getPath());
                // check if parent resides on search path
                for (String sp : resolver.getSearchPath()) {
                    if (parentPath.startsWith(sp)) {
                        parentPath = parentPath.substring(sp.length());
                        break;
                    }
                }
                scriptPath = parentPath + "/" + script;

            } else {

                scriptPath = this.script;
            }
            servlet = servletResolver.resolveServlet(resolver, scriptPath);

            if (servlet != null) {
                servletName = RequestUtil.getServletName(servlet);
                tracker.log("Including script {0} (ignoring component hierarchy): {1}", script, servletName);
            }
        }

        if (servlet == null) {
            throw new JspException("Could not find script " + script);
        }

        try {
            if (flush && !(pageContext.getOut() instanceof BodyContent)) {
                // might throw an IOException of course
                pageContext.getOut().flush();
            }

            // wrap the response to get the correct output order
            SlingHttpServletResponse response = new JspSlingHttpServletResponseWrapper(pageContext);

            tracker.startTimer(servletName);

            servlet.service(pageContext.getRequest(), response);

            tracker.logTimer(servletName);

            return EVAL_PAGE;

        } catch (Exception e) {

            throw new JspException("Error while executing script " + script, e);
        }
    }
    
    @Override
    public void setPageContext(PageContext pageContext) {
        super.setPageContext(pageContext);
        script = null;
        flush = false;
        ignoreComponentHierarchy = false;
    }

    /**
     * Sets the script attribute
     * @param script attribute value
     */
    public void setScript(String script) {
        this.script = script;
    }

    /**
     * Sets the flush attribute
     * @param flush attribute value
     */
    public void setFlush(boolean flush) {
        this.flush = flush;
    }

    /**
     * Set the ignore component hierarchy attribute
     * @param ignoreComponentHierarchy attribute value
     */
    public void setIgnoreComponentHierarchy(boolean ignoreComponentHierarchy) {
        this.ignoreComponentHierarchy = ignoreComponentHierarchy;
    }

}

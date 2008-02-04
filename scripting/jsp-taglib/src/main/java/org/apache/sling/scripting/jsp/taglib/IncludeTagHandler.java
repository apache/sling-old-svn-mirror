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

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.jcr.resource.SyntheticResource;
import org.apache.sling.scripting.jsp.util.JspSlingHttpServletResponseWrapper;
import org.apache.sling.scripting.jsp.util.TagUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>IncludeTagHandler</code> implements the
 * <code>&lt;sling:include&gt;</code> custom tag.
 */
public class IncludeTagHandler extends TagSupport {

    private static final long serialVersionUID = 6275972595573203863L;

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(IncludeTagHandler.class);

    /** flush argument */
    private boolean flush = false;

    /** resource argument */
    private Resource resource;

    /** path argument */
    private String path;

    /** resource type argument */
    private String resourceType;

    /**
     * Called after the body has been processed.
     * 
     * @return whether additional evaluations of the body are desired
     */
    public int doEndTag() throws JspException {
        log.debug("IncludeTagHandler.doEndTag");

        final SlingHttpServletRequest request = TagUtil.getRequest(pageContext);

        // only try to include, if there is anything to include !!
        if (resource == null) {
            if (path == null) {
                resource = request.getResource();
                // overwrite resource type if needed
                if (resourceType != null
                    && !resourceType.equals(resource.getResourceType())) {
                    path = resource.getPath();
                    resource = null;
                }
            } else {
                if (!path.startsWith("/")) {
                    path = request.getResource().getPath() + "/"
                        + JcrResourceUtil.normalize(this.path);
                }
                resource = request.getResourceResolver().getResource(path);
            }
        }

        // if resource could not be resolved, create synthetic one
        if (resource == null) {
            if (resourceType == null) {
                TagUtil.log(log, pageContext, "unable to include path " + path
                    + ". Resource not found and no resource type given.", null);
                return EVAL_PAGE;
            }
            resource = new SyntheticResource(path, resourceType);
        }

        try {
            // optionally flush
            if (flush && !(pageContext.getOut() instanceof BodyContent)) {
                // might throw an IOException of course
                pageContext.getOut().flush();
            }
            // include the rendered content
            RequestDispatcher dispatcher = request.getRequestDispatcher(resource);
            if (dispatcher != null) {
                SlingHttpServletResponse response = new JspSlingHttpServletResponseWrapper(
                    pageContext);
                dispatcher.include(request, response);
            } else {
                TagUtil.log(log, pageContext, "No content to include...", null);
            }

        } catch (IOException ioe) {
            throw new JspException("Error including " + path, ioe);
        } catch (ServletException ce) {
            throw new JspException("Error including " + path,
                TagUtil.getRootCause(ce));
        }

        return EVAL_PAGE;
    }

    public void setPageContext(PageContext pageContext) {
        super.setPageContext(pageContext);

        // init local fields, since tag might be reused
        flush = false;
        resource = null;
        resourceType = null;
        path = null;
    }

    public void setFlush(boolean flush) {
        this.flush = flush;
    }

    public void setResource(Resource rsrc) {
        this.resource = rsrc;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setResourceType(String rsrcType) {
        this.resourceType = rsrcType;
    }
}

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
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.Content;
import org.apache.sling.scripting.jsp.util.JspComponentResponseWrapper;
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

    /** content argument */
    private Content content;

    /** path argument */
    private String path;

    /**
     * Called after the body has been processed.
     *
     * @return whether additional evaluations of the body are desired
     */
    public int doEndTag() throws JspException {
        log.debug("IncludeTagHandler.doEndTag");

        // only try to include, if there is anything to include !!
        ComponentRequest request = TagUtil.getRequest(pageContext);
        RequestDispatcher dispatcher = null;
        if (content != null) {
            // get the request dispatcher for the content object
            dispatcher = request.getRequestDispatcher(content);
            path = content.getPath();

        } else if (path != null) {
            // ensure the child path is absolute and assign the result to path
            if (!path.startsWith("/")) {
                path = request.getContent().getPath() + "/" + path;
            }

            // get the request dispatcher for the (relative) path
            dispatcher = request.getRequestDispatcher(path);
        }

        try {

            // optionally flush
            if (flush && !(pageContext.getOut() instanceof BodyContent)) {
                // might throw an IOException of course
                pageContext.getOut().flush();
            }

            // include the rendered content
            if (dispatcher != null) {
                ComponentResponse response = new JspComponentResponseWrapper(
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

    public void setFlush(boolean flush) {
        this.flush = flush;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

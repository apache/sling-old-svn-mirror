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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.sling.component.Component;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentRequestDispatcher;
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

    /** content argument */
    private Content content;

    /**
     * Called after the body has been processed.
     *
     * @return whether additional evaluations of the body are desired
     */
    public int doEndTag() throws JspException {
        log.debug("IncludeTagHandler.doEndTag");

        // only try to include, if there is anything to include !!
        if (this.content != null) {
            // get the request dispatcher for the content object
            Component component = TagUtil.getComponent(this.pageContext);
            ComponentRequestDispatcher crd = component.getComponentContext().getRequestDispatcher(this.content);

            // include the rendered content
            try {
                ComponentRequest request = TagUtil.getRequest(this.pageContext);
                ComponentResponse response = new JspComponentResponseWrapper(this.pageContext);
                crd.include(request, response);
            } catch (IOException ioe) {
                throw new JspException("Error including " + this.content, ioe);
            } catch (ComponentException ce) {
                throw new JspException("Error including " + this.content, TagUtil.getRootCause(ce));
            }
        } else {
            TagUtil.log(log, this.pageContext, "No content to include...", null);
        }

        return EVAL_PAGE;
    }

    public void setContent(Content content) {
        this.content = content;
    }
}

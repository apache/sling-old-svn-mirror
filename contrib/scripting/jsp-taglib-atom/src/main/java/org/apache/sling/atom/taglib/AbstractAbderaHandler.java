/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.atom.taglib;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;

public class AbstractAbderaHandler extends BodyTagSupport {

    public static final String ABDERA_ATTRIBUTE = "org.apache.abdera.Abdera";

    private static final long serialVersionUID = 1L;

    public AbstractAbderaHandler() {
        super();
    }

    protected Abdera getAbdera() throws JspException {
        return getAbdera(pageContext);
    }

    protected Abdera getAbdera(PageContext pageContexxt) throws JspException {
        ServletContext context = pageContext.getServletContext();

        Object obj = context.getAttribute(ABDERA_ATTRIBUTE);
        if (obj instanceof Abdera) {
            return (Abdera) obj;
        }

        // fallback to failure !!
        throw new JspException("Abdera not available");
    }

    protected Feed getFeed(ServletRequest request) throws JspException {
        Feed feed;
        if (request.getAttribute("feed") != null
            && (request.getAttribute("feed") instanceof Feed)) {
            feed = (Feed) request.getAttribute("feed");
        } else {
            feed = getAbdera().newFeed();
            request.setAttribute("feed", feed);
        }
        return feed;
    }

    protected Feed getFeed() throws JspException {
        final ServletRequest request = pageContext.getRequest();
        return getFeed(request);
    }

    protected Entry getEntry() {
        final ServletRequest request = pageContext.getRequest();
        if (request.getAttribute("entry") instanceof Entry) {
            return (Entry) request.getAttribute("entry");
        }
        return null;
    }

    protected void setEntry(Entry entry) {
        final ServletRequest request = pageContext.getRequest();
        request.setAttribute("entry", entry);
    }

    protected void clearEntry() {
        setEntry(null);
    }

    protected boolean hasEntry() {
        return getEntry() != null;
    }

}
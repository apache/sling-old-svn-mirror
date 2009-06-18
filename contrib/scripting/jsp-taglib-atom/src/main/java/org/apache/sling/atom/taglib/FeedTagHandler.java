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

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.abdera.model.Feed;

public class FeedTagHandler extends AbstractAbderaHandler {

    private static final long serialVersionUID = 1L;

    private String baseUri;

    private String icon;

    private String id;

    private String logo;

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    private String language;

    private Date updated = new Date();

    public String getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    @Override
    public int doEndTag() throws JspException {
        final ServletRequest request = pageContext.getRequest();
        final ServletResponse response = pageContext.getResponse();
        // get the current feed
        Feed feed = getFeed(request);

        // we need tags for that
        /*
         * feed.addEntry(null);
         */

        // write the feed
        try {
            response.setContentType("application/atom+xml");

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());
            try {
                feed.writeTo(response.getOutputStream());
            } finally {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
        } catch (IOException e) {
            throw new JspException("Unable to write feed", e);
        } catch (Exception e) {
            throw new JspException("Unable to write feed", e);
        }
        return super.doEndTag();
    }

    @Override
    public int doStartTag() throws JspException {
        final ServletRequest request = pageContext.getRequest();
        // create a feed
        Feed feed = getFeed(request);

        // setting basic feed properties
        feed.setBaseUri(baseUri);
        feed.setIcon(icon);
        feed.setId(id);
        feed.setLanguage(language);
        feed.setLogo(logo);
        feed.setUpdated(updated);

        return EVAL_BODY_INCLUDE;
    }

}

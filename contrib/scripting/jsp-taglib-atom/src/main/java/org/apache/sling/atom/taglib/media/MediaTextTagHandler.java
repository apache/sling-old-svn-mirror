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
package org.apache.sling.atom.taglib.media;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.abdera.ext.media.MediaConstants;
import org.apache.abdera.ext.media.MediaContent;
import org.apache.abdera.ext.media.MediaGroup;
import org.apache.abdera.ext.media.MediaText;
import org.apache.abdera.ext.media.MediaConstants.Type;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.sling.atom.taglib.AbstractAbderaHandler;

public class MediaTextTagHandler extends AbstractAbderaHandler {

    private static final long serialVersionUID = 1L;

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    private String start;

    private String end;

    private String lang;

    private String type;

    @Override
    public int doEndTag() throws JspException {
        final ServletRequest request = pageContext.getRequest();

        MediaText text;

        if (request.getAttribute("content") != null
            && request.getAttribute("content") instanceof MediaContent) {
            text = ((MediaContent) request.getAttribute("content")).addExtension(MediaConstants.TEXT);
        } else if (request.getAttribute("group") != null
            && request.getAttribute("group") instanceof MediaGroup) {
            text = ((MediaGroup) request.getAttribute("group")).addExtension(MediaConstants.TEXT);
        } else if (request.getAttribute("entry") != null
            && request.getAttribute("entry") instanceof Entry) {
            text = ((Entry) request.getAttribute("entry")).addExtension(MediaConstants.TEXT);
        } else {
            text = ((Feed) request.getAttribute("feed")).addExtension(MediaConstants.TEXT);
        }

        // set the body content
        text.setText(getBodyContent().getString());
        // set the scheme
        if (start != null) text.setStart(start);
        if (end != null) text.setEnd(end);
        if (lang != null) text.setLang(lang);
        if (type != null) text.setType(Type.valueOf(type));

        return super.doEndTag();
    }

    @Override
    public int doStartTag() {
        return EVAL_BODY_BUFFERED;
    }

}

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
import org.apache.abdera.ext.media.MediaRating;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.sling.atom.taglib.AbstractAbderaHandler;

public class MediaRatingTagHandler extends AbstractAbderaHandler {

    private static final long serialVersionUID = 1L;

    private String scheme;

    @Override
    public int doEndTag() throws JspException {
        final ServletRequest request = pageContext.getRequest();

        MediaRating rating;

        if (request.getAttribute("content") != null
            && request.getAttribute("content") instanceof MediaContent) {
            rating = ((MediaContent) request.getAttribute("content")).addExtension(MediaConstants.RATING);
        } else if (request.getAttribute("group") != null
            && request.getAttribute("group") instanceof MediaGroup) {
            rating = ((MediaGroup) request.getAttribute("group")).addExtension(MediaConstants.RATING);
        } else if (request.getAttribute("entry") != null
            && request.getAttribute("entry") instanceof Entry) {
            rating = ((Entry) request.getAttribute("entry")).addExtension(MediaConstants.RATING);
        } else {
            rating = ((Feed) request.getAttribute("feed")).addExtension(MediaConstants.RATING);
        }

        // set the body content
        rating.setText(getBodyContent().getString());
        // set the scheme
        if (scheme != null) rating.setScheme(scheme);

        return super.doEndTag();
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public int doStartTag() {
        return EVAL_BODY_BUFFERED;
    }

}

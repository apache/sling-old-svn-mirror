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
import org.apache.abdera.ext.media.MediaPlayer;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.sling.atom.taglib.AbstractAbderaHandler;

public class MediaPlayerTagHandler extends AbstractAbderaHandler {

    private static final long serialVersionUID = 1L;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    private String url;

    private int width;

    private int height;

    @Override
    public int doEndTag() throws JspException {
        final ServletRequest request = pageContext.getRequest();

        MediaPlayer player;

        if (request.getAttribute("content") != null
            && request.getAttribute("content") instanceof MediaContent) {
            player = ((MediaContent) request.getAttribute("content")).addExtension(MediaConstants.PLAYER);
        } else if (request.getAttribute("group") != null
            && request.getAttribute("group") instanceof MediaGroup) {
            player = ((MediaGroup) request.getAttribute("group")).addExtension(MediaConstants.PLAYER);
        } else if (request.getAttribute("entry") != null
            && request.getAttribute("entry") instanceof Entry) {
            player = ((Entry) request.getAttribute("entry")).addExtension(MediaConstants.PLAYER);
        } else {
            player = ((Feed) request.getAttribute("feed")).addExtension(MediaConstants.PLAYER);
        }

        // set the scheme
        player.setUrl(url);
        if (width != 0) player.setWidth(width);
        if (height != 0) player.setHeight(height);

        return super.doEndTag();
    }

    @Override
    public int doStartTag() {
        return EVAL_BODY_BUFFERED;
    }

}

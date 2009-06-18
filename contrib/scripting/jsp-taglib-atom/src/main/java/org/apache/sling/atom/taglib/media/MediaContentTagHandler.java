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
import org.apache.abdera.ext.media.MediaConstants.Expression;
import org.apache.abdera.ext.media.MediaConstants.Medium;
import org.apache.abdera.model.Entry;
import org.apache.sling.atom.taglib.AbstractAbderaHandler;

public class MediaContentTagHandler extends AbstractAbderaHandler {

    private static final long serialVersionUID = 1L;

    private String url;

    private long fileSize;

    private String type;

    private String medium;

    private String isDefault;

    private String expression;

    private int bitrate;

    private double samplingrate;

    private int framerate;

    private int channels;

    private int duration;

    private int height;

    private int width;

    @Override
    public int doEndTag() throws JspException {
        final ServletRequest request = pageContext.getRequest();
        // clear out the group
        request.setAttribute("content", null);

        return super.doEndTag();
    }

    @Override
    public int doStartTag() {
        final ServletRequest request = pageContext.getRequest();

        MediaContent content;
        if (request.getAttribute("group") instanceof MediaGroup) {
            MediaGroup group = (MediaGroup) request.getAttribute("group");
            content = group.addExtension(MediaConstants.CONTENT);
        } else {
            Entry entry = getEntry();
            content = entry.addExtension(MediaConstants.CONTENT);
        }
        if (url != null) {
            content.setUrl(url.replaceAll(" ", "%20"));
        }
        if (fileSize != 0) {
            content.setFilesize(fileSize);
        }
        if (type != null) {
            content.setType(type);
        }
        if (medium != null) content.setMedium(Medium.valueOf(medium));
        if (expression != null)
            content.setExpression(Expression.valueOf(expression));
        if (bitrate != 0) content.setBitrate(bitrate);
        if (samplingrate != 0) content.setSamplingRate(samplingrate);
        if (framerate != 0) content.setFramerate(framerate);
        if (channels != 0) content.setChannels(channels);
        if (duration != 0) content.setDuration(duration);
        if (height != 0) content.setHeight(height);
        if (width != 0) content.setWidth(width);

        request.setAttribute("content", content);

        return EVAL_BODY_INCLUDE;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public String getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(String isDefault) {
        this.isDefault = isDefault;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public double getSamplingrate() {
        return samplingrate;
    }

    public void setSamplingrate(double samplingrate) {
        this.samplingrate = samplingrate;
    }

    public int getFramerate() {
        return framerate;
    }

    public void setFramerate(int framerate) {
        this.framerate = framerate;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

}

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

import javax.servlet.jsp.JspException;

import org.apache.abdera.model.Link;

public class LinkTagHandler extends AbstractAbderaHandler {

    private static final long serialVersionUID = 1L;

    private String href;

    private String rel;

    private String type;

    private String lang;

    private long length;

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    @Override
    public int doEndTag() throws JspException {
        Link link = getAbdera().getFactory().newLink();

        link.setHref(href.replaceAll(" ", "%20"));
        if (rel != null) link.setRel(rel);
        if (type != null) link.setMimeType(type);
        if (lang != null) link.setHrefLang(lang);
        if (length != 0) link.setLength(length);

        if (hasEntry()) {
            getEntry().addLink(link);
        } else {
            getFeed().addLink(link);
        }

        return super.doEndTag();
    }

}

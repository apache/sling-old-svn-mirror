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

import java.util.Date;

import javax.servlet.jsp.JspException;

import org.apache.abdera.model.Entry;

public class EntryTagHandler extends AbstractAbderaHandler {

    private static final long serialVersionUID = 1L;

    private boolean draft;

    private Date edited;

    private String id;

    private Date published;

    private Date updated;

    @Override
    public int doEndTag() throws JspException {
        Entry entry = getEntry();
        if (draft) entry.setDraft(draft);
        if (edited != null) entry.setEdited(edited);
        if (id != null) entry.setId(id);
        if (published != null) entry.setPublished(published);
        if (updated != null) entry.setUpdated(updated);

        clearEntry();

        return super.doEndTag();
    }

    @Override
    public int doStartTag() throws JspException {
        Entry entry = getFeed().addEntry();

        setEntry(entry);
        return EVAL_BODY_INCLUDE;
    }

    public boolean isDraft() {
        return draft;
    }

    public void setDraft(boolean draft) {
        this.draft = draft;
    }

    public Date getEdited() {
        return edited;
    }

    public void setEdited(Date edited) {
        this.edited = edited;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getPublished() {
        return published;
    }

    public void setPublished(Date published) {
        this.published = published;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

}

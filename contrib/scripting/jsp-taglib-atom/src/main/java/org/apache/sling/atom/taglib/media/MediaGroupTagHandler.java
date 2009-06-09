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
import org.apache.abdera.model.Entry;
import org.apache.sling.atom.taglib.AbstractAbderaHandler;

public class MediaGroupTagHandler extends AbstractAbderaHandler {

    private static final long serialVersionUID = 1L;

    @Override
    public int doEndTag() throws JspException {
        final ServletRequest request = pageContext.getRequest();
        // clear out the group
        request.setAttribute("group", null);

        return super.doEndTag();
    }

    @Override
    public int doStartTag() throws JspException {
        Entry entry = getEntry();
        // create the group element

        getAbdera().getConfiguration().getExtensionFactories();
        Object group = entry.addExtension(MediaConstants.GROUP);
        final ServletRequest request = pageContext.getRequest();
        request.setAttribute("group", group);

        return EVAL_BODY_INCLUDE;
    }

}

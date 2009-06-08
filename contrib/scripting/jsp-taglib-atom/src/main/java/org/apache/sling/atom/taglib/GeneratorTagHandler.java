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
import javax.servlet.jsp.tagext.BodyContent;

import org.apache.abdera.model.Generator;

public class GeneratorTagHandler extends AbstractAbderaHandler {

    private static final long serialVersionUID = 1L;

    private String uri;

    private String version;

    @Override
    public int doEndTag() throws JspException {
        Generator generator = getAbdera().getFactory().newGenerator();
        if (uri != null) generator.setUri(uri);
        if (version != null) generator.setVersion(version);
        BodyContent bc = getBodyContent();
        generator.setText(bc.getString());

        getFeed().setGenerator(generator);

        return super.doEndTag();
    }

    @Override
    public int doStartTag() {
        return EVAL_BODY_BUFFERED;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}

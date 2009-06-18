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

import org.apache.abdera.model.Person;

public class AuthorTagHandler extends AbstractAbderaHandler {

    private static final long serialVersionUID = 1L;

    private String email;

    private String name;

    private String uri;

    @Override
    public int doEndTag() throws JspException {
        Person author = getAbdera().getFactory().newAuthor();
        author.setEmail(email);
        author.setName(name);
        author.setUri(uri);

        if (hasEntry()) {
            getEntry().addAuthor(author);
        } else {
            getFeed().addAuthor(author);
        }

        return super.doEndTag();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

}

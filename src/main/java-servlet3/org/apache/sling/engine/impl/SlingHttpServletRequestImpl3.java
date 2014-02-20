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
package org.apache.sling.engine.impl;

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.sling.engine.impl.request.RequestData;

/**
 * The <code>SlingHttpServletRequestImpl3</code> extends the
 * {@link SlingHttpServletRequestImpl} class by support for new Servlet API 3
 * {@code HttpServletRequest} methods {@link #getPart(String)} and
 * {@link #getParts()}
 */
public class SlingHttpServletRequestImpl3 extends SlingHttpServletRequestImpl {

    public SlingHttpServletRequestImpl3(RequestData requestData, HttpServletRequest servletRequest) {
        super(requestData, servletRequest);
    }

    public Part getPart(String name) {
        return (Part) this.getParameterSupport().getPart(name);
    }

    @SuppressWarnings("unchecked")
    public Collection<Part> getParts() {
        return (Collection<Part>) this.getParameterSupport().getParts();
    }

}

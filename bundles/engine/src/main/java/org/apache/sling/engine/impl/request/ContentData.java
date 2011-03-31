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
package org.apache.sling.engine.impl.request;

import javax.servlet.Servlet;

import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;

/**
 * The <code>ContentData</code> class provides objects which are relevant for
 * the processing of a single Content object by its Component.
 *
 * @see RequestData
 */
public class ContentData {

    private final RequestPathInfo requestPathInfo;

    private final Resource resource;

    private Servlet servlet;

    public ContentData(final Resource resource, final RequestPathInfo requestPathInfo) {
        this.resource = resource;
        this.requestPathInfo = requestPathInfo;
    }

    public Resource getResource() {
        return resource;
    }

    public RequestPathInfo getRequestPathInfo() {
        return requestPathInfo;
    }

    public Servlet getServlet() {
        return servlet;
    }

    public void setServlet(Servlet servlet) {
        this.servlet = servlet;
    }
}

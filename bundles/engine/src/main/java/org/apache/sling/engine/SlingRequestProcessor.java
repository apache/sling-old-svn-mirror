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
package org.apache.sling.engine;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>SlingRequestProcessor</code> interface defines the service which
 * may be called to handle HTTP requests.
 * <p>
 * This interface is implemented by this bundle and is not intended to be
 * implemented by bundles other than this.
 */
@ProviderType
public interface SlingRequestProcessor {

    /**
     * The name of the <code>SlingRequestProcessor</code> service.
     */
    static final String NAME = SlingRequestProcessor.class.getName();

    /**
     * Process an HTTP request through the Sling request processing engine.
     * <p>
     * This method does <b>not</b> close the provided resource resolver!
     * <p>
     * The org.apache.sling.servlet-helpers module provides synthetic 
     * request/response classes which can be useful when using this service. 
     *
     * @param request Usually a "synthetic" request, i.e. not supplied by
     *            servlet container
     * @param response Usually a "synthetic" response, i.e. not supplied by
     *            servlet container
     * @param resourceResolver The <code>ResourceResolver</code> used for the
     *            Sling request processing.
     * @throws NullPointerException if either of the parameters is
     *             <code>null</code>
     * @throws IOException if an error occurrs reading from the request input or
     *             writing the response
     * @throws ServletException if another servlet related problem occurrs
     */
    void processRequest(HttpServletRequest request,
            HttpServletResponse response, ResourceResolver resourceResolver)
            throws ServletException, IOException;

}

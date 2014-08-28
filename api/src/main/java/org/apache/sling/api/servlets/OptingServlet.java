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
package org.apache.sling.api.servlets;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;

import aQute.bnd.annotation.ConsumerType;

/**
 * The <code>OptingServlet</code> interface may be implemented by
 * <code>Servlets</code> used by Sling which may choose to not handle all
 * requests for which they would be selected based on their registration
 * properties.
 *
 * Note that servlets implementing this interface can have an impact
 * on system performance, as their resolution cannot be cached: the 
 * resolver has no insight into which parts of the request cause
 * {@link #accepts} to return true.
 */
@ConsumerType
public interface OptingServlet extends Servlet {

    /**
     * Examines the request, and return <code>true</code> if this servlet is
     * willing to handle the request. If <code>false</code> is returned, the
     * request will be ignored by this servlet, and may be handled by other
     * servlets.
     *
     * @param request The request to examine
     * @return <code>true</code> if this servlet will handle the request,
     *         <code>false</code> otherwise
     */
    boolean accepts(SlingHttpServletRequest request);

}

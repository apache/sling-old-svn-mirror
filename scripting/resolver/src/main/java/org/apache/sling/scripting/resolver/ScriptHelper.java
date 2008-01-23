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
package org.apache.sling.scripting.resolver;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.SlingIOException;
import org.apache.sling.api.SlingServletException;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.resolver.impl.helper.OnDemandReaderRequest;
import org.apache.sling.scripting.resolver.impl.helper.OnDemandWriterResponse;

/**
 * Simple script helper providing access to the (wrapped) response, the
 * on-demand writer and a simple API for request inclusion. Instances of this
 * class are made available to the scripts as the global <code>sling</code>
 * variable.
 */
public class ScriptHelper implements SlingScriptHelper {

    private final SlingScript script;

    private final SlingHttpServletRequest request;

    private final SlingHttpServletResponse response;

    public ScriptHelper(SlingScript script, SlingHttpServletRequest request,
            SlingHttpServletResponse response) {
        this.script = script;
        this.request = new OnDemandReaderRequest(request);
        this.response = new OnDemandWriterResponse(response);
    }

    public SlingScript getScript() {
        return script;
    }

    public SlingHttpServletRequest getRequest() {
        return request;
    }

    public SlingHttpServletResponse getResponse() {
        return response;
    }

    /**
     * @trows SlingIOException Wrapping a <code>IOException</code> thrown
     *        while handling the include.
     * @throws SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the include.
     */
    public void include(String path) {
        include(path, null);
    }

    /**
     * @trows SlingIOException Wrapping a <code>IOException</code> thrown
     *        while handling the include.
     * @throws SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the include.
     */
    public void include(String path, RequestDispatcherOptions options) {
        // TODO: Implement for options !!
        RequestDispatcher dispatcher = getRequest().getRequestDispatcher(path);
        if (dispatcher != null) {
            try {
                dispatcher.include(getRequest(), getResponse());
            } catch (IOException ioe) {
                throw new SlingIOException(ioe);
            } catch (ServletException se) {
                throw new SlingServletException(se);
            }
        }
    }
}

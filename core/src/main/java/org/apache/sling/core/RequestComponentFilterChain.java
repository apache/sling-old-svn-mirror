/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core;

import java.io.IOException;

import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentFilter;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.core.filter.AbstractComponentFilterChain;


/**
 * The <code>RequestComponentFilterChain</code> implements the filter chain for
 * request scoped filters. It is used by the
 * {@link ComponentRequestHandlerImpl#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
 * method to dispatch request processing.
 */
class RequestComponentFilterChain extends AbstractComponentFilterChain {

    ComponentRequestHandlerImpl handler;

    RequestComponentFilterChain(ComponentRequestHandlerImpl handler,
            ComponentFilter[] filters) {
        super(filters);
        this.handler = handler;
    }

    protected void render(ComponentRequest request, ComponentResponse response)
            throws IOException, ComponentException {
        handler.processRequest(request, response);
    }
}
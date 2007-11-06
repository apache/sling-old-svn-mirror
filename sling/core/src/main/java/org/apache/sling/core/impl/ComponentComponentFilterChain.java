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
package org.apache.sling.core.impl;

import java.io.IOException;

import org.apache.sling.component.Component;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentFilter;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.core.impl.filter.AbstractComponentFilterChain;


/**
 * The <code>ComponentComponentFilterChain</code> implements the filter chain for
 * component scoped filters. It is used by the
 * {@link ComponentRequestHandlerImpl#processRequest(RenderRequest, ComponentResponse)}
 * method to dispatch component processing.
 */
class ComponentComponentFilterChain extends AbstractComponentFilterChain {
    ComponentComponentFilterChain(ComponentFilter[] filters) {
        super(filters);
    }

    protected void render(ComponentRequest request, ComponentResponse response)
            throws IOException, ComponentException {
        Component component = RequestData.getRequestData(request).getContentData().getComponent();
        component.service(request, response);
    }
}
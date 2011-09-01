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
package org.apache.sling.jcr.webdav.impl.handler;

import org.apache.jackrabbit.server.io.PropertyHandler;
import org.apache.jackrabbit.server.io.PropertyManagerImpl;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * PropertyManager service that uses a ServiceTracker to find available
 * PropertyHandler.
 */
public class SlingPropertyManager extends PropertyManagerImpl {

    private static final PropertyHandler[] PROPERTYHANDLERS_PROTOTYPE = new PropertyHandler[0];

    private final SlingHandlerManager<PropertyHandler> handlerManager;

    public SlingPropertyManager(final String referenceName) {
        handlerManager = new SlingHandlerManager<PropertyHandler>(referenceName);
    }

    @Override
    public void addPropertyHandler(PropertyHandler propertyHandler) {
        throw new UnsupportedOperationException(
            "This PropertyManager only supports registered PropertyHandler services");
    }

    @Override
    public PropertyHandler[] getPropertyHandlers() {
        return this.handlerManager.getHandlers(PROPERTYHANDLERS_PROTOTYPE);
    }

    public void setComponentContext(ComponentContext componentContext) {
        this.handlerManager.setComponentContext(componentContext);
    }

    public void bindPropertyHandler(final ServiceReference propertyHandlerReference) {
        this.handlerManager.bindHandler(propertyHandlerReference);
    }

    public void unbindPropertyHandler(final ServiceReference propertyHandlerReference) {
        this.handlerManager.unbindHandler(propertyHandlerReference);
    }
}

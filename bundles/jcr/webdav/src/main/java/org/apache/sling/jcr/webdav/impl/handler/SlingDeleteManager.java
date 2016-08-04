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

import org.apache.jackrabbit.server.io.DeleteHandler;
import org.apache.jackrabbit.server.io.DeleteManagerImpl;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * DeleteManager service that uses a ServiceTracker to find available
 * DeleteHandler.
 */
public class SlingDeleteManager extends DeleteManagerImpl {

    private static final DeleteHandler[] DELETEHANDLERS_PROTOTYPE = new DeleteHandler[0];
    private final SlingHandlerManager<DeleteHandler> handlerManager;

    public SlingDeleteManager(final String referenceName) {
        handlerManager = new SlingHandlerManager<DeleteHandler>(referenceName);
    }

    @Override
    public void addDeleteHandler(DeleteHandler propertyHandler) {
        throw new UnsupportedOperationException(
                "This DeleteManager only supports registered DeleteHandler services");
    }

    @Override
    public DeleteHandler[] getDeleteHandlers() {
        return this.handlerManager.getHandlers(DELETEHANDLERS_PROTOTYPE);
    }

    public void setComponentContext(ComponentContext componentContext) {
        this.handlerManager.setComponentContext(componentContext);
    }

    public void bindDeleteHandler(final ServiceReference deleteHandlerReference) {
        this.handlerManager.bindHandler(deleteHandlerReference);
    }

    public void unbindDeleteHandler(final ServiceReference deleteHandlerReference) {
        this.handlerManager.unbindHandler(deleteHandlerReference);
    }

}

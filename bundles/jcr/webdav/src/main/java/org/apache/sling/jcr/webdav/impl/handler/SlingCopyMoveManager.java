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

import org.apache.jackrabbit.server.io.CopyMoveHandler;
import org.apache.jackrabbit.server.io.CopyMoveManagerImpl;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * CopyMoveManager service that uses a ServiceTracker to find available
 * CopyMoveHandler.
 */
public class SlingCopyMoveManager extends CopyMoveManagerImpl {

    private static final CopyMoveHandler[] COPYMOVEHANDLERS_PROTOTYPE = new CopyMoveHandler[0];

    private final SlingHandlerManager<CopyMoveHandler> handlerManager;

    public SlingCopyMoveManager(final String referenceName) {
        handlerManager = new SlingHandlerManager<CopyMoveHandler>(referenceName);
    }

    @Override
    public void addCopyMoveHandler(CopyMoveHandler propertyHandler) {
        throw new UnsupportedOperationException(
            "This CopyMoveManager only supports registered CopyMoveHandler services");
    }

    @Override
    public CopyMoveHandler[] getCopyMoveHandlers() {
        return this.handlerManager.getHandlers(COPYMOVEHANDLERS_PROTOTYPE);
    }

    public void setComponentContext(ComponentContext componentContext) {
        this.handlerManager.setComponentContext(componentContext);
    }

    public void bindCopyMoveHandler(final ServiceReference copyMoveHandlerReference) {
        this.handlerManager.bindHandler(copyMoveHandlerReference);
    }

    public void unbindCopyMoveHandler(final ServiceReference copyMoveHandlerReference) {
        this.handlerManager.unbindHandler(copyMoveHandlerReference);
    }
}

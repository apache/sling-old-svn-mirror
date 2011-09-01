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

import org.apache.jackrabbit.server.io.IOHandler;
import org.apache.jackrabbit.server.io.IOManagerImpl;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * IOManager service that uses a ServiceTracker to find available IOHandlers.
 */
public class SlingIOManager extends IOManagerImpl {

    private static final IOHandler[] IOHANDLERS_PROTOTYPE = new IOHandler[0];

    private final SlingHandlerManager<IOHandler> handlerManager;

    public SlingIOManager(final String referenceName) {
        handlerManager = new SlingHandlerManager<IOHandler>(referenceName);
    }

    @Override
    public void addIOHandler(IOHandler ioHandler) {
        throw new UnsupportedOperationException(
            "This IOManager only supports registered IOHandler services");
    }

    @Override
    public IOHandler[] getIOHandlers() {
        return this.handlerManager.getHandlers(IOHANDLERS_PROTOTYPE);
    }

    public void setComponentContext(ComponentContext componentContext) {
        this.handlerManager.setComponentContext(componentContext);
    }

    public void bindIOHandler(final ServiceReference ioHandlerReference) {
        this.handlerManager.bindHandler(ioHandlerReference);
    }

    public void unbindIOHandler(final ServiceReference ioHandlerReference) {
        this.handlerManager.unbindHandler(ioHandlerReference);
    }
}

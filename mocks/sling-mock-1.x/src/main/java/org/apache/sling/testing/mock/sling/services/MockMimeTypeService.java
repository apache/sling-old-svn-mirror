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
package org.apache.sling.testing.mock.sling.services;

import java.io.IOException;
import java.io.InputStream;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.commons.mime.internal.MimeTypeServiceImpl;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.osgi.service.component.ComponentContext;

/**
 * Mock {@link MimeTypeService} implementation.
 */
@Component(inherit = false)
@Service(MimeTypeService.class)
public final class MockMimeTypeService extends MimeTypeServiceImpl {

    private boolean initialized;

    /**
     * Do lazy initializing because reading to avoid reading all defined mime
     * types from disk if not required
     */
    private void lazyInitialization() {
        if (!this.initialized) {
            this.initialized = true;
            // activate service in simulated OSGi environment
            ComponentContext componentContext = MockOsgi.newComponentContext();
            this.bindLogService(MockOsgi.newLogService(getClass()));
            activate(componentContext);
        }
    }

    @Override
    public String getMimeType(final String name) {
        lazyInitialization();
        return super.getMimeType(name);
    }

    @Override
    public String getExtension(final String mimeType) {
        lazyInitialization();
        return super.getExtension(mimeType);
    }

    @Override
    public void registerMimeType(final String mimeType, final String... extensions) {
        lazyInitialization();
        super.registerMimeType(mimeType, extensions);
    }

    @Override
    public void registerMimeType(final InputStream mimeTabStream) throws IOException {
        lazyInitialization();
        super.registerMimeType(mimeTabStream);
    }

}

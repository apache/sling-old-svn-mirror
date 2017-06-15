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
package org.apache.sling.installer.factory.model.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for the tasks.
 */
public abstract class AbstractModelTask extends InstallTask {

    /** Logger. */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final BundleContext bundleContext;

    private final Map<ServiceReference<?>, Object> services = new HashMap<>();

    public AbstractModelTask(final TaskResourceGroup group,
            final BundleContext bundleContext) {
        super(group);
        this.bundleContext = bundleContext;
    }

    protected void cleanup() {
        for(final ServiceReference<?> r : this.services.keySet()) {
            this.bundleContext.ungetService(r);
        }
        this.services.clear();
    }

    @SuppressWarnings("unchecked")
    protected <T> T getService(final Class<T> type) {
        T service = null;
        final ServiceReference<T> reference = this.bundleContext.getServiceReference(type);
        if ( reference != null ) {
            service = (T)this.services.get(reference);
            if ( service == null ) {
                service = this.bundleContext.getService(reference);
                if ( service != null ) {
                    this.services.put(reference, service);
                } else {
                }
            }
        }
        if ( service == null ) {
            logger.error("Unable to get OSGi service " + type.getName());
        }
        return service;
    }

    protected void deleteDirectory(final File dir) {
        if ( dir.exists() ) {
            for(final File f : dir.listFiles()) {
                if ( f.isDirectory() ) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
            dir.delete();
        }
    }
}

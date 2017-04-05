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
package org.apache.sling.installer.core.impl;

import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.core.impl.tasks.TaskSupport;
import org.apache.sling.installer.core.impl.util.BundleRefresher;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class providing an additional logger
 */
public abstract class AbstractInstallTask extends InstallTask {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TaskSupport support;

    /**
     * Constructor
     */
    public AbstractInstallTask(final TaskResourceGroup erl, final TaskSupport support) {
        super(erl);
        this.support = support;
    }

    /**
     * Get a logger.
     */
    protected Logger getLogger() {
        return this.logger;
    }

    /**
     * Get the bundle context.
     */
    protected BundleContext getBundleContext() {
        return this.support.getBundleContext();
    }

    protected BundleRefresher getBundleRefresher() {
        return this.support.getBundleRefresher();
    }

    protected TaskSupport getTaskSupport() {
        return this.support;
    }
}

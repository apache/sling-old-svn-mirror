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
package org.apache.sling.installer.core.impl.tasks;

import org.apache.sling.installer.core.impl.util.BundleRefresher;
import org.apache.sling.installer.core.impl.util.WABundleRefresher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * Support class for the tasks.
 */
public class TaskSupport {

    /** The bundle context. */
    private final BundleContext bundleContext;

    public TaskSupport(final BundleContext bc) {
        this.bundleContext = bc;
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public BundleRefresher getBundleRefresher() {
        return new WABundleRefresher(this.bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class),
                this.bundleContext);
    }
}

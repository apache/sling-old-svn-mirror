/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.eclipse.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.osgi.framework.Version;

public interface ISlingLaunchpadServer {

    public static final String PROP_PASSWORD = "launchpad.password";
    public static final String PROP_USERNAME = "launchpad.username";
    public static final String PROP_CONTEXT_PATH = "launchpad.contextPath";
    public static final String PROP_PORT = "launchpad.port";
    public static final String PROP_DEBUG_PORT = "launchpad.debugPort";

    public static final String PROP_INSTALL_LOCALLY = "launchpad.installLocally";
    public static final String PROP_RESOLVE_SOURCES = "launchpad.resolveSources";
    public static final String PROP_BUNDLE_VERSION_FORMAT = "launchpad.bundle.%s.version";

    ISlingLaunchpadConfiguration getConfiguration();

    void setBundleVersion(String bundleSymbolicName, Version version, IProgressMonitor monitor);

    Version getBundleVersion(String bundleSymbolicName);
}

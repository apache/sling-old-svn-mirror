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
package org.apache.sling.startupfilter;

import org.osgi.annotation.versioning.ProviderType;

/** Servlet Filter that blocks access to the Sling main
 *  servlet during startup, by returning an HTTP 503
 *  or other suitable status code.
 *
 *  A typical use case is to start this filter before
 *  the Sling main servlet (by setting a lower start level
 *  on its bundle than on the Sling engine bundle), and
 *  deactivating once startup is finished.
 */
@ProviderType
public interface StartupFilter {

    /** Enable the status filter, which outputs a default status message
     *  and a concatenation of all status messages returned
     *  by {@link StartupInfoProvider} services.
     *
     *  The filter is initially enabled.
     */
    void enable();

    /** Disable the status filter */
    void disable();

    /** True if currently enabled */
    boolean isEnabled();
}
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
package org.apache.sling.configresource.impl;


/**
 * The <code>ConfigAdminProviderConstants</code> interface
 * defines public constants for the {@link ConfigResourceProvider}.
 */
public abstract class ConfigAdminProviderConstants {

    /**
     * The common resource super type for all resources mapped into the
     * resource tree by the {@link ConfigResourceProvider} (value is
     * "sling/configadmin/resource").
     */
    public static final String RESOURCE_TYPE_ROOT = "sling/configadmin/resource";

    /**
     * The common resource super type for configuration resources mapped into the
     * resource tree by the {@link ConfigResourceProvider} (value is
     * "sling/configadmin/configurations").
     */
    public static final String RESOURCE_TYPE_CONFIGURATION_ROOT = "sling/configadmin/configurations";

    /**
     * The common resource super type for factory resources mapped into the
     * resource tree by the {@link ConfigResourceProvider} (value is
     * "sling/configadmin/factories").
     */
    public static final String RESOURCE_TYPE_FACTORIES_ROOT = "sling/configadmin/factories";

    /**
     * The resource type for a factory mapped into the resource tree by
     * the {@link ConfigResourceProvider} (value is "sling/configadmin/factory").
     */
    public static final String RESOURCE_TYPE_FACTORY = "sling/configadmin/factory";

    /**
     * The resource type for a configuration mapped into the resource tree
     * by the {@link ConfigResourceProvider} (value is "sling/configadmin/configuration").
     */
    public static final String RESOURCE_TYPE_CONFIGURATION = "sling/configadmin/configuration";

}

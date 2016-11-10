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
package org.apache.sling.jcr.base.internal;

/**
 * Define the default whitelist in its own class to better
 * keep track of it. The goal is to reduce it to the bare
 * minimum over time.
 */
class DefaultWhitelist {
    // TODO: remove bundles as their dependency on admin login is fixed, see SLING-5355 for linked issues
    static final String [] WHITELISTED_BSN = {
            "org.apache.sling.discovery.commons",
            "org.apache.sling.discovery.base",
            "org.apache.sling.discovery.oak",
            "org.apache.sling.extensions.webconsolesecurityprovider",
            "org.apache.sling.i18n",
            "org.apache.sling.installer.provider.jcr",
            "org.apache.sling.jcr.base",
            "org.apache.sling.jcr.contentloader",
            "org.apache.sling.jcr.davex",
            "org.apache.sling.jcr.jackrabbit.usermanager",
            "org.apache.sling.jcr.oak.server",
            "org.apache.sling.jcr.repoinit",
            "org.apache.sling.jcr.resource",
            "org.apache.sling.jcr.webconsole",
            "org.apache.sling.resourceresolver",
            "org.apache.sling.servlets.post", // remove when 2.3.16 is released
            "org.apache.sling.servlets.resolver"
    };
}
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
package org.apache.sling.jcr.oak.server.internal;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "Apache Sling Login Admin Whitelist",
    description = "Defines which bundles can use SlingRepository.loginAdministrative()"
)
@interface LoginAdminWhitelistConfiguration {

    /** Need to allow for bypassing the whitelist, for backwards
     *  compatibility with previous Sling versions which didn't
     *  implement it. Setting this to true is not recommended
     *  and logged as a warning.
     */
    @AttributeDefinition(
        name = "Bypass the whitelist",
        description = "Allow all bundles to use loginAdministrative(). Should ONLY be used " +
                      "for backwards compatibility reasons and if you are aware of " +
                      "the related security risks."
    )
    boolean whitelist_bypass() default false;

    @AttributeDefinition(
        name = "Whitelist regexp",
        description = "Regular expression for bundle symbolic names for which loginAdministrative() " +
                      "is allowed. NOT recommended for production use, but useful for testing with " +
                      "generated bundles."
    )
    String whitelist_bundles_regexp() default "";

    @AttributeDefinition(
        name = "Default whitelisted BSNs",
        description = "Default list of bundle symbolic names for which loginAdministrative() is allowed."
    )
    String[] whitelist_bundles_default() default {
            // TODO: remove bundles as their dependency on admin login is fixed, see SLING-5355 for linked issues
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

    @AttributeDefinition(
        name = "Additional whitelisted BSNs",
        description = "Additional list of bundle symbolic names for which loginAdministrative() is allowed."
    )
    String[] whitelist_bundles_additional() default {};
}

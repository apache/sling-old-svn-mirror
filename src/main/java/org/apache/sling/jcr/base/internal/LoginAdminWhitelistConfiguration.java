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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "Apache Sling Login Admin Whitelist",
    description = "Defines which bundles can use SlingRepository.loginAdministrative()"
)
@interface LoginAdminWhitelistConfiguration {

    /**
     * Need to allow for bypassing the whitelist, for backwards
     * compatibility with previous Sling versions which didn't
     * implement it. Setting this to true is not recommended
     * and logged as a warning.
     */
    @AttributeDefinition(
        name = "Bypass the whitelist",
        description = "Allow all bundles to use loginAdministrative(). Should ONLY be used " +
                      "for backwards compatibility reasons and if you are aware of " +
                      "the related security risks."
    )
    boolean whitelist_bypass() default false;

    /**
     * Regular expression for bundle symbolic names for which loginAdministrative()
     * is allowed. NOT recommended for production use, but useful for testing with
     * generated bundles.
     * <br>
     * Note that this property is hidden in order not to advertise its presence,
     * because it is intended only for testing purposes. Specifically for use-cases
     * like Pax-Exam, where bundles are generated on the fly and the bundle symbolic
     * name cannot be predicted, but follows a predictable pattern.
     *
     * @return The configured regular exression.
     */
    String whitelist_bundles_regexp() default "";

    /**
     * Default list of bundle symbolic names for which loginAdministrative() is allowed.
     *
     * @return The default whitelisted BSNs
     * @deprecated use {@link WhitelistFragment} configurations instead
     */
    @Deprecated
    String[] whitelist_bundles_default() default {};

    /**
     * Additional list of bundle symbolic names for which loginAdministrative() is allowed.
     *
     * @return Additional whitelisted BSNs
     * @deprecated use {@link WhitelistFragment} configurations instead
     */
    @Deprecated
    String[] whitelist_bundles_additional() default {};
}

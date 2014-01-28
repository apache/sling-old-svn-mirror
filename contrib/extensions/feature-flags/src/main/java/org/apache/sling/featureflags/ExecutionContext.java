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
package org.apache.sling.featureflags;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ProviderType;

/**
 * The {@code ExecutionContext} interface provides access to the context for
 * evaluating whether a feature is enabled or not. Instances of this object are
 * provided to the {@link Feature#isEnabled(ExecutionContext)} to help
 * evaluating whether the feature is enabled or not.
 * <p>
 * The {@link Features} service {@link ClientContext} generating methods create
 * an instance of this to collect the enabled {@link Feature} services for the
 * creation of the {@link ClientContext} instance.
 * <p>
 * This object provides access to live data and must only be used to read
 * information. Modifying content through a {@code ResourceResolver} directly or
 * indirectly provided by this object is considered inappropriate and faulty
 * behaviour.
 */
@ProviderType
public interface ExecutionContext {

    /**
     * Returns a {@code HttpServletRequest} object to retrieve information which
     * may influence the decision whether a {@link Feature} is enabled or not.
     * If a {@code HttpServletRequest} object is not available in the context,
     * this method may return {@code null}.
     * <p>
     * In a Sling request processing context, the {@code HttpServletRequest}
     * object returned may actually be a {@code SlingHttpServletRequest}.
     *
     * @return the request or <code>null</code>
     */
    HttpServletRequest getRequest();

    /**
     * Returns a {@code ResourceResolver} object to retrieve information which
     * may influence the decision whether a {@link Feature} is enabled or not.
     * If a {@code ResourceResolver} object is not available in the context,
     * this method may return {@code null}.
     *
     * @return the resource resolver
     */
    ResourceResolver getResourceResolver();
}

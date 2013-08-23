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
package org.apache.sling.api.resource;

import javax.servlet.http.HttpServletRequest;

import aQute.bnd.annotation.ConsumerType;

/**
 * Optional service to decorate {@link Resource}s returned by
 * the {@link ResourceResolver}.
 * Typical use cases for a decorator are
 * - overwrite resource type/resource super type (for example
 *   based on the resource path)
 * - add metadata
 *
 * @since 2.1
 */
@ConsumerType
public interface ResourceDecorator {

    /**
     * Decorate a resource.
     * If the service decorates the resource it should return
     * the new resource. If the service does not want to decorate
     * the resource, it should return the original resource.
     * Returning <code>null</code> is considered the same as
     * returning the original resource.
     * @param resource The resource to decorate
     * @return The decorated resource, the original resource or null.
     */
    Resource decorate(Resource resource);

    /**
     * Decorate a resource.
     * If the service decorates the resource it should return
     * the new resource. If the service does not want to decorate
     * the resource, it should return the original resource.
     * Returning <code>null</code> is considered the same as
     * returning the original resource.
     * @param resource The resource to decorate
     * @param request The current request.
     * @return The decorated resource, the original resource or null.
     *
     * @deprecated since 2.3.0 (and JCR Resource 2.1.0), this method will not be invoked.
     */
    @Deprecated
    Resource decorate(Resource resource, HttpServletRequest request);
}

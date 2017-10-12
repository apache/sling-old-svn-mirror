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
package org.apache.sling.servlethelpers;

import javax.servlet.RequestDispatcher;

import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;

import aQute.bnd.annotation.ConsumerType;

/**
 * Interface to create a mock {@link RequestDispatcher} when calling the getRequestDispatcher methods
 * on {@link MockSlingHttpServletRequest} instances.
 */
@ConsumerType
public interface MockRequestDispatcherFactory {

    /**
     * Get request dispatcher for given path.
     * @param path Path
     * @param options Options. Null if no options are provided.
     * @return Request dispatcher
     */
    RequestDispatcher getRequestDispatcher(String path, RequestDispatcherOptions options);

    /**
     * Get request dispatcher for given resource.
     * @param resource Resource
     * @param options Options. Null if no options are provided.
     * @return Request dispatcher
     */
    RequestDispatcher getRequestDispatcher(Resource resource, RequestDispatcherOptions options);

}

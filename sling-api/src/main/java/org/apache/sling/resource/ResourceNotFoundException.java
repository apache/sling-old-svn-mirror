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
package org.apache.sling.resource;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.exceptions.HttpStatusCodeException;

/**
 * The <code>ResourceNotFoundException</code> is a special
 * {@link HttpStatusCodeException} fixing the status code to 404 (Not Found).
 * This exception may be thrown by the if a requested
 * {@link org.apache.sling.resource.Resource} may not be found.
 */
public class ResourceNotFoundException extends HttpStatusCodeException {

    public ResourceNotFoundException(String message) {
        super(HttpServletResponse.SC_NOT_FOUND, message);
    }

}

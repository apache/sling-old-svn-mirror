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

import org.apache.sling.api.SlingException;

/**
 * An Exception that causes Sling to return a 404 (NOT FOUND) status code. This
 * exception should not be caught but rather let be handed up the call stack up
 * to the Sling error and exception handling.
 * <p>
 * The advantage of using this exception over the
 * <code>HttpServletResponse.sendError</code> methods is that the request can
 * be aborted immediately all the way up in the call stack and that in addition
 * to the status code and an optional message a <code>Throwable</code> may be
 * supplied providing more information.
 */
public class ResourceNotFoundException extends SlingException {

    private static final long serialVersionUID = -6684709279554347984L;

    private final String resource;

    public ResourceNotFoundException(String message) {
        this(null, message);
    }

    public ResourceNotFoundException(String resource, String message) {
        super("Resource at '"+ resource + "' not found: " + message);
        this.resource = resource;
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        this(null, message, cause);
    }

    public ResourceNotFoundException(String resource, String message,
            Throwable cause) {
        super(message, cause);
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}

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
package org.apache.sling.api.request;

import org.apache.sling.api.SlingException;

/**
 * The <code>RecursionTooDeepException</code> is thrown by the Sling
 * implementation if to many recursive content inclusions take place. The limit
 * of recursive inclusions is implementation dependent.
 */
public class RecursionTooDeepException extends SlingException {

    private static final long serialVersionUID = 776668636261012142L;

    /**
     * Creates a new instance of this class reporting the exception occurred
     * while trying to include the output for rendering the resource at the
     * given path.
     * <p>
     * The resource path is the actual message of the exception.
     *
     * @param resourcePath The path of the resource whose output inclusion
     *            causes this exception.
     */
    public RecursionTooDeepException(String resourcePath) {
        super(resourcePath);
    }

}

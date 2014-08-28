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
 * The <code>TooManyCallsException</code> is thrown by the Sling implementation
 * if to many inclusions have been called for during a single request. The limit
 * of inclusions is implementation dependent.
 */
public class TooManyCallsException extends SlingException {

    private static final long serialVersionUID = -8725296173002395104L;

    /**
     * Creates an instance of this exception naming the Servlet (or Script)
     * whose call caused this exception to be thrown.
     * <p>
     * The servlet name is the actual message of the exception.
     *
     * @param servletName The name of the Servlet (or Script) causing this
     *            exception.
     */
    public TooManyCallsException(String servletName) {
        super(servletName);
    }

}

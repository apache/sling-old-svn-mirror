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
package org.apache.sling.auth.form;

public enum FormReason {

    /**
     * The login form is request because the credentials previously entered very
     * not valid to login to the repository.
     */
    INVALID_CREDENTIALS("Username and Password do not match"),

    /**
     * The login form is requested because an existing session has timed out and
     * the credentials have to be entered again.
     */
    TIMEOUT("Session timed out, please login again");

    /**
     * The user-friendly message returned by {@link #toString()}
     */
    private final String message;

    /**
     * Creates an instance of the reason conveying the given descriptive reason.
     *
     * @param message The descriptive reason.
     */
    private FormReason(String message) {
        this.message = message;
    }

    /**
     * Returns the message set when constructing this instance. To get the
     * official name call the <code>name()</code> method.
     */
    @Override
    public String toString() {
        return message;
    }
}

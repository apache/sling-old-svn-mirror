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
package org.apache.sling.api;

import javax.servlet.ServletException;

/**
 * The <code>SlingException</code> class defines a general exception that may
 * be thrown when unexpected situations occurr while processing requests.
 */
public class SlingException extends ServletException {

    /**
     * Serial Version ID for pre Java2 RMI
     */
    private static final long serialVersionUID = -1243027389278210618L;

    /**
     * Constructs a new Sling exception.
     */
    public SlingException() {
        super();
    }

    /**
     * Constructs a new Sling exception with the given text. The Sling framework
     * may use the text to write it to a log.
     * 
     * @param text the exception text
     */
    public SlingException(String text) {
        super(text);
    }

    /**
     * Constructs a new Sling exception when the Servlet needs to do the
     * following:
     * <ul>
     * <li>throw an exception
     * <li>include the "root cause" exception
     * <li>include a description message
     * </ul>
     * 
     * @param text the exception text
     * @param cause the root cause
     */
    public SlingException(String text, Throwable cause) {
        super(text, cause);
    }

    /**
     * Constructs a new Sling exception when the Servlet needs to throw an
     * exception. The exception's message is based on the localized message of
     * the underlying exception.
     * 
     * @param cause the root cause
     */
    public SlingException(Throwable cause) {
        super(cause);
    }
}
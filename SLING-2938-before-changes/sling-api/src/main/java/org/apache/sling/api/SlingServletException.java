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
 * The <code>SlingServletException</code> is a runtime exception wrapper for
 * the Servlet API <code>ServletException</code>. This exception is used to
 * catch a <code>ServletException</code> and forward it as a runtime exception
 * to be handled at the outermost level.
 */
public class SlingServletException extends SlingException {

    private static final long serialVersionUID = 8666411662509951915L;

    public SlingServletException(ServletException cause) {
        super(cause);
    }

}

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
package org.apache.sling.scripting.jsp;

import org.apache.sling.api.SlingException;

/**
 * The <code>SlingPageException</code> is a runtime exception.
 * This exception is used to handle the JSP page exception handler
 * <code><%@ page errorPage=</code> scenario and forward as a
 *  runtime exception to be handled at the outermost level.
 */
public class SlingPageException extends SlingException {

    private final String errorPage;

    public SlingPageException(final String errorPage) {
        this.errorPage = errorPage;
    }

    public String getErrorPage() {
        return this.errorPage;
    }

}

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
package org.apache.sling.scripting.jsp.util;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.jsp.PageContext;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;

/**
 * The <code>TagUtil</code> class provides a series of utility methods which
 * may be used by tag library implementations to access Component API specific
 * objects and do other processing.
 */
public final class TagUtil {

    // no instantiation !
    private TagUtil() {}

    /**
     * Log an INFO message to the given logger with context information from
     * the <code>pageContext</code>.
     *
     * @param log The SLF4J <code>Logger</code> to use for writing the message.
     * @param pageContext The JSP page context providing contextual information
     *      for the log message.
     * @param message The message to log. If this is <code>null</code>, the
     *      message of the exception is taken, unless the expression is alos
     *      <code>null</code>, in which case this method just logs nothing.
     * @param t The exception to be logged alongside the message. This may
     *      be <code>null</code>. If this is a <code>ServletException</code> it
     *      is unwrapped by the {@link #getRootCause(ServletException)} method
     *      before being used.
     */
    public static void log(Logger log, PageContext pageContext, String message, Throwable t) {
        //unwrap any exception inside ServletException(s)
        if (t instanceof ServletException) {
            t = getRootCause((ServletException) t);
        }

        // ensure message, return if neither message nor exception
        if (message == null) {
            if (t == null) {
                return;
            }
            message = t.getMessage();
        }

        log.info("Problem on page {}: {}", new Object[] {
            pageContext.getPage(), message }, t);
    }

    /**
     * Unwrap a component exception to get the true root cause of an exception
     */
    public static Throwable getRootCause(ServletException e) {
        ServletException current = e;
        while (current.getRootCause() != null) {
            Throwable t = current.getRootCause();
            if (t instanceof ServletException) {
                current = (ServletException) t;
            } else {
                return t;
            }
        }
        return current;
    }

    public static SlingHttpServletRequest getRequest(PageContext pageContext) {
        ServletRequest req = pageContext.getRequest();
        while (!(req instanceof SlingHttpServletRequest)) {
            if (req instanceof ServletRequestWrapper) {
                req = ((ServletRequestWrapper) req).getRequest();
            } else {
                throw new IllegalStateException("request wrong class");
            }
        }

        return (SlingHttpServletRequest) req;
    }

    public static SlingHttpServletResponse getResponse(PageContext pageContext) {
        ServletResponse req = pageContext.getResponse();
        while (!(req instanceof SlingHttpServletResponse)) {
            if (req instanceof ServletResponseWrapper) {
                req = ((ServletResponseWrapper) req).getResponse();
            } else {
                throw new IllegalStateException("response wrong class");
            }
        }

        return (SlingHttpServletResponse) req;
    }
}
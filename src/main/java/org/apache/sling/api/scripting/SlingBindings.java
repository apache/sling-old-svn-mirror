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
package org.apache.sling.api.scripting;

import java.io.PrintWriter;
import java.util.HashMap;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;

/**
 * The <code>SlingBindings</code> class is used to prepare global variables
 * for script execution. The constants in this class define names of variables
 * which <em>MUST</em> or <em>MAY</em> be provided for the script execution.
 * Other variables may be define as callers see fit.
 */
public class SlingBindings extends HashMap<String, Object> {

    /**
     * The name of the global scripting variable providing the
     * {@link org.apache.sling.api.SlingHttpServletRequest} object (value is
     * "request"). The value of the scripting variable is the same as that
     * returned by the
     * {@link org.apache.sling.api.scripting.SlingScriptHelper#getRequest()}
     * method.
     * <p>
     * This bound variable is required in the bindings given the script.
     */
    public static final String REQUEST = "request";

    /**
     * The name of the global scripting variable providing the
     * {@link org.apache.sling.api.SlingHttpServletResponse} object (value is
     * "response"). The value of the scripting variable is the same as that
     * returned by the
     * {@link org.apache.sling.api.scripting.SlingScriptHelper#getResponse()}
     * method.
     * <p>
     * This bound variable is required in the bindings given the script.
     */
    public static final String RESPONSE = "response";

    /**
     * The name of the global scripting variable providing the
     * {@link org.apache.sling.api.scripting.SlingScriptHelper} for the request
     * (value is "sling").
     * <p>
     * This bound variable is optional. If existing, the script helper instance
     * must be bound to the same request and response objects as bound with the
     * {@link #REQUEST} and {@link #RESPONSE} variables. If this variable is not
     * bound, the script implementation will create it before actually
     * evaluating the script.
     */
    public static final String SLING = "sling";

    /**
     * The name of the global scripting variable providing the
     * {@link org.apache.sling.api.resource.Resource} object (value is
     * "resource"). The value of the scripting variable is the same as that
     * returned by the <code>SlingScriptHelper.getRequest().getResource()</code>
     * method.
     * <p>
     * This bound variable is optional. If existing, the resource must be bound
     * to the same resource as returned by the
     * <code>SlingHttpServletRequest.getResource()</code> method. If this
     * variable is not bound, the script implementation will bind it before
     * actually evaluating the script.
     */
    public static final String RESOURCE = "resource";

    /**
     * The name of the global scripting variable providing the
     * <code>java.io.PrintWriter</code> object to return the response content
     * (value is "out"). The value of the scripting variable is the same as that
     * returned by the <code>SlingScriptHelper.getResponse().getWriter()</code>
     * method.
     * <p>
     * Note, that it may be advisable to implement a lazy acquiring writer for
     * the <em>out</em> variable to enable the script to write binary data to
     * the response output stream instead of the writer.
     * <p>
     * This bound variable is optional. If existing, the resource must be bound
     * to the same writer as returned by the
     * <code>SlingHttpServletResponse.getWriter()</code> method of the
     * response object bound to the {@link #RESPONSE} variable. If this variable
     * is not bound, the script implementation will bind it before actually
     * evaluating the script.
     */
    public static final String OUT = "out";

    /**
     * The name of the global scripting variable indicating whether the output
     * used by the script should be flushed after the script evaluation ended
     * normally (value is "flush").
     * <p>
     * The type of this variable is <code>java.lang.Boolean</code> indicating
     * whether to flush the output (value is <code>TRUE</code>) or not (value
     * is <code>FALSE</code>). If the variable has a non-<code>null</code>
     * value of another type, the output is not flush as if the value would be
     * <code>FALSE</code>.
     */
    public static final String FLUSH = "flush";

    /**
     * The name of the global scripting variable providing a logger which may be
     * used for logging purposes (value is "log"). The logger provides the API
     * defined by the SLF4J <code>org.slf4j.Logger</code> interface.
     * <p>
     * This bound variable is optional. If this variable is not bound, the
     * script implementation will bind it before actually evaluating the script.
     */
    public static final String LOG = "log";

    /**
     * Sets the {@link #FLUSH} property to <code>flush</code>.
     */
    public void setFlush(boolean flush) {
        put(FLUSH, flush);
    }

    /**
     * Returns the {@link #FLUSH} property if not <code>null</code> and a
     * <code>boolean</code>. Otherwise <code>false</code> is returned.
     */
    public boolean getFlush() {
        Object value = get(FLUSH);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        return false;
    }

    /**
     * Sets the {@link #LOG} property to <code>log</code> if not
     * <code>null</code>.
     */
    public void setLog(Logger log) {
        if (log != null) {
            put(LOG, log);
        }
    }

    /**
     * Returns the {@link #LOG} property if not <code>null</code> and a
     * <code>org.slf4j.Logger</code> instance. Otherwise <code>null</code>
     * is returned.
     */
    public Logger getLog() {
        Object value = get(LOG);
        if (value instanceof Logger) {
            return (Logger) value;
        }

        return null;
    }

    /**
     * Sets the {@link #OUT} property to <code>out</code> if not
     * <code>null</code>.
     */
    public void setOut(PrintWriter out) {
        if (out != null) {
            put(OUT, out);
        }
    }

    /**
     * Returns the {@link #OUT} property if not <code>null</code> and a
     * <code>PrintWriter</code> instance. Otherwise <code>null</code> is
     * returned.
     */
    public PrintWriter getOut() {
        Object value = get(OUT);
        if (value instanceof PrintWriter) {
            return (PrintWriter) value;
        }

        return null;
    }

    /**
     * Sets the {@link #REQUEST} property to <code>request</code> if not
     * <code>null</code>.
     */
    public void setRequest(SlingHttpServletRequest request) {
        if (request != null) {
            put(REQUEST, request);
        }
    }

    /**
     * Returns the {@link #REQUEST} property if not <code>null</code> and a
     * <code>SlingHttpServletRequest</code> instance. Otherwise
     * <code>null</code> is returned.
     */
    public SlingHttpServletRequest getRequest() {
        Object value = get(REQUEST);
        if (value instanceof SlingHttpServletRequest) {
            return (SlingHttpServletRequest) value;
        }

        return null;
    }

    /**
     * Sets the {@link #RESOURCE} property to <code>resource</code> if not
     * <code>null</code>.
     */
    public void setResource(Resource resource) {
        if (resource != null) {
            put(RESOURCE, resource);
        }
    }

    /**
     * Returns the {@link #RESOURCE} property if not <code>null</code> and a
     * <code>Resource</code> instance. Otherwise <code>null</code> is
     * returned.
     */
    public Resource getResource() {
        Object value = get(RESOURCE);
        if (value instanceof Resource) {
            return (Resource) value;
        }

        return null;
    }

    /**
     * Sets the {@link #RESPONSE} property to <code>response</code> if not
     * <code>null</code>.
     */
    public void setResponse(SlingHttpServletResponse response) {
        if (response != null) {
            put(RESPONSE, response);
        }
    }

    /**
     * Returns the {@link #RESPONSE} property if not <code>null</code> and a
     * <code>SlingHttpServletResponse</code> instance. Otherwise
     * <code>null</code> is returned.
     */
    public SlingHttpServletResponse getResponse() {
        Object value = get(RESPONSE);
        if (value instanceof SlingHttpServletResponse) {
            return (SlingHttpServletResponse) value;
        }

        return null;
    }

    /**
     * Sets the {@link #SLING} property to <code>sling</code> if not
     * <code>null</code>.
     */
    public void setSling(SlingScriptHelper sling) {
        if (sling != null) {
            put(SLING, sling);
        }
    }

    /**
     * Returns the {@link #SLING} property if not <code>null</code> and a
     * <code>SlingScriptHelper</code> instance. Otherwise <code>null</code>
     * is returned.
     */
    public SlingScriptHelper getSling() {
        Object value = get(SLING);
        if (value instanceof SlingScriptHelper) {
            return (SlingScriptHelper) value;
        }

        return null;
    }

}

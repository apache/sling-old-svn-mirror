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

import java.io.IOException;
import java.util.Map;

import org.apache.sling.api.exceptions.SlingException;

/**
 * The <code>SlingScriptEngine</code> interface defines the API to be
 * implemented by scripting engines supporting the evaluation of scripts on
 * behalf of the request.
 * <p>
 * Scripts are located by the {@link SlingScriptResolver} which takes the
 * extension of the script file name to find a scripting engine supporting the
 * respective extension as per the contents of the string array returned by the
 * {@link #getExtensions()} method.
 * <p>
 * For example, a script whose path is <code>/scripts/sample.js</code> has an
 * extension of <code>js</code> and thus requires a scripting engine returning
 * at least that extension contained in the response to the
 * {@link #getExtensions()} method.
 */
public interface SlingScriptEngine {

    /**
     * The name of the global scripting variable providing the
     * {@link SlingScriptHelper} for the request (value is "sling").
     */
    static final String SLING = "sling";

    /**
     * The name of the global scripting variable providing the
     * {@link org.apache.sling.api.SlingHttpServletRequest} object (value is
     * "request"). The value of the scripting variable is the same as that
     * returned by the {@link SlingScriptHelper#getRequest()} method.
     */
    static final String REQUEST = "request";

    /**
     * The name of the global scripting variable providing the
     * {@link org.apache.sling.api.SlingHttpServletResponse} object (value is
     * "response"). The value of the scripting variable is the same as that
     * returned by the {@link SlingScriptHelper#getResponse()} method.
     */
    static final String RESPONSE = "response";

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
     */
    static final String OUT = "out";

    /**
     * A list of script file name extensions identifying scripts which may be
     * evaluated by the scripting engine. For example a scripting engine
     * supporting JavaScript might return a single element array
     * <code>[ "js" ]</code>.
     */
    String[] getExtensions();

    /**
     * Return a descriptive name for the scripting engine. For example a
     * scripting engine supporting JavaScript using the Mozilla Rhino engine
     * might return "Rhino JavaScript Engine". The string returned is used for
     * informational purposes only.
     */
    String getEngineName();

    /**
     * Returns a version identifier for the scripting engine. For example a
     * scripting engine supporting JavaScript using the Mozilla Rhino engine of
     * version 1.6R7 might return "1.6R7".
     */
    String getEngineVersion();

    /**
     * Evaluates the given script setting the contents of the <code>props</code>
     * map as global variables of the script.
     * <p>
     * The <code>props</code> map is expected to contain the entries for the
     * {@link #SLING}, {@link #REQUEST}, {@link #RESPONSE} and {@link #OUT}
     * variables.
     *
     * @param script The {@link SlingScript} to evaluate
     * @param props The map of properties used as global variables for the
     *            script.
     * @throws SlingException If an error occurrs running the script. The cause
     *             of the problem should be provided as the exceptions cause.
     * @throws IOException If an error occurrs reading the script or producing
     *             the response or if any other input or output problem occurrs.
     */
    void eval(SlingScript script, Map<String, Object> props)
            throws SlingException, IOException;

}

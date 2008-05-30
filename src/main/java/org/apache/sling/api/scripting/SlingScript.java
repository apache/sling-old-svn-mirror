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

import org.apache.sling.api.resource.Resource;

/**
 * The <code>SlingScript</code> defines the API for objects which encapsulate
 * a script resolved by the {@link SlingScriptResolver}. To have a script
 * evaluated prepare a {@link SlingBindings} instance of variables used as
 * global variables to the script and call the {@link #eval(SlingBindings)}
 * method.
 * <p>
 * Objects implementing this interface are returned by the
 * {@link SlingScriptResolver#findScript(org.apache.sling.api.resource.ResourceResolver, String)}
 * method.
 */
public interface SlingScript {

    /**
     * Returns the Resource providing the script source code.
     */
    Resource getScriptResource();

    /**
     * Evaluates this script using the bound variables as global variables to
     * the script.
     *
     * @param props The {@link SlingBindings} providing the bound variables for
     *            evaluating the script. Any bound variables must conform to the
     *            requirements of the {@link SlingBindings} predefined variables
     *            set.
     * @return The value returned by the script.
     * @throws ScriptEvaluationException If an error occurrs executing the
     *             script or preparing the script execution. The cause of the
     *             evaluation execption is available as the exception cause.
     */
    Object eval(SlingBindings props);

    /**
     * Evaluates this script using the bound variables as global variables to
     * the script and then calls the given method with the arguments.
     *
     * @param props The {@link SlingBindings} providing the bound variables for
     *            evaluating the script. Any bound variables must conform to the
     *            requirements of the {@link SlingBindings} predefined variables
     *            set.
     * @param method The name of the method to call.
     * @param args The arguments for the method call.
     * @return The value returned by the method from the script.
     * @throws ScriptEvaluationException If an error occurrs executing the
     *             script or preparing the script execution. The cause of the
     *             evaluation execption is available as the exception cause.
     */
    Object call(SlingBindings props, String method, Object... args);
}

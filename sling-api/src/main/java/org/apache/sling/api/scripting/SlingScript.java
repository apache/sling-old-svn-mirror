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
import java.io.Reader;

/**
 * The <code>SlingScript</code> defines the API for objects which encapsulate
 * a script resolved by the {@link SlingScriptResolver}. To have a script
 * evaluated by its {@link SlingScriptEngine} prepare a
 * <code>Map&lt;<String, Object&gt;</code> of properties used as top level
 * (global) variables to the script and call the
 * {@link SlingScriptEngine#eval(SlingScript, java.util.Map)} method.
 * <p>
 * Objects implementing this interface are returned by the
 * {@link SlingScriptResolver#resolveScript(org.apache.sling.api.SlingHttpServletRequest)}
 * method.
 */
public interface SlingScript {

    /**
     * Returns the (path) name of this script.
     */
    String getScriptPath();

    /**
     * Returns the {@link SlingScriptEngine} used to evaluate this script.
     */
    SlingScriptEngine getScriptEngine();

    /**
     * Returns a <code>java.io.Reader</code> from which the scripting engine
     * may read the script source to be evaluated.
     * <p>
     * The <code>Reader</code> returned need not be buffered. Any buffering
     * used for enhanced performance is expected to be implemented by consumers
     * of this method.
     *
     * @throws IOException If an error occurrs acquiring the <code>Reader</code>
     *             for the script source.
     */
    Reader getScriptReader() throws IOException;
}

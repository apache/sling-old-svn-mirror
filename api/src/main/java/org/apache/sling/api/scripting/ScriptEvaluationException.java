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

import org.apache.sling.api.SlingException;

/**
 * The <code>ScriptEvaluationException</code> is thrown by the
 * {@link SlingScript#eval(SlingBindings)} method if an error occurrs evaluating
 * the script.
 */
public class ScriptEvaluationException extends SlingException {

    private static final long serialVersionUID = -274759591325189020L;

    private final String scriptName;

    public ScriptEvaluationException(String scriptName, String message) {
        super(message);

        this.scriptName = scriptName;
    }

    public ScriptEvaluationException(String scriptName, String message,
            Throwable cause) {
        super(message, cause);

        this.scriptName = scriptName;
    }

    public String getScriptName() {
        return scriptName;
    }
}

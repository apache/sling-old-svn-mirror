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
package org.apache.sling.scripting.python;

import javax.script.ScriptEngine;

import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.python.core.PySystemState;

public class PythonScriptEngineFactory extends AbstractScriptEngineFactory {

    public final static String PYTON_SCRIPT_EXTENSION = "py";

    public final static String PYTHON_MIME_TYPE = "application/x-python";

    public final static String SHORT_NAME = "python";

    public PythonScriptEngineFactory() {
        setExtensions(PYTON_SCRIPT_EXTENSION);
        setMimeTypes(PYTHON_MIME_TYPE);
        setNames(SHORT_NAME);
    }

    public ScriptEngine getScriptEngine() {
        return new PythonScriptEngine(this);
    }

    public String getLanguageName() {
        return "python";
    }

    public String getLanguageVersion() {
        return PySystemState.version;
    }

}

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
package org.apache.sling.scripting.jst;

import javax.script.ScriptEngine;

import org.apache.sling.scripting.api.AbstractScriptEngineFactory;

/** Defines attributes of the JST script engine */
public class JstScriptEngineFactory extends AbstractScriptEngineFactory {

    public static final String JST_SCRIPT_EXTENSION = "jst";
    public final static String JST_MIME_TYPE = "application/x-sling-jst";
    public final static String SHORT_NAME = "jst";

    public JstScriptEngineFactory() {
        setExtensions(JST_SCRIPT_EXTENSION);
        setMimeTypes(JST_MIME_TYPE);
        setNames(SHORT_NAME);
    }
    
    public ScriptEngine getScriptEngine() {
        return new JstScriptEngine(this);
    }

    public String getEngineName() {
        return "JST script engine (sling JavaScript Templates)";
    }

    public String getEngineVersion() {
        return "1.0";
    }

    public String getLanguageName() {
        return "JavaScript Templates";
    }

    public String getLanguageVersion() {
        return "1.0";
    }

}

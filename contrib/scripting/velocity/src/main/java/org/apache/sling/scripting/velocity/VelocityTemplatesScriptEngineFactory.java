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
package org.apache.sling.scripting.velocity;

import javax.script.ScriptEngine;

import org.apache.sling.scripting.api.AbstractScriptEngineFactory;

public class VelocityTemplatesScriptEngineFactory extends AbstractScriptEngineFactory {

    public final static String VELOCITY_SCRIPT_EXTENSION = "vlt";

    public final static String VELOCITY_MIME_TYPE = "text/x-velocity";

    public final static String VELOCITY_SHORT_NAME = "velocity";

    public VelocityTemplatesScriptEngineFactory() {
        setExtensions(VELOCITY_SCRIPT_EXTENSION);
        setMimeTypes(VELOCITY_MIME_TYPE);
        setNames(VELOCITY_SHORT_NAME);
    }

    public ScriptEngine getScriptEngine() {
        return new VelocityTemplatesScriptEngine(this);
    }

    public String getLanguageName() {
        return "velocity";
    }

    public String getLanguageVersion() {
        return "1.7";
    }
    
}
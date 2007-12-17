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
package org.apache.sling.scripting.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

public abstract class AbstractScriptEngineFactory implements
        ScriptEngineFactory {

    private String engineName;

    private String engineVersion;

    private List<String> extensions;

    private List<String> mimeTypes;

    private List<String> names;

    protected AbstractScriptEngineFactory() {
        String name = null;
        String version = null;

        // try to get the manifest
        Manifest manifest = null;
        InputStream ins = null;
        try {
            ins = getClass().getResourceAsStream("/META-INF/MANIFEST.MF");
            if (ins != null) {
                manifest = new Manifest(ins);
                Attributes attrs = manifest.getMainAttributes();
                name = attrs.getValue("ScriptEngine-Name");
                version = attrs.getValue("ScriptEngine-Version");
            }
        } catch (IOException ioe) {
            // might want to log ?
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
        }

        // fall back to class name and version zero
        if (name == null) {
            String className = getClass().getName();
            name = className.substring(className.lastIndexOf('.') + 1);
        }
        if (version == null) {
            version = "0";
        }

        setEngineName(name);
        setEngineVersion(version);
        
        setExtensions((String[]) null);
        setMimeTypes((String[]) null);
        setNames((String[]) null);
    }

    public String getEngineName() {
        return engineName;
    }

    protected void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    protected void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    protected void setExtensions(String... extensions) {
        if (extensions == null) {
            this.extensions = Collections.emptyList();
        } else {
            this.extensions = Arrays.asList(extensions);
        }
    }

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    protected void setMimeTypes(String... mimeTypes) {
        if (mimeTypes == null) {
            this.mimeTypes = Collections.emptyList();
        } else {
            this.mimeTypes = Arrays.asList(mimeTypes);
        }
    }

    public List<String> getNames() {
        return names;
    }

    protected void setNames(String... names) {
        if (names == null) {
            this.names = Collections.emptyList();
        } else {
            this.names = Arrays.asList(names);
        }
    }

    public String getMethodCallSyntax(String obj, String m, String[] args) {
        StringBuffer callSyntax = new StringBuffer();
        callSyntax.append(obj).append('.').append(m).append('(');
        for (int i = 0; args != null && i < args.length; i++) {
            if (i > 0) callSyntax.append(',');
            callSyntax.append(args[i]);
        }
        callSyntax.append(')');
        return callSyntax.toString();
    }

    public String getOutputStatement(String value) {
        return "out.print(" + value + ")";
    }

    public Object getParameter(String name) {
        if (ScriptEngine.ENGINE.equals(name)) {
            return getEngineName();
        } else if (ScriptEngine.ENGINE_VERSION.equals(name)) {
            return getEngineVersion();
        } else if (ScriptEngine.NAME.equals(name)) {
            return getNames();
        } else if (ScriptEngine.LANGUAGE.equals(name)) {
            return getLanguageName();
        } else if (ScriptEngine.LANGUAGE_VERSION.equals(name)) {
            return getLanguageVersion();
        }
        return null;
    }

    public String getProgram(String[] arg0) {
        return null;
    }

}

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

/**
 * This abstract class can be used for own script engine
 * factory implementations.
 */
public abstract class AbstractScriptEngineFactory
    implements ScriptEngineFactory {

    /** The engine name. */
    private String engineName;

    /** The version of the engine. */
    private String engineVersion;

    /** List of extensions. */
    private List<String> extensions;

    /** List of mime types. */
    private List<String> mimeTypes;

    /** List of names. */
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

    /**
     * @see javax.script.ScriptEngineFactory#getEngineName()
     */
    public String getEngineName() {
        return engineName;
    }

    /**
     * Set the engine name.
     * @param engineName The new engine name
     */
    protected void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    /**
     * @see javax.script.ScriptEngineFactory#getEngineVersion()
     */
    public String getEngineVersion() {
        return engineVersion;
    }

    /**
     * Set the engine version
     * @param engineVersion The version string
     */
    protected void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    /**
     * @see javax.script.ScriptEngineFactory#getExtensions()
     */
    public List<String> getExtensions() {
        return extensions;
    }

    /**
     * Set the extensions
     * @param extensions The array of extensions
     */
    protected void setExtensions(String... extensions) {
        if (extensions == null) {
            this.extensions = Collections.emptyList();
        } else {
            this.extensions = Arrays.asList(extensions);
        }
    }

    /**
     * @see javax.script.ScriptEngineFactory#getMimeTypes()
     */
    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    /**
     * Set the mime types
     * @param mimeTypes The array of mime types
     */
    protected void setMimeTypes(String... mimeTypes) {
        if (mimeTypes == null) {
            this.mimeTypes = Collections.emptyList();
        } else {
            this.mimeTypes = Arrays.asList(mimeTypes);
        }
    }

    /**
     * @see javax.script.ScriptEngineFactory#getNames()
     */
    public List<String> getNames() {
        return names;
    }

    /**
     * Set the names
     * @param names The array of names.
     */
    protected void setNames(String... names) {
        if (names == null) {
            this.names = Collections.emptyList();
        } else {
            this.names = Arrays.asList(names);
        }
    }

    /**
     * @see javax.script.ScriptEngineFactory#getMethodCallSyntax(java.lang.String, java.lang.String, java.lang.String[])
     */
    public String getMethodCallSyntax(String obj, String m, String... args) {
        StringBuilder callSyntax = new StringBuilder();
        callSyntax.append(obj).append('.').append(m).append('(');
        for (int i = 0; args != null && i < args.length; i++) {
            if (i > 0) callSyntax.append(',');
            callSyntax.append(args[i]);
        }
        callSyntax.append(')');
        return callSyntax.toString();
    }

    /**
     * @see javax.script.ScriptEngineFactory#getOutputStatement(java.lang.String)
     */
    public String getOutputStatement(String value) {
        return "out.print(" + value + ")";
    }

    /**
     * @see javax.script.ScriptEngineFactory#getParameter(java.lang.String)
     */
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

    /**
     * @see javax.script.ScriptEngineFactory#getProgram(java.lang.String[])
     */
    public String getProgram(String... arg0) {
        return null;
    }

}

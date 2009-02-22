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
package org.apache.sling.scripting.freemarker;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.script.ScriptEngine;

import org.apache.sling.scripting.api.AbstractScriptEngineFactory;

public class FreemarkerScriptEngineFactory extends AbstractScriptEngineFactory {

    /** The extensions of FreeMarker scripts (value is "ftl"). */
    public final static String FREEMARKER_SCRIPT_EXTENSION = "ftl";

    /** The MIME type of FreeMarker script files (value is "text/x-freemarker"). */
    public final static String FREEMARKER_MIME_TYPE = "text/x-freemarker";

    /**
     * The short name of the FreeMarker script engine factory (value is
     * "freemarker").
     */
    public final static String SHORT_NAME = "freemarker";

    /** The name of the FreeMarker language (value is "FreeMarker"). */
    private static final String FREEMARKER_NAME = "FreeMarker";

    /**
     * The absolute path to the FreeMarker version properties file (value is
     * "/freemarker/version.properties").
     */
    private static final String FREEMARKER_VERSION_PROPERTIES = "/freemarker/version.properties";

    /**
     * The name of the property containing the FreeMarker version (value is
     * "version").
     */
    private static final String PROP_FREEMARKER_VERSION = "version";

    /**
     * The default version of FreeMarker if the version property cannot be read
     * (value is "2.3", which is the latest minor version release as of
     * 17.Dec.2007).
     */
    private static final String DEFAULT_FREEMARKER_VERSION = "2.3";

    /**
     * The FreeMarker language version extracted from the FreeMarker version
     * properties file. If this file cannot be read the language version
     * defaults to ...
     */
    private final String languageVersion;

    public FreemarkerScriptEngineFactory() {
        setExtensions(FREEMARKER_SCRIPT_EXTENSION);
        setMimeTypes(FREEMARKER_MIME_TYPE);
        setNames(SHORT_NAME);

        // extract language version from version.properties file
        String langVersion = null;
        InputStream ins = null;
        try {
            ins = getClass().getResourceAsStream(FREEMARKER_VERSION_PROPERTIES);
            if (ins != null) {
                Properties props = new Properties();
                props.load(ins);
                langVersion = props.getProperty(PROP_FREEMARKER_VERSION);
            }
        } catch (IOException ioe) {
            // don't really care, just use default
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }

        // if we could not extract the actual version, assume version 2.3
        // which is the current minor release as of 17.Dec.2007
        languageVersion = (langVersion == null)
                ? DEFAULT_FREEMARKER_VERSION
                : langVersion;
    }

    public ScriptEngine getScriptEngine() {
        return new FreemarkerScriptEngine(this);
    }

    public String getLanguageName() {
        return FREEMARKER_NAME;
    }

    public String getLanguageVersion() {
        return languageVersion;
    }
}

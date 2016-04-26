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
package org.apache.sling.scripting.freemarker.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = ScriptEngineFactory.class,
    immediate = true,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Scripting engine for FreeMarker templates",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
@Designate(
    ocd = FreemarkerScriptEngineFactoryConfiguration.class
)
public class FreemarkerScriptEngineFactory extends AbstractScriptEngineFactory {

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

    private final Logger logger = LoggerFactory.getLogger(FreemarkerScriptEngineFactory.class);

    public FreemarkerScriptEngineFactory() {

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

    @Activate
    private void activate(final FreemarkerScriptEngineFactoryConfiguration configuration) {
        logger.debug("activate");
        configure(configuration);
    }

    @Modified
    private void modified(final FreemarkerScriptEngineFactoryConfiguration configuration) {
        logger.debug("modified");
        configure(configuration);
    }

    @Deactivate
    private void deactivate() {
        logger.debug("deactivate");
    }

    private void configure(final FreemarkerScriptEngineFactoryConfiguration configuration) {
        setExtensions(configuration.extensions());
        setMimeTypes(configuration.mimeTypes());
        setNames(configuration.names());
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

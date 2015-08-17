/*******************************************************************************
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
 ******************************************************************************/

package org.apache.sling.scripting.sightly.impl.engine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;

/**
 * Holds various Sightly engine global configurations.
 */
@Component(
        metatype = true,
        label = "Apache Sling Scripting Sightly Engine Configuration",
        description = "Sightly Engine Configuration Options"
)
@Service(SightlyEngineConfiguration.class)
@Properties({
        @Property(
                name = SightlyEngineConfiguration.SCR_PROP_NAME_DEVMODE,
                boolValue = SightlyEngineConfiguration.SCR_PROP_DEFAULT_DEVMODE,
                label = "Development Mode",
                description = "If enabled, Sightly components will be recompiled at every request instead of loading objects from memory." +
                    " This will also write the generated Java classes' source code for Sightly templates through the ClassLoaderWriter " +
                    "service."
        ),
        @Property(
                name = SightlyEngineConfiguration.SCR_PROP_NAME_ENCODING,
                value = SightlyEngineConfiguration.SCR_PROP_DEFAULT_ENCODING,
                label = "Template Files Default Encoding",
                description = "The default encoding used for reading Sightly template files (this directly affects how Sightly templates" +
                        "are rendered)."
        )
})
public class SightlyEngineConfiguration {

    public static final String SCR_PROP_NAME_DEVMODE = "org.apache.sling.scripting.sightly.devmode";
    public static final boolean SCR_PROP_DEFAULT_DEVMODE = false;

    public static final String SCR_PROP_NAME_ENCODING = "org.apache.sling.scripting.sightly.encoding";
    public static final String SCR_PROP_DEFAULT_ENCODING = "UTF-8";

    private String engineVersion = "0";
    private boolean devMode = false;
    private String encoding = SCR_PROP_DEFAULT_ENCODING;

    public String getEngineVersion() {
        return engineVersion;
    }

    public boolean isDevMode() {
        return devMode;
    }

    public String getEncoding() {
        return encoding;
    }

    protected void activate(ComponentContext componentContext) {
        InputStream ins = null;
        try {
            ins = getClass().getResourceAsStream("/META-INF/MANIFEST.MF");
            if (ins != null) {
                Manifest manifest = new Manifest(ins);
                Attributes attrs = manifest.getMainAttributes();
                String version = attrs.getValue("ScriptEngine-Version");
                if (version != null) {
                    engineVersion = version;
                }
            }
        } catch (IOException ioe) {
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
        }
        Dictionary properties = componentContext.getProperties();
        devMode = PropertiesUtil.toBoolean(properties.get(SCR_PROP_NAME_DEVMODE), SCR_PROP_DEFAULT_DEVMODE);
        encoding = PropertiesUtil.toString(properties.get(SCR_PROP_NAME_ENCODING), SCR_PROP_DEFAULT_ENCODING);
    }
}

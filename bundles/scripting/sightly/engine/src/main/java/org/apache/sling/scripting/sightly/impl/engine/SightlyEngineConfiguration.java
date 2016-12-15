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
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Holds various HTL engine global configurations.
 */
@Component(
        service = SightlyEngineConfiguration.class,
        configurationPid = "org.apache.sling.scripting.sightly.impl.engine.SightlyEngineConfiguration"
)
public class SightlyEngineConfiguration {

    @interface Configuration {

        @AttributeDefinition(
                name = "Keep Generated Java Source Code",
                description = "If enabled, the Java source code generated during HTL template files compilation will be stored. " +
                        "Its location is dependent on the available org.apache.sling.commons.classloader.ClassLoaderWriter."

        )
        boolean keepGenerated() default true;

    }

    private String engineVersion = "0";
    private boolean keepGenerated;
    private String bundleSymbolicName = "org.apache.sling.scripting.sightly";

    public String getEngineVersion() {
        return engineVersion;
    }

    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    public String getScratchFolder() {
        return "/" + bundleSymbolicName.replaceAll("\\.", "/");
    }

    public boolean keepGenerated() {
        return keepGenerated;
    }

    @Activate
    protected void activate(Configuration configuration) {
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
                String symbolicName = attrs.getValue("Bundle-SymbolicName");
                if (StringUtils.isNotEmpty(symbolicName)) {
                    bundleSymbolicName = symbolicName;
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
        keepGenerated = configuration.keepGenerated();
    }
}

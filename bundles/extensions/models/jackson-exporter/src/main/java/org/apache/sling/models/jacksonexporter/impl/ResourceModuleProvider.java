/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.models.jacksonexporter.impl;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.models.jacksonexporter.ModuleProvider;
import org.osgi.framework.Constants;

import java.util.Map;

@Component(metatype = true, label = "Apache Sling Models Jackson Exporter - Resource object support",
    description = "Provider of a Jackson Module which enables support for proper serialization of Resource objects")
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 0, propertyPrivate = true)
public class ResourceModuleProvider implements ModuleProvider {

    private static final int DEFAULT_MAX_RECURSION_LEVELS = -1;

    @Property(label = "Maximum Recursion Levels",
            description = "Maximum number of levels of child resources which will be exported for each resource. Specify -1 for infinite.",
            intValue = DEFAULT_MAX_RECURSION_LEVELS)
    private static final String PROP_MAX_RECURSION_LEVELS = "max.recursion.levels";

    private SimpleModule moduleInstance;

    @Activate
    private void activate(Map<String, Object> props) {
        final int maxRecursionLevels = PropertiesUtil.toInteger(props.get(PROP_MAX_RECURSION_LEVELS), DEFAULT_MAX_RECURSION_LEVELS);
        this.moduleInstance = new SimpleModule();
        SimpleSerializers serializers = new SimpleSerializers();
        serializers.addSerializer(Resource.class, new ResourceSerializer(maxRecursionLevels));
        moduleInstance.setSerializers(serializers);
    }

    @Override
    public Module getModule() {
        return moduleInstance;
    }

}

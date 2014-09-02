/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.scripting.groovy.json.internal;

import groovy.json.JsonBuilder;

import java.util.HashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

/**
 * BindingsValuesProvider which binds an instance of JsonBuilder.
 */
@SuppressWarnings("serial")
@Component(immediate=true, metatype=false)
@Service
@Properties({
    @Property(name="service.description", value="JSONGroovyBuilder BindingsValuesProvider"),
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="javax.script.name", value="groovy")
})
public class JsonBuilderBindingsValuesProvider extends HashMap<String, Object> {

    public JsonBuilderBindingsValuesProvider() {
        super();
        put("jsonBuilder", new JsonBuilder());
    }

}

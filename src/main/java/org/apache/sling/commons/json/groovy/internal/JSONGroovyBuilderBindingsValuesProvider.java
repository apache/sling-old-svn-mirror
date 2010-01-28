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
package org.apache.sling.commons.json.groovy.internal;

import javax.script.Bindings;

import org.apache.sling.commons.json.groovy.JSONGroovyBuilder;
import org.apache.sling.scripting.api.BindingsValuesProvider;

/**
 * BindingsValuesProvider which binds an instance of JSONGroovyBuilder.
 *
 * @scr.component immediate="true" metatype="no"
 * @scr.service
 *
 * @scr.property name="service.description" value="JSONGroovyBuilder BindingsValuesProvider"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 *
 * @scr.property name="javax.script.name" value="groovy"
 */
public class JSONGroovyBuilderBindingsValuesProvider implements BindingsValuesProvider {

    /**
     * {@inheritDoc}
     */
    public void addBindings(Bindings bindings) {
        bindings.put("jsonBuilder", new JSONGroovyBuilder());
    }

}

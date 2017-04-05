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
package org.apache.sling.crankstart.launcher;

import java.util.Properties;

import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.ModelUtility.VariableResolver;

/** VariableResolver that uses a Properties object */
public class PropertiesVariableResolver implements VariableResolver {
    private final Properties props;
    private final String variableNamePrefix;
    
    public PropertiesVariableResolver(Properties props, String variableNamePrefix) {
        this.props = props;
        this.variableNamePrefix = variableNamePrefix;
    }
    
    @Override
    public String resolve(Feature f, String variableName) {
        final String propertyName = variableNamePrefix + variableName;
        final String value = props.getProperty(propertyName);
        if(value == null) {
            return f.getVariables().get(variableName);
        } else {
            onOverride(variableName, value, propertyName);
            return value;
        }
    }
    
    protected void onOverride(String variableName, String value, String propertyName) {
    }
}
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
package org.apache.sling.maven.slingstart;

import org.apache.maven.project.MavenProject;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.ModelUtility.VariableResolver;

/**
 * Provisioning variable resolver that supports setting or overriding variables via POM properties.
 * Properties in POM have higher precedence than variables defined in the provisioning file.
 */
public class PomVariableResolver implements VariableResolver {
    
    private final MavenProject project;
    
    /**
     * @param project Maven project
     */
    public PomVariableResolver(MavenProject project) {
        this.project = project;
    }

    @Override
    public String resolve(Feature feature, String name) {
        Object pomValue = project.getProperties().get(name);
        if (pomValue != null) {
            return pomValue.toString();
        }
        return feature.getVariables().get(name);
    }
    
}

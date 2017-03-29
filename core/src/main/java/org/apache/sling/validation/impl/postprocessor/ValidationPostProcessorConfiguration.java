/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.impl.postprocessor;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(name = "Apache Sling Validation Post Processor", description = "Allows to influence the validation behaviour when using the Sling POST servlet")
public @interface ValidationPostProcessorConfiguration {
    @AttributeDefinition(name = "Disabled", description = "If set to true, no POST toward the Sling POST servlet will ever be validated through Sling Validation.")
    boolean disabled() default false;
    @AttributeDefinition(name = "Enabled for path prefixes", description = "If not globally disabled, a POST towards a path which starts with any of the given prefixes is automatically validated. No wildcards (*,?) supported.")
    String[] enabledForPathPrefix();
    @AttributeDefinition(name = "Consider resource super types", description = "If set to false the Post Processor will not use the validation models of any of the resource super types.")
    boolean considerResourceSuperTypes() default true;
    @AttributeDefinition(name = "Fail for missing validation models", description = "In case validation should be performed but no validation model could be found this is either silently ignored (false) or leads to an error (true)")
    boolean failForMissingValidationModels() default false;
}

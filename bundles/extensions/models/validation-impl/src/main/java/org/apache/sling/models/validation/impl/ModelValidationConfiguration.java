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
package org.apache.sling.models.validation.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Apache Sling Models Validation Configuration (for Sling Validation)", description = "Allows to configure how Sling Models are validated with the help of Sling Validation")
public @interface ModelValidationConfiguration {
    @AttributeDefinition(name = "Disabled")
    boolean disabled() default false;
    @AttributeDefinition(name = "Severity Threshold", description = "This threshold specifies the minimum severity of the underlying Sling validation failures for making a Sling Model invalid. If all validation failures are below that threshold the model is considered valid.")
    int severityThreshold() default 0;
}

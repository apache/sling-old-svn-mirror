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
package org.apache.sling.scripting.thymeleaf.internal;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "Apache Sling Scripting Thymeleaf “Sling Resource TemplateResolver”",
    description = "Resolves template resources by using Sling's resource resolver from script bindings."
)
@interface SlingResourceTemplateResolverConfiguration {

    @AttributeDefinition(
        name = "order",
        description = "Property for ordering template resolvers inside the Thymeleaf template engine."
    )
    int order() default 0;

    @AttributeDefinition(
        name = "use decoupled logic",
        description = "Setting for 'use decoupled logic' in template resolution."
    )
    boolean useDecoupledLogic() default true;

}

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
    name = "Apache Sling Scripting Thymeleaf “ScriptEngine Factory”",
    description = "scripting engine for Thymeleaf templates"
)
@interface ThymeleafScriptEngineFactoryConfiguration {

    @AttributeDefinition(
        name = "service ranking",
        description = "service property for identifying the service's ranking number"
    )
    int service_ranking() default 0;

    @AttributeDefinition(
        name = "extensions",
        description = "extensions"
    )
    String[] extensions() default {
        "html"
    };

    @AttributeDefinition(
        name = "mime types",
        description = "mime types"
    )
    String[] mimeTypes() default {
        "text/html"
    };

    @AttributeDefinition(
        name = "names",
        description = "names"
    )
    String[] names() default {
        "thymeleaf"
    };

    @AttributeDefinition(
        name = "use standard message resolver",
        description = "" // TODO
    )
    boolean useStandardMessageResolver() default true;

    @AttributeDefinition(
        name = "use standard link builder",
        description = "" // TODO
    )
    boolean useStandardLinkBuilder() default true;

    @AttributeDefinition(
        name = "use standard dialect",
        description = "" // TODO
    )
    boolean useStandardDialect() default true;

    @AttributeDefinition(
        name = "use standard decoupled template logic resolver",
        description = "" // TODO
    )
    boolean useStandardDecoupledTemplateLogicResolver() default true;

    @AttributeDefinition(
        name = "use standard cache manager",
        description = "" // TODO
    )
    boolean useStandardCacheManager() default true;

    @AttributeDefinition(
        name = "use standard engine context factory",
        description = "" // TODO
    )
    boolean useStandardEngineContextFactory() default true;

}

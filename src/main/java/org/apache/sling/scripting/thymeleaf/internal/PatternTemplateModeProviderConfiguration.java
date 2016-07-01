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
    name = "Apache Sling Scripting Thymeleaf “Pattern TemplateModeProvider”",
    description = "Provides template modes by matching template paths against configured patterns."
)
@interface PatternTemplateModeProviderConfiguration {

    @AttributeDefinition(
        name = "patterns for template mode HTML",
        description = "The template patterns (regular expressions) for templates which should be processed with template mode HTML (e.g. *.html - NOTE: extension needs to be enabled for this script engine)."
    )
    String[] htmlPatterns() default {
        "*.html"
    };

    @AttributeDefinition(
        name = "patterns for template mode XML",
        description = "The template patterns (regular expressions) for templates which should be processed with template mode XML (e.g. *.xml - NOTE: extension needs to be enabled for this script engine)."
    )
    String[] xmlPatterns() default {
        "*.xml"
    };

    @AttributeDefinition(
        name = "patterns for template mode TEXT",
        description = "The template patterns (regular expressions) for templates which should be processed with template mode TEXT (e.g. *.txt - NOTE: extension needs to be enabled for this script engine)."
    )
    String[] textPatterns() default {
        "*.txt"
    };

    @AttributeDefinition(
        name = "patterns for template mode JAVASCRIPT",
        description = "The template patterns (regular expressions) for templates which should be processed with template mode JAVASCRIPT (e.g. *.js - NOTE: extension needs to be enabled for this script engine)."
    )
    String[] javascriptPatterns() default {
        "*.js"
    };

    @AttributeDefinition(
        name = "patterns for template mode CSS",
        description = "The template patterns (regular expressions) for templates which should be processed with template mode CSS (e.g. *.css - NOTE: extension needs to be enabled for this script engine)."
    )
    String[] cssPatterns() default {
        "*.css"
    };

    @AttributeDefinition(
        name = "patterns for template mode RAW",
        description = "The template patterns (regular expressions) for templates which should be processed with template mode RAW (e.g. *.raw - NOTE: extension needs to be enabled for this script engine)."
    )
    String[] rawPatterns();

}

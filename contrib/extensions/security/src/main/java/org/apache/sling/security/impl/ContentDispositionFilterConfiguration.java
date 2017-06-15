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
package org.apache.sling.security.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name="Apache Sling Content Disposition Filter", description="Request filter adding Content Disposition header with value 'attachment' for certain paths/content types. Independent of the configuration only resource paths are covered which contain a property named 'jcr:data' or 'jcr:content\\jcr:data'.")
public @interface ContentDispositionFilterConfiguration {

    @AttributeDefinition(name="Included Resource Paths & Content Types", description="These resource paths are covered by the filter. "+
            "Each entry is of the form '<path> [ : <excluded content type> {,<excluded content type>} ]'. " +
            "Invalid entries are logged and ignored. <path> must be an absolute path and may contain a wildcard ('*') at the end, to match every resource path with the given path prefix.")
    String[] sling_content_disposition_paths() default {};

    @AttributeDefinition(name="Excluded Resource Paths", description="These resource paths are excluded from the filter. "+
                    "Each resource path must be given as absolute and fully qualified path. Prefix matching/wildcards are not supported.")
    String[] sling_content_disposition_excluded_paths() default {};

    @AttributeDefinition(name="Enable For All Resource Paths", description="This flag controls whether to enable" +
            " this filter for all paths, except for the excluded paths defined by sling.content.disposition.excluded.paths. Setting this to 'true' leads to ignoring 'sling.content.disposition.path'.")
    boolean sling_content_disposition_all_paths() default false;

}

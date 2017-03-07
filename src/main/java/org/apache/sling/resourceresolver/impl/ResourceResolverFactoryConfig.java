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
package org.apache.sling.resourceresolver.impl;

import org.apache.sling.resourceresolver.impl.mapping.MapEntries;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Apache Sling Resource Resolver Factory",
            description = "Configures the Resource Resolver for request URL and resource path rewriting.")
public @interface ResourceResolverFactoryConfig {

	String LEGACY_REQUIRED_PROVIDER_PID = "org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderFactory";
	String REQUIRED_PROVIDER_NAME = "JCR";
	
    @AttributeDefinition(name = "Resource Search Path",
        description = "The list of absolute path prefixes " +
                      "applied to find resources whose path is just specified with a relative path. " +
                      "The default value is [ \"/apps\", \"/libs\" ]. If an empty path is specified a " +
                      "single entry path of [ \"/\" ] is assumed.")
    String[] resource_resolver_searchpath() default {"/apps", "/libs"};

    /**
     * Defines whether namespace prefixes of resource names inside the path
     * (e.g. <code>jcr:</code> in <code>/home/path/jcr:content</code>) are
     * mangled or not.
     * <p>
     * Mangling means that any namespace prefix contained in the path is replaced as per the generic
     * substitution pattern <code>/([^:]+):/_$1_/</code> when calling the <code>map</code> method of
     * the resource resolver. Likewise the <code>resolve</code> methods will unmangle such namespace
     * prefixes according to the substitution pattern <code>/_([^_]+)_/$1:/</code>.
     * <p>
     * This feature is provided since there may be systems out there in the wild which cannot cope
     * with URLs containing colons, even though they are perfectly valid characters in the path part
     * of URI references with a scheme.
     * <p>
     * The default value of this property if no configuration is provided is <code>true</code>.
     *
     */
    @AttributeDefinition(name = "Namespace Mangling",
          description = "Defines whether namespace " +
                        "prefixes of resource names inside the path (e.g. \"jcr:\" in \"/home/path/jcr:content\") " +
                        "are mangled or not. Mangling means that any namespace prefix contained in the " +
                        "path is replaced as per the generic substitution pattern \"/([^:]+):/_$1_/\" " +
                        "when calling the \"map\" method of the resource resolver. Likewise the " +
                        "\"resolve\" methods will unmangle such namespace prefixes according to the " +
                        "substituation pattern \"/_([^_]+)_/$1:/\". This feature is provided since " +
                        "there may be systems out there in the wild which cannot cope with URLs " +
                        "containing colons, even though they are perfectly valid characters in the " +
                        "path part of URI references with a scheme. The default value of this property " +
                        "if no configuration is provided is \"true\".")
    boolean resource_resolver_manglenamespaces() default true;

    @AttributeDefinition(name = "Allow Direct Mapping",
        description = "Whether to add a direct URL mapping to the front of the mapping list.")
    boolean resource_resolver_allowDirect() default true;

    @AttributeDefinition(name = "Required Providers (Deprecated)",
        description = "A resource resolver factory is only " +
                 "available (registered) if all resource providers mentioned in this configuration " +
                 "are available. Each entry is either a service PID or a filter expression.  " +
                 "Invalid filters are ignored.")
    String[] resource_resolver_required_providers();

    @AttributeDefinition(name = "Required Providers ",
        description = "A resource resolver factory is only " +
                       "available (registered) if all resource providers mentioned in this configuration " +
                       "are available. Each entry is refers to the name of a registered provider.")
    String[] resource_resolver_required_providernames() default {REQUIRED_PROVIDER_NAME};

    /**
     * The resolver.virtual property has no default configuration. But the Sling
     * maven plugin and the sling management console cannot handle empty
     * multivalue properties at the moment. So we just add a dummy direct
     * mapping.
     */
    @AttributeDefinition(name = "Virtual URLs",
        description = "List of virtual URLs and there mappings to real URLs. " +
                    "Format is <externalURL>:<internalURL>. Mappings are " +
                    "applied on the complete request URL only.")
    String[] resource_resolver_virtual() default {"/:/"};

    @AttributeDefinition(name = "URL Mappings",
        description = "List of mappings to apply to paths. Incoming mappings are " +
                    "applied to request paths to map to resource paths, " +
                    "outgoing mappings are applied to map resource paths to paths used on subsequent " +
                    "requests. Form is <internalPathPrefix><op><externalPathPrefix> where <op> is " +
                    "\">\" for incoming mappings, \"<\" for outgoing mappings and \":\" for mappings " +
                    "applied in both directions. Mappings are applied in configuration order by " +
                    "comparing and replacing URL prefixes. Note: The use of \"-\" as the <op> value " +
                    "indicating a mapping in both directions is deprecated.")
    String[] resource_resolver_mapping() default {"/:/", "/content/:/", "/system/docroot/:/"};

    @AttributeDefinition(name = "Mapping Location",
        description = "The path to the root of the configuration to setup and configure " +
                      "the ResourceResolver mapping. The default value is /etc/map.")
    String resource_resolver_map_location() default MapEntries.DEFAULT_MAP_ROOT;

    @AttributeDefinition(name = "Mapping Observation",
        description = "The paths where vanity paths or aliases can be found. These paths are used to " +
                      "listen for resource events.")
    String[] resource_resolver_map_observation() default "/";

    @AttributeDefinition(name = "Default Vanity Path Redirect Status",
        description = "The default status code used when a sling:vanityPath is configured to redirect " +
                    "and does not have a specific status code associated with it " +
                    "(via a sling:redirectStatus property)")
    int resource_resolver_default_vanity_redirect_status() default MapEntries.DEFAULT_DEFAULT_VANITY_PATH_REDIRECT_STATUS;

    @AttributeDefinition(name = "Enable Vanity Paths",
        description = "This flag controls whether all resources with a sling:vanityPath property " +
                            "are processed and added to the mappoing table.")
    boolean resource_resolver_enable_vanitypath() default true;

    @AttributeDefinition(name = "Maximum number of cached vanity path entries",
        description = "The maximum number of cached vanity path entries. " +
                    "Default is -1 (no limit)")
    int resource_resolver_vanitypath_maxEntries() default -1;

    @AttributeDefinition(name = "Limit the maximum number of cached vanity path entries only at startup",
        description = "Limit the maximum number of cached vanity path entries only at startup. " +
                      "Default is true")
    boolean resource_resolver_vanitypath_maxEntries_startup() default true;

    @AttributeDefinition(name = "Maximum number of vanity bloom filter bytes",
        description = "The maximum number of vanity bloom filter bytes. " +
                      "Changing this value is subject to vanity bloom filter rebuild")
    int resource_resolver_vanitypath_bloomfilter_maxBytes() default 1024000;

    @AttributeDefinition(name = "Optimize alias resolution",
        description ="This flag controls whether to optimize" +
                     " the alias resolution by creating an internal cache of aliases. This might have an impact on the startup time"+
                     " and on the alias update time if the number of aliases is huge (over 10000).")
    boolean resource_resolver_optimize_alias_resolution() default true;

    @AttributeDefinition(name = "Allowed Vanity Path Location",
        description ="This setting can contain a list of path prefixes, e.g. /libs/, /content/. If " +
                    "such a list is configured, only vanity paths from resources starting with this prefix " +
                    " are considered. If the list is empty, all vanity paths are used.")
    String[] resource_resolver_vanitypath_whitelist();

    @AttributeDefinition(name = "Denied Vanity Path Location",
        description ="This setting can contain a list of path prefixes, e.g. /misc/. If " +
                    "such a list is configured,vanity paths from resources starting with this prefix " +
                    " are not considered. If the list is empty, all vanity paths are used.")
    String[] resource_resolver_vanitypath_blacklist();

    @AttributeDefinition(name = "Vanity Path Precedence",
        description ="This flag controls whether vanity paths" +
                     " will have precedence over existing /etc/map mapping")
    boolean resource_resolver_vanity_precedence() default false;

    @AttributeDefinition(name = "Paranoid Provider Handling",
        description = "If this flag is enabled, an unregistration of a resource provider (not factory), "
                      + "is causing the resource resolver factory to restart, potentially cleaning up "
                      + "for memory leaks caused by objects hold from that resource provider.")
    boolean resource_resolver_providerhandling_paranoid() default false;

    @AttributeDefinition(name = "Log resource resolver closing",
        description = "When enabled CRUD operations with a closed resource resolver will log a stack trace " +
                      "with the point where the used resolver was closed. It's advisable to not enable this feature on " +
                      "production systems.")
    boolean resource_resolver_log_closing() default false;

    @AttributeDefinition(name = "Log unclosed resource resolvers",
            description = "When enabled unclosed resource resolvers will be logged. Not closing " +
                          "a resource resolver is a bug in the code using the resolver and should be fixed.")
    boolean resource_resolver_log_unclosed() default true;
}


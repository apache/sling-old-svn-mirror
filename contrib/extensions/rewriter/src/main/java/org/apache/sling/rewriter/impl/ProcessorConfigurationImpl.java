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
package org.apache.sling.rewriter.impl;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.rewriter.PipelineConfiguration;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;

/**
 * Configuration of a processor.
 * This configuration consists either of a pipeline (generator, transformer
 * and serializer) or a processor. A processor is configured with mime types
 * indicating when to apply this processor.
 */
public class ProcessorConfigurationImpl implements PipelineConfiguration {

    static final String PROPERTY_ORDER = "order";

    static final String PROPERTY_PATHS = "paths";

    static final String PROPERTY_EXTENSIONS = "extensions";

    static final String PROPERTY_PROCESSOR_TYPE = "processorType";

    static final String PROPERTY_CONTENT_TYPES = "contentTypes";

    static final String PROPERTY_RESOURCE_TYPES = "resourceTypes";

    static final String PROPERTY_UNWRAP_RESOURCES = "unwrapResources";

    static final String PROPERTY_SELECTORS = "selectors";

    static final String PROPERTY_TRANFORMERS = "transformerTypes";

    static final String PROPERTY_GENERATOR = "generatorType";

    static final String PROPERTY_SERIALIZER = "serializerType";

    static final String PROPERTY_ACTIVE = "enabled";

    static final String PROPERTY_PROCESS_ERROR = "processError";


    /** For which content types should this processor be applied. */
    private final String[] contentTypes;

    /** For which paths should this processor be applied. */
    private final String[] paths;

    /** For which extensions should this processor be applied. */
    private final String[] extensions;

    /** For which resource types should this processor be applied. */
    private final String[] resourceTypes;

    /** Whether unwrapped resources should be validated as well when checking for resource types. */
    private final boolean unwrapResources;

    /** For which selectors should this processor be applied. */
    private final String[] selectors;

    /** The order of this processor */
    private final int order;

    /** The generator for the pipeline. */
    private final ProcessingComponentConfiguration generatorConfiguration;

    /** The transformers for the pipeline. */
    private final ProcessingComponentConfiguration[] transformerConfigurations;

    /** The serializer for the pipeline. */
    private final ProcessingComponentConfiguration serializerConfiguration;

    /** The processor configuration. */
    private final ProcessingComponentConfiguration processorConfig;

    /** Is this configuration active? */
    private final boolean isActive;

    /** Is this configuration valid? */
    private final boolean isValid;

    /** Is this a pipeline configuration? */
    private final boolean isPipeline;

    private final boolean processErrorResponse;

    private final String descString;

    /**
     * This is the constructor for a pipeline
     */
    public ProcessorConfigurationImpl(String[] contentTypes,
                                      String[] paths,
                                      String[] extensions,
                                      String[] resourceTypes,
                                      boolean unwrapResources,
                                      String[] selectors,
                                      int      order,
                                      ProcessingComponentConfiguration generatorConfig,
                                      ProcessingComponentConfiguration[] transformerConfigs,
                                      ProcessingComponentConfiguration serializerConfig,
                                      boolean processErrorResponse) {
        this.contentTypes = contentTypes;
        this.resourceTypes = resourceTypes;
        this.unwrapResources = unwrapResources;
        this.selectors = selectors;
        this.paths = paths;
        this.extensions = extensions;
        this.order = order;
        this.generatorConfiguration = generatorConfig;
        this.transformerConfigurations = transformerConfigs;
        this.serializerConfiguration = serializerConfig;
        this.processorConfig = null;
        this.isActive = true;
        this.isValid = true;
        this.isPipeline = true;
        this.processErrorResponse = processErrorResponse;
        this.descString = this.buildDescString();
    }

    /**
     * This is the constructor for a pipeline
     */
    public ProcessorConfigurationImpl(String[] contentTypes,
                                      String[] paths,
                                      String[] extensions,
                                      String[] resourceTypes,
                                      String[] selectors) {
        this(contentTypes, paths, extensions, resourceTypes, false, selectors, 0, null, null, null, false);
    }

    /**
     * Constructor.
     * This constructor reads the configuration from the specified resource.
     */
    public ProcessorConfigurationImpl(final Resource resource) {
        final ValueMap properties = ResourceUtil.getValueMap(resource);
        this.contentTypes = properties.get(PROPERTY_CONTENT_TYPES, String[].class);
        this.resourceTypes = properties.get(PROPERTY_RESOURCE_TYPES, String[].class);
        this.unwrapResources = properties.get(PROPERTY_UNWRAP_RESOURCES, false);
        this.selectors = properties.get(PROPERTY_SELECTORS, String[].class);
        this.paths = properties.get(PROPERTY_PATHS, String[].class);
        this.extensions = properties.get(PROPERTY_EXTENSIONS, String[].class);

        this.processorConfig = this.getComponentConfig(resource, PROPERTY_PROCESSOR_TYPE, "processor");
        this.generatorConfiguration = this.getComponentConfig(resource, PROPERTY_GENERATOR, "generator");
        this.transformerConfigurations = this.getComponentConfigs(resource, PROPERTY_TRANFORMERS, "transformer");
        this.serializerConfiguration = this.getComponentConfig(resource, PROPERTY_SERIALIZER, "serializer");

        this.order = properties.get(PROPERTY_ORDER, 0);
        this.isActive = properties.get(PROPERTY_ACTIVE, true);
        this.processErrorResponse = properties.get(PROPERTY_PROCESS_ERROR, true);
        this.isPipeline = this.processorConfig == null;

        // let's do a sanity check!
        if ( this.isPipeline ) {
            if ( this.generatorConfiguration == null
                 || this.generatorConfiguration.getType() == null
                 || this.generatorConfiguration.getType().length() == 0 ) {
                this.isValid = false;
            } else if ( this.serializerConfiguration == null
                        || this.generatorConfiguration.getType() == null
                        || this.generatorConfiguration.getType().length() == 0 ) {
                this.isValid = false;
            } else {
                this.isValid = true;
            }
        } else {
            this.isValid = (this.processorConfig != null);
        }
        this.descString = this.buildDescString();
    }

    void printConfiguration(final PrintWriter pw) {
        if ( this.contentTypes != null ) {
            pw.print("Content Types : ");
            pw.println(Arrays.toString(this.contentTypes));
        }
        if ( this.resourceTypes != null ) {
            pw.print("Resource Types : ");
            pw.println(Arrays.toString(this.resourceTypes));
        }
        if ( this.selectors != null ) {
            pw.print("Selectors : ");
            pw.println(Arrays.toString(this.selectors));
        }
        if ( this.paths != null ) {
            pw.print("Paths : ");
            pw.println(Arrays.toString(this.paths));
        }
        if ( this.extensions != null ) {
            pw.print("Extensions : ");
            pw.println(Arrays.toString(this.extensions));
        }
        pw.print("Order : ");
        pw.println(this.order);
        pw.print("Active : ");
        pw.println(this.isActive);
        pw.print("Valid : ");
        pw.println(this.isValid);
        pw.print("Process Error Response : ");
        pw.println(this.processErrorResponse);
        if ( this.isPipeline ) {
            pw.println("Pipeline : ");
            pw.println("    Generator : ");
            pw.print("        ");
            printConfiguration(pw, this.generatorConfiguration);
            pw.println("    Transformers : ");
            for(int i=0; i<this.transformerConfigurations.length; i++) {
                pw.print("        ");
                printConfiguration(pw, this.transformerConfigurations[i]);
            }
            pw.println("    Serializer : ");
            pw.print("        ");
            printConfiguration(pw, this.serializerConfiguration);
        } else {
            pw.print("Configuration : ");
            printConfiguration(pw, this.processorConfig);
        }

    }

    private void printConfiguration(final PrintWriter pw, final ProcessingComponentConfiguration config) {
        if ( config instanceof ProcessingComponentConfigurationImpl ) {
            ((ProcessingComponentConfigurationImpl)config).printConfiguration(pw);
        } else {
            pw.println(config);
        }
    }

    private String buildDescString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ProcessorConfiguration: {");
        if ( this.contentTypes != null ) {
            sb.append("contentTypes=");
            sb.append(Arrays.toString(this.contentTypes));
            sb.append(", ");
        }
        if ( this.resourceTypes != null ) {
            sb.append("resourceTypes=");
            sb.append(Arrays.toString(this.resourceTypes));
            sb.append(", ");
        }
        if ( this.selectors != null ) {
            sb.append("selectors=");
            sb.append(Arrays.toString(this.selectors));
            sb.append(", ");
        }
        if ( this.paths != null ) {
            sb.append("paths=");
            sb.append(Arrays.toString(this.paths));
            sb.append(", ");
        }
        if ( this.extensions != null ) {
            sb.append("extensions=");
            sb.append(Arrays.toString(this.extensions));
            sb.append(", ");
        }
        sb.append("order=");
        sb.append(this.order);
        sb.append(", active=");
        sb.append(this.isActive);
        sb.append(", valid=");
        sb.append(this.isValid);
        sb.append(", processErrorResponse=");
        sb.append(this.processErrorResponse);
        if ( this.isPipeline ) {
            sb.append(", pipeline=(generator=");
            sb.append(this.generatorConfiguration);
            sb.append(", transformers=(");
            if ( this.transformerConfigurations != null ) {
                for(int i=0; i<this.transformerConfigurations.length; i++) {
                    if ( i > 0 ) {
                        sb.append(", ");
                    }
                    sb.append(this.transformerConfigurations[i]);
                }
            }
            sb.append(", serializer=");
            sb.append(this.serializerConfiguration);
            sb.append(')');
        } else {
            sb.append(", config=");
            sb.append(this.processorConfig);
        }
        sb.append("}");
        return sb.toString();
    }

    protected ProcessingComponentConfiguration getComponentConfig(final Resource configResource,
                                                                  final String propertyName,
                                                                  final String prefix) {
        ProcessingComponentConfiguration[] configs = this.getComponentConfigs(configResource, propertyName, prefix);
        if ( configs != null && configs.length > 0 ) {
            return configs[0];
        }
        return null;
    }

    protected ProcessingComponentConfiguration[] getComponentConfigs(final Resource configResource,
                                                                     final String propertyName,
                                                                     final String prefix) {
        final ValueMap properties = ResourceUtil.getValueMap(configResource);
        final String[] types = properties.get(propertyName, String[].class);
        if ( types != null && types.length > 0 ) {
            final ProcessingComponentConfiguration[] configs = new ProcessingComponentConfiguration[types.length];
            for(int i=0; i<types.length; i++) {
                // there are two possible ways for a component configuration:
                // 1. {prefix}-{type}, like generator-html
                // 2. {prefix}-{index}, like generator-1 (with the index starting at 1)
                // while usually the first way is sufficient, the second one is required if the
                // same transformer is used more than once in a pipeline.
                final String resourceName = prefix + '-' + types[i];
                Resource childResource = configResource.getResourceResolver().getResource(configResource, resourceName);
                if ( childResource == null ) {
                    final String secondResourceName = prefix + '-' + (i+1);
                    childResource = configResource.getResourceResolver().getResource(configResource, secondResourceName);
                }
                final ValueMap config;
                if ( childResource != null ) {
                    final ValueMap childProps = ResourceUtil.getValueMap(childResource);
                    config = childProps;
                } else {
                    config = null;
                }
                configs[i] = new ProcessingComponentConfigurationImpl(types[i], config);
            }
            return configs;
        }
        return null;
    }

    /**
     * Return the order of this configuration for sorting.
     */
    public int getOrder() {
        return this.order;
    }

    /**
     * @see org.apache.sling.rewriter.ProcessorConfiguration#match(org.apache.sling.rewriter.ProcessingContext)
     */
    public boolean match(final ProcessingContext processContext) {
        if ( !this.processErrorResponse && processContext.getRequest().getAttribute("javax.servlet.error.status_code") != null ) {
            return false;
        }
        String contentType = processContext.getContentType();
        // if no content type is supplied, we assume html
        if ( contentType == null ) {
            contentType = ProcessorManagerImpl.MIME_TYPE_HTML;
        } else {
            final int idx = contentType.indexOf(';');
            if (idx != -1) {
                contentType = contentType.substring(0, idx);
            }
        }

        // check content type first
        // if no content type is configured we apply to all
        if ( this.contentTypes != null && this.contentTypes.length > 0 ) {
            int index = 0;
            boolean found = false;
            while ( !found && index < this.contentTypes.length ) {
                if ( this.contentTypes[index].equals("*") ) {
                    found = true;
                } else if ( this.contentTypes[index].equals(contentType) ) {
                    found = true;
                }
                index++;
            }
            if ( !found ) {
                return false;
            }
        }
        // now check extenstions
        // if no extenstion is configured, we apply to all extenstions
        if ( this.extensions != null && this.extensions.length > 0 ) {
             boolean found = false;
             int index = 0;
             while ( !found && index < this.extensions.length ) {
                 if ( this.extensions[index].equals(processContext.getRequest().getRequestPathInfo().getExtension()) ) {
                     found = true;
                 }
                 index++;
             }
             if ( !found ) {
                 return false;
             }
        }
        // check resource types
        if ( this.resourceTypes != null && this.resourceTypes.length > 0 ) {
            final ResourceResolver resourceResolver = processContext.getRequest().getResourceResolver();
            final Resource resource = processContext.getRequest().getResource();
            boolean found = false;
            int index = 0;
            while ( !found && index < this.resourceTypes.length ) {
                if ( resourceResolver.isResourceType(resource, resourceTypes[index]) ) {
                    found = true;
                }
                else if ( unwrapResources && resource instanceof ResourceWrapper ) {
                    // accept resource as well if type was overridden and unwrapped resource has a matching type
                    final Resource unwrappedResource = unwrap(resource);
                    if ( resourceResolver.isResourceType(unwrappedResource, resourceTypes[index]) ) {
                        found = true;
                    }
                }
                index++;
            }
            if ( !found ) {
                return false;
            }
        }

        // now check for path
        // if no path is configured, we apply to all paths
        if ( this.paths != null && this.paths.length > 0 ) {
            final String path = processContext.getRequest().getRequestPathInfo().getResourcePath();
            int index = 0;
            boolean found = false;
            while ( !found && index < this.paths.length ) {
                if ( this.paths[index].equals("*") ) {
                    found = true;
                } else if ( path.startsWith(this.paths[index]) ) {
                    found = true;
                }
                index++;
            }
            if ( !found ) {
                return false;
            }
        }

        // now check for selectors
        if( this.selectors != null && this.selectors.length > 0 ) {
            final String selectorString = processContext.getRequest().getRequestPathInfo().getSelectorString();
            if ( selectorString == null || "".equals(selectorString )) {
                // selectors required but not set
                return false;
            }

            final Set<String> selectors = new HashSet<String>(Arrays.asList(selectorString.split("\\.")));
            int index = 0;
            boolean found = false;
            while ( !found && index < this.selectors.length ) {
                final String selector = this.selectors[index];
                if( selectors.contains(selector) ) {
                    found = true;
                }
                index++;
            }

            if( !found ) {
                return false;
            }
        }

        return true;
    }

    /**
     * Unwrap the resource and return the wrapped implementation.
     * Copied from ResourceUtil.unwrap which is available in Sling API 2.7.0 and up.
     * @param rsrc The resource to unwrap
     * @return The unwrapped resource
     */
    private static Resource unwrap(final Resource rsrc) {
        Resource result = rsrc;
        while (result instanceof ResourceWrapper) {
            result = ((ResourceWrapper)result).getResource();
        }
        return result;
    }
    
    /**
     * The configuration for the generator.
     */
    public ProcessingComponentConfiguration getGeneratorConfiguration() {
        return this.generatorConfiguration;
    }

    /**
     * The configuration for the serializer.
     */
    public ProcessingComponentConfiguration getSerializerConfiguration() {
        return this.serializerConfiguration;
    }

    /**
     * The configuration for the transformers.
     */
    public ProcessingComponentConfiguration[] getTransformerConfigurations() {
        return this.transformerConfigurations;
    }

    /**
     * Is this a pipeline?
     */
    public boolean isPipeline() {
        return this.isPipeline;
    }

    /**
     * Is this component active?
     */
    public boolean isActive() {
        return this.isValid & this.isActive;
    }


    /**
     * @see org.apache.sling.rewriter.ProcessorConfiguration#getConfiguration()
     */
    public Map<String, Object> getConfiguration() {
        if ( this.isPipeline ) {
            return ProcessingComponentConfigurationImpl.EMPTY_CONFIG;
        }
        return this.processorConfig.getConfiguration();
    }

    /**
     * @see org.apache.sling.rewriter.ProcessorConfiguration#getType()
     */
    public String getType() {
        if ( this.isPipeline ) {
            return "{pipeline}";
        }
        return this.processorConfig.getType();
    }

    @Override
    public String toString() {
        return this.descString;
    }
}

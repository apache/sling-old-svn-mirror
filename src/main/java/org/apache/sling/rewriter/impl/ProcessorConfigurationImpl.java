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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

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

    private static final String PROPERTY_ORDER = "order";

    private static final String PROPERTY_PATHS = "paths";

    private static final String PROPERTY_EXTENSIONS = "extensions";

    private static final String PROPERTY_PROCESSOR_TYPE = "processorType";

    private static final String PROPERTY_CONTENT_TYPE = "contentTypes";

    private static final String PROPERTY_TRANFORMER = "transformerTypes";

    private static final String PROPERTY_GENERATOR = "generatorType";

    private static final String PROPERTY_SERIALIZER = "serializerType";

    private static final String PROPERTY_ACTIVE = "enabled";

    private static final String PROPERTY_PROCESS_ERROR = "processError";


    private static final String MIME_TYPE_HTML = "text/html";

    /** For which content types should this processor be applied. */
    private final String[] contentTypes;

    /** For which paths should this processor be applied. */
    private final String[] paths;

    /** For which extensions should this processor be applied. */
    private final String[] extensions;

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

    /**
     * Constructor.
     * This constructor reads the configuration from the specified node.
     */
    public ProcessorConfigurationImpl(final Node configNode)
    throws RepositoryException {
        this.contentTypes = this.getValues(configNode, ProcessorConfigurationImpl.PROPERTY_CONTENT_TYPE);
        this.paths = this.getValues(configNode, ProcessorConfigurationImpl.PROPERTY_PATHS);
        this.extensions = this.getValues(configNode, ProcessorConfigurationImpl.PROPERTY_EXTENSIONS);

        this.processorConfig = this.getComponentConfig(configNode, ProcessorConfigurationImpl.PROPERTY_PROCESSOR_TYPE, "processor");
        this.generatorConfiguration = this.getComponentConfig(configNode, ProcessorConfigurationImpl.PROPERTY_GENERATOR, "generator");
        this.transformerConfigurations = this.getComponentConfigs(configNode, ProcessorConfigurationImpl.PROPERTY_TRANFORMER, "transformer");
        this.serializerConfiguration = this.getComponentConfig(configNode, ProcessorConfigurationImpl.PROPERTY_SERIALIZER, "serializer");

        if ( configNode.hasProperty(ProcessorConfigurationImpl.PROPERTY_ORDER) ) {
            this.order = (int) configNode.getProperty(ProcessorConfigurationImpl.PROPERTY_ORDER).getLong();
        } else {
            this.order = 0; // default
        }
        if ( configNode.hasProperty(ProcessorConfigurationImpl.PROPERTY_ACTIVE) ) {
            this.isActive = configNode.getProperty(ProcessorConfigurationImpl.PROPERTY_ACTIVE).getBoolean();
        } else {
            this.isActive = true; // default
        }
        if ( configNode.hasProperty(ProcessorConfigurationImpl.PROPERTY_PROCESS_ERROR) ) {
            this.processErrorResponse = configNode.getProperty(ProcessorConfigurationImpl.PROPERTY_PROCESS_ERROR).getBoolean();
        } else {
            this.processErrorResponse = true; // default
        }
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
    }

    protected ProcessingComponentConfiguration getComponentConfig(final Node configNode, final String propertyName, final String prefix)
    throws RepositoryException {
        ProcessingComponentConfiguration[] configs = this.getComponentConfigs(configNode, propertyName, prefix);
        if ( configs != null && configs.length > 0 ) {
            return configs[0];
        }
        return null;
    }

    protected ProcessingComponentConfiguration[] getComponentConfigs(final Node configNode, final String propertyName, final String prefix)
    throws RepositoryException {
        final String[] types = this.getValues(configNode, propertyName);
        if ( types != null && types.length > 0 ) {
            final ProcessingComponentConfiguration[] configs = new ProcessingComponentConfiguration[types.length];
            for(int i=0; i<types.length; i++) {
                final String nodeName = prefix + '-' + types[i];
                final Map<String, Object> config;
                if ( configNode.hasNode(nodeName) ) {
                    final Node componentNode = configNode.getNode(nodeName);
                    if ( componentNode.hasProperties() ) {
                        config = new HashMap<String, Object>();
                        final PropertyIterator iter = componentNode.getProperties();
                        while ( iter.hasNext() ) {
                            final Property property = iter.nextProperty();
                            if ( property.getDefinition().isMultiple() ) {
                                final Value[] values = property.getValues();
                                if ( values != null && values.length > 0 ) {
                                    final Object[] v = new Object[values.length];
                                    for(int m=0; m<values.length; m++) {
                                        v[m] = this.getValueObject(values[m]);
                                    }
                                    config.put(property.getName(), v);
                                }
                            } else {
                                final Object value = this.getValueObject(property.getValue());
                                if ( value != null ) {
                                    config.put(property.getName(), value);
                                }
                            }
                        }
                    } else {
                        config = null;
                    }
                } else {
                    config = null;
                }
                configs[i] = new ProcessingComponentConfigurationImpl(types[i], config);
            }
            return configs;
        }
        return null;
    }

    private Object getValueObject(Value v)
    throws ValueFormatException, IllegalStateException, RepositoryException {
        switch (v.getType()) {
            case PropertyType.BINARY:
                return null;
            case PropertyType.DATE:
                return v.getDate();
            case PropertyType.BOOLEAN:
                return v.getBoolean();
            case PropertyType.LONG:
                return v.getLong();
            case PropertyType.DOUBLE:
                return v.getDouble();
            default:
                return v.getString();
        }
    }

    protected String[] getValues(final Node configNode, final String propName)
    throws RepositoryException {
        if ( configNode.hasProperty(propName) ) {
            if ( configNode.getProperty(propName).getDefinition().isMultiple() ) {
                final Value[] values = configNode.getProperty(propName).getValues();
                if ( values.length > 0 ) {
                    final String[] result = new String[values.length];
                    for(int i=0; i<values.length; i++) {
                        result[i] = values[i].getString();
                    }
                    return result;
                }
            } else {
                return new String[] {configNode.getProperty(propName).getString()};
            }
        }
        return null;
    }

    protected String getValue(final Node configNode, final String propName)
    throws RepositoryException {
        if ( configNode.hasProperty(propName) ) {
            if ( configNode.getProperty(propName).getDefinition().isMultiple() ) {
                final Value[] values = configNode.getProperty(propName).getValues();
                if ( values.length > 0 ) {
                    return values[0].getString();
                }
            } else {
                return configNode.getProperty(propName).getString();
            }
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
            contentType = MIME_TYPE_HTML;
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
        return true;
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
}

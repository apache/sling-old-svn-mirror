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

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.sling.rewriter.Generator;
import org.apache.sling.rewriter.PipelineConfiguration;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Processor;
import org.apache.sling.rewriter.ProcessorConfiguration;
import org.apache.sling.rewriter.Serializer;
import org.apache.sling.rewriter.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * The <code>PipelineImpl</code> is the heart of the pipeline
 * processing. It uses the configured pipeline components,
 * assembles a pipeline and runs the pipeline.
 */
public class PipelineImpl implements Processor {

    private static Logger LOGGER = LoggerFactory.getLogger(PipelineImpl.class);

    /** Empty array of transformers. */
    private static final Transformer[] EMPTY_TRANSFORMERS = new Transformer[0];

    /** The starting point of the pipeline. */
    private Generator generator;

    /** The transformers. */
    private Transformer[] transformers;

    /** The end point. */
    private Serializer serializer;

    /** The first component in the pipeline after the generator */
    private ContentHandler firstContentHandler;

    /** The factory cache. */
    private final FactoryCache factoryCache;

    /**
     * Setup this pipeline.
     */
    public PipelineImpl(final FactoryCache factoryCache) {
        this.factoryCache = factoryCache;
    }

    /**
     * @see org.apache.sling.rewriter.Processor#init(org.apache.sling.rewriter.ProcessingContext, org.apache.sling.rewriter.ProcessorConfiguration)
     */
    public void init(ProcessingContext processingContext,
                     ProcessorConfiguration c)
    throws IOException {
        LOGGER.debug("Setting up pipeline...");
        final PipelineConfiguration config = (PipelineConfiguration)c;
        final ProcessingComponentConfiguration[] transformerConfigs = config.getTransformerConfigurations();

        // create components and initialize them

        // lets get custom rewriter transformers
        final Transformer[][] rewriters = this.factoryCache.getGlobalTransformers(processingContext);

        final ProcessingComponentConfiguration generatorConfig = config.getGeneratorConfiguration();
        this.generator = this.getPipelineComponent(Generator.class, generatorConfig.getType(), false);
        LOGGER.debug("Using generator type {}: {}.", generatorConfig.getType(), generator);
        generator.init(processingContext, generatorConfig);

        final int transformerCount = (transformerConfigs == null ? 0 : transformerConfigs.length) + rewriters[0].length + rewriters[1].length;
        int index = 0;
        if ( transformerCount > 0 ) {
            // add all pre rewriter transformers
            transformers = new Transformer[transformerCount];
            for(int i=0; i< rewriters[0].length; i++) {
                transformers[index] = rewriters[0][i];
                LOGGER.debug("Using pre transformer: {}.", transformers[index]);
                transformers[index].init(processingContext, ProcessingComponentConfigurationImpl.EMPTY);
                index++;
            }
            if ( transformerConfigs != null ) {
                for(int i=0; i< transformerConfigs.length;i++) {
                    transformers[index] = this.getPipelineComponent(Transformer.class, transformerConfigs[i].getType(),
                            transformerConfigs[i].getConfiguration().get(ProcessingComponentConfiguration.CONFIGURATION_COMPONENT_OPTIONAL, false));
                    if ( transformers[index] != null ) {
                        LOGGER.debug("Using transformer type {}: {}.", transformerConfigs[i].getType(), transformers[index]);
                        transformers[index].init(processingContext, transformerConfigs[i]);
                        index++;
                    } else {
                        LOGGER.debug("Skipping missing optional transformer of type {}", transformerConfigs[i].getType());
                    }
                }
            }
            for(int i=0; i< rewriters[1].length; i++) {
                transformers[index] = rewriters[1][i];
                LOGGER.debug("Using post transformer: {}.", transformers[index]);
                transformers[index].init(processingContext, ProcessingComponentConfigurationImpl.EMPTY);
                index++;
            }
        } else {
            transformers = EMPTY_TRANSFORMERS;
        }

        final ProcessingComponentConfiguration serializerConfig = config.getSerializerConfiguration();
        this.serializer = this.getPipelineComponent(Serializer.class, serializerConfig.getType(), false);
        LOGGER.debug("Using serializer type {}: {}.", serializerConfig.getType(), serializer);
        serializer.init(processingContext, serializerConfig);

        ContentHandler pipelineComponent = serializer;
        // now chain pipeline
        for(int i=index; i>0; i--) {
            transformers[i-1].setContentHandler(pipelineComponent);
            pipelineComponent = transformers[i-1];
        }

        this.firstContentHandler = pipelineComponent;
        generator.setContentHandler(this.firstContentHandler);
        LOGGER.debug("Finished pipeline setup.");
    }

    /**
     * Lookup a pipeline component.
     */
    @SuppressWarnings("unchecked")
    private <ComponentType> ComponentType getPipelineComponent(final Class<ComponentType> typeClass,
                                                               final String type,
                                                               final boolean optional)
    throws IOException {
        final ComponentType component;
        if ( typeClass == Generator.class ) {
            component = (ComponentType)this.factoryCache.getGenerator(type);
        } else if ( typeClass == Transformer.class ) {
            component = (ComponentType)this.factoryCache.getTransformer(type);
        } else if ( typeClass == Serializer.class ) {
            component = (ComponentType)this.factoryCache.getSerializer(type);
        } else {
            component = null;
        }

        if ( component == null && !optional ) {
            throw new IOException("Unable to get component of class '" + typeClass + "' with type '" + type + "'.");
        }

        return component;
    }

    /**
     * @see org.apache.sling.rewriter.Processor#getWriter()
     */
    public PrintWriter getWriter() {
        return this.generator.getWriter();
    }

    /**
     * @see org.apache.sling.rewriter.Processor#getContentHandler()
     */
    public ContentHandler getContentHandler() {
        return this.firstContentHandler;
    }

    /**
     * @see org.apache.sling.rewriter.Processor#finished(boolean)
     */
    public void finished(final boolean errorOccured) throws IOException {
        try {
            // if an error occurred, we only clean up
            if ( !errorOccured ) {
                try {
                    this.generator.finished();
                } catch (final SAXException se) {
                    if ( se.getCause() != null && se.getCause() instanceof IOException ) {
                        throw (IOException)se.getCause();
                    } else {
                        final IOException ioe = new IOException("Pipeline exception: " + se.getMessage());
                        ioe.initCause(se);
                        throw ioe;
                    }
                }
            }
        } finally {
            // dispose components
            if ( this.generator != null ) {
                this.generator.dispose();
            }
            if ( this.transformers != null ) {
                for(final Transformer transformer : this.transformers ) {
                    if ( transformer != null ) {
                        transformer.dispose();
                    }
                }
            }
            if ( this.serializer != null ) {
                this.serializer.dispose();
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Pipeline Processor (");
        sb.append(super.toString());
        sb.append(") : ");
        sb.append("generator: ");
        sb.append(this.generator != null ? this.generator : "-");
        sb.append(", transformers: [");
        if ( this.transformers != null && this.transformers.length > 0 ) {
            boolean first = true;
            for(final Transformer t : this.transformers ) {
                if ( !first ) {
                    sb.append(", ");
                }
                first = false;
                sb.append(t);
            }
            sb.append("]");
        } else {
            sb.append("-");
        }
        sb.append(", serializer: ");
        sb.append(this.serializer != null ? this.serializer : "-");
        return sb.toString();
    }
}

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

import java.util.Comparator;

import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.rewriter.Generator;
import org.apache.sling.rewriter.GeneratorFactory;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Processor;
import org.apache.sling.rewriter.ProcessorConfiguration;
import org.apache.sling.rewriter.ProcessorFactory;
import org.apache.sling.rewriter.Serializer;
import org.apache.sling.rewriter.SerializerFactory;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an utility class for accessing the various pipeline components.
 * it also acts like a cache for the factories.
 */
public class FactoryCache {

    /** The required property containing the component type. */
    static final String PROPERTY_TYPE = "pipeline.type";

    /** The optional property for the pipeline mode (global) */
    static final String PROPERTY_MODE = "pipeline.mode";

    /** The global mode. */
    static final String MODE_GLOBAL = "global";

    /** The optional property for the paths the component should apply to */
    private static final String PROPERTY_PATHS = "pipeline.paths";

    /** The optional property for the extensions the component should apply to */
    private static final String PROPERTY_EXTENSIONS = "pipeline.extensions";

    /** The optional property for the content types the component should apply to */
    private static final String PROPERTY_CONTENT_TYPES = "pipeline.contentTypes";

    /** The optional property for the selectors the component should apply to */
    private static final String PROPERTY_SELECTORS = "pipeline.selectors";

    /** The optional property for the resource types the component should apply to */
    private static final String PROPERTY_RESOURCE_TYPES = "pipeline.resourceTypes";

    /** The logger. */
    static final Logger LOGGER = LoggerFactory.getLogger(FactoryCache.class);

    /** The tracker for generator factories. */
    private final HashingServiceTrackerCustomizer<GeneratorFactory> generatorTracker;

    /** The tracker for serializers factories. */
    private final HashingServiceTrackerCustomizer<SerializerFactory> serializerTracker;

    /** The tracker for transformer factories. */
    private final TransformerFactoryServiceTracker<TransformerFactory> transformerTracker;

    /** The tracker for processor factories. */
    private final HashingServiceTrackerCustomizer<ProcessorFactory> processorTracker;

    public FactoryCache(final BundleContext context)
    throws InvalidSyntaxException {
        this.generatorTracker = new HashingServiceTrackerCustomizer<GeneratorFactory>(context,
                GeneratorFactory.class.getName());
        this.serializerTracker = new HashingServiceTrackerCustomizer<SerializerFactory>(context,
                SerializerFactory.class.getName());
        this.transformerTracker = new TransformerFactoryServiceTracker<TransformerFactory>(context,
                TransformerFactory.class.getName());
        this.processorTracker = new HashingServiceTrackerCustomizer<ProcessorFactory>(context,
                ProcessorFactory.class.getName());
    }

    /**
     * Start tracking
     */
    public void start() {
        this.generatorTracker.open();
        this.serializerTracker.open();
        this.transformerTracker.open();
        this.processorTracker.open();
    }

    /**
     * Stop tracking
     */
    public void stop() {
        this.generatorTracker.close();
        this.serializerTracker.close();
        this.transformerTracker.close();
        this.processorTracker.close();
    }

    /**
     * Get the generator of the given type.
     * @param type The generator type.
     * @return The generator or null if the generator is not available.
     */
    public Generator getGenerator(final String type) {
        final GeneratorFactory factory = this.generatorTracker.getFactory(type);
        if ( factory == null ) {
            LOGGER.debug("Requested generator factory for type '{}' not found.", type);
            return null;
        }
        return factory.createGenerator();
    }

    /**
     * Get the serializer of the given type.
     * @param type The serializer type.
     * @return The serializer or null if the serializer is not available.
     */
    public Serializer getSerializer(final String type) {
        final SerializerFactory factory = this.serializerTracker.getFactory(type);
        if ( factory == null ) {
            LOGGER.debug("Requested serializer factory for type '{}' not found.", type);
            return null;
        }
        return factory.createSerializer();
    }

    /**
     * Get the transformer of the given type.
     * @param type The transformer type.
     * @return The transformer or null if the transformer is not available.
     */
    public Transformer getTransformer(final String type) {
        final TransformerFactory factory = this.transformerTracker.getFactory(type);
        if ( factory == null ) {
            LOGGER.debug("Requested transformer factory for type '{}' not found.", type);
            return null;
        }
        return factory.createTransformer();
    }

    /**
     * Get the processor of the given type.
     * @param type The processor type.
     * @return The processor or null if the processor is not available.
     */
    public Processor getProcessor(final String type) {
        final ProcessorFactory factory = this.processorTracker.getFactory(type);
        if ( factory == null ) {
            LOGGER.debug("Requested processor factory for type '{}' not found.", type);
            return null;
        }
        return factory.createProcessor();
    }

    private static final Transformer[] EMPTY_ARRAY = new Transformer[0];
    private static final Transformer[][] EMPTY_DOUBLE_ARRAY = new Transformer[][] {EMPTY_ARRAY, EMPTY_ARRAY};

    /**
     * Lookup all global transformers that apply to the current request and return
     * the transformer instances in two arrays.
     * The first array contains all pre transformers and the second one contains
     * all post transformers.
     * @param context The current processing context.
     */
    public Transformer[][] getGlobalTransformers(final ProcessingContext context) {
        final TransformerFactory[][] factories = this.transformerTracker.getGlobalTransformerFactories(context);
        return createTransformers(factories);
    }

    /**
     * Create new instances from the factories
     * @param factories The transformer factories
     * @return The transformer instances
     */
    private Transformer[][] createTransformers(final TransformerFactory[][] factories) {
        if ( factories == EMPTY_DOUBLE_ARRAY ) {
            return FactoryCache.EMPTY_DOUBLE_ARRAY;
        }
        final Transformer[][] transformers = new Transformer[2][];
        for(int arrayIndex = 0; arrayIndex < 2; arrayIndex++) {
            int count = factories[arrayIndex].length;
            for(final TransformerFactory factory : factories[arrayIndex]) {
                if ( factory == null ) count--;
            }
            if ( count == 0 ) {
                transformers[arrayIndex] = FactoryCache.EMPTY_ARRAY;
            } else {
                transformers[arrayIndex] = new Transformer[count];
                for(int i=0; i < factories[arrayIndex].length; i++) {
                    final TransformerFactory factory = factories[arrayIndex][i];
                    if ( factory != null ) {
                        transformers[arrayIndex][i] = factory.createTransformer();
                    }
                }
            }
        }

        return transformers;
    }

    /**
     * Comparator for service references.
     */
    static final class ServiceReferenceComparator implements Comparator<ServiceReference> {
        public static ServiceReferenceComparator INSTANCE = new ServiceReferenceComparator();

        @Override
        public int compare(ServiceReference o1, ServiceReference o2) {
            return o1.compareTo(o2);
        }
    }

    static final class TransformerFactoryEntry {
        public final TransformerFactory factory;

        private final ProcessorConfiguration configuration;

        public TransformerFactoryEntry(final TransformerFactory factory, final ServiceReference ref) {
            this.factory = factory;
            final String[] paths = OsgiUtil.toStringArray(ref.getProperty(PROPERTY_PATHS), null);
            final String[] extensions = OsgiUtil.toStringArray(ref.getProperty(PROPERTY_EXTENSIONS), null);
            final String[] contentTypes = OsgiUtil.toStringArray(ref.getProperty(PROPERTY_CONTENT_TYPES), null);
            final String[] resourceTypes = OsgiUtil.toStringArray(ref.getProperty(PROPERTY_RESOURCE_TYPES), null);
            final String[] selectors = OsgiUtil.toStringArray(ref.getProperty(PROPERTY_SELECTORS), null);
            final boolean noCheckRequired = (paths == null || paths.length == 0) &&
                                   (extensions == null || extensions.length == 0) &&
                                   (contentTypes == null || contentTypes.length == 0) &&
                                   (resourceTypes == null || resourceTypes.length == 0) &&
                                   (selectors == null || selectors.length == 0);
            if ( !noCheckRequired ) {
                this.configuration = new ProcessorConfigurationImpl(contentTypes, paths, extensions, resourceTypes, selectors);
            } else {
                this.configuration = null;
            }
        }

        public boolean match(final ProcessingContext context) {
            if ( configuration == null ) {
                return true;
            }
            return configuration.match(context);
        }
    }
}

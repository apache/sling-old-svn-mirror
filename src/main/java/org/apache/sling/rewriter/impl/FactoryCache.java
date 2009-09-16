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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an utility class for accessing the various pipeline components.
 * it also acts like a cache for the factories.
 */
public class FactoryCache {

    /** The required property containing the component type. */
    private static final String PROPERTY_TYPE = "pipeline.type";

    /** The optional property for the pipeline mode (global) */
    private static final String PROPERTY_MODE = "pipeline.mode";

    /** The global mode. */
    private static final String MODE_GLOBAL = "global";

    /** The optional property for the paths the component should apply to */
    private static final String PROPERTY_PATHS = "pipeline.paths";

    /** The optional property for the paths the component should apply to */
    private static final String PROPERTY_EXTENSIONS = "pipeline.extensions";

    /** The optional property for the paths the component should apply to */
    private static final String PROPERTY_CONTENT_TYPES = "pipeline.contentTypes";

    /** The optional property for the paths the component should apply to */
    private static final String PROPERTY_RESOURCE_TYPES = "pipeline.resourceTypes";

    /** The logger. */
    protected static final Logger LOGGER = LoggerFactory.getLogger(FactoryCache.class);

    /** The tracker for generator factories. */
    protected final HashingServiceTrackerCustomizer<GeneratorFactory> generatorTracker;

    /** The tracker for serializers factories. */
    protected final HashingServiceTrackerCustomizer<SerializerFactory> serializerTracker;

    /** The tracker for transformer factories. */
    protected final TransformerFactoryServiceTracker<TransformerFactory> transformerTracker;

    /** The tracker for processor factories. */
    protected final HashingServiceTrackerCustomizer<ProcessorFactory> processorTracker;

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
    protected Transformer[][] createTransformers(final TransformerFactory[][] factories) {
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
     * This service tracker stores all services into a hash map.
     */
    protected static class HashingServiceTrackerCustomizer<T> extends ServiceTracker {

        /** The services hashed by their name property. */
        private final Map<String, T> services = new ConcurrentHashMap<String, T>();

        /** The bundle context. */
        protected final BundleContext context;

        public HashingServiceTrackerCustomizer(final BundleContext bc, final String serviceClassName) {
            super(bc, serviceClassName, null);
            this.context = bc;
        }

        public T getFactory(final String type) {
            return services.get(type);
        }

        private String getType(final ServiceReference ref) {
            final String type = (String) ref.getProperty(PROPERTY_TYPE);
            return type;
        }

        /**
         * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
         */
        public Object addingService(final ServiceReference reference) {
            final String type = this.getType(reference);
            @SuppressWarnings("unchecked")
            final T factory = (type == null ? null : (T) this.context.getService(reference));
            if ( factory != null ) {
                if ( LOGGER.isDebugEnabled() ) {
                    LOGGER.debug("Found service {}, type={}.", factory, type);
                }
                this.services.put(type, factory);
            }
            return factory;
        }

        /**
         * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
         */
        public void removedService(final ServiceReference reference, final Object service) {
            final String type = this.getType(reference);
            if ( type != null ) {
                this.services.remove(type);
            }
        }
    }

    protected static final class TransformerFactoryServiceTracker<T> extends HashingServiceTrackerCustomizer<T> {

        private String getMode(final ServiceReference ref) {
            final String mode = (String) ref.getProperty(PROPERTY_MODE);
            return mode;
        }

        private boolean isGlobal(final ServiceReference ref) {
            return MODE_GLOBAL.equalsIgnoreCase(this.getMode(ref));
        }

        public static final TransformerFactoryEntry[] EMPTY_ENTRY_ARRAY = new TransformerFactoryEntry[0];
        public static final TransformerFactoryEntry[][] EMPTY_DOUBLE_ENTRY_ARRAY = new TransformerFactoryEntry[][] {EMPTY_ENTRY_ARRAY, EMPTY_ENTRY_ARRAY};

        public static final TransformerFactory[] EMPTY_FACTORY_ARRAY = new TransformerFactory[0];
        public static final TransformerFactory[][] EMPTY_DOUBLE_FACTORY_ARRAY = new TransformerFactory[][] {EMPTY_FACTORY_ARRAY, EMPTY_FACTORY_ARRAY};

        private TransformerFactoryEntry[][] cached = EMPTY_DOUBLE_ENTRY_ARRAY;

        /** flag for cache. */
        private boolean cacheIsValid = true;

        public TransformerFactoryServiceTracker(final BundleContext bc, final String serviceClassName) {
            super(bc, serviceClassName);
        }

        /**
         * Is this cache still valid?
         */
        public boolean isCacheValid() {
            return this.cacheIsValid;
        }

        /**
         * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
         */
        public Object addingService(ServiceReference reference) {
            final boolean isGlobal = isGlobal(reference);
            if ( isGlobal ) {
                this.cacheIsValid = false;
            }
            Object obj = super.addingService(reference);
            if ( obj == null && isGlobal ) {
                obj = this.context.getService(reference);
            }
            return obj;
        }

        /**
         * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
         */
        public void removedService(ServiceReference reference, Object service) {
            if ( isGlobal(reference) ) {
                this.cacheIsValid = false;
            }
            super.removedService(reference, service);
        }

        /**
         * Get all global transformer factories.
         * @return Two arrays of transformer factories
         */
        public TransformerFactoryEntry[][] getGlobalTransformerFactories() {
            if ( !this.cacheIsValid ) {
                synchronized ( this ) {
                    if ( !this.cacheIsValid ) {
                        final ServiceReference[] refs = this.getServiceReferences();
                        if ( refs == null || refs.length == 0 ) {
                            this.cached = EMPTY_DOUBLE_ENTRY_ARRAY;
                        } else {
                            Arrays.sort(refs, ServiceReferenceComparator.INSTANCE);

                            int preCount = 0;
                            int postCount = 0;
                            for(final ServiceReference ref : refs) {
                                if ( isGlobal(ref) ) {
                                    final Object r = ref.getProperty(Constants.SERVICE_RANKING);
                                    int ranking = (r instanceof Integer ? (Integer)r : 0);
                                    if ( ranking < 0 ) {
                                        preCount++;
                                    } else {
                                        postCount++;
                                    }
                                }
                            }
                            final TransformerFactoryEntry[][] globalFactories = new TransformerFactoryEntry[2][];
                            if ( preCount == 0 ) {
                                globalFactories[0] = EMPTY_ENTRY_ARRAY;
                            } else {
                                globalFactories[0] = new TransformerFactoryEntry[preCount];
                            }
                            if ( postCount == 0) {
                                globalFactories[1] = EMPTY_ENTRY_ARRAY;
                            } else {
                                globalFactories[1] = new TransformerFactoryEntry[postCount];
                            }
                            int index = 0;
                            for(final ServiceReference ref : refs) {
                                if ( isGlobal(ref) ) {
                                    if ( index < preCount ) {
                                        globalFactories[0][index] = new TransformerFactoryEntry((TransformerFactory) this.getService(ref), ref);
                                    } else {
                                        globalFactories[1][index - preCount] = new TransformerFactoryEntry((TransformerFactory) this.getService(ref), ref);
                                    }
                                    index++;
                                }
                            }
                            this.cached = globalFactories;
                        }
                    }
                    this.cacheIsValid = true;
                }
            }

            return this.cached;
        }

        /**
         * Get all global transformer factories that apply to the current request.
         * @param context The current processing context.
         * @return Two arrays containing the transformer factories.
         */
        public TransformerFactory[][] getGlobalTransformerFactories(final ProcessingContext context) {
            final TransformerFactoryEntry[][] globalFactoryEntries = this.getGlobalTransformerFactories();
            // quick check
            if ( globalFactoryEntries == EMPTY_DOUBLE_ENTRY_ARRAY ) {
                return EMPTY_DOUBLE_FACTORY_ARRAY;
            }
            final TransformerFactory[][] factories = new TransformerFactory[2][];
            for(int i=0; i<2; i++) {
                if ( globalFactoryEntries[i] == EMPTY_ENTRY_ARRAY ) {
                    factories[i] = EMPTY_FACTORY_ARRAY;
                } else {
                    factories[i] = new TransformerFactory[globalFactoryEntries[i].length];
                    for(int m=0; m<globalFactoryEntries[i].length; m++) {
                        final TransformerFactoryEntry entry = globalFactoryEntries[i][m];
                        if ( entry.match(context) ) {
                            factories[i][m] = entry.factory;
                        }
                    }
                }
            }
            return factories;
        }
    }

    /**
     * Comparator for service references.
     */
    protected static final class ServiceReferenceComparator implements Comparator<ServiceReference> {
        public static ServiceReferenceComparator INSTANCE = new ServiceReferenceComparator();

        public int compare(ServiceReference o1, ServiceReference o2) {
            return o1.compareTo(o2);
        }
    }

    protected static final class TransformerFactoryEntry {
        public final TransformerFactory factory;

        private final ProcessorConfiguration configuration;

        public TransformerFactoryEntry(final TransformerFactory factory, final ServiceReference ref) {
            this.factory = factory;
            final String[] paths = OsgiUtil.toStringArray(ref.getProperty(PROPERTY_PATHS), null);
            final String[] extensions = OsgiUtil.toStringArray(ref.getProperty(PROPERTY_EXTENSIONS), null);
            final String[] contentTypes = OsgiUtil.toStringArray(ref.getProperty(PROPERTY_CONTENT_TYPES), null);
            final String[] resourceTypes = OsgiUtil.toStringArray(ref.getProperty(PROPERTY_RESOURCE_TYPES), null);
            final boolean noCheckRequired = (paths == null || paths.length == 0) &&
                                   (extensions == null || extensions.length == 0) &&
                                   (contentTypes == null || contentTypes.length == 0) &&
                                   (resourceTypes == null || resourceTypes.length == 0);
            if ( !noCheckRequired ) {
                this.configuration = new ProcessorConfigurationImpl(contentTypes, paths, extensions, resourceTypes);
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

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
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.rewriter.Generator;
import org.apache.sling.rewriter.GeneratorFactory;
import org.apache.sling.rewriter.Processor;
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
     * Lookup all factory components for rewriter transformers and return
     * new rewriter transformer instances in two arrays.
     * The first array contains all pre transformers and the second one contains
     * all post transformers.
     */
    public Transformer[][] getRewriterTransformers() {
        final TransformerFactory[][] factories = this.transformerTracker.getTransformerFactories();
        return createTransformers(factories);
    }

    protected Transformer[][] createTransformers(final TransformerFactory[][] factories) {
        if ( factories == EMPTY_DOUBLE_ARRAY ) {
            return FactoryCache.EMPTY_DOUBLE_ARRAY;
        }
        final Transformer[][] transformers = new Transformer[2][];
        if ( factories[0].length == 0 ) {
            transformers[0] = FactoryCache.EMPTY_ARRAY;
        } else {
            transformers[0] = new Transformer[factories[0].length];
            for(int i=0; i < factories[0].length; i++) {
                transformers[0][i] = factories[0][i].createTransformer();
            }
        }
        if ( factories[1].length == 0 ) {
            transformers[1] = FactoryCache.EMPTY_ARRAY;
        } else {
            transformers[1] = new Transformer[factories[1].length];
            for(int i=0; i < factories[1].length; i++) {
                transformers[1][i] = factories[1][i].createTransformer();
            }
        }
        return transformers;
    }

    /**
     * This service tracker stores all services into a hash map.
     */
    protected static class HashingServiceTrackerCustomizer<T> extends ServiceTracker {

        /** The services hashed by their name property. */
        private final Map<String, T> services = new HashMap<String, T>();

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
            final String type = (String) ref.getProperty("pipeline.type");
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
            final String mode = (String) ref.getProperty("pipeline.mode");
            return mode;
        }

        private boolean isGlobal(final ServiceReference ref) {
            return "global".equalsIgnoreCase(this.getMode(ref));
        }

        public static final TransformerFactory[] EMPTY_ARRAY = new TransformerFactory[0];
        public static final TransformerFactory[][] EMPTY_DOUBLE_ARRAY = new TransformerFactory[][] {EMPTY_ARRAY, EMPTY_ARRAY};

        private TransformerFactory[][] cached = EMPTY_DOUBLE_ARRAY;

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

        public TransformerFactory[][] getTransformerFactories() {
            if ( !this.cacheIsValid ) {
                synchronized ( this ) {
                    if ( !this.cacheIsValid ) {
                        final ServiceReference[] refs = this.getServiceReferences();
                        if ( refs == null || refs.length == 0 ) {
                            this.cached = EMPTY_DOUBLE_ARRAY;
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
                            final TransformerFactory[][] rewriters = new TransformerFactory[2][];
                            if ( preCount == 0 ) {
                                rewriters[0] = EMPTY_ARRAY;
                            } else {
                                rewriters[0] = new TransformerFactory[preCount];
                            }
                            if ( postCount == 0) {
                                rewriters[1] = EMPTY_ARRAY;
                            } else {
                                rewriters[1] = new TransformerFactory[postCount];
                            }
                            int index = 0;
                            for(final ServiceReference ref : refs) {
                                if ( isGlobal(ref) ) {
                                    if ( index < preCount ) {
                                        rewriters[0][index] = (TransformerFactory) this.getService(ref);
                                    } else {
                                        rewriters[1][index - preCount] = (TransformerFactory) this.getService(ref);
                                    }
                                    index++;
                                }
                            }
                            this.cached = rewriters;
                        }
                    }
                    this.cacheIsValid = true;
                }
            }
            return this.cached;
        }
    }

    /**
     * Comparator for service references.
     */
    protected static final class ServiceReferenceComparator implements Comparator<ServiceReference> {
        public static ServiceReferenceComparator INSTANCE = new ServiceReferenceComparator();

        public int compare(ServiceReference o1, ServiceReference o2) {

            Long id = (Long) o1.getProperty(Constants.SERVICE_ID);
            Long otherId = (Long) o2.getProperty(Constants.SERVICE_ID);

            if (id.equals(otherId)) {
                return 0; // same service
            }

            Object rankObj = o1.getProperty(Constants.SERVICE_RANKING);
            Object otherRankObj = o2.getProperty(Constants.SERVICE_RANKING);

            // If no rank, then spec says it defaults to zero.
            rankObj = (rankObj == null) ? new Integer(0) : rankObj;
            otherRankObj = (otherRankObj == null) ? new Integer(0) : otherRankObj;

            // If rank is not Integer, then spec says it defaults to zero.
            Integer rank = !(rankObj instanceof Integer)
                ? new Integer(0) : (Integer) rankObj;
            Integer otherRank = !(otherRankObj instanceof Integer)
                ? new Integer(0) : (Integer) otherRankObj;

            // Sort by rank in ascending order.
            if (rank.compareTo(otherRank) < 0) {
                return -1; // lower rank
            } else if (rank.compareTo(otherRank) > 0) {
                return 1; // higher rank
            }

            // If ranks are equal, then sort by service id in descending order.
            return (id.compareTo(otherId) < 0) ? 1 : -1;
        }
    }
}

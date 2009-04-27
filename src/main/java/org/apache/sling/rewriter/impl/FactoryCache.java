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
import java.util.List;
import java.util.Map;

import org.apache.sling.rewriter.Generator;
import org.apache.sling.rewriter.Processor;
import org.apache.sling.rewriter.RewriterTransformerFactory;
import org.apache.sling.rewriter.Serializer;
import org.apache.sling.rewriter.Transformer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an utility class for accessing the various pipeline components.
 * it also acts like a cache for the factories.
 */
public class FactoryCache {

    /** The logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FactoryCache.class);

    /** The tracker for generator factories. */
    private final HashingServiceTrackerCustomizer generatorTracker;

    /** The tracker for serializers factories. */
    private final HashingServiceTrackerCustomizer serializerTracker;

    /** The tracker for transformer factories. */
    private final HashingServiceTrackerCustomizer transformerTracker;

    /** The tracker for rewriting transformer factories. */
    private final RewriterTransformerFactoryServiceTracker rewritingTransformerTracker;

    /** The tracker for processor factories. */
    private final HashingServiceTrackerCustomizer processorTracker;

    public FactoryCache(final BundleContext context)
    throws InvalidSyntaxException {
        this.generatorTracker = new HashingServiceTrackerCustomizer(context,
                context.createFilter("(component.factory=" + Generator.class.getName() + "/*)"));
        this.serializerTracker = new HashingServiceTrackerCustomizer(context,
                context.createFilter("(component.factory=" + Serializer.class.getName() + "/*)"));
        this.transformerTracker = new HashingServiceTrackerCustomizer(context,
                context.createFilter("(component.factory=" + Transformer.class.getName() + "/*)"));
        this.processorTracker = new HashingServiceTrackerCustomizer(context,
                context.createFilter("(component.factory=" + Processor.class.getName() + "/*)"));
        this.rewritingTransformerTracker = new RewriterTransformerFactoryServiceTracker(context);
    }

    /**
     * Start tracking
     */
    public void start() {
        this.generatorTracker.open();
        this.serializerTracker.open();
        this.transformerTracker.open();
        this.rewritingTransformerTracker.open();
        this.processorTracker.open();
    }

    /**
     * Stop tracking
     */
    public void stop() {
        this.generatorTracker.close();
        this.serializerTracker.close();
        this.transformerTracker.close();
        this.rewritingTransformerTracker.close();
        this.processorTracker.close();
    }

    /**
     * Lookup a factory component.
     */
    private <ComponentType> ComponentType getComponent(final List<ComponentInstance> instanceList,
                                                       final Class<ComponentType> typeClass,
                                                       final String type,
                                                       final HashingServiceTrackerCustomizer customizer) {
        // get factory
        final ComponentFactory factory = customizer.get(type);
        if ( factory == null ) {
            LOGGER.debug("Request component factory for class '{}' and type '{}' not found.", typeClass, type);
            return null;
        }
        final ComponentInstance instance = factory.newInstance(null);
        instanceList.add(instance);
        @SuppressWarnings("unchecked")
        final ComponentType component = (ComponentType)instance.getInstance();
        if ( component == null ) {
            // this should never happen
            LOGGER.debug("Unable to get instance '{}' of type '{}' from factory.", typeClass, type);
        }
        return component;
    }

    /**
     * Get the generator of the given type.
     * @param type The generator type.
     * @param instanceList The instance is added to this list for later disposal.
     * @return The generator or null if the generator is not available.
     */
    public Generator getGenerator(final String type, final List<ComponentInstance> instanceList) {
        return this.getComponent(instanceList, Generator.class, type, this.generatorTracker);
    }

    /**
     * Get the serializer of the given type.
     * @param type The serializer type.
     * @param instanceList The instance is added to this list for later disposal.
     * @return The serializer or null if the serializer is not available.
     */
    public Serializer getSerializer(final String type, final List<ComponentInstance> instanceList) {
        return this.getComponent(instanceList, Serializer.class, type, this.serializerTracker);
    }

    /**
     * Get the transformer of the given type.
     * @param type The transformer type.
     * @param instanceList The instance is added to this list for later disposal.
     * @return The transformer or null if the transformer is not available.
     */
    public Transformer getTransformer(final String type, final List<ComponentInstance> instanceList) {
        return this.getComponent(instanceList, Transformer.class, type, this.transformerTracker);
    }

    /**
     * Get the processor of the given type.
     * @param type The processor type.
     * @param instanceList The instance is added to this list for later disposal.
     * @return The processor or null if the processor is not available.
     */
    public Processor getProcessor(final String type, final List<ComponentInstance> instanceList) {
        return this.getComponent(instanceList, Processor.class, type, this.processorTracker);
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
        return this.rewritingTransformerTracker.getTransformers();
    }

    /**
     * This service tracker stores all services into a hash map.
     */
    private final class HashingServiceTrackerCustomizer extends ServiceTracker {

        private final Map<String, ComponentFactory> services = new HashMap<String, ComponentFactory>();

        private final BundleContext context;

        public HashingServiceTrackerCustomizer(final BundleContext bc, final Filter filter) {
            super(bc, filter, null);
            this.context = bc;
        }

        public ComponentFactory get(final String type) {
            return services.get(type);
        }

        private String getType(final ServiceReference ref) {
            final String factory = (String) ref.getProperty("component.factory");
            final int pos = factory.lastIndexOf('/');
            if ( pos != -1 ) {
                final String type = factory.substring(pos + 1);
                return type;
            }
            return null;
        }

        /**
         * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
         */
        public Object addingService(final ServiceReference reference) {
            final String type = this.getType(reference);
            final ComponentFactory factory = (type == null ? null : (ComponentFactory) this.context.getService(reference));
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

    private static final class RewriterTransformerFactoryServiceTracker extends ServiceTracker {

        private static final RewriterTransformerFactory[] EMPTY_ARRAY = new RewriterTransformerFactory[0];
        private static final RewriterTransformerFactory[][] EMPTY_DOUBLE_ARRAY = new RewriterTransformerFactory[][] {EMPTY_ARRAY, EMPTY_ARRAY};

        private RewriterTransformerFactory[][] cached = EMPTY_DOUBLE_ARRAY;

        private boolean cacheIsValid = true;

        public RewriterTransformerFactoryServiceTracker(final BundleContext bc) {
            super(bc, RewriterTransformerFactory.class.getName(), null);
        }

        /**
         * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
         */
        public Object addingService(ServiceReference reference) {
            this.cacheIsValid = false;
            return super.addingService(reference);
        }

        /**
         * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
         */
        public void removedService(ServiceReference reference, Object service) {
            this.cacheIsValid = false;
            super.removedService(reference, service);
        }

        private RewriterTransformerFactory[][] getTransformerFactories() {
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
                                final Object r = ref.getProperty(Constants.SERVICE_RANKING);
                                int ranking = (r instanceof Integer ? (Integer)r : 0);
                                if ( ranking < 0 ) {
                                    preCount++;
                                } else {
                                    postCount++;
                                }
                            }
                            final RewriterTransformerFactory[][] rewriters = new RewriterTransformerFactory[2][];
                            if ( preCount == 0 ) {
                                rewriters[0] = EMPTY_ARRAY;
                            } else {
                                rewriters[0] = new RewriterTransformerFactory[preCount];
                            }
                            if ( postCount == 0) {
                                rewriters[1] = EMPTY_ARRAY;
                            } else {
                                rewriters[1] = new RewriterTransformerFactory[postCount];
                            }
                            int index = 0;
                            for(final ServiceReference ref : refs) {
                                if ( index < preCount ) {
                                    rewriters[0][index] = (RewriterTransformerFactory) this.getService(ref);
                                } else {
                                    rewriters[1][index - preCount] = (RewriterTransformerFactory) this.getService(ref);
                                }
                                index++;
                            }
                            this.cached = rewriters;
                        }
                    }
                    this.cacheIsValid = true;
                }
            }
            return this.cached;
        }

        public Transformer[][] getTransformers() {
            final RewriterTransformerFactory[][] factories = this.getTransformerFactories();
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
    }

    /**
     * Comparator for service references.
     */
    private static final class ServiceReferenceComparator implements Comparator<ServiceReference> {
        public static ServiceReferenceComparator INSTANCE = new ServiceReferenceComparator();

        public int compare(ServiceReference o1, ServiceReference o2) {

            Long id = (Long) o1.getProperty(Constants.SERVICE_ID);
            Long otherId = (Long) o2.getProperty(Constants.SERVICE_ID);

            if (id.equals(otherId))
            {
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
            if (rank.compareTo(otherRank) < 0)
            {
                return -1; // lower rank
            }
            else if (rank.compareTo(otherRank) > 0)
            {
                return 1; // higher rank
            }

            // If ranks are equal, then sort by service id in descending order.
            return (id.compareTo(otherId) < 0) ? 1 : -1;
        }
    }
}

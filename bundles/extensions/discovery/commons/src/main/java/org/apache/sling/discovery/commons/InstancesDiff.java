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
package org.apache.sling.discovery.commons;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyView;

/**
 * The {@code InstancesDiff} allows to combine and filter two collections of {@code InstanceDescription} instances,
 * an "old" collection and a "new" collection.<p>
 *
 * The comparison between {@code InstanceDescription} instances is done only on the basis of the Sling identifier.
 * Two instances with the same Sling identifier are considered as equal.<p>
 *
 * <b>Note</b>: Each collection must contain only unique instances (no two instances with the same Sling identifier).
 * Using the {@code InstancesDiff} with collections containing duplicated Sling id
 * will throw an {@code IllegalArgumentException}.<p>
 *
 * @since 1.0.0
 */
public final class InstancesDiff {

    /**
     * A filter that keeps local instances (see {@link InstanceDescription#isLocal()}.
     */
    private static final InstanceFilter LOCAL_INSTANCE = new LocalInstanceFilter();

    /**
     * A filter that filters out local instances (see {@link InstanceDescription#isLocal()}.
     */
    private static final InstanceFilter NOT_LOCAL_INSTANCE = new NotFilter(LOCAL_INSTANCE);

    /**
     * A filter that keeps leader instances (see {@link InstanceDescription#isLeader()}.
     */
    private static final InstanceFilter LEADER_INSTANCE = new LeaderInstanceFilter();

    /**
     * A filter that filters out leader instances (see {@link InstanceDescription#isLeader()}.
     */
    private static final InstanceFilter NOT_LEADER_INSTANCE = new NotFilter(LEADER_INSTANCE);

    /**
     * Keeps track of the old {@code InstanceDescription} instances
     *
     * The map keys are the instance Sling identifiers and values are
     * the {@code InstanceDescription} instances descriptions.
     */
    private final Map<String, InstanceDescription> oldInstances;

    /**
     * Keeps track of the new {@code InstanceDescription} instances
     *
     * The map keys are the instance Sling identifiers and values are
     * the {@code InstanceDescription} instances descriptions.
     */
    private final Map<String, InstanceDescription> newInstances;

    /**
     * Create a new {@code InstancesDiff} based on the instances from the old and
     * new {@code TopologyView} topology views contained in the {@code TopologyEvent} event provided.
     *
     * @param event the non {@code null} event from which the old and new topology views are used for computing.
     *              If either of the topology views are {@code null}, then they will be substituted by an
     *              empty collection of instances.
     * @throws IllegalArgumentException if either of the collections contains duplicated Sling identifiers.
     */
    public InstancesDiff(@Nonnull TopologyEvent event) {
        this(instancesOrEmpty(event.getOldView()), instancesOrEmpty(event.getNewView()));
    }

    /**
     * Create a new {@code InstancesDiff} based on the instances from the old and
     * new {@code TopologyView} topology views provided.
     *
     * @param oldView the non {@code null} old topology view from which the old collection is used for computing.
     * @param newView the non {@code null} new topology view form which the new collection is used for computing.
     * @throws IllegalArgumentException if either of the collections contains duplicated Sling identifiers.
     */
    public InstancesDiff(@Nonnull TopologyView oldView, @Nonnull TopologyView newView) {
        this(oldView.getInstances(), newView.getInstances());
    }

    /**
     * Create a new {@code InstancesDiff} based on the instances from the old and
     * new {@code ClusterView} cluster views provided.
     *
     * @param oldView the non {@code null} old cluster view used for computing.
     * @param newView the non {@code null} new cluster view used for computing.
     * @throws IllegalArgumentException if either of the collections contains duplicated Sling identifiers.
     */
    public InstancesDiff(@Nonnull ClusterView oldView, @Nonnull ClusterView newView) {
        this(oldView.getInstances(), newView.getInstances());
    }

    /**
     * Create a new {@code InstancesDiff} based on the provided old and
     * new {@code Collection} collections of instances.
     *
     * @param oldInstances the non {@code null} old collection of instances used for computing.
     * @param newInstances the non {@code null} new collection of instances used for computing.
     * @param <T> the type of instance which must extend {@code InstanceDescription}.
     * @throws IllegalArgumentException if either of the collections contains duplicated Sling identifiers.
     */
    public <T extends InstanceDescription> InstancesDiff(@Nonnull Collection<T> oldInstances, @Nonnull Collection<T> newInstances) {
        this.newInstances = getInstancesMap(newInstances);
        this.oldInstances = getInstancesMap(oldInstances);
    }

    /**
     * Returns the {@code InstanceSet} set containing the {@code InstanceDescription} instances that are
     * contained in either the old or the new collection.<p>
     *
     * For {@code InstanceDescription} instances contained in both the old and
     * the new collections, the method will retain those from either of the collections
     * depending on the parameter #retainFromNewView.<p>
     *
     * @param retainFromNewCollection {@code true} in order to retain the instances from the new collection ;
     *                          {@code false} in order to retain the instances from the old collection.
     * @return the {@code InstanceCollection} collection containing the {@code InstanceDescription} instances
     *         from both collections.
     */
    @Nonnull
    public InstanceCollection all(boolean retainFromNewCollection) {
        return new InstanceCollection(partitionAll(retainFromNewCollection));
    }

    /**
     * Returns the {@code InstanceCollection} collection containing the {@code InstanceDescription} instances that are
     * contained in the new collection but not in the old collection.
     *
     * @return the {@code InstanceCollection} collection containing the instances in the new
     *         topology collection but not in the old collection.
     */
    @Nonnull
    public InstanceCollection added() {
        return new InstanceCollection(partitionAdded());
    }

    /**
     * Returns the {@code InstanceCollection} collection containing the {@code InstanceDescription} instances that are
     * contained in the old collection but not in the new collection.
     *
     * @return the {@code InstanceSet} set containing the instances in the old collection but not in the new collection.
     */
    @Nonnull
    public InstanceCollection removed() {
        return new InstanceCollection(partitionRemoved());
    }

    /**
     * Returns the {@code InstanceSet} collection containing the {@code InstanceDescription} instances that are
     * contained in both the old collection and the new collection.<p>
     *
     * The method will retain the {@code InstanceDescription} instances from either of the collections
     * depending on the parameter #retainFromNewView.<p>
     *
     * @param retainFromNewCollection {@code true} in order to retain the instances from the new collection ;
     *                                {@code false} in order to retain the instances from the old collection.
     * @return the {@code InstanceCollection} collection containing the {@code InstanceDescription} instances
     *         contained in both collections.
     */
    @Nonnull
    public InstanceCollection retained(boolean retainFromNewCollection) {
        return new InstanceCollection(partitionRetained(retainFromNewCollection));
    }

    /**
     * Returns the {@code InstanceCollection} collection containing the {@code InstanceDescription} instances that are
     * contained in both the old and the new collections.<p>
     *
     * The method will retain the {@code InstanceDescription} instances from either of the collections
     * depending on the parameter #retainFromNewView.<p>
     *
     * @param retainFromNewCollection {@code true} in order to retain the instances from the new collection ;
     *                                {@code false} in order to retain the instances from the old collection.
     * @param propertyChanged {@code true} in order to keep only the instances which
     *                        properties have changed between the old and new collections ;
     *                        {@code false} in order to keep only the instances which properties have not changed.
     * @return the {@code InstanceCollection} collection containing the {@code InstanceDescription} instances
     *         contained in both views.
     */
    @Nonnull
    public InstanceCollection retained(boolean retainFromNewCollection, boolean propertyChanged) {
        return new InstanceCollection(partitionRetained(retainFromNewCollection, propertyChanged));
    }

    //

    @Nonnull
    private Map<String, InstanceDescription> partitionAll(boolean retainFromNewCollection) {
        Map<String, InstanceDescription> partition = new HashMap<String, InstanceDescription>();
        if (retainFromNewCollection) {
            partition.putAll(oldInstances);
            partition.putAll(newInstances);
        } else {
            partition.putAll(newInstances);
            partition.putAll(oldInstances);
        }
        return partition;
    }

    @Nonnull
    private Map<String, InstanceDescription> partitionRemoved() {
        Map<String, InstanceDescription> partition = new HashMap<String, InstanceDescription>(oldInstances);
        partition.keySet().removeAll(newInstances.keySet());
        return partition;
    }

    @Nonnull
    private Map<String, InstanceDescription> partitionAdded() {
        Map<String, InstanceDescription> partition = new HashMap<String, InstanceDescription>(newInstances);
        partition.keySet().removeAll(oldInstances.keySet());
        return partition;
    }

    @Nonnull
    private Map<String, InstanceDescription> partitionRetained(boolean retainFromNewCollection, boolean propertyChanged) {
        Map<String, InstanceDescription> partition = new HashMap<String, InstanceDescription>();
        for (Map.Entry<String, InstanceDescription> oldEntry : oldInstances.entrySet()) {
            String slingId = oldEntry.getKey();
            InstanceDescription newDescription = newInstances.get(slingId);
            if(newDescription != null) {
                InstanceDescription oldDescription = oldEntry.getValue();
                boolean propertiesSame = newDescription.getProperties().equals(oldDescription.getProperties());
                if ((propertiesSame && ! propertyChanged) || (! propertiesSame && propertyChanged)) {
                    partition.put(slingId, retainFromNewCollection ? newDescription : oldDescription);
                }
            }
        }
        return partition;
    }

    @Nonnull
    private Map<String, InstanceDescription> partitionRetained(boolean retainFromNewCollection) {
        Map<String, InstanceDescription> partition = new HashMap<String, InstanceDescription>();
        if (retainFromNewCollection) {
            partition.putAll(newInstances);
            partition.keySet().retainAll(oldInstances.keySet());
        } else {
            partition.putAll(oldInstances);
            partition.keySet().retainAll(newInstances.keySet());
        }
        return partition;
    }

    @Nonnull
    private static Set<InstanceDescription> instancesOrEmpty(@Nullable TopologyView topologyView) {
        return (topologyView != null) ? topologyView.getInstances() : Collections.<InstanceDescription>emptySet();
    }

    @Nonnull
    private static <T extends InstanceDescription> Map<String, InstanceDescription> getInstancesMap(@Nonnull Collection<T> instances) {
        Map<String, InstanceDescription> instancesMap = new HashMap<String, InstanceDescription>();
        for (InstanceDescription instance : instances) {
            String slingId = instance.getSlingId();
            if (slingId != null) {
                if (instancesMap.put(slingId, instance) != null) {
                    throw new IllegalArgumentException(String.format("Duplicated instance found for slingId: %s", slingId));
                }
            }
        }
        return instancesMap;
    }

    private static final class NotFilter implements InstanceFilter {

        final InstanceFilter filter;

        private NotFilter(InstanceFilter filter) {
            this.filter = filter;
        }

        public boolean accept(InstanceDescription instance) {
            return ! filter.accept(instance);
        }
    }

    private static final class LocalInstanceFilter implements InstanceFilter {

        public boolean accept(InstanceDescription instance) {
            return instance.isLocal();
        }
    }

    private static final class LeaderInstanceFilter implements InstanceFilter {

        public boolean accept(InstanceDescription instance) {
            return instance.isLeader();
        }
    }

    private static final class InClusterView implements InstanceFilter {

        private final ClusterView view;

        private InClusterView(ClusterView view) {
            this.view = view;
        }

        public boolean accept(InstanceDescription instance) {
            return view.getId().equals(instance.getClusterView().getId());
        }
    }

    /**
     * The {@code InstanceCollection} collection allows to filter the instances using a set of custom filter
     * either implementing {@code InstanceFilter} or pre-defined ones.<p>
     *
     * Filters conditions are joined combined together using the logical operator "AND".<p>
     */
    public final class InstanceCollection {

        /**
         * Holds the instances to be filtered.
         *
         * The map keys are the instance Sling identifiers and values are
         * the {@code InstanceDescription} instances descriptions.
         */
        private final Map<String, InstanceDescription> instances;

        /**
         * Holds the set of filters to be applied (ANDed).
         */
        private final Set<InstanceFilter> filters = new HashSet<InstanceFilter>();

        /**
         * Filter the instances with a custom {@code InstanceFilter} filter.
         *
         * @param filter the filter to be applied on the instances
         * @return {@code this}
         */
        @Nonnull
        public InstanceCollection filterWith(@Nullable InstanceFilter filter) {
            if (filter != null) {
                filters.add(filter);
            }
            return this;
        }

        /**
         * Keep only the local instance (see {@link InstanceDescription#isLocal()}.
         *
         * @return {@code this}
         */
        @Nonnull
        public InstanceCollection isLocal() {
            filters.add(LOCAL_INSTANCE);
            return this;
        }

        /**
         * Filter out the local instances (see {@link InstanceDescription#isLocal()}.
         *
         * @return {@code this}
         */
        @Nonnull
        public InstanceCollection isNotLocal() {
            filters.add(NOT_LOCAL_INSTANCE);
            return this;
        }

        /**
         * Keep only the leader instances (see {@link InstanceDescription#isLeader()}.
         *
         * @return {@code this}
         */
        @Nonnull
        public InstanceCollection isLeader() {
            filters.add(LEADER_INSTANCE);
            return this;
        }

        /**
         * Filter out the leader instances (see {@link InstanceDescription#isLeader()}.
         *
         * @return {@code this}
         */
        @Nonnull
        public InstanceCollection isNotLeader() {
            filters.add(NOT_LEADER_INSTANCE);
            return this;
        }

        /**
         * Keep only the instances that are contained in the same {@code ClusterView} cluster view
         * as the one provided.<p>
         *
         * The comparison between cluster views is done on the basis of the cluster
         * view identifier. Two cluster views with the same identifier are considered equal.<p>
         *
         * @param clusterView the cluster view used to filter the instances
         * @return {@code this}
         */
        @Nonnull
        public InstanceCollection isInClusterView(@Nullable ClusterView clusterView) {
            if (clusterView != null) {
                filters.add(new InClusterView(clusterView));
            }
            return this;
        }

        /**
         * Filter out the instances that are contained in the same {@code ClusterView} cluster view
         * as the one provided.<p>
         *
         * The comparison between cluster views is done on the basis of the cluster
         * view identifier. Two cluster views with the same identifier are considered equal.<p>
         *
         * @param clusterView the cluster view used to filter the instances
         * @return {@code this}
         */
        @Nonnull
        public InstanceCollection isNotInClusterView(@Nullable ClusterView clusterView) {
            if (clusterView != null) {
                filters.add(new NotFilter(new InClusterView(clusterView)));
            }
            return this;
        }

        /**
         * Return the collection of {@code InstanceDescription} instances that have not been filtered out.
         *
         * @return the filtered collection of instances.
         */
        @Nonnull
        public Collection<InstanceDescription> get() {
            return applyFilters();
        }

        //

        /**
         * Instances of this class can only be obtained through the {@code InstancesDiff} class.
         * @param instances the map of instances to be filtered
         */
        private InstanceCollection(@Nonnull Map<String, InstanceDescription> instances) {
            this.instances = instances;
        }

        @Nonnull
        private Collection<InstanceDescription> applyFilters() {
            Iterator<Map.Entry<String, InstanceDescription>> entries = instances.entrySet().iterator();
            for ( ; entries.hasNext() ; ) {
                Map.Entry<String, InstanceDescription> entry = entries.next();
                for (InstanceFilter filter : filters) {
                    if (! filter.accept(entry.getValue())) {
                        entries.remove();
                        break;
                    }
                }
            }
            return Collections.<InstanceDescription>unmodifiableCollection(instances.values());
        }
    }
}

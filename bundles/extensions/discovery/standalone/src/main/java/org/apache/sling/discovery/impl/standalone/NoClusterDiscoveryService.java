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
package org.apache.sling.discovery.impl.standalone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.apache.sling.discovery.PropertyProvider;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a simple implementation of the discovery service
 * which can be used for a cluster less installation (= single instance).
 * It is disabled by default and can be enabled through a OSGi configuration.
 */
@Component(immediate=true)
@Service(value = {DiscoveryService.class})
public class NoClusterDiscoveryService implements DiscoveryService {

    /** The logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Sling settings service to get the Sling ID and run modes.
     */
    @Reference
    private SlingSettingsService settingsService;

    /**
     * All topology event listeners.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private TopologyEventListener[] listeners = new TopologyEventListener[0];

    /**
     * All property providers.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC,
               referenceInterface=PropertyProvider.class, updated="updatedPropertyProvider")
    private List<ProviderInfo> providerInfos = new ArrayList<ProviderInfo>();

    /**
     * Special lock object to sync data structure access
     */
    private final Object lock = new Object();

    /**
     * The current topology view.
     */
    private TopologyView topologyView;

    private Map<String, String> cachedProperties = new HashMap<String, String>();

    /**
     * Activate this service
     * Create a new description.
     */
    @Activate
    protected void activate() {
        logger.debug("NoClusterDiscoveryService started.");
        final InstanceDescription myDescription = new InstanceDescription() {

            public boolean isLocal() {
                return true;
            }

            public boolean isLeader() {
                return true;
            }

            public String getSlingId() {
                return settingsService.getSlingId();
            }

            public String getProperty(final String name) {
            	synchronized(lock) {
            		return cachedProperties.get(name);
            	}
            }

			public Map<String, String> getProperties() {
				synchronized(lock) {
					return Collections.unmodifiableMap(cachedProperties);
				}
			}

			public ClusterView getClusterView() {
				final Collection<ClusterView> clusters = topologyView.getClusterViews();
				if (clusters==null || clusters.size()==0) {
					return null;
				}
				return clusters.iterator().next();
			}
        };
        final Set<InstanceDescription> instances = new HashSet<InstanceDescription>();
        instances.add(myDescription);

        final TopologyEventListener[] registeredServices;
		synchronized ( lock ) {
            registeredServices = this.listeners;
            final ClusterView clusterView = new ClusterView() {

                public InstanceDescription getLeader() {
                    return myDescription;
                }

                public List<InstanceDescription> getInstances() {
                    return new LinkedList<InstanceDescription>(instances);
                }

				public String getId() {
					return "0";
				}
            };
            this.topologyView = new TopologyView() {

    			public InstanceDescription getLocalInstance() {
    				return myDescription;
    			}

    			public boolean isCurrent() {
    				return true;
    			}

    			public Set<InstanceDescription> getInstances() {
    				return instances;
    			}

    			public Set<InstanceDescription> findInstances(InstanceFilter picker) {
    				Set<InstanceDescription> result = new HashSet<InstanceDescription>();
    				for (Iterator<InstanceDescription> it = getTopology().getInstances().iterator(); it.hasNext();) {
    					InstanceDescription instance = it.next();
    					if (picker.accept(instance)) {
    						result.add(instance);
    					}
    				}
    				return result;
    			}

    			public Set<ClusterView> getClusterViews() {
    				Set<ClusterView> clusters = new HashSet<ClusterView>();
    				clusters.add(clusterView);
    				return clusters;
    			}

    		};
        }
        for(final TopologyEventListener da: registeredServices) {
        	da.handleTopologyEvent(new TopologyEvent(Type.TOPOLOGY_INIT, null, topologyView));
        }
    }

    /**
     * Deactivate this service.
     */
    @Deactivate
    protected void deactivate() {
        logger.debug("NoClusterDiscoveryService stopped.");
        this.topologyView = null;
    }

    /**
     * Bind a new property provider.
     */
    @SuppressWarnings("unused")
	private void bindPropertyProvider(final PropertyProvider propertyProvider, final Map<String, Object> props) {
    	logger.debug("bindPropertyProvider: Binding PropertyProvider {}", propertyProvider);

        final TopologyEventListener[] awares;
        synchronized (lock) {
            final ProviderInfo info = new ProviderInfo(propertyProvider, props);
            this.providerInfos.add(info);
            Collections.sort(this.providerInfos);
            this.updatePropertiesCache();
            if ( this.topologyView == null ) {
                awares = null;
            } else {
                awares = this.listeners;
            }
        }
        if ( awares != null ) {
            for(final TopologyEventListener da : awares) {
                da.handleTopologyEvent(new TopologyEvent(Type.PROPERTIES_CHANGED, this.topologyView, this.topologyView));
            }
        }
    }

    /**
     * Update a property provider.
     */
    @SuppressWarnings("unused")
    private void updatedPropertyProvider(final PropertyProvider propertyProvider, final Map<String, Object> props) {
        logger.debug("bindPropertyProvider: Updating PropertyProvider {}", propertyProvider);

        this.unbindPropertyProvider(propertyProvider, props, false);
        this.bindPropertyProvider(propertyProvider, props);
    }

    /**
     * Unbind a property provider
     */
    @SuppressWarnings("unused")
	private void unbindPropertyProvider(final PropertyProvider propertyProvider, final Map<String, Object> props) {
        this.unbindPropertyProvider(propertyProvider, props, true);
    }

    /**
     * Unbind a property provider
     */
    @SuppressWarnings("unused")
    private void unbindPropertyProvider(final PropertyProvider propertyProvider,
            final Map<String, Object> props,
            final boolean inform) {
    	logger.debug("unbindPropertyProvider: Releasing PropertyProvider {}", propertyProvider);

    	final TopologyEventListener[] awares;
        synchronized (lock) {
            final ProviderInfo info = new ProviderInfo(propertyProvider, props);
            this.providerInfos.remove(info);
            this.updatePropertiesCache();
            if ( this.topologyView == null ) {
                awares = null;
            } else {
                awares = this.listeners;
            }
        }
        if ( inform && awares != null ) {
            for(final TopologyEventListener da : awares) {
                da.handleTopologyEvent(new TopologyEvent(Type.PROPERTIES_CHANGED, this.topologyView, this.topologyView));
            }
        }
    }

    private void updatePropertiesCache() {
        final Map<String, String> newProps = new HashMap<String, String>();
        for(final ProviderInfo info : this.providerInfos) {
            newProps.putAll(info.properties);
        }
        this.cachedProperties = newProps;
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("New properties: {}", this.cachedProperties);
        }
    }

    @SuppressWarnings("unused")
    private void bindTopologyEventListener(final TopologyEventListener clusterAware) {

        logger.debug("bindTopologyEventListener: Binding TopologyEventListener {}", clusterAware);

        boolean inform = true;
        synchronized (lock) {
            List<TopologyEventListener> currentList = new ArrayList<TopologyEventListener>(
                Arrays.asList(listeners));
            currentList.add(clusterAware);
            this.listeners = currentList.toArray(new TopologyEventListener[currentList.size()]);
            if ( this.topologyView == null ) {
                inform = false;
            }
        }

        if ( inform ) {
        	clusterAware.handleTopologyEvent(new TopologyEvent(Type.TOPOLOGY_INIT, null, topologyView));
        }
    }

    @SuppressWarnings("unused")
    private void unbindTopologyEventListener(final TopologyEventListener clusterAware) {

        logger.debug("unbindTopologyEventListener: Releasing TopologyEventListener {}", clusterAware);

        synchronized (lock) {
            List<TopologyEventListener> currentList = new ArrayList<TopologyEventListener>(
                Arrays.asList(listeners));
            currentList.remove(clusterAware);
            this.listeners = currentList.toArray(new TopologyEventListener[currentList.size()]);
        }
    }

    /**
     * @see DiscoveryService#getTopology()
     */
    public TopologyView getTopology() {
    	return topologyView;
    }

    /**
     * Internal class caching some provider infos like service id and ranking.
     */
    private final static class ProviderInfo implements Comparable<ProviderInfo> {

        public final PropertyProvider provider;
        public final int ranking;
        public final long serviceId;
        public final Map<String, String> properties = new HashMap<String, String>();

        public ProviderInfo(final PropertyProvider provider, final Map<String, Object> serviceProps) {
            this.provider = provider;
            final Object sr = serviceProps.get(Constants.SERVICE_RANKING);
            if ( sr == null || !(sr instanceof Integer)) {
                this.ranking = 0;
            } else {
                this.ranking = (Integer)sr;
            }
            this.serviceId = (Long)serviceProps.get(Constants.SERVICE_ID);
            final Object namesObj = serviceProps.get(PropertyProvider.PROPERTY_PROPERTIES);
            if ( namesObj instanceof String ) {
                final String val = provider.getProperty((String)namesObj);
                if ( val != null ) {
                    this.properties.put((String)namesObj, val);
                }
            } else if ( namesObj instanceof String[] ) {
                for(final String name : (String[])namesObj ) {
                    final String val = provider.getProperty(name);
                    if ( val != null ) {
                        this.properties.put(name, val);
                    }
                }
            }
        }

        /**
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(final ProviderInfo o) {
            // Sort by rank in ascending order.
            if ( this.ranking < o.ranking ) {
                return -1; // lower rank
            } else if (this.ranking > o.ranking ) {
                return 1; // higher rank
            }
            // If ranks are equal, then sort by service id in descending order.
            return (this.serviceId < o.serviceId) ? 1 : -1;
        }

        @Override
        public boolean equals(final Object obj) {
            if ( obj instanceof ProviderInfo ) {
                return ((ProviderInfo)obj).serviceId == this.serviceId;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return provider.hashCode();
        }
    }
}

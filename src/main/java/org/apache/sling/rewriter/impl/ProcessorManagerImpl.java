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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.apache.sling.rewriter.PipelineConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Processor;
import org.apache.sling.rewriter.ProcessorConfiguration;
import org.apache.sling.rewriter.ProcessorManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This manager keeps track of configured processors.
 *
 * @scr.component metatype="no"
 * @scr.service interface="ProcessorManager"
 */
public class ProcessorManagerImpl implements ProcessorManager {

    private static final String CONFIG_REL_PATH = "config/rewriter";
    private static final String CONFIG_PATH = "/" + CONFIG_REL_PATH;

    protected static final String MIME_TYPE_HTML = "text/html";

    /** The logger */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /** The bundle context. */
    private BundleContext bundleContext;

    /** The admin session. */
    private Session adminSession;

    /** @scr.reference */
    private SlingRepository repository;

    /** @scr.reference */
    private JcrResourceResolverFactory resourceResolverFactory;

    /** The resource resolver. */
    private ResourceResolver resourceResolver;

    /** loaded processor configurationss */
    private final Map<String, ConfigEntry[]> processors = new HashMap<String, ConfigEntry[]>();

    /** Ordered processor configs. */
    private List<ProcessorConfiguration> orderedProcessors = new ArrayList<ProcessorConfiguration>();

    /** Registered listeners */
    private final List<EventListenerWrapper> listeners = new ArrayList<EventListenerWrapper>();

    /** Search paths */
    private String[] searchPaths;

    /** The factory cache. */
    private FactoryCache factoryCache;

    /**
     * Activate this component.
     * @param ctx
     */
    protected void activate(final ComponentContext ctx)
    throws RepositoryException, InvalidSyntaxException {
        this.bundleContext = ctx.getBundleContext();
        this.factoryCache = new FactoryCache(this.bundleContext);
        this.adminSession = this.repository.loginAdministrative(null);

        // create array of search paths for actions and constraints
        this.resourceResolver = this.resourceResolverFactory.getResourceResolver(adminSession);
        this.searchPaths = resourceResolver.getSearchPath();

        // set up observation listener
        for(final String p : searchPaths) {
            // remove trailing slash
            final String path = p.substring(0, p.length() - 1);
            // we have to set up a listener for the whole search path
            // as we support {searchPath}/{appName}/config/rewriter
            EventListenerWrapper wrapper = new EventListenerWrapper(this, path);
            adminSession.getWorkspace().getObservationManager()
                       .addEventListener(wrapper,
                               Event.NODE_ADDED|Event.NODE_REMOVED|Event.PROPERTY_CHANGED|Event.PROPERTY_ADDED|Event.PROPERTY_REMOVED,
                               path,
                               true /* isDeep */,
                               null /* uuid */,
                               null /* nodeTypeName */,
                               true /* noLocal */
                               );
            this.listeners.add(wrapper);
        }

        this.initProcessors();

        this.factoryCache.start();
    }

    /**
     * Deactivate this component.
     * @param ctx
     */
    protected void deactivate(final ComponentContext ctx) {
        this.factoryCache.stop();
        this.factoryCache = null;
        if ( this.adminSession != null ) {
            for(final EventListener listener : this.listeners) {
                try {
                    this.adminSession.getWorkspace().getObservationManager().removeEventListener(listener);
                } catch (RepositoryException e) {
                    this.log.error("Unable to unregister observation manager.", e);
                }
            }
            listeners.clear();
            this.adminSession.logout();
            this.adminSession = null;
        }
        this.bundleContext = null;
    }

    /**
     * Initializes the current processors
     * @throws RepositoryException
     */
    private synchronized void initProcessors()
    throws RepositoryException {
        for(final String path : this.searchPaths ) {
            // check if the search path exists
            if ( this.adminSession.itemExists(path) ) {
                final Item item = this.adminSession.getItem(path);
                if ( item.isNode() ) {
                    // now iterate over the child nodes
                    final Node searchPathNode = (Node)item;
                    final NodeIterator spIter = searchPathNode.getNodes();
                    while ( spIter.hasNext() ) {
                        // check if the node has a rewriter config
                        final Node appNode = spIter.nextNode();
                        if ( appNode.hasNode(CONFIG_REL_PATH) ) {
                            final Node parentNode = appNode.getNode(CONFIG_REL_PATH);
                            // now read configs
                            final NodeIterator iter = parentNode.getNodes();
                            while ( iter.hasNext() ) {
                                final Node configNode = iter.nextNode();
                                final String key = configNode.getName();
                                final ProcessorConfigurationImpl config = this.getProcessorConfiguration(configNode);
                                this.log.debug("Found new processor configuration {}", config);
                                this.addProcessor(key, configNode.getPath(), config);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Read the configuration for the processor from the repository.
     */
    private ProcessorConfigurationImpl getProcessorConfiguration(Node configNode)
    throws RepositoryException {
        configNode.getSession().refresh(true);
        final ProcessorConfigurationImpl config = new ProcessorConfigurationImpl(this.resourceResolver.getResource(configNode.getPath()));
        return config;
    }

    /**
     * adds a processor configuration
     */
    protected void addProcessor(final String key, final String configPath, final ProcessorConfigurationImpl config) {
        ConfigEntry[] configs = this.processors.get(key);
        if ( configs == null ) {
            configs = new ConfigEntry[1];
            configs[0] = new ConfigEntry(configPath, config);
        } else {
            ConfigEntry[] newConfigs = new ConfigEntry[configs.length + 1];
            System.arraycopy(configs, 0, newConfigs, 0, configs.length);
            newConfigs[configs.length] = new ConfigEntry(configPath, config);
        }

        this.processors.put(key, configs);
        // only add active configurations
        if ( config.isActive() ) {
            this.orderedProcessors.add(config);
            Collections.sort(this.orderedProcessors, new ProcessorConfiguratorComparator());
        }
    }

    /**
     * This method is invoked by the event listener wrapper.
     * The second argument is the search path - which is the prefix we have to strip
     * to check if this is a rewriter configuration change.
     */
    public synchronized void onEvent(final EventIterator iter, final String searchPath) {
        final Set<String>removedPaths = new HashSet<String>();
        final Set<String>changedPaths = new HashSet<String>();
        while ( iter.hasNext() ) {
            final Event event = iter.nextEvent();
            try {
                String nodePath = event.getPath();
                if ( event.getType() == Event.PROPERTY_ADDED
                     || event.getType() == Event.PROPERTY_REMOVED
                     || event.getType() == Event.PROPERTY_CHANGED ) {
                    final int lastSlash = nodePath.lastIndexOf('/');
                    nodePath = nodePath.substring(0, lastSlash);
                }
                // relative path after the search path
                String checkPath = nodePath.substring(searchPath.length() + 1);
                final int firstSlash = checkPath.indexOf('/');
                final int pattern = checkPath.indexOf(CONFIG_PATH);
                // only if firstSlash and pattern are at the same position, this migt be a rewriter config
                if ( firstSlash == pattern && firstSlash != -1 ) {
                    // the node should be a direct child of CONFIG_PATH
                    if ( checkPath.length() > pattern + CONFIG_PATH.length() ) {
                        checkPath = checkPath.substring(pattern + CONFIG_PATH.length() + 1);
                        if ( checkPath.indexOf('/') == -1 ) {
                            switch (event.getType()) {
                                case Event.NODE_ADDED:
                                    changedPaths.add(nodePath);
                                    break;

                                case Event.PROPERTY_ADDED:
                                case Event.PROPERTY_REMOVED:
                                case Event.PROPERTY_CHANGED:
                                    changedPaths.add(nodePath);
                                    break;

                                case Event.NODE_REMOVED:
                                    // remove processor
                                    removedPaths.add(nodePath);
                                    break;
                            }
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error("Error during modification: {}", e.getMessage());
            }
        }
        // handle removed first
        changedPaths.removeAll(removedPaths);
        for(final String path : removedPaths) {
            this.removeProcessor(path);
        }
        // now update changed/added processors
        for(final String path : changedPaths) {
            try {
                this.updateProcessor(path);
            } catch (RepositoryException e) {
                log.error("Error during modification: {}", e.getMessage());
            }
        }
    }

    /**
     * updates a processor
     */
    private void updateProcessor(String path)
    throws RepositoryException {
        final int pos = path.lastIndexOf('/');
        final String key = path.substring(pos + 1);
        int keyIndex = 0;
        // search the search path
        for(final String searchPath : this.searchPaths) {
            if ( path.startsWith(searchPath) ) {
                break;
            }
            keyIndex++;
        }

        final Node configNode = (Node) this.adminSession.getItem(path);
        final ProcessorConfigurationImpl config = this.getProcessorConfiguration(configNode);

        final ConfigEntry[] configs = this.processors.get(key);
        if ( configs != null ) {
            // search path
            int index = -1;
            for(int i=0; i<configs.length; i++) {
                if ( configs[i].path.equals(path) ) {
                    index = i;
                    break;
                }
            }
            if ( index != -1 ) {
                // we are already in the array
                if ( index == 0 ) {
                    // we are the first, so remove the old, and add the new
                    this.orderedProcessors.remove(configs[index].config);
                    configs[index] = new ConfigEntry(path, config);
                    if ( config.isActive() ) {
                        this.orderedProcessors.add(config);
                        Collections.sort(this.orderedProcessors, new ProcessorConfiguratorComparator());
                    }
                } else {
                    // we are not the first, so we can simply exchange
                    configs[index] = new ConfigEntry(path, config);
                }
            } else {
                // now we have to insert the new config at the correct place
                int insertIndex = 0;
                boolean found = false;
                while ( !found && insertIndex < configs.length) {
                    final ConfigEntry current = configs[insertIndex];
                    int currentIndex = -1;
                    for(int i=0; i<this.searchPaths.length; i++) {
                        if ( current.path.startsWith(this.searchPaths[i]) ) {
                            currentIndex = i;
                            break;
                        }
                    }
                    if ( currentIndex >= keyIndex ) {
                        found = true;
                        insertIndex = currentIndex;
                    }
                }

                if ( !found ) {
                    // just append
                    this.addProcessor(key, path, config);
                } else {
                    ConfigEntry[] newArray = new ConfigEntry[configs.length + 1];
                    int i = 0;
                    for(final ConfigEntry current : configs) {
                        if ( i == insertIndex ) {
                            newArray[i] = new ConfigEntry(path, config);
                            i++;
                        }
                        newArray[i] = current;
                        i++;
                    }
                    this.processors.put(key, newArray);
                    if ( insertIndex == 0 ) {
                        // we are the first, so remove the old, and add the new
                        this.orderedProcessors.remove(configs[1].config);
                        if ( config.isActive() ) {
                            this.orderedProcessors.add(config);
                            Collections.sort(this.orderedProcessors, new ProcessorConfiguratorComparator());
                        }
                    }
                }
            }
        } else {
            // completly new, just add it
            this.addProcessor(key, path, config);
        }
    }

    /**
     * removes a pipeline
     */
    private void removeProcessor(String path) {
        final int pos = path.lastIndexOf('/');
        final String key = path.substring(pos + 1);
        // we have to search the config

        final ConfigEntry[] configs = this.processors.get(key);
        if ( configs != null ) {
            // search path
            ConfigEntry found = null;
            for(final ConfigEntry current : configs) {
                if ( current.path.equals(path) ) {
                    found = current;
                    break;
                }
            }
            if ( found != null ) {
                this.orderedProcessors.remove(found.config);
                if ( configs.length == 1 ) {
                    this.processors.remove(key);
                } else {
                    if ( found == configs[0] ) {
                        this.orderedProcessors.add(configs[1].config);
                        Collections.sort(this.orderedProcessors, new ProcessorConfiguratorComparator());
                    }
                    ConfigEntry[] newArray = new ConfigEntry[configs.length];
                    int index = 0;
                    for(final ConfigEntry current : configs) {
                        if ( current != found ) {
                            newArray[index] = current;
                            index++;
                        }
                    }
                    this.processors.put(key, newArray);
                }
            }
        }
    }

    /**
     * @see org.apache.sling.rewriter.ProcessorManager#getProcessor(org.apache.sling.rewriter.ProcessorConfiguration, org.apache.sling.rewriter.ProcessingContext)
     */
    public Processor getProcessor(ProcessorConfiguration configuration,
                                  ProcessingContext      context)
    throws IOException {
        if ( configuration == null ) {
            throw new IllegalArgumentException("Processor configuration is missing.");
        }
        if ( context == null ) {
            throw new IllegalArgumentException("Processor context is missing.");
        }
        boolean isPipeline = false;
        if ( configuration instanceof ProcessorConfigurationImpl ) {
            isPipeline = ((ProcessorConfigurationImpl)configuration).isPipeline();
        } else {
            isPipeline = configuration instanceof PipelineConfiguration;
        }
        if ( isPipeline ) {
            final PipelineImpl pipeline = new PipelineImpl(this.factoryCache);
            pipeline.init(context, configuration);
            return pipeline;
        }
        final Processor processor = new ProcessorWrapper(configuration, this.factoryCache);
        processor.init(context, configuration);
        return processor;
    }

    /**
     * @see org.apache.sling.rewriter.ProcessorManager#getProcessorConfigurations()
     */
    public List<ProcessorConfiguration> getProcessorConfigurations() {
        return this.orderedProcessors;
    }

    protected static final class ProcessorConfiguratorComparator implements Comparator<ProcessorConfiguration> {

        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(ProcessorConfiguration config0, ProcessorConfiguration config1) {
            final int o0 = ((ProcessorConfigurationImpl)config0).getOrder();
            final int o1 = ((ProcessorConfigurationImpl)config1).getOrder();
            if ( o0 == o1 ) {
                return 0;
            } else if ( o0 < o1 ) {
                return 1;
            }
            return -1;
        }

    }

    /**
     * Event listener wrapper to be able to add this service several times with different paths.
     */
    public final static class EventListenerWrapper implements EventListener {

        private final ProcessorManagerImpl delegatee;

        private final String path;

        public EventListenerWrapper(final ProcessorManagerImpl listener, final String path) {
            this.delegatee = listener;
            this.path = path;
        }

        /**
         * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
         */
        public void onEvent(EventIterator i) {
            this.delegatee.onEvent(i, path);
        }
    }

    public static final class ConfigEntry {
        public final String path;
        public final ProcessorConfiguration config;

        public ConfigEntry(final String p, final ProcessorConfiguration pc) {
            this.path = p;
            this.config = pc;
        }
    }
}

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.rewriter.PipelineConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Processor;
import org.apache.sling.rewriter.ProcessorConfiguration;
import org.apache.sling.rewriter.ProcessorManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This manager keeps track of configured processors.
 *
 */
@Component
@Service(value=ProcessorManager.class)
public class ProcessorManagerImpl
    implements ProcessorManager, EventHandler {

    private static final String CONFIG_REL_PATH = "config/rewriter";
    private static final String CONFIG_PATH = "/" + CONFIG_REL_PATH;

    protected static final String MIME_TYPE_HTML = "text/html";

    /** The logger */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /** The bundle context. */
    private BundleContext bundleContext;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    /** The resource resolver. */
    private ResourceResolver resourceResolver;

    /** loaded processor configurationss */
    private final Map<String, ConfigEntry[]> processors = new HashMap<String, ConfigEntry[]>();

    /** Ordered processor configs. */
    private List<ProcessorConfiguration> orderedProcessors = new ArrayList<ProcessorConfiguration>();

    /** Event handler registration */
    private ServiceRegistration eventHandlerRegistration;

    /** Search paths */
    private String[] searchPaths;

    /** The factory cache. */
    private FactoryCache factoryCache;

    /**
     * Activate this component.
     * @param ctx
     */
    protected void activate(final ComponentContext ctx)
    throws LoginException, InvalidSyntaxException {
        this.bundleContext = ctx.getBundleContext();
        this.factoryCache = new FactoryCache(this.bundleContext);

        // create array of search paths for actions and constraints
        this.resourceResolver = this.resourceResolverFactory.getAdministrativeResourceResolver(null);
        this.searchPaths = resourceResolver.getSearchPath();

        this.initProcessors();

        // register event handler
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("event.topics", new String[] { "org/apache/sling/api/resource/Resource/*",
            "org/apache/sling/api/resource/ResourceProvider/*" });
        props.put("service.description","Processor Configuration/Modification Handler");
        props.put("service.vendor","The Apache Software Foundation");

        this.eventHandlerRegistration = ctx.getBundleContext()
                  .registerService(EventHandler.class.getName(), this, props);

        this.factoryCache.start();

        WebConsoleConfigPrinter.register(this.bundleContext, this);
    }

    /**
     * Deactivate this component.
     * @param ctx
     */
    protected void deactivate(final ComponentContext ctx) {
        if ( this.eventHandlerRegistration != null ) {
            this.eventHandlerRegistration.unregister();
            this.eventHandlerRegistration = null;
        }
        this.factoryCache.stop();
        this.factoryCache = null;
        if ( this.resourceResolver != null ) {
            this.resourceResolver.close();
            this.resourceResolver = null;
        }

        WebConsoleConfigPrinter.unregister();

        this.bundleContext = null;
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(final org.osgi.service.event.Event event) {
        // check if the event handles something in the search paths
        String path = (String)event.getProperty(SlingConstants.PROPERTY_PATH);
        int foundPos = -1;
        for(final String sPath : this.searchPaths) {
            if ( path.startsWith(sPath) ) {
                foundPos = sPath.length();
                break;
            }
        }
        if ( foundPos != -1 ) {
            // now check if this is a rewriter config
            // relative path after the search path
            final int firstSlash = path.indexOf('/', foundPos);
            final int pattern = path.indexOf(CONFIG_PATH, foundPos);
            // only if firstSlash and pattern are at the same position, this migt be a rewriter config
            if ( firstSlash == pattern && firstSlash != -1 ) {
                // the node should be a child of CONFIG_PATH
                if ( path.length() > pattern + CONFIG_PATH.length() && path.charAt(pattern + CONFIG_PATH.length()) == '/') {
                    // if a child resource is changed, make sure we have the correct path
                    final int slashPos = path.indexOf('/', pattern + CONFIG_PATH.length() + 1);
                    if ( slashPos != -1 ) {
                        path = path.substring(0, slashPos);
                    }
                    // we should do the update async as we don't want to block the event delivery
                    final String configPath = path;
                    final Thread t = new Thread() {
                        public void run() {
                            if ( event.getTopic().equals(SlingConstants.TOPIC_RESOURCE_REMOVED) ) {
                                removeProcessor(configPath);
                            } else {
                                updateProcessor(configPath);
                            }
                        }
                    };
                    t.start();
                }
            }
        }
    }

    /**
     * Initializes the current processors
     */
    private synchronized void initProcessors() {
        for(final String path : this.searchPaths ) {
            // check if the search path exists
            final Resource spResource = this.resourceResolver.getResource(path.substring(0, path.length() - 1));
            if ( spResource != null ) {
                // now iterate over the child nodes
                final Iterator<Resource> spIter = spResource.listChildren();
                while ( spIter.hasNext() ) {
                    // check if the node has a rewriter config
                    final Resource appResource = spIter.next();
                    final Resource parentResource = this.resourceResolver.getResource(appResource.getPath() + CONFIG_PATH);
                    if ( parentResource != null ) {
                        // now read configs
                        final Iterator<Resource> iter = parentResource.listChildren();
                        while ( iter.hasNext() ) {
                            final Resource configResource = iter.next();
                            final String key = configResource.getName();
                            final ProcessorConfigurationImpl config = this.getProcessorConfiguration(configResource);
                            this.log.debug("Found new processor configuration {}", config);
                            this.addProcessor(key, configResource.getPath(), config);
                        }
                    }
                }
            }
        }
    }

    /**
     * Read the configuration for the processor from the repository.
     */
    private ProcessorConfigurationImpl getProcessorConfiguration(final Resource configResource) {
        final ProcessorConfigurationImpl config = new ProcessorConfigurationImpl(configResource);
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

    private void printConfiguration(final PrintWriter pw, final ConfigEntry entry) {
        if ( entry.config instanceof ProcessorConfigurationImpl ) {
            ((ProcessorConfigurationImpl)entry.config).printConfiguration(pw);
        } else {
            pw.println(entry.config.toString());
        }
        pw.print("Resource path: ");
        pw.println(entry.path);
    }

    synchronized void printConfiguration(final PrintWriter pw) {
        pw.println("Current Apache Sling Rewriter Configuration");
        pw.println("=================================================================");
        pw.println("Active Configurations");
        pw.println("-----------------------------------------------------------------");
        // we process the configs in their order
        for(final ProcessorConfiguration config : this.orderedProcessors) {
            // search the corresponding full config
            for(final Map.Entry<String, ConfigEntry[]> entry : this.processors.entrySet()) {
                if ( entry.getValue().length > 0 && entry.getValue()[0].config == config ) {
                    pw.print("Configuration ");
                    pw.println(entry.getKey());
                    pw.println();
                    printConfiguration(pw, entry.getValue()[0]);
                    if ( entry.getValue().length > 1 ) {
                        pw.println("Overriding configurations from the following resource paths: ");
                        for(int i=1; i < entry.getValue().length; i++) {
                            pw.print("- ");
                            pw.println(entry.getValue()[i].path);
                        }
                    }
                    pw.println();
                    pw.println();
                    break;
                }
            }
        }
    }

    /**
     * updates a processor
     */
    private synchronized void updateProcessor(String path) {
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

        final Resource configResource = this.resourceResolver.getResource(path);
        if ( configResource == null ) {
            return;
        }
        final ProcessorConfigurationImpl config = this.getProcessorConfiguration(configResource);

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
    private synchronized void removeProcessor(String path) {
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
                    ConfigEntry[] newArray = new ConfigEntry[configs.length - 1];
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
                                  ProcessingContext      context) {
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
        try {
            if ( isPipeline ) {
                final PipelineImpl pipeline = new PipelineImpl(this.factoryCache);
                pipeline.init(context, configuration);
                return pipeline;
            }
            final Processor processor = new ProcessorWrapper(configuration, this.factoryCache);
            processor.init(context, configuration);
            return processor;
        } catch (final IOException ioe) {
            throw new SlingException("Unable to setup processor: " + ioe.getMessage(), ioe);
        }
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

    public static final class ConfigEntry {
        public final String path;
        public final ProcessorConfiguration config;

        public ConfigEntry(final String p, final ProcessorConfiguration pc) {
            this.path = p;
            this.config = pc;
        }
    }
}

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
package org.apache.sling.ide.eclipse.core.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.ide.artifacts.EmbeddedArtifactLocator;
import org.apache.sling.ide.eclipse.core.ServiceUtil;
import org.apache.sling.ide.eclipse.core.debug.PluginLoggerRegistrar;
import org.apache.sling.ide.filter.FilterLocator;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.osgi.OsgiClientFactory;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.transport.Batcher;
import org.apache.sling.ide.transport.BatcherFactory;
import org.apache.sling.ide.transport.CommandExecutionProperties;
import org.apache.sling.ide.transport.RepositoryFactory;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator class controls the plug-in life cycle
 * 
 * <p>
 * Since the WST framework is based on Eclipse extension points, rather than OSGi services, this class provides a static
 * entry point to well-known services.
 * </p>
 */
public class Activator extends Plugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.apache.sling.ide.eclipse-core"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

    private ServiceTracker<EventAdmin, EventAdmin> eventAdmin;
    private ServiceTracker<RepositoryFactory, RepositoryFactory> repositoryFactory;
    private ServiceTracker<SerializationManager, SerializationManager> serializationManager;
    private ServiceTracker<FilterLocator, FilterLocator> filterLocator;
    private ServiceTracker<OsgiClientFactory, OsgiClientFactory> osgiClientFactory;
    private ServiceTracker<EmbeddedArtifactLocator, EmbeddedArtifactLocator> artifactLocator;
    private ServiceTracker<?, ?> tracer;
    private ServiceTracker<BatcherFactory, BatcherFactory> batcherFactoryLocator;

    private ServiceRegistration<?> tracerRegistration;

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

        tracerRegistration = PluginLoggerRegistrar.register(this);

        eventAdmin = new ServiceTracker<EventAdmin, EventAdmin>(context, EventAdmin.class,
                null);
        eventAdmin.open();

        repositoryFactory = new ServiceTracker<RepositoryFactory, RepositoryFactory>(context, RepositoryFactory.class,
                null);
        repositoryFactory.open();

        serializationManager = new ServiceTracker<SerializationManager, SerializationManager>(context,
                SerializationManager.class, null);
        serializationManager.open();

        filterLocator = new ServiceTracker<FilterLocator, FilterLocator>(context, FilterLocator.class, null);
        filterLocator.open();

        osgiClientFactory = new ServiceTracker<OsgiClientFactory, OsgiClientFactory>(context, OsgiClientFactory.class,
                null);
        osgiClientFactory.open();

        artifactLocator = new ServiceTracker<EmbeddedArtifactLocator, EmbeddedArtifactLocator>(context,
                EmbeddedArtifactLocator.class, null);
        artifactLocator.open();

        // ugh
        ServiceReference<Object> reference = (ServiceReference<Object>) tracerRegistration.getReference();
        tracer = new ServiceTracker<Object, Object>(context, reference, null);
        tracer.open();
        
        batcherFactoryLocator = new ServiceTracker<BatcherFactory, BatcherFactory>(context, BatcherFactory.class, null);
        batcherFactoryLocator.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {

        tracerRegistration.unregister();

        repositoryFactory.close();
        serializationManager.close();
        filterLocator.close();
        osgiClientFactory.close();
        artifactLocator.close();
        tracer.close();
        batcherFactoryLocator.close();

        plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

    public RepositoryFactory getRepositoryFactory() {

        return ServiceUtil.getNotNull(repositoryFactory);
	}

    public SerializationManager getSerializationManager() {
        return ServiceUtil.getNotNull(serializationManager);
    }

    public FilterLocator getFilterLocator() {
        return ServiceUtil.getNotNull(filterLocator);
    }

    public OsgiClientFactory getOsgiClientFactory() {
        return ServiceUtil.getNotNull(osgiClientFactory);
    }

    public EmbeddedArtifactLocator getArtifactLocator() {

        return ServiceUtil.getNotNull(artifactLocator);
    }

    public Logger getPluginLogger() {
        return (Logger) ServiceUtil.getNotNull(tracer);
    }
    
    public BatcherFactory getBatcherFactory() {
        return (BatcherFactory) ServiceUtil.getNotNull(batcherFactoryLocator);
    }
    
    /**
     * @deprecated This should not be used directly to communicate with the client . There is no direct replacement
     */
    @Deprecated
    public void issueConsoleLog(String actionType, String path, String message) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(CommandExecutionProperties.RESULT_TEXT, message);
        props.put(CommandExecutionProperties.ACTION_TYPE, actionType);
        props.put(CommandExecutionProperties.ACTION_TARGET, path);
        props.put(CommandExecutionProperties.TIMESTAMP_START, System.currentTimeMillis());
        props.put(CommandExecutionProperties.TIMESTAMP_END, System.currentTimeMillis());
        Event event = new Event(CommandExecutionProperties.REPOSITORY_TOPIC, props);
        getEventAdmin().postEvent(event);
    }
    
    public EventAdmin getEventAdmin() {
        return (EventAdmin) ServiceUtil.getNotNull(eventAdmin);
    }
}

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

import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.transport.Repository;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

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

    private Repository repository;
    private SerializationManager serializationManager;
	private Tracer tracer;

    private ServiceReference<Repository> repositoryRef;
    private ServiceReference<SerializationManager> serializationManagerRef;
    private ServiceReference<Tracer> tracerRef;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

        tracerRef = context.getServiceReference(Tracer.class);
        tracer = context.getService(tracerRef);

        repositoryRef = context.getServiceReference(Repository.class);
        repository = context.getService(repositoryRef);

        serializationManagerRef = context.getServiceReference(SerializationManager.class);
        serializationManager = context.getService(serializationManagerRef);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
        context.ungetService(repositoryRef);
        context.ungetService(serializationManagerRef);
        context.ungetService(tracerRef);

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

    public Tracer getTracer() {
        return tracer;
    }

	public Repository getRepository() {
        return repository;
	}

    public SerializationManager getSerializationManager() {
        return serializationManager;
    }
}

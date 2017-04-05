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
package org.apache.sling.commons.fsclassloader.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.apache.sling.commons.classloader.ClassLoaderWriterListener;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.fsclassloader.FSClassLoaderMBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>FSClassLoaderProvider</code> is a dynamic class loader provider
 * which uses the file system to store and read class files from.
 *
 */
@Component(service = ClassLoaderWriter.class, scope = ServiceScope.BUNDLE,
    property = {
            Constants.SERVICE_RANKING + ":Integer=100"
    })
public class FSClassLoaderProvider implements ClassLoaderWriter {

	private static final String LISTENER_FILTER = "(" + Constants.OBJECTCLASS + "="
			+ ClassLoaderWriterListener.class.getName() + ")";

	/** File root */
	private File root;

	/** File root URL */
	private URL rootURL;

	/** Current class loader */
	private FSDynamicClassLoader loader;

	private static ServiceListener classLoaderWriterServiceListener;

	private Map<Long, ServiceReference<ClassLoaderWriterListener>> classLoaderWriterListeners = new HashMap<Long, ServiceReference<ClassLoaderWriterListener>>();

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Reference(service = DynamicClassLoaderManager.class)
	private ServiceReference<DynamicClassLoaderManager> dynamicClassLoaderManager;

	/** The bundle asking for this service instance */
	private Bundle callerBundle;

	private static ServiceRegistration<?> mbeanRegistration;

	/**
	 * Activate this component. Create the root directory.
	 *
	 * @param componentContext
	 * @throws MalformedURLException
	 * @throws InvalidSyntaxException
	 * @throws MalformedObjectNameException
	 */
	@Activate
	protected void activate(final ComponentContext componentContext)
			throws MalformedURLException, InvalidSyntaxException, MalformedObjectNameException {
		// get the file root
		this.root = new File(componentContext.getBundleContext().getDataFile(""), "classes");
		this.root.mkdirs();
		this.rootURL = this.root.toURI().toURL();
		this.callerBundle = componentContext.getUsingBundle();

		classLoaderWriterListeners.clear();
		if (classLoaderWriterServiceListener != null) {
			componentContext.getBundleContext().removeServiceListener(classLoaderWriterServiceListener);
			classLoaderWriterServiceListener = null;
		}
		classLoaderWriterServiceListener = new ServiceListener() {
			@Override
			public void serviceChanged(ServiceEvent event) {
				ServiceReference<ClassLoaderWriterListener> reference = (ServiceReference<ClassLoaderWriterListener>) event
						.getServiceReference();
				if (event.getType() == ServiceEvent.MODIFIED || event.getType() == ServiceEvent.REGISTERED) {
					classLoaderWriterListeners.put(getId(reference), reference);
				} else {
					classLoaderWriterListeners.remove(getId(reference));
				}
			}

			private Long getId(ServiceReference<ClassLoaderWriterListener> reference) {
			    return (Long)reference.getProperty(Constants.SERVICE_ID);
			}
		};
		componentContext.getBundleContext().addServiceListener(classLoaderWriterServiceListener, LISTENER_FILTER);

		// handle the MBean Installation
		if (mbeanRegistration != null) {
			mbeanRegistration.unregister();
			mbeanRegistration = null;
		}
		Hashtable<String, String> jmxProps = new Hashtable<String, String>();
		jmxProps.put("type", "ClassLoader");
		jmxProps.put("name", "FSClassLoader");

		final Hashtable<String, Object> mbeanProps = new Hashtable<String, Object>();
		mbeanProps.put(Constants.SERVICE_DESCRIPTION, "Apache Sling FSClassLoader Controller Service");
		mbeanProps.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
		mbeanProps.put("jmx.objectname", new ObjectName("org.apache.sling.classloader", jmxProps));
		mbeanRegistration = componentContext.getBundleContext().registerService(FSClassLoaderMBean.class.getName(),
				new FSClassLoaderMBeanImpl(this, componentContext.getBundleContext()), mbeanProps);
	}

	/**
	 * Deactivate this component. Create the root directory.
	 */
	@Deactivate
	protected void deactivate(ComponentContext componentContext) {
		this.root = null;
		this.rootURL = null;
		this.destroyClassLoader();
		if (classLoaderWriterServiceListener != null) {
			componentContext.getBundleContext().removeServiceListener(classLoaderWriterServiceListener);
		}
		if (mbeanRegistration != null) {
			mbeanRegistration.unregister();
			mbeanRegistration = null;
		}
	}

	private void destroyClassLoader() {
		final ClassLoader rcl = this.loader;
		if (rcl != null) {
			this.loader = null;

			final ServiceReference<DynamicClassLoaderManager> localDynamicClassLoaderManager = this.dynamicClassLoaderManager;
			final Bundle localCallerBundle = this.callerBundle;
			if (localDynamicClassLoaderManager != null && localCallerBundle != null) {
				localCallerBundle.getBundleContext().ungetService(localDynamicClassLoaderManager);
			}
		}
	}

	/**
	 * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getClassLoader()
	 */
	@Override
    public ClassLoader getClassLoader() {
		synchronized (this) {
			if (loader == null || !loader.isLive()) {
				this.destroyClassLoader();
				// get the dynamic class loader for the bundle using this
				// class loader writer
				final DynamicClassLoaderManager dclm = this.callerBundle.getBundleContext()
						.getService(this.dynamicClassLoaderManager);

				loader = new FSDynamicClassLoader(new URL[] { this.rootURL }, dclm.getDynamicClassLoader());
			}
			return this.loader;
		}
	}

	private void checkClassLoader(final String filePath) {
		if (filePath.endsWith(".class")) {
			// remove store directory and .class
			final String path = filePath.substring(this.root.getAbsolutePath().length() + 1, filePath.length() - 6);
			// convert to a class name
			final String className = path.replace(File.separatorChar, '.');

			synchronized (this) {
				final FSDynamicClassLoader currentLoader = this.loader;
				if (currentLoader != null) {
					currentLoader.check(className);
				}
			}
		}
	}

	// ---------- SCR Integration ----------------------------------------------

	private boolean deleteRecursive(final File f, final List<String> names) {
		if (f.isDirectory()) {
			for (final File c : f.listFiles()) {
				if (!deleteRecursive(c, names)) {
					return false;
				}
			}
		}
		names.add(f.getAbsolutePath());
		return f.delete();
	}

	/**
	 * @see org.apache.sling.commons.classloader.ClassLoaderWriter#delete(java.lang.String)
	 */
	@Override
    public boolean delete(final String name) {
		final String path = cleanPath(name);
		final File file = new File(path);
		if (file.exists()) {
			final List<String> names = new ArrayList<String>();
			final boolean result = deleteRecursive(file, names);
			logger.debug("Deleted {} : {}", name, result);
			if (result) {
				for (final String n : names) {
					this.checkClassLoader(n);
				}
				for (ServiceReference<ClassLoaderWriterListener> reference : classLoaderWriterListeners.values()) {
					if (reference != null) {
						ClassLoaderWriterListener listener = callerBundle.getBundleContext().getService(reference);
						if (listener != null) {
							listener.onClassLoaderClear(name);
						} else {
							logger.warn("Found ClassLoaderWriterListener Service reference with no service bound");
						}
					}
				}
			}

			return result;
		}
		// file does not exist so we return false
		return false;
	}

	/**
	 * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getOutputStream(java.lang.String)
	 */
	@Override
    public OutputStream getOutputStream(final String name) {
		logger.debug("Get stream for {}", name);
		final String path = cleanPath(name);
		final File file = new File(path);
		final File parentDir = file.getParentFile();
		if (!parentDir.exists()) {
			parentDir.mkdirs();
		}
		try {
			if (file.exists()) {
				this.checkClassLoader(path);
			}
			return new FileOutputStream(path);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @see org.apache.sling.commons.classloader.ClassLoaderWriter#rename(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
    public boolean rename(final String oldName, final String newName) {
		logger.debug("Rename {} to {}", oldName, newName);
		final String oldPath = cleanPath(oldName);
		final String newPath = cleanPath(newName);
		final File old = new File(oldPath);
		final boolean result = old.renameTo(new File(newPath));
		if (result) {
			this.checkClassLoader(oldPath);
			this.checkClassLoader(newPath);
		}
		return result;
	}

	/**
	 * Clean the path by converting slashes to the correct format and prefixing
	 * the root directory.
	 *
	 * @param path
	 *            The path
	 * @return The file path
	 */
	private String cleanPath(String path) {
		// replace backslash by slash
		path = path.replace('\\', '/');

		// cut off trailing slash
		while (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		if (File.separatorChar != '/') {
			path = path.replace('/', File.separatorChar);
		}
		return this.root.getAbsolutePath() + path;
	}

	/**
	 * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getInputStream(java.lang.String)
	 */
	@Override
    public InputStream getInputStream(final String name) throws IOException {
		logger.debug("Get input stream of {}", name);
		final String path = cleanPath(name);
		final File file = new File(path);
		return new FileInputStream(file);
	}

	/**
	 * @see org.apache.sling.commons.classloader.ClassLoaderWriter#getLastModified(java.lang.String)
	 */
	@Override
    public long getLastModified(final String name) {
		logger.debug("Get last modified of {}", name);
		final String path = cleanPath(name);
		final File file = new File(path);
		if (file.exists()) {
			return file.lastModified();
		}

		// fallback to "non-existant" in case of problems
		return -1;
	}
}

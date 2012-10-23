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

package org.apache.sling.jcr.jackrabbit.base.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.jackrabbit.core.config.BeanConfig;
import org.apache.jackrabbit.core.config.BeanConfigVisitor;
import org.apache.jackrabbit.core.config.BeanFactory;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.apache.jackrabbit.core.config.SimpleBeanFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;


public class OsgiBeanFactory implements BeanFactory, ServiceTrackerCustomizer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final BeanFactory delegate = new SimpleBeanFactory();
    private final BundleContext bundleContext;

    /**
     * Tracker to track all services which are possible Jackrabbit extensions
     */
    private final ServiceTracker tracker;

    /**
     * Set of all interface class instances for which actual instances need to
     * be lookedup from OSGi Service Registry
     */
    private final Set<Class> dependencies = new HashSet<Class>();

    /**
     * Map of className to class instances
     */
    private final Map<String, Class> classNameMapping = new HashMap<String, Class>();

    /**
     * Map of the interface name -> instance where the instance provides an implementation
     * of the given interface
     */
    private final Map<Class, Object> instanceMap = new ConcurrentHashMap<Class, Object>();

    private ServiceRegistration beanFactoryReg;

    public OsgiBeanFactory(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        Filter filter = null;
        try {
            filter = bundleContext.createFilter("(jackrabbit.extension=true)");
        } catch (InvalidSyntaxException e) {
            //Should not happen
            throw new RuntimeException("Invalid filter", e);
        }
        this.tracker = new ServiceTracker(bundleContext, filter, this);
    }

    public void initialize(InputSource configSource) throws IOException, ConfigurationException {
        determineDependencies(configSource);
        createClassNameMappings();
        tracker.open();
        checkState();
    }

    public void close() {
        if (beanFactoryReg != null) {
            beanFactoryReg.unregister();
            beanFactoryReg = null;
        }
        tracker.close();
        dependencies.clear();
        instanceMap.clear();
        classNameMapping.clear();
    }

    //-----------------------------------------------< BeanFactory >

    public Object newInstance(Class<?> clazz, BeanConfig config) throws ConfigurationException {
        Class targetClass = getClassFromConfig(config);
        if (targetClass.isInterface()) {
            Object o = instanceMap.get(targetClass);
            if (o == null) {
                throw new ConfigurationException("No instance registered for type " + targetClass.getName());
            }
            return o;
        }
        return delegate.newInstance(clazz, config);
    }

    //-----------------------------------------------< ServiceTrackerCustomizer >

    public Object addingService(ServiceReference reference) {
        Object instance = bundleContext.getService(reference);
        Class[] depsProvided = determineProvidedDependencies(reference);
        registerInstance(depsProvided, instance);
        checkState();
        return depsProvided;
    }

    public void modifiedService(ServiceReference serviceReference, Object o) {

    }

    public void removedService(ServiceReference reference, Object o) {
        deregisterInstance((Class[]) o);
        checkState();
        bundleContext.ungetService(reference);
    }

    //------------------------- Callback methods

    /**
     * Callback method invoked when all services required by Jackrabbit are available. Implementing class
     * can use this to manage repository lifecycle. Default implementation registers the BeanFactory
     * instance which would be used by the Repository creator to create the repository
     */
    protected void dependenciesSatisfied() {
        //TODO Review the thread safety aspect
        ServiceRegistration reg = beanFactoryReg;
        if (reg == null) {
            beanFactoryReg = bundleContext.registerService(BeanFactory.class.getName(), this, new Properties());
            log.info("All dependencies are satisfied. Registering the BeanFactory instance");
        }
    }

    /**
     * Callback method invoked when any of the service required by Jackrabbit goes away. Implementing class
     * can use this to manage repository lifecycle. Default implementation de-registers the BeanFactory
     * instance. And repository creator service which then depends on BeanFactory reference would then be notified and
     * can react accordingly
     */
    protected void dependenciesUnSatisfied() {
        ServiceRegistration reg = beanFactoryReg;
        if (reg != null) {
            reg.unregister();
            beanFactoryReg = null;
            log.info("Dependencies unsatisfied. Deregistering the BeanFactory instance");
        }
    }

    private Class getClassFromConfig(BeanConfig config) {
        String cname = config.getClassName();
        try {
            return config.getClassLoader().loadClass(cname);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load class for " + cname, e);
        }
    }

    private void checkState() {
        if (instanceMap.size() == dependencies.size()) {
            dependenciesSatisfied();
        } else {
            dependenciesUnSatisfied();
        }
    }

    private void determineDependencies(InputSource source) throws ConfigurationException, IOException {
        Properties p = new Properties();
        p.putAll(System.getProperties());
        p.setProperty(RepositoryConfigurationParser.REPOSITORY_HOME_VARIABLE, "/fake/path");
        RepositoryConfigurationParser parser = new RepositoryConfigurationParser(p);
        parser.setConfigVisitor(new DepFinderBeanConfigVisitor());

        try {
            parser.parseRepositoryConfig(source);
        } finally {
            InputStream is = source.getByteStream();
            if (is != null) {
                is.close();
            }
        }

        if (dependencies.isEmpty()) {
            log.info("No dependencies configured. Repository would be created without any OSGi dependency getting injected");
            return;
        }

        log.info("Following dependencies have been determined for the repository {}. Repository would be started " +
                "once all these dependencies have been satisfied", dependencies);
    }

    private void registerInstance(Class[] depsProvided, Object o) {
        for (Class c : depsProvided) {
            instanceMap.put(c, o);
        }
    }

    private void deregisterInstance(Class[] depsProvided) {
        for (Class c : depsProvided) {
            instanceMap.remove(c);
        }
    }

    /**
     * Determines all the dependencies which this ServiceReference can satisfy
     */
    private Class[] determineProvidedDependencies(ServiceReference ref) {
        //Use OBJECTCLASS property from SR as that determines under what classes
        //a given service instance is published
        //Class[] interfaces = o.getClass().getInterfaces();
        String[] interfaces = (String[]) ref.getProperty(Constants.OBJECTCLASS);
        List<Class> depsProvided = new ArrayList<Class>(interfaces.length);
        for (String intf : interfaces) {
            if (classNameMapping.containsKey(intf)) {
                depsProvided.add(classNameMapping.get(intf));
            }
        }
        return depsProvided.toArray(new Class[depsProvided.size()]);
    }

    private void createClassNameMappings() {
        for (Class clazz : dependencies) {
            classNameMapping.put(clazz.getName(), clazz);
        }
    }

    private class DepFinderBeanConfigVisitor implements BeanConfigVisitor {

        public void visit(BeanConfig config) {
            Class clazz = getClassFromConfig(config);
            if (clazz.isInterface()) {
                dependencies.add(clazz);
            }
        }
    }
}

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
package org.apache.sling.testing.mock.osgi;

import static org.apache.sling.testing.mock.osgi.MapMergeUtil.propertiesMergeWithOsgiMetadata;
import static org.apache.sling.testing.mock.osgi.MapUtil.toDictionary;
import static org.apache.sling.testing.mock.osgi.MapUtil.toMap;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

/**
 * Factory for mock OSGi objects.
 */
public final class MockOsgi {

    private MockOsgi() {
        // static methods only
    }

    /**
     * @return Mocked {@link BundleContext} instance
     */
    public static BundleContext newBundleContext() {
        return new MockBundleContext();
    }

    /**
     * Simulates a bundle event on the given bundle context (that is forwarded
     * to registered bundle listeners).
     * @param bundleContext Bundle context
     * @param bundleEvent Bundle event
     */
    public static void sendBundleEvent(BundleContext bundleContext, BundleEvent bundleEvent) {
        ((MockBundleContext) bundleContext).sendBundleEvent(bundleEvent);
    }

    /**
     * @return Mocked {@link ComponentContext} instance
     */
    public static ComponentContext newComponentContext() {
        return componentContext().build();
    }

    /**
     * @param properties Properties
     * @return Mocked {@link ComponentContext} instance
     */
    public static ComponentContext newComponentContext(Dictionary<String, Object> properties) {
        return componentContext().properties(properties).build();
    }

    /**
     * @param properties Properties
     * @return Mocked {@link ComponentContext} instance
     */
    public static ComponentContext newComponentContext(Map<String, Object> properties) {
        return componentContext().properties(properties).build();
    }

    /**
     * @param properties Properties
     * @return Mocked {@link ComponentContext} instance
     */
    public static ComponentContext newComponentContext(Object... properties) {
        return componentContext().properties(properties).build();
    }

    /**
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return Mocked {@link ComponentContext} instance
     */
    public static ComponentContext newComponentContext(BundleContext bundleContext,
            Dictionary<String, Object> properties) {
        return componentContext().bundleContext(bundleContext).properties(properties).build();
    }

    /**
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return Mocked {@link ComponentContext} instance
     */
    public static ComponentContext newComponentContext(BundleContext bundleContext,
            Map<String, Object> properties) {
        return componentContext().bundleContext(bundleContext).properties(properties).build();
    }

    /**
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return Mocked {@link ComponentContext} instance
     */
    public static ComponentContext newComponentContext(BundleContext bundleContext,
            Object... properties) {
        return componentContext().bundleContext(bundleContext).properties(properties).build();
    }

    /**
     * @return {@link ComponentContextBuilder} to build a mocked {@link ComponentContext}
     */
    public static ComponentContextBuilder componentContext() {
        return new ComponentContextBuilder();
    }
    
    /**
     * @param loggerContext Context class for logging
     * @return Mocked {@link LogService} instance
     */
    public static LogService newLogService(final Class<?> loggerContext) {
        return new MockLogService(loggerContext);
    }

    /**
     * Simulate OSGi service dependency injection. Injects direct references and
     * multiple references. If a some references could not be injected no error
     * is thrown.
     * @param target Service instance
     * @param bundleContext Bundle context from which services are fetched to inject.
     * @return true if all dependencies could be injected, false if the service has no dependencies.
     */
    public static boolean injectServices(Object target, BundleContext bundleContext) {
        return OsgiServiceUtil.injectServices(target, bundleContext);
    }

    /**
     * Simulate activation of service instance. Invokes the @Activate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @return true if activation method was called. False if no activate method is defined.
     */
    public static boolean activate(Object target, BundleContext bundleContext) {
        return MockOsgi.activate(target, bundleContext, (Dictionary<String, Object>)null);
    }

    /**
     * Simulate activation of service instance. Invokes the @Activate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if activation method was called. False if no activate method is defined.
     */
    public static boolean activate(Object target, BundleContext bundleContext, Dictionary<String, Object> properties) {
        Dictionary<String, Object> mergedProperties = propertiesMergeWithOsgiMetadata(target, getConfigAdmin(bundleContext), properties);
        ComponentContext componentContext = newComponentContext(bundleContext, mergedProperties);
        return OsgiServiceUtil.activateDeactivate(target, componentContext, true);
    }

    /**
     * Simulate activation of service instance. Invokes the @Activate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if activation method was called. False if no activate method is defined.
     */
    public static boolean activate(Object target, BundleContext bundleContext, Map<String, Object> properties) {
        return activate(target, bundleContext, toDictionary(properties));
    }

    /**
     * Simulate activation of service instance. Invokes the @Activate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if activation method was called. False if no activate method is defined.
     */
    public static boolean activate(Object target, BundleContext bundleContext, Object... properties) {
        return activate(target, bundleContext, toDictionary(properties));
    }

    /**
     * Simulate deactivation of service instance. Invokes the @Deactivate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context.
     * @return true if deactivation method was called. False if no deactivate method is defined.
     */
    public static boolean deactivate(Object target, BundleContext bundleContext) {
        return MockOsgi.deactivate(target, bundleContext, (Dictionary<String, Object>)null);
    }

    /**
     * Simulate deactivation of service instance. Invokes the @Deactivate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if deactivation method was called. False if no deactivate method is defined.
     */
    public static boolean deactivate(Object target, BundleContext bundleContext, Dictionary<String, Object> properties) {
        Dictionary<String, Object> mergedProperties = propertiesMergeWithOsgiMetadata(target, getConfigAdmin(bundleContext), properties);
        ComponentContext componentContext = newComponentContext(bundleContext, mergedProperties);
        return OsgiServiceUtil.activateDeactivate(target, componentContext, false);
    }

    /**
     * Simulate activation of service instance. Invokes the @Deactivate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if deactivation method was called. False if no deactivate method is defined.
     */
    public static boolean deactivate(Object target, BundleContext bundleContext, Map<String, Object> properties) {
        return deactivate(target, bundleContext, toDictionary(properties));
    }

    /**
     * Simulate activation of service instance. Invokes the @Deactivate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if deactivation method was called. False if no deactivate method is defined.
     */
    public static boolean deactivate(Object target, BundleContext bundleContext, Object... properties) {
        return deactivate(target, bundleContext, toDictionary(properties));
    }

    /**
     * Simulate configuration modification of service instance. Invokes the @Modified annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if modified method was called. False if no modified method is defined.
     */
    public static boolean modified(Object target, BundleContext bundleContext, Dictionary<String, Object> properties) {
        return modified(target, bundleContext, toMap(properties));
    }

    /**
     * Simulate configuration modification of service instance. Invokes the @Modified annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if modified method was called. False if no modified method is defined.
     */
    public static boolean modified(Object target, BundleContext bundleContext, Map<String, Object> properties) {
        Map<String, Object> mergedProperties = propertiesMergeWithOsgiMetadata(target, getConfigAdmin(bundleContext), properties);
        ComponentContext componentContext = newComponentContext(bundleContext, mergedProperties);
        return OsgiServiceUtil.modified(target, componentContext, mergedProperties);
    }
    
    /**
     * Simulate configuration modification of service instance. Invokes the @Modified annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if modified method was called. False if no modified method is defined.
     */
    public static boolean modified(Object target, BundleContext bundleContext, Object... properties) {
        return modified(target, bundleContext, toDictionary(properties));
    }
    
    /**
     * Set configuration via ConfigurationAdmin service in bundle context for component with given pid.
     * @param bundleContext Bundle context
     * @param pid PID
     * @param properties Configuration properties
     */
    public static void setConfigForPid(BundleContext bundleContext, String pid, Map<String,Object> properties) {
        setConfigForPid(bundleContext, pid, toDictionary(properties));
    }
    
    /**
     * Set configuration via ConfigurationAdmin service in bundle context for component with given pid.
     * @param bundleContext Bundle context
     * @param pid PID
     * @param properties Configuration properties
     */
    public static void setConfigForPid(BundleContext bundleContext, String pid, Object... properties) {
        setConfigForPid(bundleContext, pid, toDictionary(properties));
    }
    
    private static void setConfigForPid(BundleContext bundleContext, String pid, Dictionary<String, Object> properties) {
        ConfigurationAdmin configAdmin = getConfigAdmin(bundleContext);
        if (configAdmin == null) {
            throw new RuntimeException("ConfigurationAdmin service is not registered in bundle context.");
        }
        try {
            Configuration config = configAdmin.getConfiguration(pid);
            config.update(properties);
        }
        catch (IOException ex) {
            throw new RuntimeException("Unable to update configuration for pid '" + pid + "'.", ex);
        }
    }
    
    /**
     * Deactivates all bundles registered in the mocked bundle context.
     * @param bundleContext Bundle context
     */
    public static void shutdown(BundleContext bundleContext) {
        ((MockBundleContext)bundleContext).shutdown();
    }
    
    /**
     * Get configuration admin.
     * @param bundleContext Bundle context
     * @return Configuration admin or null if not registered.
     */
    private static ConfigurationAdmin getConfigAdmin(BundleContext bundleContext) {
        ServiceReference<?> ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        if (ref != null) {
            return (ConfigurationAdmin)bundleContext.getService(ref);
        }
        return null;
    }
    
}

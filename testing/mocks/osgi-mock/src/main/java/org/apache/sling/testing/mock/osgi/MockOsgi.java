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

import static org.apache.sling.testing.mock.osgi.MapUtil.propertiesMergeWithOsgiMetadata;
import static org.apache.sling.testing.mock.osgi.MapUtil.toDictionary;
import static org.apache.sling.testing.mock.osgi.MapUtil.toMap;

import java.util.Dictionary;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
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
    public static ComponentContext newComponentContext(BundleContext bundleContext, Map<String, Object> properties) {
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
     * @return true if activation method was called. False if no activate method is defined.
     * @deprecated Please use {@link #activate(Object, BundleContext)}
     *   and shutdown the bundle context after usage.
     */
    @Deprecated
    public static boolean activate(Object target) {
        return MockOsgi.activate(target, (Dictionary<String, Object>)null);
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
     * @param properties Properties
     * @return true if activation method was called. False if no activate method is defined.
     * @deprecated Please use {@link #activate(Object, BundleContext, Dictionary)}
     *   and shutdown the bundle context after usage.
     */
    @Deprecated
    public static boolean activate(Object target, Dictionary<String, Object> properties) {
        Dictionary<String, Object> mergedProperties = propertiesMergeWithOsgiMetadata(target, properties);
        ComponentContext componentContext = newComponentContext(mergedProperties);
        return OsgiServiceUtil.activateDeactivate(target, componentContext, true);
    }

    /**
     * Simulate activation of service instance. Invokes the @Activate annotated method.
     * @param target Service instance.
     * @param properties Properties
     * @return true if activation method was called. False if no activate method is defined.
     * @deprecated Please use {@link #activate(Object, BundleContext, Map)}
     *   and shutdown the bundle context after usage.
     */
    @Deprecated
    public static boolean activate(Object target, Map<String, Object> properties) {
        return activate(target, toDictionary(properties));
    }

    /**
     * Simulate activation of service instance. Invokes the @Activate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if activation method was called. False if no activate method is defined.
     */
    public static boolean activate(Object target, BundleContext bundleContext, Dictionary<String, Object> properties) {
        Dictionary<String, Object> mergedProperties = propertiesMergeWithOsgiMetadata(target, properties);
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
     * Simulate deactivation of service instance. Invokes the @Deactivate annotated method.
     * @param target Service instance.
     * @return true if deactivation method was called. False if no deactivate method is defined.
     * @deprecated Please use {@link #deactivate(Object, BundleContext)}
     *   and shutdown the bundle context after usage.
     */
    @Deprecated
    public static boolean deactivate(Object target) {
        return MockOsgi.deactivate(target, (Dictionary<String, Object>)null);
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
     * @param properties Properties
     * @return true if deactivation method was called. False if no deactivate method is defined.
     * @deprecated Please use {@link #deactivate(Object, BundleContext, Dictionary)}
     *   and shutdown the bundle context after usage.
     */
    @Deprecated
    public static boolean deactivate(Object target, Dictionary<String, Object> properties) {
        Dictionary<String, Object> mergedProperties = propertiesMergeWithOsgiMetadata(target, properties);
        ComponentContext componentContext = newComponentContext(mergedProperties);
        return OsgiServiceUtil.activateDeactivate(target, componentContext, false);
    }

    /**
     * Simulate deactivation of service instance. Invokes the @Deactivate annotated method.
     * @param target Service instance.
     * @param properties Properties
     * @return true if deactivation method was called. False if no deactivate method is defined.
     * @deprecated Please use {@link #deactivate(Object, BundleContext, Map)}
     *   and shutdown the bundle context after usage.
     */
    @Deprecated
    public static boolean deactivate(Object target, Map<String, Object> properties) {
        return deactivate(target, toDictionary(properties));
    }

    /**
     * Simulate deactivation of service instance. Invokes the @Deactivate annotated method.
     * @param target Service instance.
     * @param bundleContext Bundle context
     * @param properties Properties
     * @return true if deactivation method was called. False if no deactivate method is defined.
     */
    public static boolean deactivate(Object target, BundleContext bundleContext, Dictionary<String, Object> properties) {
        Dictionary<String, Object> mergedProperties = propertiesMergeWithOsgiMetadata(target, properties);
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
        Map<String, Object> mergedProperties = propertiesMergeWithOsgiMetadata(target, properties);
        return OsgiServiceUtil.modified(target, bundleContext, mergedProperties);
    }
    
    /**
     * Deactivates all bundles registered in the mocked bundle context.
     * @param bundleContext Bundle context
     */
    public static void shutdown(BundleContext bundleContext) {
        ((MockBundleContext)bundleContext).shutdown();
    }
    
}

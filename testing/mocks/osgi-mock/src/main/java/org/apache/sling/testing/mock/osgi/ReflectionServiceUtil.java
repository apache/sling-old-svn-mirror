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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.Reference;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.w3c.dom.Document;

/**
 * Helper methods to inject dependencies and activate services via reflection.
 */
final class ReflectionServiceUtil {

    private ReflectionServiceUtil() {
        // static methods only
    }

    /**
     * Simulate activation or deactivation of OSGi service instance.
     * @param target Service instance.
     * @param componentContext Component context
     * @return true if activation/deactivation method was called. False if it failed.
     */
    @SuppressWarnings("unchecked")
    public static boolean activateDeactivate(Object target, ComponentContext componentContext, boolean activate) {
        Class<?> targetClass = target.getClass();

        // get method name for activation/deactivation from osgi metadata
        Document metadata = OsgiMetadataUtil.getMetadata(targetClass);
        if (metadata==null) {
            throw new NoScrMetadataException(targetClass);
        }
        String methodName;
        if (activate) {
            methodName = OsgiMetadataUtil.getActivateMethodName(targetClass, metadata);
        } else {
            methodName = OsgiMetadataUtil.getDeactivateMethodName(targetClass, metadata);
        }
        if (StringUtils.isEmpty(methodName)) {
            return false;
        }

        // try to find matching activate/deactivate method and execute it
        
        // 1. componentContext
        Method method = getMethod(targetClass, methodName, new Class<?>[] { ComponentContext.class });
        if (method != null) {
            invokeMethod(target, method, new Object[] { componentContext });
            return true;
        }
        
        // 2. bundleContext
        method = getMethod(targetClass, methodName, new Class<?>[] { BundleContext.class });
        if (method != null) {
            invokeMethod(target, method, new Object[] { componentContext.getBundleContext() });
            return true;
        }
        
        // 3. map
        method = getMethod(targetClass, methodName, new Class<?>[] { Map.class });
        if (method != null) {
            invokeMethod(target, method, new Object[] { MapUtil.toMap(componentContext.getProperties()) });
            return true;
        }
        
        // 4. int (deactivation only)
        if (!activate) {
            method = getMethod(targetClass, methodName, new Class<?>[] { int.class });
            if (method != null) {
                invokeMethod(target, method, new Object[] { 0 });
                return true;
            }
        }
        
        // 5. Integer (deactivation only)
        if (!activate) {
            method = getMethod(targetClass, methodName, new Class<?>[] { Integer.class });
            if (method != null) {
                invokeMethod(target, method, new Object[] { 0 });
                return true;
            }
        }
        
        // 6. mixed arguments of componentContext, bundleContext and map
        Class<?>[] mixedArgsAllowed = activate ? new Class<?>[] { ComponentContext.class, BundleContext.class, Map.class }
                : new Class<?>[] { ComponentContext.class, BundleContext.class, Map.class, int.class, Integer.class };
        method = getMethodWithAnyCombinationArgs(targetClass, methodName, mixedArgsAllowed);
        if (method != null) {
            Object[] args = new Object[method.getParameterTypes().length];
            for (int i=0; i<args.length; i++) {
                if (method.getParameterTypes()[i] == ComponentContext.class) {
                    args[i] = componentContext;
                }
                else if (method.getParameterTypes()[i] == BundleContext.class) {
                    args[i] = componentContext.getBundleContext();
                }
                else if (method.getParameterTypes()[i] == Map.class) {
                    args[i] = MapUtil.toMap(componentContext.getProperties());
                }
                else if (method.getParameterTypes()[i] == int.class || method.getParameterTypes()[i] == Integer.class) {
                    args[i] = 0;
                }
            }
            invokeMethod(target, method, args);
            return true;
        }

        // 7. noargs
        method = getMethod(targetClass, methodName, new Class<?>[0]);
        if (method != null) {
            invokeMethod(target, method, new Object[0]);
            return true;
        }
        
        throw new RuntimeException("No matching " + (activate ? "activation" : "deactivation") + " method with name '" + methodName + "' "
                + " found in class " + targetClass.getName());
    }

    /**
     * Simulate modification of configuration of OSGi service instance.
     * @param target Service instance.
     * @param properties Updated configuration
     * @return true if modified method was called. False if it failed.
     */
    public static boolean modified(Object target, BundleContext bundleContext, Map<String,Object> properties) {
        Class<?> targetClass = target.getClass();

        // get method name for activation/deactivation from osgi metadata
        Document metadata = OsgiMetadataUtil.getMetadata(targetClass);
        if (metadata==null) {
            throw new NoScrMetadataException(targetClass);
        }
        String methodName = OsgiMetadataUtil.getModifiedMethodName(targetClass, metadata);
        if (StringUtils.isEmpty(methodName)) {
            return false;
        }
        
        // try to find matching modified method and execute it
        Method method = getMethod(targetClass, methodName, new Class<?>[] { Map.class });
        if (method != null) {
            invokeMethod(target, method, new Object[] { properties });
            return true;
        }
        
        throw new RuntimeException("No matching modified method with name '" + methodName + "' "
                + " found in class " + targetClass.getName());
    }

    private static Method getMethod(Class clazz, String methodName, Class<?>[] types) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (StringUtils.equals(method.getName(), methodName)
                    && Arrays.equals(method.getParameterTypes(), types)) {
                return method;
            }
        }
        // not found? check super classes
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return getMethod(superClass, methodName, types);
        }
        return null;
    }
    
    private static Method getMethodWithAssignableTypes(Class clazz, String methodName, Class<?>[] types) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (StringUtils.equals(method.getName(), methodName) && method.getParameterTypes().length==types.length) {
                boolean foundMismatch = false;
                for (int i=0; i<types.length; i++) {
                    if (!method.getParameterTypes()[i].isAssignableFrom(types[i])) {
                        foundMismatch = false;
                        break;
                    }
                }
                if (!foundMismatch) {
                    return method;
                }
            }
        }
        // not found? check super classes
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return getMethodWithAssignableTypes(superClass, methodName, types);
        }
        return null;
    }
    
    private static Method getMethodWithAnyCombinationArgs(Class clazz, String methodName, Class<?>[] types) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (StringUtils.equals(method.getName(), methodName) && method.getParameterTypes().length > 1) {
                for (Class<?> parameterType : method.getParameterTypes()) {
                    if (!ArrayUtils.contains(types,  parameterType)) {
                        return null;
                    }
                }
                return method;
            }
        }
        // not found? check super classes
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return getMethodWithAnyCombinationArgs(superClass, methodName, types);
        }
        return null;
    }
    
    private static void invokeMethod(Object target, Method method, Object[] args) {
        try {
            method.setAccessible(true);
            method.invoke(target, args);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Unable to invoke activate/deactivate method for class "
                    + target.getClass().getName(), ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Unable to invoke activate/deactivate method for class "
                    + target.getClass().getName(), ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("Unable to invoke activate/deactivate method for class "
                    + target.getClass().getName(), ex.getCause());
        }
    }

    /**
     * Simulate OSGi service dependency injection. Injects direct references and
     * multiple references.
     * @param target Service instance
     * @param bundleContext Bundle context from which services are fetched to inject.
     * @return true if all dependencies could be injected, false if the service has no dependencies.
     */
    public static boolean injectServices(Object target, BundleContext bundleContext) {

        // collect all declared reference annotations on class and field level
        Class<?> targetClass = target.getClass();

        Document metadata = OsgiMetadataUtil.getMetadata(targetClass);
        if (metadata==null) {
            throw new NoScrMetadataException(targetClass);
        }
        List<Reference> references = OsgiMetadataUtil.getReferences(targetClass, metadata);
        if (references.isEmpty()) {
            return false;
        }

        // try to inject services
        for (Reference reference : references) {
            injectServiceReference(reference, target, bundleContext);
        }
        return true;
    }

    private static void injectServiceReference(Reference reference, Object target, BundleContext bundleContext) {
        Class<?> targetClass = target.getClass();

        // get reference type
        Class<?> type;
        try {
            type = Class.forName(reference.getInterfaceType());
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Unable to instantiate reference type: " + reference.getInterfaceType(), ex);
        }

        // get matching service references
        List<ServiceInfo> matchingServices = getMatchingServices(type, bundleContext);

        // no references found? check if reference was optional
        if (matchingServices.isEmpty()) {
            boolean isOptional = (reference.getCardinality() == ReferenceCardinality.OPTIONAL_UNARY || reference
                    .getCardinality() == ReferenceCardinality.OPTIONAL_MULTIPLE);
            if (!isOptional) {
                throw new RuntimeException("Unable to inject mandatory reference '" + reference.getName() + "' for class " + targetClass.getName());
            }
        }

        // multiple references found? check if reference is not multiple
        if (matchingServices.size() > 1
                && (reference.getCardinality() == ReferenceCardinality.MANDATORY_UNARY || reference.getCardinality() == ReferenceCardinality.OPTIONAL_UNARY)) {
            throw new RuntimeException("Multiple matches found for unary reference '" + reference.getName() + "' for class "+ targetClass.getName());
        }

        // try to invoke bind method
        String bindMethodName = reference.getBind();
        if (StringUtils.isNotEmpty(bindMethodName)) {
            
            // 1. ServiceReference
            Method bindMethod = getMethod(targetClass, bindMethodName, new Class<?>[] { ServiceReference.class });
            if (bindMethod != null) {
                for (ServiceInfo matchingService : matchingServices) {
                    invokeMethod(target, bindMethod, new Object[] { matchingService.getServiceReference() });
                }
                return;
            }
            
            // 2. assignable from service instance
            Class<?> interfaceType;
            try {
                interfaceType = Class.forName(reference.getInterfaceType());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Service reference type not found: " + reference.getInterfaceType());
            }
            bindMethod = getMethodWithAssignableTypes(targetClass, bindMethodName, new Class<?>[] { interfaceType });
            if (bindMethod != null) {
                for (ServiceInfo matchingService : matchingServices) {
                    invokeMethod(target, bindMethod, new Object[] { matchingService.getServiceInstance() });
                }
                return;
            }
            
            // 3. assignable from service instance plus map
            bindMethod = getMethodWithAssignableTypes(targetClass, bindMethodName, new Class<?>[] { interfaceType, Map.class });
            if (bindMethod != null) {
                for (ServiceInfo matchingService : matchingServices) {
                    invokeMethod(target, bindMethod, new Object[] { matchingService.getServiceInstance(), matchingService.getServiceConfig() });
                }
                return;
            }
        }

        throw new RuntimeException("Bind method with name " + bindMethodName + " not found "
                + "for reference '" + reference.getName() + "' for class {}" +  targetClass.getName());
    }

    private static List<ServiceInfo> getMatchingServices(Class<?> type, BundleContext bundleContext) {
        List<ServiceInfo> matchingServices = new ArrayList<ServiceInfo>();
        try {
            ServiceReference[] references = bundleContext.getServiceReferences(type.getName(), null);
            if (references != null) {
                for (ServiceReference serviceReference : references) {
                    Object serviceInstance = bundleContext.getService(serviceReference);
                    Map<String, Object> serviceConfig = new HashMap<String, Object>();
                    String[] keys = serviceReference.getPropertyKeys();
                    for (String key : keys) {
                        serviceConfig.put(key, serviceReference.getProperty(key));
                    }
                    matchingServices.add(new ServiceInfo(serviceInstance, serviceConfig, serviceReference));
                }
            }
        } catch (InvalidSyntaxException ex) {
            // ignore
        }
        return matchingServices;
    }

    private static class ServiceInfo {

        private final Object serviceInstance;
        private final Map<String, Object> serviceConfig;
        private final ServiceReference serviceReference;

        public ServiceInfo(Object serviceInstance, Map<String, Object> serviceConfig, ServiceReference serviceReference) {
            this.serviceInstance = serviceInstance;
            this.serviceConfig = serviceConfig;
            this.serviceReference = serviceReference;
        }

        public Object getServiceInstance() {
            return this.serviceInstance;
        }

        public Map<String, Object> getServiceConfig() {
            return this.serviceConfig;
        }

        public ServiceReference getServiceReference() {
            return serviceReference;
        }

    }

}

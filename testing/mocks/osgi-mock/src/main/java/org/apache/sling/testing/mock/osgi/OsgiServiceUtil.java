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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.impl.inject.Annotations;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.FieldCollectionType;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.OsgiMetadata;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.Reference;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.ReferencePolicy;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * Helper methods to inject dependencies and activate services.
 */
final class OsgiServiceUtil {

    private OsgiServiceUtil() {
        // static methods only
    }

    /**
     * Simulate activation or deactivation of OSGi service instance.
     * @param target Service instance.
     * @param componentContext Component context
     * @return true if activation/deactivation method was called. False if it failed.
     */
    public static boolean activateDeactivate(Object target, ComponentContext componentContext, boolean activate) {
        Class<?> targetClass = target.getClass();

        // get method name for activation/deactivation from osgi metadata
        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(targetClass);
        if (metadata == null) {
            throw new NoScrMetadataException(targetClass);
        }
        String methodName;
        if (activate) {
            methodName = metadata.getActivateMethodName();
        } else {
            methodName = metadata.getDeactivateMethodName();
        }
        boolean fallbackDefaultName = false;
        if (StringUtils.isEmpty(methodName)) {
            fallbackDefaultName = true;
            if (activate) {
                methodName = "activate";
            } else {
                methodName = "deactivate";
            }
        }

        // try to find matching activate/deactivate method and execute it
        if (invokeLifecycleMethod(target, targetClass, methodName, !activate, 
                componentContext, MapUtil.toMap(componentContext.getProperties()))) {
            return true;
        }
        
        if (fallbackDefaultName) {
            return false;
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
    public static boolean modified(Object target, ComponentContext componentContext, Map<String,Object> properties) {
        Class<?> targetClass = target.getClass();

        // get method name for activation/deactivation from osgi metadata
        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(targetClass);
        if (metadata == null) {
            throw new NoScrMetadataException(targetClass);
        }
        String methodName = metadata.getModifiedMethodName();
        if (StringUtils.isEmpty(methodName)) {
            return false;
        }
        
        // try to find matching modified method and execute it
        if (invokeLifecycleMethod(target, targetClass, methodName, false, componentContext, properties)) {
            return true;
        }

        throw new RuntimeException("No matching modified method with name '" + methodName + "' "
                + " found in class " + targetClass.getName());
    }
    
    /**
     * Invokes a lifecycle method (activation, deactivation or modified) with variable method arguments.
     * @param target Target object
     * @param targetClass Target object class
     * @param methodName Method name
     * @param allowIntegerArgument Allow int or Integer as arguments (only decactivate)
     * @param componentContext Component context
     * @param properties Component properties
     * @return true if a method was found and invoked
     */
    private static boolean invokeLifecycleMethod(Object target, Class<?> targetClass, 
            String methodName, boolean allowIntegerArgument,
            ComponentContext componentContext, Map<String,Object> properties) {

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
        
        // 4. Component property type (annotation lass)
        method = getMethod(targetClass, methodName, new Class<?>[] { Annotation.class });
        if (method != null) {
            invokeMethod(target, method, new Object[] { Annotations.toObject(method.getParameterTypes()[0],
                    MapUtil.toMap(componentContext.getProperties()), 
                    componentContext.getBundleContext().getBundle(), false) });
            return true;
        }
        
        // 5. int (deactivation only)
        if (allowIntegerArgument) {
            method = getMethod(targetClass, methodName, new Class<?>[] { int.class });
            if (method != null) {
                invokeMethod(target, method, new Object[] { 0 });
                return true;
            }
        }
        
        // 6. Integer (deactivation only)
        if (allowIntegerArgument) {
            method = getMethod(targetClass, methodName, new Class<?>[] { Integer.class });
            if (method != null) {
                invokeMethod(target, method, new Object[] { 0 });
                return true;
            }
        }
        
        // 7. mixed arguments
        Class<?>[] mixedArgsAllowed = allowIntegerArgument ?
                new Class<?>[] { ComponentContext.class, BundleContext.class, Map.class, Annotation.class, int.class, Integer.class }
                : new Class<?>[] { ComponentContext.class, BundleContext.class, Map.class, Annotation.class };
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
                else if (method.getParameterTypes()[i].isAnnotation()) {
                    args[i] = Annotations.toObject(method.getParameterTypes()[i],
                            MapUtil.toMap(componentContext.getProperties()), 
                            componentContext.getBundleContext().getBundle(), false);
                }
                else if (method.getParameterTypes()[i] == int.class || method.getParameterTypes()[i] == Integer.class) {
                    args[i] = 0;
                }
            }
            invokeMethod(target, method, args);
            return true;
        }

        // 8. noargs
        method = getMethod(targetClass, methodName, new Class<?>[0]);
        if (method != null) {
            invokeMethod(target, method, new Object[0]);
            return true;
        }        
        
        return false;
    }

    private static Method getMethod(Class clazz, String methodName, Class<?>[] types) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (StringUtils.equals(method.getName(), methodName) && method.getParameterTypes().length==types.length) {
                boolean foundMismatch = false;
                for (int i=0; i<types.length; i++) {
                    if (!((method.getParameterTypes()[i]==types[i]) 
                            || (types[i]==Annotation.class && method.getParameterTypes()[i].isAnnotation()))) {
                        foundMismatch = true;
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
                        foundMismatch = true;
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
                boolean foundMismatch = false;
                for (Class<?> parameterType : method.getParameterTypes()) {
                    boolean foundAnyMatch = false;
                    for (int i=0; i<types.length; i++) {
                        if ((parameterType==types[i]) 
                                || (types[i]==Annotation.class && parameterType.isAnnotation())) {
                            foundAnyMatch = true;
                            break;
                        }
                    }
                    if (!foundAnyMatch) {
                        foundMismatch = true;
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
            return getMethodWithAnyCombinationArgs(superClass, methodName, types);
        }
        return null;
    }
    
    private static void invokeMethod(Object target, Method method, Object[] args) {
        try {
            method.setAccessible(true);
            method.invoke(target, args);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Unable to invoke method '" + method.getName() + "' for class "
                    + target.getClass().getName(), ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Unable to invoke method '" + method.getName() + "' for class "
                    + target.getClass().getName(), ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("Unable to invoke method '" + method.getName() + "' for class "
                    + target.getClass().getName(), ex.getCause());
        }
    }

    private static Field getField(Class clazz, String fieldName, Class<?> type) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (StringUtils.equals(field.getName(), fieldName) && field.getType().equals(type)) {
                return field;
            }
        }
        // not found? check super classes
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return getField(superClass, fieldName, type);
        }
        return null;
    }
    
    private static Field getFieldWithAssignableType(Class clazz, String fieldName, Class<?> type) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (StringUtils.equals(field.getName(), fieldName) && field.getType().isAssignableFrom(type)) {
                return field;
            }
        }
        // not found? check super classes
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return getFieldWithAssignableType(superClass, fieldName, type);
        }
        return null;
    }
    
    private static void setField(Object target, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Unable to set field '" + field.getName() + "' for class "
                    + target.getClass().getName(), ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Unable to set field '" + field.getName() + "' for class "
                    + target.getClass().getName(), ex);
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

        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(targetClass);
        if (metadata == null) {
            throw new NoScrMetadataException(targetClass);
        }
        List<Reference> references = metadata.getReferences();
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
        Class<?> type = reference.getInterfaceTypeAsClass();

        // get matching service references
        List<ServiceInfo> matchingServices = getMatchingServices(type, bundleContext, reference.getTarget());

        // no references found? check if reference was optional
        if (matchingServices.isEmpty()) {
            if (!reference.isCardinalityOptional()) {
                throw new ReferenceViolationException("Unable to inject mandatory reference '" + reference.getName() + "' for class " + targetClass.getName() + " : no matching services were found.");
            }
            if (reference.isCardinalityMultiple()) {
                // make sure at least empty array is set  
                invokeBindUnbindMethod(reference, target, null, true);
            }
        }

        // multiple references found? check if reference is not multiple
        if (matchingServices.size() > 1 && !reference.isCardinalityMultiple()) {
            throw new ReferenceViolationException("Multiple matches found for unary reference '" + reference.getName() + "' for class "+ targetClass.getName());
        }

        // try to invoke bind method
        for (ServiceInfo matchingService : matchingServices) {
            invokeBindUnbindMethod(reference, target, matchingService, true);
        }
    }
    
    private static void invokeBindUnbindMethod(Reference reference, Object target, ServiceInfo serviceInfo, boolean bind) {
        Class<?> targetClass = target.getClass();

        // try to invoke bind method
        String methodName = bind ? reference.getBind() : reference.getUnbind();
        String fieldName = reference.getField();
        
        if (StringUtils.isEmpty(methodName) && StringUtils.isEmpty(fieldName)) {
            throw new RuntimeException("No bind/unbind method name or file name defined "
                    + "for reference '" + reference.getName() + "' for class " +  targetClass.getName());
        }

        if (StringUtils.isNotEmpty(methodName) && serviceInfo != null) {
            
            // 1. ServiceReference
            Method method = getMethod(targetClass, methodName, new Class<?>[] { ServiceReference.class });
            if (method != null) {
                invokeMethod(target, method, new Object[] { serviceInfo.getServiceReference() });
                return;
            }
            
            // 2. assignable from service instance
            Class<?> interfaceType = reference.getInterfaceTypeAsClass();
            method = getMethodWithAssignableTypes(targetClass, methodName, new Class<?>[] { interfaceType });
            if (method != null) {
                invokeMethod(target, method, new Object[] { serviceInfo.getServiceInstance() });
                return;
            }
            
            // 3. assignable from service instance plus map
            method = getMethodWithAssignableTypes(targetClass, methodName, new Class<?>[] { interfaceType, Map.class });
            if (method != null) {
                invokeMethod(target, method, new Object[] { serviceInfo.getServiceInstance(), serviceInfo.getServiceConfig() });
                return;
            }
        
            throw new RuntimeException((bind ? "Bind" : "Unbind") + " method with name " + methodName + " not found "
                    + "for reference '" + reference.getName() + "' for class " +  targetClass.getName());
        }
        
        // in OSGi declarative services 1.3 there are no bind/unbind methods - modify the field directly
        else if (StringUtils.isNotEmpty(fieldName)) {
            
            // check for field with list/collection reference
            if (reference.isCardinalityMultiple()) {
                switch (reference.getFieldCollectionType()) {
                    case SERVICE:
                    case REFERENCE:
                        Object item = null;
                        if (serviceInfo != null) {
                            item = serviceInfo.getServiceInstance();
                            if (reference.getFieldCollectionType() == FieldCollectionType.REFERENCE) {
                                item = serviceInfo.getServiceReference();
                            }
                        }
                        // 1. collection
                        Field field = getFieldWithAssignableType(targetClass, fieldName, Collection.class);
                        if (field != null) {
                            if (bind) {
                                addToCollection(target, field, item);
                            }
                            else {
                                removeFromCollection(target, field, item);
                            }
                            return;
                        }
                        
                        // 2. list
                        field = getField(targetClass, fieldName, List.class);
                        if (field != null) {
                            if (bind) {
                                addToCollection(target, field, item);
                            }
                            else {
                                removeFromCollection(target, field, item);
                            }
                            return;
                        }
                        break;
                    default:
                        throw new RuntimeException("Field collection type '" + reference.getFieldCollectionType() + "' not supported "
                                + "for reference '" + reference.getName() + "' for class " +  targetClass.getName());
                }
            }
            
            // check for single field reference
            else {
                // 1. assignable from service instance
                Class<?> interfaceType = reference.getInterfaceTypeAsClass();
                Field field = getFieldWithAssignableType(targetClass, fieldName, interfaceType);
                if (field != null) {
                    setField(target, field, bind && serviceInfo != null ? serviceInfo.getServiceInstance() : null);
                    return;
                }
                
                // 2. ServiceReference
                field = getField(targetClass, fieldName, ServiceReference.class);
                if (field != null) {
                    setField(target, field, bind && serviceInfo != null ? serviceInfo.getServiceReference() : null);
                    return;
                }
            }
        }

    }
    
    @SuppressWarnings("unchecked")
    private static void addToCollection(Object target, Field field, Object item) {
        try {
            field.setAccessible(true);
            Collection<Object> collection = (Collection<Object>)field.get(target);
            if (collection == null) {
                collection = new ArrayList<Object>();
            }
            if (item != null) {
                collection.add(item);
            }
            field.set(target, collection);
            
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Unable to set field '" + field.getName() + "' for class "
                    + target.getClass().getName(), ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Unable to set field '" + field.getName() + "' for class "
                    + target.getClass().getName(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static void removeFromCollection(Object target, Field field, Object item) {
        try {
            field.setAccessible(true);
            Collection<Object> collection = (Collection<Object>)field.get(target);
            if (collection == null) {
                collection = new ArrayList<Object>();
            }
            if (item != null) {
                collection.remove(item);
            }
            field.set(target, collection);
            
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Unable to set field '" + field.getName() + "' for class "
                    + target.getClass().getName(), ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Unable to set field '" + field.getName() + "' for class "
                    + target.getClass().getName(), ex);
        }
    }

    /**
     * Directly invoke bind method on service for the given reference.
     * @param reference Reference metadata
     * @param target Target object for reference
     * @param serviceInfo Service on which to invoke the method
     */
    public static void invokeBindMethod(Reference reference, Object target, ServiceInfo serviceInfo) {
        invokeBindUnbindMethod(reference,  target, serviceInfo, true);
    }
    
    /**
     * Directly invoke unbind method on service for the given reference.
     * @param reference Reference metadata
     * @param target Target object for reference
     * @param serviceInfo Service on which to invoke the method
     */
    public static void invokeUnbindMethod(Reference reference, Object target, ServiceInfo serviceInfo) {
        invokeBindUnbindMethod(reference,  target, serviceInfo, false);
    }
    
    private static List<ServiceInfo> getMatchingServices(Class<?> type, BundleContext bundleContext, String filter) {
        List<ServiceInfo> matchingServices = new ArrayList<ServiceInfo>();
        try {
            ServiceReference[] references = bundleContext.getServiceReferences(type.getName(), filter);
            if (references != null) {
                for (ServiceReference<?> serviceReference : references) {
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

    /**
     * Collects all references of any registered service that match with any of the exported interfaces of the given service registration.
     * @param registeredServices Registered Services
     * @param registration Service registration
     * @return List of references
     */
    public static List<ReferenceInfo> getMatchingDynamicReferences(SortedSet<MockServiceRegistration> registeredServices,
            MockServiceRegistration<?> registration) {
        List<ReferenceInfo> references = new ArrayList<ReferenceInfo>();
        for (MockServiceRegistration existingRegistration : registeredServices) {
            OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(existingRegistration.getService().getClass());
            if (metadata != null) {
                for (Reference reference : metadata.getReferences()) {
                    if (reference.getPolicy() == ReferencePolicy.DYNAMIC) {
                        for (String serviceInterface : registration.getClasses()) {
                            if (StringUtils.equals(serviceInterface, reference.getInterfaceType())) {
                                references.add(new ReferenceInfo(existingRegistration, reference));
                            }
                        }
                    }
                }
            }
        }
        return references;
    }
            
    static class ServiceInfo {

        private final Object serviceInstance;
        private final Map<String, Object> serviceConfig;
        private final ServiceReference serviceReference;

        public ServiceInfo(Object serviceInstance, Map<String, Object> serviceConfig, ServiceReference serviceReference) {
            this.serviceInstance = serviceInstance;
            this.serviceConfig = serviceConfig;
            this.serviceReference = serviceReference;
        }

        @SuppressWarnings("unchecked")
        public ServiceInfo(MockServiceRegistration registration) {
            this.serviceInstance = registration.getService();
            this.serviceConfig = MapUtil.toMap(registration.getProperties());
            this.serviceReference = registration.getReference();
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

    static class ReferenceInfo {

        private final MockServiceRegistration serviceRegistration;
        private final Reference reference;
        
        public ReferenceInfo(MockServiceRegistration serviceRegistration, Reference reference) {
            this.serviceRegistration = serviceRegistration;
            this.reference = reference;
        }

        public MockServiceRegistration getServiceRegistration() {
            return serviceRegistration;
        }

        public Reference getReference() {
            return reference;
        }

    }

}

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
package org.apache.sling.models.impl;

import java.lang.annotation.Annotation;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.commons.osgi.ServiceUtil;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.spi.DisposalCallback;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ModelAdapterFactory implements AdapterFactory, Runnable {

    /**
     * Comparator which sorts constructors by the number of parameters
     * in reverse order (most params to least params).
     */
    public class ParameterCountComparator implements Comparator<Constructor<?>> {

        @Override
        public int compare(Constructor<?> o1, Constructor<?> o2) {
            return Integer.compare(o2.getParameterTypes().length, o1.getParameterTypes().length);
        }

    }

    private static class DisposalCallbackRegistryImpl implements DisposalCallbackRegistry {

        private List<DisposalCallback> callbacks = new ArrayList<DisposalCallback>();

        @Override
        public void addDisposalCallback(DisposalCallback callback) {
            callbacks.add(callback);
        }

        private void seal() {
            callbacks = Collections.unmodifiableList(callbacks);
        }

        private void onDisposed() {
            for (DisposalCallback callback : callbacks) {
                callback.onDisposed();
            }
        }

    }

    private ReferenceQueue<Object> queue;

    private ConcurrentMap<java.lang.ref.Reference<Object>, DisposalCallbackRegistryImpl> disposalCallbacks;

    private static class MapBackedInvocationHandler implements InvocationHandler {

        private Map<Method, Object> methods;

        public MapBackedInvocationHandler(Map<Method, Object> methods) {
            this.methods = methods;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return methods.get(method);
        }

    }

    @Override
    public void run() {
        java.lang.ref.Reference<? extends Object> ref = queue.poll();
        if (ref != null) {
            log.debug("calling disposal for " + ref.toString());
            DisposalCallbackRegistryImpl registry = disposalCallbacks.remove(ref);
            registry.onDisposed();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(ModelAdapterFactory.class);

    @Reference(name = "injector", referenceInterface = Injector.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Map<Object, Injector> injectors = new TreeMap<Object, Injector>();

    private volatile Injector[] sortedInjectors = new Injector[0];

    private ModelPackageBundleListener listener;

    private ServiceRegistration jobRegistration;

    private ServiceRegistration configPrinterRegistration;

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        Model modelAnnotation = type.getAnnotation(Model.class);
        if (modelAnnotation == null) {
            return null;
        }
        boolean isAdaptable = false;

        Class<?>[] declaredAdaptable = modelAnnotation.adaptables();
        for (Class<?> clazz : declaredAdaptable) {
            if (clazz.isInstance(adaptable)) {
                isAdaptable = true;
            }
        }
        if (!isAdaptable) {
            return null;
        }

        if (type.isInterface()) {
            InvocationHandler handler = createInvocationHandler(adaptable, type);
            if (handler != null) {
                return (AdapterType) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                        handler);
            } else {
                return null;
            }
        } else {
            try {
                return createObject(adaptable, type);
            } catch (Exception e) {
                log.error("unable to create object", e);
                return null;
            }
        }
    }

    private Set<Field> collectInjectableFields(Class<?> type) {
        Set<Field> result = new HashSet<Field>();
        while (type != null) {
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                Inject injection = field.getAnnotation(Inject.class);
                if (injection != null) {
                    result.add(field);
                }
            }
            type = type.getSuperclass();
        }
        return result;
    }

    private Set<Method> collectInjectableMethods(Class<?> type) {
        Set<Method> result = new HashSet<Method>();
        while (type != null) {
            Method[] methods = type.getDeclaredMethods();
            for (Method method : methods) {
                Inject injection = method.getAnnotation(Inject.class);
                if (injection != null) {
                    result.add(method);
                }
            }
            type = type.getSuperclass();
        }
        return result;
    }

    private InvocationHandler createInvocationHandler(final Object adaptable, final Class<?> type) {
        Set<Method> injectableMethods = collectInjectableMethods(type);
        Map<Method, Object> methods = new HashMap<Method, Object>();
        MapBackedInvocationHandler handler = new MapBackedInvocationHandler(methods);

        DisposalCallbackRegistryImpl registry = createAndRegisterCallbackRegistry(handler);

        for (Injector injector : sortedInjectors) {
            Iterator<Method> it = injectableMethods.iterator();
            while (it.hasNext()) {
                Method method = it.next();
                String source = getSource(method);
                if (source == null || source.equals(injector.getName())) {
                    String name = getName(method);
                    Type returnType = mapPrimitiveClasses(method.getGenericReturnType());
                    Object injectionAdaptable = getAdaptable(adaptable, method);
                    if (injectionAdaptable != null) {
                        Object value = injector.getValue(injectionAdaptable, name, returnType, method, registry);
                        if (setMethod(method, methods, value)) {
                            it.remove();
                        }
                    }
                }
            }
        }

        registry.seal();

        Iterator<Method> it = injectableMethods.iterator();
        while (it.hasNext()) {
            Method method = it.next();
            Default defaultAnnotation = method.getAnnotation(Default.class);
            if (defaultAnnotation != null) {
                Type returnType = mapPrimitiveClasses(method.getGenericReturnType());
                Object value = getDefaultValue(defaultAnnotation, returnType);
                if (setMethod(method, methods, value)) {
                    it.remove();
                }
            }
        }

        if (injectableMethods.isEmpty()) {
            return handler;
        } else {
            Set<Method> requiredMethods = new HashSet<Method>();
            for (Method method : injectableMethods) {
                if (method.getAnnotation(Optional.class) == null) {
                    requiredMethods.add(method);
                }
            }

            if (!requiredMethods.isEmpty()) {
                log.warn("Required methods {} on model class {} were not able to be injected.", requiredMethods,
                        type);
                return null;
            } else {
                return handler;
            }
        }
    }

    private DisposalCallbackRegistryImpl createAndRegisterCallbackRegistry(Object object) {
        PhantomReference<Object> reference = new PhantomReference<Object>(object, queue);
        DisposalCallbackRegistryImpl registry = new DisposalCallbackRegistryImpl();
        disposalCallbacks.put(reference, registry);
        return registry;
    }

    private String getSource(AnnotatedElement element) {
        Source source = element.getAnnotation(Source.class);
        if (source != null) {
            return source.value();
        } else {
            for (Annotation ann : element.getAnnotations()) {
                source = ann.annotationType().getAnnotation(Source.class);
                if (source != null) {
                    return source.value();
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <AdapterType> AdapterType createObject(Object adaptable, Class<AdapterType> type)
            throws InstantiationException, InvocationTargetException, IllegalAccessException {
        Set<Field> injectableFields = collectInjectableFields(type);

        Constructor<?>[] constructors = type.getConstructors();
        if (constructors.length == 0) {
            log.warn("Model class {} does not have a public constructor.", type.getName());
            return null;
        }

        // sort the constructor list in order from most params to least params
        Arrays.sort(constructors, new ParameterCountComparator());

        Constructor<AdapterType> constructorToUse = null;
        boolean constructorHasParam = false;
        for (Constructor<?> constructor : constructors) {
            final Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length == 1) {
                Class<?> paramType = constructor.getParameterTypes()[0];
                if (paramType.isInstance(adaptable)) {
                    constructorToUse = (Constructor<AdapterType>) constructor;
                    constructorHasParam = true;
                    break;
                }
            }

            if (constructor.getParameterTypes().length == 0) {
                constructorToUse = (Constructor<AdapterType>) constructor;
                constructorHasParam = false;
                break;
            }
        }

        if (constructorToUse == null) {
            log.warn("Model class {} does not have a usable constructor", type.getName());
            return null;
        }

        final AdapterType object;
        if (constructorHasParam) {
            object = constructorToUse.newInstance(adaptable);
        } else {
            object = constructorToUse.newInstance();
        }

        DisposalCallbackRegistryImpl registry = createAndRegisterCallbackRegistry(object);

        for (Injector injector : sortedInjectors) {
            Iterator<Field> it = injectableFields.iterator();
            while (it.hasNext()) {
                Field field = it.next();
                String source = getSource(field);
                if (source == null || source.equals(injector.getName())) {
                    String name = getName(field);
                    Type fieldType = mapPrimitiveClasses(field.getGenericType());
                    Object injectionAdaptable = getAdaptable(adaptable, field);
                    if (injectionAdaptable != null) {
                        Object value = injector.getValue(injectionAdaptable, name, fieldType, field, registry);
                        if (setField(field, object, value)) {
                            it.remove();
                        }
                    }
                }
            }
        }

        registry.seal();

        Iterator<Field> it = injectableFields.iterator();
        while (it.hasNext()) {
            Field field = it.next();
            Default defaultAnnotation = field.getAnnotation(Default.class);
            if (defaultAnnotation != null) {
                Type fieldType = mapPrimitiveClasses(field.getGenericType());
                Object value = getDefaultValue(defaultAnnotation, fieldType);
                if (setField(field, object, value)) {
                    it.remove();
                }
            }
        }

        if (injectableFields.isEmpty()) {
            try {
                invokePostConstruct(object);
                return object;
            } catch (Exception e) {
                log.error("Unable to invoke post construct method.", e);
                return null;
            }
        } else {
            Set<Field> requiredFields = new HashSet<Field>();
            for (Field field : injectableFields) {
                if (field.getAnnotation(Optional.class) == null) {
                    requiredFields.add(field);
                }
            }

            if (!requiredFields.isEmpty()) {
                log.warn("Required properties {} on model class {} were not able to be injected.", requiredFields,
                        type);
                return null;
            } else {
                try {
                    invokePostConstruct(object);
                    return object;
                } catch (Exception e) {
                    log.error("Unable to invoke post construct method.", e);
                    return null;
                }
            }
        }
    }

    private Object getDefaultValue(Default defaultAnnotation, Type type) {
        if (type instanceof Class) {
            Class<?> injectedClass = (Class<?>) type;
            if (injectedClass.isArray()) {
                Class<?> componentType = injectedClass.getComponentType();
                if (componentType == String.class) {
                    return defaultAnnotation.values();
                }
                if (componentType == Integer.TYPE) {
                    return defaultAnnotation.intValues();
                }
                if (componentType == Long.TYPE) {
                    return defaultAnnotation.longValues();
                }
                if (componentType == Boolean.TYPE) {
                    return defaultAnnotation.booleanValues();
                }
                if (componentType == Short.TYPE) {
                    return defaultAnnotation.shortValues();
                }
                if (componentType == Float.TYPE) {
                    return defaultAnnotation.floatValues();
                }
                if (componentType == Double.TYPE) {
                    return defaultAnnotation.doubleValues();
                }

                log.warn("Default values for {} are not supported", componentType);
                return null;
            } else {
                if (injectedClass == String.class) {
                    return defaultAnnotation.values()[0];
                }
                if (injectedClass == Integer.TYPE) {
                    return defaultAnnotation.intValues()[0];
                }
                if (injectedClass == Long.TYPE) {
                    return defaultAnnotation.longValues()[0];
                }
                if (injectedClass == Boolean.TYPE) {
                    return defaultAnnotation.booleanValues()[0];
                }
                if (injectedClass == Short.TYPE) {
                    return defaultAnnotation.shortValues()[0];
                }
                if (injectedClass == Float.TYPE) {
                    return defaultAnnotation.floatValues()[0];
                }
                if (injectedClass == Double.TYPE) {
                    return defaultAnnotation.doubleValues()[0];
                }

                log.warn("Default values for {} are not supported", injectedClass);
                return null;
            }
        } else {
            log.warn("Cannot provide default for {}", type);
            return null;
        }
    }

    private Object getAdaptable(Object adaptable, AnnotatedElement point) {
        Via viaAnnotation = point.getAnnotation(Via.class);
        if (viaAnnotation == null) {
            return adaptable;
        }
        String viaPropertyName = viaAnnotation.value();
        try {
            return PropertyUtils.getProperty(adaptable, viaPropertyName);
        } catch (Exception e) {
            log.error("Unable to execution projection " + viaPropertyName, e);
            return null;
        }
    }

    private String getName(Field field) {
        Named named = field.getAnnotation(Named.class);
        if (named != null) {
            return named.value();
        }
        return field.getName();
    }

    private String getName(Method method) {
        Named named = method.getAnnotation(Named.class);
        if (named != null) {
            return named.value();
        }
        String methodName = method.getName();
        if (methodName.startsWith("get")) {
            return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        } else if (methodName.startsWith("is")) {
            return methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
        } else {
            return methodName;
        }
    }

    private void invokePostConstruct(Object object) throws Exception {
        Class<?> clazz = object.getClass();
        List<Method> postConstructMethods = new ArrayList<Method>();
        while (clazz != null) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(PostConstruct.class)) {
                    postConstructMethods.add(method);
                }
            }
            clazz = clazz.getSuperclass();
        }
        Collections.reverse(postConstructMethods);
        for (Method method : postConstructMethods) {
            boolean accessible = method.isAccessible();
            try {
                if (!accessible) {
                    method.setAccessible(true);
                }
                method.invoke(object);
            } finally {
                if (!accessible) {
                    method.setAccessible(false);
                }
            }
        }
    }

    private Type mapPrimitiveClasses(Type type) {
        if (type == Integer.TYPE) {
            return Integer.class;
        }
        if (type == Long.TYPE) {
            return Long.class;
        }
        if (type == Boolean.TYPE) {
            return Boolean.class;
        }
        if (type == Double.TYPE) {
            return Double.class;
        }
        if (type == Float.TYPE) {
            return Float.class;
        }
        if (type == Short.TYPE) {
            return Short.class;
        }
        if (type == Character.TYPE) {
            return Character.class;
        }

        return type;
    }

    private boolean setField(Field field, Object createdObject, Object value) {
        if (value != null) {
            if (!isAcceptableType(field.getClass(), value) && value instanceof Adaptable) {
                value = ((Adaptable) value).adaptTo(field.getClass());
                if (value == null) {
                    return false;
                }
            }
            boolean accessible = field.isAccessible();
            try {
                if (!accessible) {
                    field.setAccessible(true);
                }
                field.set(createdObject, value);
                return true;
            } catch (Exception e) {
                log.error("unable to inject field", e);
                return false;
            } finally {
                if (!accessible) {
                    field.setAccessible(false);
                }
            }
        } else {
            return false;
        }
    }

    private boolean setMethod(Method method, Map<Method, Object> methods, Object value) {
        if (value != null) {
            if (!isAcceptableType(method.getReturnType(), value) && value instanceof Adaptable) {
                value = ((Adaptable) value).adaptTo(method.getReturnType());
                if (value == null) {
                    return false;
                }
            }
            methods.put(method, value);
            return true;
        } else {
            return false;
        }
    }

    private boolean isAcceptableType(Class<?> type, Object value) {
        if (type.isInstance(value)) {
            return true;
        }

        if (type == Integer.TYPE) {
            return Integer.class.isInstance(value);
        }
        if (type == Long.TYPE) {
            return Long.class.isInstance(value);
        }
        if (type == Boolean.TYPE) {
            return Boolean.class.isInstance(value);
        }
        if (type == Double.TYPE) {
            return Double.class.isInstance(value);
        }
        if (type == Float.TYPE) {
            return Float.class.isInstance(value);
        }
        if (type == Short.TYPE) {
            return Short.class.isInstance(value);
        }
        if (type == Character.TYPE) {
            return Character.class.isInstance(value);
        }

        return false;
    }

    @Activate
    protected void activate(final ComponentContext ctx) {
        BundleContext bundleContext = ctx.getBundleContext();
        this.queue = new ReferenceQueue<Object>();
        this.disposalCallbacks = new ConcurrentHashMap<java.lang.ref.Reference<Object>, DisposalCallbackRegistryImpl>();
        Hashtable<Object, Object> properties = new Hashtable<Object, Object>();
        properties.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        properties.put(Constants.SERVICE_DESCRIPTION, "Sling Models OSGi Service Disposal Job");
        properties.put("scheduler.concurrent", false);
        properties.put("scheduler.period", Long.valueOf(30));

        this.jobRegistration = bundleContext.registerService(Runnable.class.getName(), this,
                properties);

        this.listener = new ModelPackageBundleListener(ctx.getBundleContext(), this);

        Hashtable<Object, Object> printerProps = new Hashtable<Object, Object>();
        printerProps.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        printerProps.put(Constants.SERVICE_DESCRIPTION, "Sling Models Configuration Printer");
        printerProps.put("felix.webconsole.label", "slingmodels");
        printerProps.put("felix.webconsole.title", "Sling Models");
        printerProps.put("felix.webconsole.configprinter.modes", "always");

        this.configPrinterRegistration = bundleContext.registerService(Object.class.getName(),
            new ModelConfigurationPrinter(this), printerProps);
    }

    @Deactivate
    protected void deactivate() {
        this.listener.unregisterAll();
        if (jobRegistration != null) {
            jobRegistration.unregister();
            jobRegistration = null;
        }
        if (configPrinterRegistration != null) {
            configPrinterRegistration.unregister();
            configPrinterRegistration = null;
        }
    }

    protected void bindInjector(final Injector injector, final Map<String, Object> props) {
        synchronized (injectors) {
            injectors.put(ServiceUtil.getComparableForServiceRanking(props), injector);
            sortedInjectors = injectors.values().toArray(new Injector[injectors.size()]);
        }
    }

    protected void unbindInjector(final Injector injector, final Map<String, Object> props) {
        synchronized (injectors) {
            injectors.remove(ServiceUtil.getComparableForServiceRanking(props));
            sortedInjectors = injectors.values().toArray(new Injector[injectors.size()]);
        }
    }

    Injector[] getInjectors() {
        return sortedInjectors;
    }

}

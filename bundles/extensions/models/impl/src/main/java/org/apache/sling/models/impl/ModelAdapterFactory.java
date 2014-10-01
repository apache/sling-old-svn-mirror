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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
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
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.osgi.ServiceUtil;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.Required;
import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.spi.AcceptsNullName;
import org.apache.sling.models.spi.DisposalCallback;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotation;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype = true)
public class ModelAdapterFactory implements AdapterFactory, Runnable {

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

    @Override
    public void run() {
        java.lang.ref.Reference<? extends Object> ref = queue.poll();
        while (ref != null) {
            log.debug("calling disposal for {}.", ref.toString());
            DisposalCallbackRegistryImpl registry = disposalCallbacks.remove(ref);
            registry.onDisposed();
            ref = queue.poll();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(ModelAdapterFactory.class);

    private static final int DEFAULT_MAX_RECURSION_DEPTH = 20;

    @Property(label = "Maximum Recursion Depth", description = "Maximum depth adaptation will be attempted.", intValue = DEFAULT_MAX_RECURSION_DEPTH)
    private static final String PROP_MAX_RECURSION_DEPTH = "max.recursion.depth";

    @Reference(name = "injector", referenceInterface = Injector.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Map<Object, Injector> injectors = new TreeMap<Object, Injector>();

    private volatile Injector[] sortedInjectors = new Injector[0];

    @Reference(name = "injectAnnotationProcessorFactory", referenceInterface = InjectAnnotationProcessorFactory.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Map<Object, InjectAnnotationProcessorFactory> injectAnnotationProcessorFactories = new TreeMap<Object, InjectAnnotationProcessorFactory>();

    private volatile InjectAnnotationProcessorFactory[] sortedInjectAnnotationProcessorFactories = new InjectAnnotationProcessorFactory[0];

    @Reference(name = "implementationPicker", referenceInterface = ImplementationPicker.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Map<Object, ImplementationPicker> implementationPickers = new TreeMap<Object, ImplementationPicker>();

    ModelPackageBundleListener listener;

    final AdapterImplementations adapterImplementations = new AdapterImplementations();

    private ServiceRegistration jobRegistration;

    private ServiceRegistration configPrinterRegistration;

    // Use threadlocal to count recursive invocations and break recursing if a max. limit is reached (to avoid cyclic dependencies)
    private ThreadLocal<ThreadInvocationCounter> invocationCountThreadLocal;

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        ThreadInvocationCounter threadInvocationCounter = invocationCountThreadLocal.get();
        if (threadInvocationCounter.isMaximumReached()) {
            log.error("Adapting {} to {} failed, too much recursive invocations (>={}).",
                    new Object[] { adaptable, type, threadInvocationCounter.maxRecursionDepth });
            return null;
        };
        threadInvocationCounter.increase();
        try {
            // check if a different implementation class was registered for this adapter type
            Class<?> implementationType = this.adapterImplementations.lookup(type, adaptable);
            if (implementationType != null) {
                type = (Class<AdapterType>) implementationType;
            }

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
                InvocationHandler handler = createInvocationHandler(adaptable, type, modelAnnotation);
                if (handler != null) {
                    return (AdapterType) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
                } else {
                    return null;
                }
            } else {
                try {
                    return createObject(adaptable, type, modelAnnotation);
                } catch (Exception e) {
                    log.error("unable to create object", e);
                    return null;
                }
            }
        } finally {
            threadInvocationCounter.decrease();
        }
    }

    private Set<Field> collectInjectableFields(Class<?> type) {
        Set<Field> result = new HashSet<Field>();
        while (type != null) {
            Field[] fields = type.getDeclaredFields();
            addAnnotated(fields, result);
            type = type.getSuperclass();
        }
        return result;
    }

    private Set<Method> collectInjectableMethods(Class<?> type) {
        Set<Method> result = new HashSet<Method>();
        while (type != null) {
            Method[] methods = type.getDeclaredMethods();
            addAnnotated(methods, result);
            type = type.getSuperclass();
        }
        return result;
    }

    private <T extends AnnotatedElement> void addAnnotated(T[] elements, Set<T> set) {
        for (T element : elements) {
            Inject injection = getAnnotation(element, Inject.class);
            if (injection != null) {
                set.add(element);
            } else {
                InjectAnnotation modelInject = getAnnotation(element, InjectAnnotation.class);
                if (modelInject != null) {
                    set.add(element);
                }
            }
        }
    }

    private static interface InjectCallback {
        /**
         * Is called each time when the given value should be injected into the given element
         * @param element
         * @param value
         * @return true if injection was successful otherwise false
         */
        public boolean inject(AnnotatedElement element, Object value);
    }

    private static class SetFieldCallback implements InjectCallback {

        private final Object object;

        private SetFieldCallback(Object object) {
            this.object = object;
        }

        @Override
        public boolean inject(AnnotatedElement element, Object value) {
            return setField((Field) element, object, value);
        }
    }

    private static class SetMethodsCallback implements InjectCallback {

        private final Map<Method, Object> methods;

        private SetMethodsCallback( Map<Method, Object> methods) {
            this.methods = methods;
        }

        @Override
        public boolean inject(AnnotatedElement element, Object value) {
            return setMethod((Method) element, methods, value);
        }
    }

    private static class SetConstructorParameterCallback implements InjectCallback {

        private final List<Object> parameterValues;

        private SetConstructorParameterCallback(List<Object> parameterValues) {
            this.parameterValues = parameterValues;
        }

        @Override
        public boolean inject(AnnotatedElement element, Object value) {
            return setConstructorParameter((ConstructorParameter)element, parameterValues, value);
        }
    }

    private boolean injectElement(final AnnotatedElement element, final Object adaptable, final Type type,
            final boolean injectPrimitiveInitialValue, final Model modelAnnotation, final DisposalCallbackRegistry registry,
            final InjectCallback callback) {

        InjectAnnotationProcessor annotationProcessor = null;
        String source = getSource(element);
        boolean wasInjectionSuccessful = false;

        // find an appropriate annotation processor
        for (InjectAnnotationProcessorFactory factory : sortedInjectAnnotationProcessorFactories) {
            annotationProcessor = factory.createAnnotationProcessor(adaptable, element);
            if (annotationProcessor != null) {
                break;
            }
        }

        String name = getName(element, annotationProcessor);
        Object injectionAdaptable = getAdaptable(adaptable, element, annotationProcessor);

        // find the right injector
        for (Injector injector : sortedInjectors) {
            if (source == null || source.equals(injector.getName())) {
                if (name != null || injector instanceof AcceptsNullName) {
                    if (injectionAdaptable != null) {
                        Object value = injector.getValue(injectionAdaptable, name, type, element, registry);
                        if (callback.inject(element, value)) {
                            wasInjectionSuccessful = true;
                            break;
                        }
                    }
                }
            }
        }
        // if injection failed, use default
        if (!wasInjectionSuccessful) {
            wasInjectionSuccessful = injectDefaultValue(element, type, annotationProcessor, callback);
        }

        // if default is not set, check if mandatory
        if (!wasInjectionSuccessful) {
            if (isOptional(element, modelAnnotation, annotationProcessor)) {
                if (injectPrimitiveInitialValue) {
                    injectPrimitiveInitialValue(element, type, callback);
                }
            } else {
                return false;
            }
        }
        
        return true;
    }

    private InvocationHandler createInvocationHandler(final Object adaptable, final Class<?> type, Model modelAnnotation) {
        Set<Method> injectableMethods = collectInjectableMethods(type);
        final Map<Method, Object> methods = new HashMap<Method, Object>();
        SetMethodsCallback callback = new SetMethodsCallback(methods);
        MapBackedInvocationHandler handler = new MapBackedInvocationHandler(methods);

        DisposalCallbackRegistryImpl registry = new DisposalCallbackRegistryImpl();
        registerCallbackRegistry(handler, registry);
        Set<Method> requiredMethods = new HashSet<Method>();

        for (Method method : injectableMethods) {
            Type genericReturnType = method.getGenericReturnType();
            Type returnType = mapPrimitiveClasses(genericReturnType);
            boolean isPrimitive = false;
            if (returnType != genericReturnType) {
                isPrimitive = true;
            }
            if (!injectElement(method, adaptable, returnType, isPrimitive, modelAnnotation, registry, callback)) {
                requiredMethods.add(method);
            }
        }
        registry.seal();
        if (!requiredMethods.isEmpty()) {
            log.warn("Required methods {} on model interface {} were not able to be injected.", requiredMethods, type);
            return null;
        }
        return handler;
    }

    private void registerCallbackRegistry(Object object, DisposalCallbackRegistryImpl registry) {
        PhantomReference<Object> reference = new PhantomReference<Object>(object, queue);
        disposalCallbacks.put(reference, registry);
    }

    private String getSource(AnnotatedElement element) {
        Source source = getAnnotation(element, Source.class);
        if (source != null) {
            return source.value();
        } else {
            return null;
        }
    }

    /**
     * Get an annotation from either the element itself or on any of the
     * element's annotations (meta-annotations).
     * 
     * @param element the element
     * @param annotationClass the annotation class
     * @return the found annotation or null
     */
    private <T extends Annotation> T getAnnotation(AnnotatedElement element, Class<T> annotationClass) {
        T annotation = element.getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        } else {
            for (Annotation ann : element.getAnnotations()) {
                annotation = ann.annotationType().getAnnotation(annotationClass);
                if (annotation != null) {
                    return annotation;
                }
            }
        }
        return null;
    }

    private <AdapterType> AdapterType createObject(Object adaptable, Class<AdapterType> type, Model modelAnnotation)
            throws InstantiationException, InvocationTargetException, IllegalAccessException {
        DisposalCallbackRegistryImpl registry = new DisposalCallbackRegistryImpl();

        Constructor<AdapterType> constructorToUse = getBestMatchingConstructor(adaptable, type);
        if (constructorToUse == null) {
            log.warn("Model class {} does not have a usable constructor", type.getName());
            return null;
        }

        final AdapterType object;
        if (constructorToUse.getParameterTypes().length == 0) {
            // no parameters for constructor injection? instantiate it right away
            object = constructorToUse.newInstance();
        } else {
            // instantiate with constructor injection
            // if this fails, make sure resources that may be claimed by injectors are cleared up again
            try {
                object = newInstanceWithConstructorInjection(constructorToUse, adaptable, type, modelAnnotation, registry);
            } catch (InstantiationException ex) {
                registry.onDisposed();
                throw ex;
            } catch (InvocationTargetException ex) {
                registry.onDisposed();
                throw ex;
            } catch (IllegalAccessException ex) {
                registry.onDisposed();
                throw ex;
            }
        }

        if (object == null) {
            registry.onDisposed();
            return null;
        }

        registerCallbackRegistry(object, registry);

        InjectCallback callback = new SetFieldCallback(object);

        Set<Field> requiredFields = new HashSet<Field>();

        Set<Field> injectableFields = collectInjectableFields(type);
        for (Field field : injectableFields) {
            Type fieldType = mapPrimitiveClasses(field.getGenericType());
            if (!injectElement(field, adaptable, fieldType, false, modelAnnotation, registry, callback)) {
                requiredFields.add(field);
            }
        }

        registry.seal();
        if (!requiredFields.isEmpty()) {
            log.warn("Required properties {} on model class {} were not able to be injected.", requiredFields, type);
            return null;
        }
        try {
            invokePostConstruct(object);
            return object;
        } catch (Exception e) {
            log.error("Unable to invoke post construct method.", e);
            return null;
        }

    }

    /**
     * Gets best matching constructor for constructor injection - or default constructor if none is found.
     * @param adaptable Adaptable instance
     * @param type Model type
     * @return Constructor or null if none found
     */
    @SuppressWarnings("unchecked")
    private <AdapterType> Constructor<AdapterType> getBestMatchingConstructor(Object adaptable, Class<AdapterType> type) {
        Constructor<?>[] constructors = type.getConstructors();

        // sort the constructor list in order from most params to least params, and constructors with @Inject annotation first
        Arrays.sort(constructors, new ParameterCountInjectComparator());

        for (Constructor<?> constructor : constructors) {
            // first try to find the constructor with most parameters and @Inject annotation
            if (constructor.isAnnotationPresent(Inject.class)) {
                return (Constructor<AdapterType>) constructor;
            }
            // compatibility mode for sling models implementation <= 1.0.6:
            // support constructor without @Inject if it has exactly one parameter matching the adaptable class
            final Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length == 1) {
                Class<?> paramType = constructor.getParameterTypes()[0];
                if (paramType.isInstance(adaptable)) {
                    return (Constructor<AdapterType>) constructor;
                }
            }
            // if no constructor for injection found use public constructor without any params
            if (constructor.getParameterTypes().length == 0) {
                return (Constructor<AdapterType>) constructor;
            }
        }
        return null;
    }

    private <AdapterType> AdapterType newInstanceWithConstructorInjection(Constructor<AdapterType> constructor, Object adaptable,
            Class<AdapterType> type, Model modelAnnotation, DisposalCallbackRegistry registry)
            throws InstantiationException, InvocationTargetException, IllegalAccessException {
        Set<ConstructorParameter> requiredParameters = new HashSet<ConstructorParameter>();
        Type[] parameterTypes = constructor.getGenericParameterTypes();
        List<Object> paramValues = new ArrayList<Object>(Arrays.asList(new Object[parameterTypes.length]));
        InjectCallback callback = new SetConstructorParameterCallback(paramValues);

        for (int i = 0; i < parameterTypes.length; i++) {
            Type genericType = mapPrimitiveClasses(parameterTypes[i]);

            boolean isPrimitive = false;
            if (parameterTypes[i] != genericType) {
                isPrimitive = true;
            }
            ConstructorParameter constructorParameter = new ConstructorParameter(
                    constructor.getParameterAnnotations()[i], constructor.getParameterTypes()[i], genericType, i);
            if (!injectElement(constructorParameter, adaptable, genericType, isPrimitive, modelAnnotation, registry, callback)) {
                requiredParameters.add(constructorParameter);
            }
        }
        if (!requiredParameters.isEmpty()) {
            log.warn("Required constructor parameters {} on model class {} were not able to be injected.", requiredParameters, type);
            return null;
        }
        return constructor.newInstance(paramValues.toArray(new Object[paramValues.size()]));
    }

    private boolean isOptional(AnnotatedElement point, Model modelAnnotation, InjectAnnotationProcessor annotationProcessor) {
        if (annotationProcessor != null) {
            Boolean isOptional = annotationProcessor.isOptional();
            if (isOptional != null) {
                return isOptional.booleanValue();
            }
        }
        if (modelAnnotation.defaultInjectionStrategy() == DefaultInjectionStrategy.REQUIRED) {
            return (point.getAnnotation(Optional.class) != null);
        } else {
            return (point.getAnnotation(Required.class) == null);
        }
        
    }

    private boolean injectDefaultValue(AnnotatedElement point, Type type, InjectAnnotationProcessor processor,
            InjectCallback callback) {

        if (processor != null) {
            if (processor.hasDefault()) {
                return callback.inject(point, processor.getDefault());
            }
        }
        Default defaultAnnotation = point.getAnnotation(Default.class);
        if (defaultAnnotation == null) {
            return false;
        }

        type = mapPrimitiveClasses(type);
        Object value = null;

        if (type instanceof Class) {
            Class<?> injectedClass = (Class<?>) type;
            if (injectedClass.isArray()) {
                Class<?> componentType = injectedClass.getComponentType();
                if (componentType == String.class) {
                    value = defaultAnnotation.values();
                } else if (componentType == Integer.TYPE) {
                    value = defaultAnnotation.intValues();
                } else if (componentType == Integer.class) {
                    value = ArrayUtils.toObject(defaultAnnotation.intValues());
                } else if (componentType == Long.TYPE) {
                    value = defaultAnnotation.longValues();
                } else if (componentType == Long.class) {
                    value = ArrayUtils.toObject(defaultAnnotation.longValues());
                } else if (componentType == Boolean.TYPE) {
                    value = defaultAnnotation.booleanValues();
                } else if (componentType == Boolean.class) {
                    value = ArrayUtils.toObject(defaultAnnotation.booleanValues());
                } else if (componentType == Short.TYPE) {
                    value = defaultAnnotation.shortValues();
                } else if (componentType == Short.class) {
                    value = ArrayUtils.toObject(defaultAnnotation.shortValues());
                } else if (componentType == Float.TYPE) {
                    value = defaultAnnotation.floatValues();
                } else if (componentType == Float.class) {
                    value = ArrayUtils.toObject(defaultAnnotation.floatValues());
                } else if (componentType == Double.TYPE) {
                    value = defaultAnnotation.doubleValues();
                } else if (componentType == Double.class) {
                    value = ArrayUtils.toObject(defaultAnnotation.doubleValues());
                } else {
                    log.warn("Default values for {} are not supported", componentType);
                    return false;
                }
            } else {
                if (injectedClass == String.class) {
                    value = defaultAnnotation.values().length == 0 ? "" : defaultAnnotation.values()[0];
                } else if (injectedClass == Integer.class) {
                    value = defaultAnnotation.intValues().length == 0 ? 0 : defaultAnnotation.intValues()[0];
                } else if (injectedClass == Long.class) {
                    value = defaultAnnotation.longValues().length == 0 ? 0l : defaultAnnotation.longValues()[0];
                } else if (injectedClass == Boolean.class) {
                    value = defaultAnnotation.booleanValues().length == 0 ? false : defaultAnnotation.booleanValues()[0];
                } else if (injectedClass == Short.class) {
                    value = defaultAnnotation.shortValues().length == 0 ? ((short) 0) : defaultAnnotation.shortValues()[0];
                } else if (injectedClass == Float.class) {
                    value = defaultAnnotation.floatValues().length == 0 ? 0f : defaultAnnotation.floatValues()[0];
                } else if (injectedClass == Double.class) {
                    value = defaultAnnotation.doubleValues().length == 0 ? 0d : defaultAnnotation.doubleValues()[0];
                } else {
                    log.warn("Default values for {} are not supported", injectedClass);
                    return false;
                }
            }
        } else {
            log.warn("Cannot provide default for {}", type);
            return false;
        }
        return callback.inject(point, value);
    }

    /**
     * Injects the default initial value for the given primitive class which
     * cannot be null (e.g. int = 0, boolean = false).
     * 
     * @param point Annotated element
     * @param wrapperType Non-primitive wrapper class for primitive class
     * @param callback Inject callback
     */
    private void injectPrimitiveInitialValue(AnnotatedElement point, Type wrapperType, InjectCallback callback) {
        Type primitiveType = mapWrapperClasses(wrapperType);
        Object value = null;
        if (primitiveType == int.class) {
            value = Integer.valueOf(0);
        } else if (primitiveType == long.class) {
            value = Long.valueOf(0);
        } else if (primitiveType == boolean.class) {
            value = Boolean.FALSE;
        } else if (primitiveType == double.class) {
            value = Double.valueOf(0);
        } else if (primitiveType == float.class) {
            value = Float.valueOf(0);
        } else if (primitiveType == short.class) {
            value = Short.valueOf((short) 0);
        } else if (primitiveType == byte.class) {
            value = Byte.valueOf((byte) 0);
        } else if (primitiveType == char.class) {
            value = Character.valueOf('\u0000');
        }
        if (value != null) {
            callback.inject(point, value);
        };
    }
    
    private Object getAdaptable(Object adaptable, AnnotatedElement point, InjectAnnotationProcessor processor) {
        String viaPropertyName = null;
        if (processor != null) {
            viaPropertyName = processor.getVia();
        }
        if (viaPropertyName == null) {
            Via viaAnnotation = point.getAnnotation(Via.class);
            if (viaAnnotation == null) {
                return adaptable;
            }
            viaPropertyName = viaAnnotation.value();
        }
        try {
            return PropertyUtils.getProperty(adaptable, viaPropertyName);
        } catch (Exception e) {
            log.error("Unable to execution projection " + viaPropertyName, e);
            return null;
        }
    }

    private String getName(AnnotatedElement element, InjectAnnotationProcessor processor) {
        // try to get the name from injector-specific annotation
        if (processor != null) {
            String name = processor.getName();
            if (name != null) {
                return name;
            }
        }
        // alternative for name attribute
        Named named = element.getAnnotation(Named.class);
        if (named != null) {
            return named.value();
        }
        if (element instanceof Method) {
            return getNameFromMethod((Method) element);
        } else if (element instanceof Field) {
            return getNameFromField((Field) element);
        } else if (element instanceof ConstructorParameter) {
            // implicit name not supported for constructor parameters - but do not throw exception because class-based injection is still possible
            return null;
        } else {
            throw new IllegalArgumentException("The given element must be either method or field but is " + element);
        }
    }

    private String getNameFromField(Field field) {
        return field.getName();
    }

    private String getNameFromMethod(Method method) {
        String methodName = method.getName();
        if (methodName.startsWith("get")) {
            return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        } else if (methodName.startsWith("is")) {
            return methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
        } else {
            return methodName;
        }
    }
    
    private boolean addMethodIfNotOverriden(List<Method> methods, Method newMethod) {
        for (Method method : methods) {
            if (method.getName().equals(newMethod.getName())) {
                if (Arrays.equals(method.getParameterTypes(),newMethod.getParameterTypes())) {
                    return false;
                }
            }
        }
        methods.add(newMethod);
        return true;
    }

    private void invokePostConstruct(Object object) throws Exception {
        Class<?> clazz = object.getClass();
        List<Method> postConstructMethods = new ArrayList<Method>();
        while (clazz != null) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(PostConstruct.class)) {
                    addMethodIfNotOverriden(postConstructMethods, method);
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

    private static Type mapPrimitiveClasses(Type type) {
        if (type instanceof Class<?>) {
            return ClassUtils.primitiveToWrapper((Class<?>) type);
        } else {
            return type;
        }
    }

    private static Type mapWrapperClasses(Type type) {
        if (type instanceof Class<?>) {
            return ClassUtils.wrapperToPrimitive((Class<?>) type);
        } else {
            return type;
        }
    }

    private static boolean setField(Field field, Object createdObject, Object value) {
        if (value != null) {
            value = adaptIfNecessary(value, field.getType(), field.getGenericType());
            // value may now be null due to the adaptation done above
            if (value == null) {
                return false;
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

    private static boolean setMethod(Method method, Map<Method, Object> methods, Object value) {
        if (value != null) {
            value = adaptIfNecessary(value, method.getReturnType(), method.getGenericReturnType());
            // value may now be null due to the adaptation done above
            if (value == null) {
                return false;
            }
            methods.put(method, value);
            return true;
        } else {
            return false;
        }
    }

    private static boolean setConstructorParameter(ConstructorParameter constructorParameter, List<Object> parameterValues, Object value) {
        if (value != null && constructorParameter.getType() instanceof Class<?>) {
            value = adaptIfNecessary(value, (Class<?>) constructorParameter.getType(), constructorParameter.getGenericType());
            // value may now be null due to the adaptation done above
            if (value == null) {
                return false;
            }
            parameterValues.set(constructorParameter.getParameterIndex(), value);
            return true;
        } else {
            return false;
        }
    }

    private static Object adaptIfNecessary(Object value, Class<?> type, Type genericType) {
        if (!isAcceptableType(type, genericType, value)) {
            Class<?> declaredType = type;
            if (value instanceof Adaptable) {
                value = ((Adaptable) value).adaptTo(type);
            } else if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                Class<?> collectionType = (Class<?>) declaredType;
                if (value instanceof Collection &&
                        (collectionType.equals(Collection.class) || collectionType.equals(List.class)) &&
                        parameterizedType.getActualTypeArguments().length == 1) {
                    List<Object> result = new ArrayList<Object>();
                    for (Object valueObject : (Collection<?>) value) {
                        if (valueObject instanceof Adaptable) {
                            Object adapted = ((Adaptable) valueObject).adaptTo((Class<?>) parameterizedType.getActualTypeArguments()[0]);
                            if (adapted != null) {
                                result.add(adapted);
                            }
                        }
                    }
                    value = result;
                }
            }
        }
        return value;
    }

    private static boolean isAcceptableType(Class<?> type, Type genericType, Object value) {
        if (type.isInstance(value)) {
            if ((type == Collection.class || type == List.class) && genericType instanceof ParameterizedType &&
                    value instanceof Collection) {
                Iterator<?> it = ((Collection<?>) value).iterator();
                if (!it.hasNext()) {
                    // empty collection, so it doesn't really matter
                    return true;
                } else {
                    // this is not an ideal way to get the actual component type, but erasure...
                    Class<?> actualComponentType = it.next().getClass();
                    Class<?> desiredComponentType = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
                    return desiredComponentType.isAssignableFrom(actualComponentType);
                }
            } else {
                return true;
            }
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
        if (type == Byte.TYPE) {
            return Byte.class.isInstance(value);
        }
        if (type == Character.TYPE) {
            return Character.class.isInstance(value);
        }

        return false;
    }

    @Activate
    protected void activate(final ComponentContext ctx) {
        Dictionary<?, ?> props = ctx.getProperties();
        final int maxRecursionDepth = PropertiesUtil.toInteger(props.get(PROP_MAX_RECURSION_DEPTH), DEFAULT_MAX_RECURSION_DEPTH);
        this.invocationCountThreadLocal = new ThreadLocal<ThreadInvocationCounter>() {
            @Override
            protected ThreadInvocationCounter initialValue() {
                return new ThreadInvocationCounter(maxRecursionDepth);
            }
        };

        BundleContext bundleContext = ctx.getBundleContext();
        this.queue = new ReferenceQueue<Object>();
        this.disposalCallbacks = new ConcurrentHashMap<java.lang.ref.Reference<Object>, DisposalCallbackRegistryImpl>();
        Hashtable<Object, Object> properties = new Hashtable<Object, Object>();
        properties.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        properties.put(Constants.SERVICE_DESCRIPTION, "Sling Models OSGi Service Disposal Job");
        properties.put("scheduler.concurrent", false);
        properties.put("scheduler.period", Long.valueOf(30));

        this.jobRegistration = bundleContext.registerService(Runnable.class.getName(), this, properties);

        this.listener = new ModelPackageBundleListener(ctx.getBundleContext(), this, this.adapterImplementations);

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
        this.adapterImplementations.removeAll();
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

    protected void bindInjectAnnotationProcessorFactory(final InjectAnnotationProcessorFactory injector, final Map<String, Object> props) {
        synchronized (injectAnnotationProcessorFactories) {
            injectAnnotationProcessorFactories.put(ServiceUtil.getComparableForServiceRanking(props), injector);
            sortedInjectAnnotationProcessorFactories = injectAnnotationProcessorFactories.values().toArray(new InjectAnnotationProcessorFactory[injectAnnotationProcessorFactories.size()]);
        }
    }

    protected void unbindInjectAnnotationProcessorFactory(final InjectAnnotationProcessorFactory injector, final Map<String, Object> props) {
        synchronized (injectAnnotationProcessorFactories) {
            injectAnnotationProcessorFactories.remove(ServiceUtil.getComparableForServiceRanking(props));
            sortedInjectAnnotationProcessorFactories = injectAnnotationProcessorFactories.values().toArray(new InjectAnnotationProcessorFactory[injectAnnotationProcessorFactories.size()]);
        }
    }

    protected void bindImplementationPicker(final ImplementationPicker implementationPicker, final Map<String, Object> props) {
        synchronized (implementationPickers) {
            implementationPickers.put(ServiceUtil.getComparableForServiceRanking(props), implementationPicker);
            this.adapterImplementations.setImplementationPickers(implementationPickers.values());
        }
    }

    protected void unbindImplementationPicker(final ImplementationPicker implementationPicker, final Map<String, Object> props) {
        synchronized (implementationPickers) {
            implementationPickers.remove(ServiceUtil.getComparableForServiceRanking(props));
            this.adapterImplementations.setImplementationPickers(implementationPickers.values());
        }
    }

    Injector[] getInjectors() {
        return sortedInjectors;
    }

    InjectAnnotationProcessorFactory[] getInjectAnnotationProcessorFactories() {
        return sortedInjectAnnotationProcessorFactories;
    }

    ImplementationPicker[] getImplementationPickers() {
        return adapterImplementations.getImplementationPickers();
    }

}

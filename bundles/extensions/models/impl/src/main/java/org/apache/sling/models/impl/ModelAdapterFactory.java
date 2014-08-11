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
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.commons.osgi.ServiceUtil;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.Required;
import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.spi.DisposalCallback;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.InvalidAdaptableException;
import org.apache.sling.models.spi.ModelFactory;
import org.apache.sling.models.spi.ValidationException;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotation;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(value=ModelFactory.class)
public class ModelAdapterFactory implements AdapterFactory, Runnable, ModelFactory {

    /**
     * Comparator which sorts constructors by the number of parameters
     * in reverse order (most params to least params).
     */
    public class ParameterCountComparator implements Comparator<Constructor<?>> {

        @Override
        public int compare(Constructor<?> o1, Constructor<?> o2) {
            return compare(o2.getParameterTypes().length, o1.getParameterTypes().length);
        }

        public int compare(int x, int y) {
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
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
        while (ref != null) {
            log.debug("calling disposal for {}.", ref.toString());
            DisposalCallbackRegistryImpl registry = disposalCallbacks.remove(ref);
            registry.onDisposed();
            ref = queue.poll();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(ModelAdapterFactory.class);

    @Reference(name = "injector", referenceInterface = Injector.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Map<Object, Injector> injectors = new TreeMap<Object, Injector>();

    private volatile Injector[] sortedInjectors = new Injector[0];

    @Reference(name = "injectAnnotationProcessorFactory", referenceInterface = InjectAnnotationProcessorFactory.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Map<Object, InjectAnnotationProcessorFactory> injectAnnotationProcessorFactories = new TreeMap<Object, InjectAnnotationProcessorFactory>();

    private volatile InjectAnnotationProcessorFactory[] sortedInjectAnnotationProcessorFactories = new InjectAnnotationProcessorFactory[0];

    private ModelPackageBundleListener listener;

    private ServiceRegistration jobRegistration;

    private ServiceRegistration configPrinterRegistration;

    @Override
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        try {
            return createModel(adaptable, type);
        } catch (ValidationException e) {
            log.warn("Some validation error occured: {}", e.toString());
            return null;
        } catch (InvalidAdaptableException e) {
            log.debug("Could not adapt from the given class", e);
            return null;
        } catch (IllegalArgumentException e) {
            log.debug("Could not instanciate class, probably not a Sling Model:", e);
            return null;
        } catch (IllegalStateException e) {
            log.error("Invalid Sling Model class", e);
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <ModelType> ModelType createModel(Object adaptable, Class<ModelType> type) throws ValidationException, InvalidAdaptableException, IllegalStateException, IllegalArgumentException {
        Model modelAnnotation = type.getAnnotation(Model.class);
        if (modelAnnotation == null) {
            throw new IllegalArgumentException("Class " + type + " does not have a model annotation, probably not a Sling Model class");
        }
        boolean isAdaptable = false;

        Class<?>[] declaredAdaptable = modelAnnotation.adaptables();
        for (Class<?> clazz : declaredAdaptable) {
            if (clazz.isInstance(adaptable)) {
                isAdaptable = true;
            }
        }
        if (!isAdaptable) {
            throw new InvalidAdaptableException("Class " + type + " cannot be adapted from " + adaptable);
        }

        if (type.isInterface()) {
            InvocationHandler handler = createInvocationHandler(adaptable, type, modelAnnotation);
            return (ModelType) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
        } else {
            try {
                return createObject(adaptable, type, modelAnnotation);
            } catch (ValidationException e1) {
                // just rethrow validation exceptions
                throw e1;
            }
            catch (Exception e2) {
                // wrap all other exceptions (e.g. the reflection exceptions) into ISEs
                throw new IllegalStateException("Could not instanciate object", e2);
            }
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

    private class SetFieldCallback implements InjectCallback {

        private final Object object;

        private SetFieldCallback(Object object) {
            this.object = object;
        }

        @Override
        public boolean inject(AnnotatedElement element, Object value) {
            return setField((Field) element, object, value);
        }
    }

    private class SetMethodsCallback implements InjectCallback {

        private final Map<Method, Object> methods;

        private SetMethodsCallback( Map<Method, Object> methods) {
            this.methods = methods;
        }

        @Override
        public boolean inject(AnnotatedElement element, Object value) {
            return setMethod((Method) element, methods, value);
        }
    }

    private boolean injectFieldOrMethod(final AnnotatedElement element, final Object adaptable, final Type type,
            final Model modelAnnotation, final DisposalCallbackRegistry registry, InjectCallback callback) {

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

        // find the right injector
        for (Injector injector : sortedInjectors) {
            if (source == null || source.equals(injector.getName())) {
                String name = getName(element, annotationProcessor);
                Object injectionAdaptable = getAdaptable(adaptable, element, annotationProcessor);
                if (injectionAdaptable != null) {
                    Object value = injector.getValue(injectionAdaptable, name, type, element, registry);
                    if (callback.inject(element, value)) {
                        wasInjectionSuccessful = true;
                        break;
                    }
                }
            }
        }
        // if injection failed, use default
        if (!wasInjectionSuccessful) {
            wasInjectionSuccessful = injectDefaultValue(element, type, annotationProcessor, callback);
        }

        // if default is not set, check if mandatory
        if (!wasInjectionSuccessful && !isOptional(element, modelAnnotation, annotationProcessor)) {
            return false;
        }
        return true;
    }

    private InvocationHandler createInvocationHandler(final Object adaptable, final Class<?> type, Model modelAnnotation) throws ValidationException {
        Set<Method> injectableMethods = collectInjectableMethods(type);
        final Map<Method, Object> methods = new HashMap<Method, Object>();
        SetMethodsCallback callback = new SetMethodsCallback(methods);
        MapBackedInvocationHandler handler = new MapBackedInvocationHandler(methods);

        DisposalCallbackRegistryImpl registry = createAndRegisterCallbackRegistry(handler);
        Set<Method> requiredMethods = new HashSet<Method>();

        for (Method method : injectableMethods) {
            Type returnType = mapPrimitiveClasses(method.getGenericReturnType());
            if (!injectFieldOrMethod(method, adaptable, returnType, modelAnnotation, registry, callback)) {
                requiredMethods.add(method);
            }
        }
        registry.seal();
        if (!requiredMethods.isEmpty()) {
            throw new ValidationException("Required methods "+ requiredMethods + " on model interface " + type + " were not able to be injected.");
        }
        return handler;
    }

    private DisposalCallbackRegistryImpl createAndRegisterCallbackRegistry(Object object) {
        PhantomReference<Object> reference = new PhantomReference<Object>(object, queue);
        DisposalCallbackRegistryImpl registry = new DisposalCallbackRegistryImpl();
        disposalCallbacks.put(reference, registry);
        return registry;
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

    @SuppressWarnings("unchecked")
    private <AdapterType> AdapterType createObject(Object adaptable, Class<AdapterType> type, Model modelAnnotation)
            throws InstantiationException, InvocationTargetException, IllegalAccessException, IllegalStateException {
        Set<Field> injectableFields = collectInjectableFields(type);

        Constructor<?>[] constructors = type.getConstructors();
        if (constructors.length == 0) {
            throw new IllegalStateException("Model class " +  type.getName() + " does not have a public constructor.");
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
            throw new IllegalStateException("Model class " +  type.getName() + " does not have a usable constructor");
        }

        final AdapterType object;
        if (constructorHasParam) {
            object = constructorToUse.newInstance(adaptable);
        } else {
            object = constructorToUse.newInstance();
        }
        InjectCallback callback = new SetFieldCallback(object);

        DisposalCallbackRegistryImpl registry = createAndRegisterCallbackRegistry(object);

        Set<Field> requiredFields = new HashSet<Field>();

        for (Field field : injectableFields) {
            Type fieldType = mapPrimitiveClasses(field.getGenericType());
            if (!injectFieldOrMethod(field, adaptable, fieldType, modelAnnotation, registry, callback)) {
                requiredFields.add(field);
            }
        }

        registry.seal();
        if (!requiredFields.isEmpty()) {
            throw new ValidationException("Required properties "+ requiredFields +" on model class " + type +" were not able to be injected.");
        }
        try {
            invokePostConstruct(object);
            return object;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to invoke post construct method.", e);
        }

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
                    value = defaultAnnotation.values()[0];
                } else if (injectedClass == Integer.class) {
                    value = defaultAnnotation.intValues()[0];
                } else if (injectedClass == Long.class) {
                    value = defaultAnnotation.longValues()[0];
                } else if (injectedClass == Boolean.class) {
                    value = defaultAnnotation.booleanValues()[0];
                } else if (injectedClass == Short.class) {
                    value = defaultAnnotation.shortValues()[0];
                } else if (injectedClass == Float.class) {
                    value = defaultAnnotation.floatValues()[0];
                } else if (injectedClass == Double.class) {
                    value = defaultAnnotation.doubleValues()[0];
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
        if (type instanceof Class<?>) {
            return ClassUtils.primitiveToWrapper((Class<?>) type);
        } else {
            return type;
        }
    }

    private boolean setField(Field field, Object createdObject, Object value) {
        if (value != null) {
            if (!isAcceptableType(field.getType(), field.getGenericType(), value)) {
                Class<?> declaredType = field.getType();
                Type genericType = field.getGenericType();
                if (value instanceof Adaptable) {
                    value = createModelOrAdaptTo((Adaptable) value, field.getType());
                    if (value == null) {
                        return false;
                    }
                } else if (genericType instanceof ParameterizedType) {
                    ParameterizedType type = (ParameterizedType) genericType;
                    Class<?> collectionType = (Class<?>) declaredType;
                    if (value instanceof Collection &&
                            (collectionType.equals(Collection.class) || collectionType.equals(List.class)) &&
                            type.getActualTypeArguments().length == 1) {
                        List<Object> result = new ArrayList<Object>();
                        for (Object valueObject : (Collection<?>) value) {
                            if (valueObject instanceof Adaptable) {
                                Object adapted = createModelOrAdaptTo((Adaptable) valueObject, (Class<?>) type.getActualTypeArguments()[0]);
                                if (adapted != null) {
                                    result.add(adapted);
                                }
                            }
                        }
                        value = result;
                    }
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
    
    /**
     * First try to instanciate via ModelFactory and only if that fails use adaptTo mechanism. Necessary to make exception propagation work.
     * @param adaptable
     * @param type
     * @return the instanciated model/adapted object (might be null)
     */
    private Object createModelOrAdaptTo(Adaptable adaptable, Class<?> type) {
        try {
            return this.createModel(adaptable, type);
        } catch (InvalidAdaptableException e) {
            log.debug("Could not adapt from the given class", e);
        } catch (IllegalArgumentException e) {
            log.debug("Could not instanciate class, probably not a Sling Model:", e);
        }
        return adaptable.adaptTo(type);
    }

    private boolean setMethod(Method method, Map<Method, Object> methods, Object value) {
        if (value != null) {
            if (!isAcceptableType(method.getReturnType(), method.getGenericReturnType(), value) && value instanceof Adaptable) {
                value = createModelOrAdaptTo((Adaptable) value, method.getReturnType());
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

        this.jobRegistration = bundleContext.registerService(Runnable.class.getName(), this, properties);

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

    protected void bindInjectAnnotationProcessorFactory(final InjectAnnotationProcessorFactory injector, final Map<String, Object> props) {
        synchronized (injectors) {
            injectAnnotationProcessorFactories.put(ServiceUtil.getComparableForServiceRanking(props), injector);
            sortedInjectAnnotationProcessorFactories = injectAnnotationProcessorFactories.values().toArray(new InjectAnnotationProcessorFactory[injectAnnotationProcessorFactories.size()]);
        }
    }

    protected void unbindInjectAnnotationProcessorFactory(final InjectAnnotationProcessorFactory injector, final Map<String, Object> props) {
        synchronized (injectors) {
            injectAnnotationProcessorFactories.remove(ServiceUtil.getComparableForServiceRanking(props));
            sortedInjectAnnotationProcessorFactories = injectAnnotationProcessorFactories.values().toArray(new InjectAnnotationProcessorFactory[injectAnnotationProcessorFactories.size()]);
        }
    }

    Injector[] getInjectors() {
        return sortedInjectors;
    }

    InjectAnnotationProcessorFactory[] getInjectAnnotationProcessorFactories() {
        return sortedInjectAnnotationProcessorFactories;
    }

}

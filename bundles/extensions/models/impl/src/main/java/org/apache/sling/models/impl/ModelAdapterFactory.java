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

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.AnnotatedElement;
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

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferencePolicyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.osgi.ServiceUtil;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.ValidationStrategy;
import org.apache.sling.models.factory.InvalidAdaptableException;
import org.apache.sling.models.factory.InvalidResourceException;
import org.apache.sling.models.factory.InvalidValidationModelException;
import org.apache.sling.models.factory.MissingElementsException;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.models.impl.Result.FailureType;
import org.apache.sling.models.impl.model.ConstructorParameter;
import org.apache.sling.models.impl.model.InjectableElement;
import org.apache.sling.models.impl.model.InjectableField;
import org.apache.sling.models.impl.model.InjectableMethod;
import org.apache.sling.models.impl.model.ModelClass;
import org.apache.sling.models.impl.model.ModelClassConstructor;
import org.apache.sling.models.impl.validation.ModelValidation;
import org.apache.sling.models.spi.AcceptsNullName;
import org.apache.sling.models.spi.DisposalCallback;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype = true, immediate = true)
@Service(value = ModelFactory.class)
@SuppressWarnings("deprecation")
public class ModelAdapterFactory implements AdapterFactory, Runnable, ModelFactory {

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
    
    @Reference(name = "injectAnnotationProcessorFactory2", referenceInterface = InjectAnnotationProcessorFactory2.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Map<Object, InjectAnnotationProcessorFactory2> injectAnnotationProcessorFactories2 = new TreeMap<Object, InjectAnnotationProcessorFactory2>();

    private volatile InjectAnnotationProcessorFactory2[] sortedInjectAnnotationProcessorFactories2 = new InjectAnnotationProcessorFactory2[0];

    @Reference(name = "staticInjectAnnotationProcessorFactory", referenceInterface = StaticInjectAnnotationProcessorFactory.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Map<Object, StaticInjectAnnotationProcessorFactory> staticInjectAnnotationProcessorFactories = new TreeMap<Object, StaticInjectAnnotationProcessorFactory>();

    @Reference(name = "implementationPicker", referenceInterface = ImplementationPicker.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Map<Object, ImplementationPicker> implementationPickers = new TreeMap<Object, ImplementationPicker>();
    
    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY, policyOption=ReferencePolicyOption.GREEDY)
    private ModelValidation modelValidation;

    ModelPackageBundleListener listener;

    final AdapterImplementations adapterImplementations = new AdapterImplementations();

    private ServiceRegistration jobRegistration;

    private ServiceRegistration configPrinterRegistration;

    // Use threadlocal to count recursive invocations and break recursing if a max. limit is reached (to avoid cyclic dependencies)
    private ThreadLocal<ThreadInvocationCounter> invocationCountThreadLocal;

    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        Result<AdapterType> result = internalCreateModel(adaptable, type);
        result.logFailures(log);
        return result.getModel();
    }

    @Override
    public <ModelType> ModelType createModel(Object adaptable, Class<ModelType> type) throws MissingElementsException,
            InvalidAdaptableException, InvalidValidationModelException, InvalidResourceException {
        if (adaptable == null) {
            throw new IllegalArgumentException("Given adaptable is null!");
        }
        if (type == null) {
            throw new IllegalArgumentException("Given type is null");
        }
        Result<ModelType> result = internalCreateModel(adaptable, type);
        result.throwException(log);
        return result.getModel();
    }

    @Override
    public boolean canCreateFromAdaptable(Object adaptable, Class<?> modelClass) throws ModelClassException {
        return innerCanCreateFromAdaptable(adaptable, modelClass);
    }

    private boolean innerCanCreateFromAdaptable(Object adaptable, Class<?> requestedType) throws ModelClassException {
        ModelClass<?> modelClass = getImplementationTypeForAdapterType(requestedType, adaptable);
        if (!modelClass.hasModelAnnotation()) {
            throw new ModelClassException(String.format("Model class '%s' does not have a model annotation", modelClass));
        }

        Class<?>[] declaredAdaptable = modelClass.getModelAnnotation().adaptables();
        for (Class<?> clazz : declaredAdaptable) {
            if (clazz.isInstance(adaptable)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isModelClass(Object adaptable, Class<?> requestedType) {
        ModelClass<?> type = getImplementationTypeForAdapterType(requestedType, adaptable);
        return type.hasModelAnnotation();
    }

    /**
     * 
     * @param type
     * @param adaptable
     * @return the implementation type to use for the desired model type
     * @see <a
     *      href="http://sling.apache.org/documentation/bundles/models.html#specifying-an-alternate-adapter-class-since-sling-models-110">Specifying
     *      an Alternate Adapter Class</a>
     */
    private <ModelType> ModelClass<ModelType> getImplementationTypeForAdapterType(Class<ModelType> requestedType, Object adaptable) {
        // lookup ModelClass wrapper for implementation type
        // additionally check if a different implementation class was registered for this adapter type
        ModelClass<ModelType> modelClass = this.adapterImplementations.lookup(requestedType, adaptable);
        if (modelClass != null) {
            log.debug("Using implementation type {} for requested adapter type {}", modelClass, requestedType);
            return modelClass;
        }
        // normally this code path is not executed, because all types are cached in adapterImplementations
        // it is still useful for unit testing
        return new ModelClass<ModelType>(requestedType, this.adapterImplementations.getStaticInjectAnnotationProcessorFactories());
    }

    @SuppressWarnings("unchecked")
    private <ModelType> Result<ModelType> internalCreateModel(Object adaptable, Class<ModelType> requestedType) {
        Result<ModelType> result = new Result<ModelType>();
        ThreadInvocationCounter threadInvocationCounter = invocationCountThreadLocal.get();
        if (threadInvocationCounter.isMaximumReached()) {
            String msg = String.format("Adapting %s to %s failed, too much recursive invocations (>=%s).",
                    new Object[] { adaptable, requestedType, threadInvocationCounter.maxRecursionDepth });
            result.addFailure(FailureType.OTHER, msg);
            return result;
        };
        threadInvocationCounter.increase();
        try {
            // check if a different implementation class was registered for this adapter type
            ModelClass<ModelType> modelClass = getImplementationTypeForAdapterType(requestedType, adaptable);

            if (!modelClass.hasModelAnnotation()) {
                result.addFailure(FailureType.NO_MODEL_ANNOTATION, modelClass.getType());
                return result;
            }
            boolean isAdaptable = false;

            Model modelAnnotation = modelClass.getModelAnnotation();
            Class<?>[] declaredAdaptable = modelAnnotation.adaptables();
            for (Class<?> clazz : declaredAdaptable) {
                if (clazz.isInstance(adaptable)) {
                    isAdaptable = true;
                }
            }
            if (!isAdaptable) {
                result.addFailure(FailureType.ADAPTABLE_DOES_NOT_MATCH, modelClass.getType());
            } else {
                if (!validateModel(modelAnnotation, adaptable, result)) {
                    return result;
                }
                if (modelClass.getType().isInterface()) {
                    InvocationHandler handler = createInvocationHandler(adaptable, modelClass, result);
                    if (handler != null) {
                        ModelType model = (ModelType) Proxy.newProxyInstance(modelClass.getType().getClassLoader(), new Class<?>[] { modelClass.getType() }, handler);
                        result.setModel(model);
                    }
                } else {
                    try {
                        ModelType model = createObject(adaptable, modelClass, result);
                        result.setModel(model);
                        return result;
                    } catch (Exception e) {
                        result.addFailure(FailureType.OTHER, "Unable to create object", e);
                    }
                }
            }
            return result;
        } finally {
            threadInvocationCounter.decrease();
        }
    }
    
    private <ModelType> boolean validateModel(Model modelAnnotation, Object adaptable, Result<ModelType> result) {
        if (modelAnnotation.validation() != ValidationStrategy.DISABLED) {
            if (modelValidation == null) {
                result.addFailure(FailureType.VALIDATION_NOT_AVAILABLE);
                return false;
            }
            Resource resource = null;
            if (adaptable instanceof SlingHttpServletRequest) {
                resource = ((SlingHttpServletRequest)adaptable).getResource();
            } else if (adaptable instanceof Resource) {
                resource = (Resource)adaptable;
            }
            if (resource != null) {
                if (!modelValidation.validate(resource, modelAnnotation.validation() == ValidationStrategy.REQUIRED, result)) {
                    return false;
                }
            } else {
                result.addFailureWithParameters(FailureType.ADAPTABLE_NOT_USABLE_FOR_VALIDATION, adaptable.getClass().getName());
                return false;
            }
        }
        return true;
    }

    private static interface InjectCallback {
        /**
         * Is called each time when the given value should be injected into the given element
         * @param element
         * @param value
         * @param result
         * @return true if injection was successful otherwise false
         */
        public boolean inject(InjectableElement element, Object value, Result<?> result);
    }

    private class SetFieldCallback implements InjectCallback {

        private final Object object;

        private SetFieldCallback(Object object) {
            this.object = object;
        }

        @Override
        public boolean inject(InjectableElement element, Object value, Result<?> result) {
            return setField((InjectableField) element, object, value, result);
        }
    }

    private class SetMethodsCallback implements InjectCallback {

        private final Map<Method, Object> methods;

        private SetMethodsCallback( Map<Method, Object> methods) {
            this.methods = methods;
        }

        @Override
        public boolean inject(InjectableElement element, Object value, Result<?> result) {
            return setMethod((InjectableMethod) element, methods, value, result);
        }
    }

    private class SetConstructorParameterCallback implements InjectCallback {

        private final List<Object> parameterValues;

        private SetConstructorParameterCallback(List<Object> parameterValues) {
            this.parameterValues = parameterValues;
        }

        @Override
        public boolean inject(InjectableElement element, Object value, Result<?> result) {
            return setConstructorParameter((ConstructorParameter)element, parameterValues, value, result);
        }
    }

    private boolean injectElement(final InjectableElement element, final Object adaptable, 
            final Model modelAnnotation, final DisposalCallbackRegistry registry,
            final InjectCallback callback, Result<?> result) {

        InjectAnnotationProcessor annotationProcessor = null;
        String source = element.getSource();
        boolean wasInjectionSuccessful = false;

        // find an appropriate annotation processor
        for (InjectAnnotationProcessorFactory2 factory : sortedInjectAnnotationProcessorFactories2) {
            annotationProcessor = factory.createAnnotationProcessor(adaptable, element.getAnnotatedElement());
            if (annotationProcessor != null) {
                break;
            }
        }
        if (annotationProcessor == null) {
            for (InjectAnnotationProcessorFactory factory : sortedInjectAnnotationProcessorFactories) {
                annotationProcessor = factory.createAnnotationProcessor(adaptable, element.getAnnotatedElement());
                if (annotationProcessor != null) {
                    break;
                }
            }
        }

        String name = getName(element, annotationProcessor);
        Object injectionAdaptable = getAdaptable(adaptable, element, annotationProcessor);

        if (injectionAdaptable != null) {
            // find the right injector
            for (Injector injector : sortedInjectors) {
                if (source == null || source.equals(injector.getName())) {
                    if (name != null || injector instanceof AcceptsNullName) {
                        Object value = injector.getValue(injectionAdaptable, name, element.getType(), element.getAnnotatedElement(), registry);
                        if (callback.inject(element, value, result)) {
                            wasInjectionSuccessful = true;
                            break;
                        }
                    }
                }
            }
        }
        // if injection failed, use default
        if (!wasInjectionSuccessful) {
            wasInjectionSuccessful = injectDefaultValue(element, annotationProcessor, callback, result);
        }

        // if default is not set, check if mandatory
        if (!wasInjectionSuccessful) {
            if (element.isOptional(annotationProcessor)) {
                if (element.isPrimitive()) {
                    injectPrimitiveInitialValue(element, callback, result);
                }
            } else {
                return false;
            }
        }
        
        return true;
    }

    private <ModelType> InvocationHandler createInvocationHandler(final Object adaptable, final ModelClass<ModelType> modelClass, final Result<ModelType> result) {
        InjectableMethod[] injectableMethods = modelClass.getInjectableMethods();
        final Map<Method, Object> methods = new HashMap<Method, Object>();
        SetMethodsCallback callback = new SetMethodsCallback(methods);
        MapBackedInvocationHandler handler = new MapBackedInvocationHandler(methods);

        DisposalCallbackRegistryImpl registry = new DisposalCallbackRegistryImpl();
        registerCallbackRegistry(handler, registry);
        Set<Method> requiredMethods = new HashSet<Method>();

        for (InjectableMethod method : injectableMethods) {
            if (!injectElement(method, adaptable, modelClass.getModelAnnotation(), registry, callback, result)) {
                requiredMethods.add(method.getMethod());
            }
        }
        registry.seal();
        if (!requiredMethods.isEmpty()) {
            result.addFailure(FailureType.MISSING_METHODS, requiredMethods, modelClass.getType());
            return null;
        }
        return handler;
    }

    private void registerCallbackRegistry(Object object, DisposalCallbackRegistryImpl registry) {
        PhantomReference<Object> reference = new PhantomReference<Object>(object, queue);
        disposalCallbacks.put(reference, registry);
    }

    private <ModelType> ModelType createObject(final Object adaptable, final ModelClass<ModelType> modelClass, final Result<ModelType> result)
            throws InstantiationException, InvocationTargetException, IllegalAccessException {
        DisposalCallbackRegistryImpl registry = new DisposalCallbackRegistryImpl();

        ModelClassConstructor<ModelType> constructorToUse = getBestMatchingConstructor(adaptable, modelClass);
        if (constructorToUse == null) {
            result.addFailure(FailureType.NO_USABLE_CONSTRUCTOR, modelClass.getType());
            return null;
        }

        final ModelType object;
        if (constructorToUse.getConstructor().getParameterTypes().length == 0) {
            // no parameters for constructor injection? instantiate it right away
            object = constructorToUse.getConstructor().newInstance();
        } else {
            // instantiate with constructor injection
            // if this fails, make sure resources that may be claimed by injectors are cleared up again
            try {
                object = newInstanceWithConstructorInjection(constructorToUse, adaptable, modelClass, registry, result);
                if (object == null) {
                    registry.onDisposed();
                    return null;
                }
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

        registerCallbackRegistry(object, registry);

        InjectCallback callback = new SetFieldCallback(object);

        Set<Field> requiredFields = new HashSet<Field>();

        InjectableField[] injectableFields = modelClass.getInjectableFields();
        for (InjectableField field : injectableFields) {
            if (!injectElement(field, adaptable, modelClass.getModelAnnotation(), registry, callback, result)) {
                requiredFields.add(field.getField());
            }
        }

        registry.seal();
        if (!requiredFields.isEmpty()) {
            result.addFailure(FailureType.MISSING_FIELDS, requiredFields, modelClass.getType());
            return null;
        }
        try {
            invokePostConstruct(object);
            return object;
        } catch (InvocationTargetException e) {
            result.addFailure(FailureType.FAILED_CALLING_POST_CONSTRUCT, e.getCause());
            return null;
        } catch (IllegalAccessException e) {
            result.addFailure(FailureType.FAILED_CALLING_POST_CONSTRUCT, e);
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
    private <ModelType> ModelClassConstructor<ModelType> getBestMatchingConstructor(Object adaptable, ModelClass<ModelType> type) {
        ModelClassConstructor[] constructors = type.getConstructors();

        for (ModelClassConstructor constructor : constructors) {
            // first try to find the constructor with most parameters and @Inject annotation
            if (constructor.hasInjectAnnotation()) {
                return constructor;
            }
            // compatibility mode for sling models implementation <= 1.0.6:
            // support constructor without @Inject if it has exactly one parameter matching the adaptable class
            final Class<?>[] paramTypes = constructor.getConstructor().getParameterTypes();
            if (paramTypes.length == 1) {
                Class<?> paramType = constructor.getConstructor().getParameterTypes()[0];
                if (paramType.isInstance(adaptable)) {
                    return constructor;
                }
            }
            // if no constructor for injection found use public constructor without any params
            if (constructor.getConstructor().getParameterTypes().length == 0) {
                return constructor;
            }
        }
        return null;
    }

    private <ModelType> ModelType newInstanceWithConstructorInjection(final ModelClassConstructor<ModelType> constructor, final Object adaptable,
            final ModelClass<ModelType> modelClass, final DisposalCallbackRegistry registry, final Result<ModelType> result)
            throws InstantiationException, InvocationTargetException, IllegalAccessException {
        ConstructorParameter[] parameters = constructor.getConstructorParameters();

        Set<AnnotatedElement> requiredParameters = new HashSet<AnnotatedElement>();
        List<Object> paramValues = new ArrayList<Object>(Arrays.asList(new Object[parameters.length]));
        InjectCallback callback = new SetConstructorParameterCallback(paramValues);

        for (int i = 0; i < parameters.length; i++) {
            if (!injectElement(parameters[i], adaptable, modelClass.getModelAnnotation(), registry, callback, result)) {
                requiredParameters.add(parameters[i].getAnnotatedElement());
            }
        }
        if (!requiredParameters.isEmpty()) {
            result.addFailure(FailureType.MISSING_CONSTRUCTOR_PARAMS, requiredParameters, modelClass.getType());
            return null;
        }
        return constructor.getConstructor().newInstance(paramValues.toArray(new Object[paramValues.size()]));
    }

    private boolean injectDefaultValue(InjectableElement point, InjectAnnotationProcessor processor,
            InjectCallback callback, Result<?> result) {

        if (processor != null) {
            if (processor.hasDefault()) {
                return callback.inject(point, processor.getDefault(), result);
            }
        }

        Object value = point.getDefaultValue();
        if (value != null) {
            return callback.inject(point, value, result);
        }
        else {
            return false;
        }
    }

    /**
     * Injects the default initial value for the given primitive class which
     * cannot be null (e.g. int = 0, boolean = false).
     * 
     * @param point Annotated element
     * @param wrapperType Non-primitive wrapper class for primitive class
     * @param callback Inject callback
     * @param result
     */
    private void injectPrimitiveInitialValue(InjectableElement point, InjectCallback callback, Result<?> result) {
        Type primitiveType = ReflectionUtil.mapWrapperClasses(point.getType());
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
            callback.inject(point, value, result);
        };
    }
    
    private Object getAdaptable(Object adaptable, InjectableElement point, InjectAnnotationProcessor processor) {
        String viaPropertyName = null;
        if (processor != null) {
            viaPropertyName = processor.getVia();
        }
        if (viaPropertyName == null) {
            viaPropertyName = point.getVia();
        }
        if (viaPropertyName == null) {
            return adaptable;
        }
        try {
            return PropertyUtils.getProperty(adaptable, viaPropertyName);
        } catch (Exception e) {
            log.error("Unable to execution projection " + viaPropertyName, e);
            return null;
        }
    }

    private String getName(InjectableElement element, InjectAnnotationProcessor processor) {
        // try to get the name from injector-specific annotation
        if (processor != null) {
            String name = processor.getName();
            if (name != null) {
                return name;
            }
        }
        // get name from @Named annotation or element name
        return element.getName();
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

    private void invokePostConstruct(Object object) throws InvocationTargetException, IllegalAccessException {
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

    private boolean setField(InjectableField injectableField, Object createdObject, Object value, Result<?> result) {
        if (value != null) {
            Field field = injectableField.getField();
            value = adaptIfNecessary(value, field.getType(), field.getGenericType(), result);
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

    private boolean setMethod(InjectableMethod injectableMethod, Map<Method, Object> methods, Object value, Result<?> result) {
        if (value != null) {
            Method method = injectableMethod.getMethod();
            value = adaptIfNecessary(value, method.getReturnType(), method.getGenericReturnType(), result);
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

    private boolean setConstructorParameter(ConstructorParameter constructorParameter, List<Object> parameterValues, Object value, Result<?> result) {
        if (value != null && constructorParameter.getParameterType() instanceof Class<?>) {
            value = adaptIfNecessary(value, (Class<?>) constructorParameter.getParameterType(), constructorParameter.getGenericType(), result);
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

    private Object adaptIfNecessary(Object value, Class<?> type, Type genericType, Result<?> parentResult) {
        if (!isAcceptableType(type, genericType, value)) {
            Class<?> declaredType = type;
            if (isModelClass(value, type) && canCreateFromAdaptable(value, type)) {
                Result<?> result = internalCreateModel(value, type);
                if (result.getModel() == null) {
                    parentResult.appendFailures(result);
                    value = null;
                } else {
                    value = result.getModel();
                }
            } else if (value instanceof Adaptable) {
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

    protected void bindInjectAnnotationProcessorFactory(final InjectAnnotationProcessorFactory factory, final Map<String, Object> props) {
        synchronized (injectAnnotationProcessorFactories) {
            injectAnnotationProcessorFactories.put(ServiceUtil.getComparableForServiceRanking(props), factory);
            sortedInjectAnnotationProcessorFactories = injectAnnotationProcessorFactories.values().toArray(new InjectAnnotationProcessorFactory[injectAnnotationProcessorFactories.size()]);
        }
    }

    protected void unbindInjectAnnotationProcessorFactory(final InjectAnnotationProcessorFactory factory, final Map<String, Object> props) {
        synchronized (injectAnnotationProcessorFactories) {
            injectAnnotationProcessorFactories.remove(ServiceUtil.getComparableForServiceRanking(props));
            sortedInjectAnnotationProcessorFactories = injectAnnotationProcessorFactories.values().toArray(new InjectAnnotationProcessorFactory[injectAnnotationProcessorFactories.size()]);
        }
    }
    protected void bindInjectAnnotationProcessorFactory2(final InjectAnnotationProcessorFactory2 factory, final Map<String, Object> props) {
        synchronized (injectAnnotationProcessorFactories2) {
            injectAnnotationProcessorFactories2.put(ServiceUtil.getComparableForServiceRanking(props), factory);
            sortedInjectAnnotationProcessorFactories2 = injectAnnotationProcessorFactories2.values().toArray(new InjectAnnotationProcessorFactory2[injectAnnotationProcessorFactories2.size()]);
        }
    }

    protected void unbindInjectAnnotationProcessorFactory2(final InjectAnnotationProcessorFactory2 factory, final Map<String, Object> props) {
        synchronized (injectAnnotationProcessorFactories2) {
            injectAnnotationProcessorFactories2.remove(ServiceUtil.getComparableForServiceRanking(props));
            sortedInjectAnnotationProcessorFactories2 = injectAnnotationProcessorFactories2.values().toArray(new InjectAnnotationProcessorFactory2[injectAnnotationProcessorFactories2.size()]);
        }
    }

    protected void bindStaticInjectAnnotationProcessorFactory(final StaticInjectAnnotationProcessorFactory factory, final Map<String, Object> props) {
        synchronized (staticInjectAnnotationProcessorFactories) {
            staticInjectAnnotationProcessorFactories.put(ServiceUtil.getComparableForServiceRanking(props), factory);
            this.adapterImplementations.setStaticInjectAnnotationProcessorFactories(staticInjectAnnotationProcessorFactories.values());
        }
    }

    protected void unbindStaticInjectAnnotationProcessorFactory(final StaticInjectAnnotationProcessorFactory factory, final Map<String, Object> props) {
        synchronized (staticInjectAnnotationProcessorFactories) {
            staticInjectAnnotationProcessorFactories.remove(ServiceUtil.getComparableForServiceRanking(props));
            this.adapterImplementations.setStaticInjectAnnotationProcessorFactories(staticInjectAnnotationProcessorFactories.values());
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

    InjectAnnotationProcessorFactory2[] getInjectAnnotationProcessorFactories2() {
        return sortedInjectAnnotationProcessorFactories2;
    }

    Collection<StaticInjectAnnotationProcessorFactory> getStaticInjectAnnotationProcessorFactories() {
        return staticInjectAnnotationProcessorFactories.values();
    }

    ImplementationPicker[] getImplementationPickers() {
        return adapterImplementations.getImplementationPickers();
    }

}

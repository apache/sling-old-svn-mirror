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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferencePolicyOption;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.osgi.RankedServices;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.ValidationStrategy;
import org.apache.sling.models.factory.InvalidAdaptableException;
import org.apache.sling.models.factory.InvalidModelException;
import org.apache.sling.models.factory.MissingElementException;
import org.apache.sling.models.factory.MissingElementsException;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.models.factory.PostConstructException;
import org.apache.sling.models.factory.ValidationException;
import org.apache.sling.models.impl.model.ConstructorParameter;
import org.apache.sling.models.impl.model.InjectableElement;
import org.apache.sling.models.impl.model.InjectableField;
import org.apache.sling.models.impl.model.InjectableMethod;
import org.apache.sling.models.impl.model.ModelClass;
import org.apache.sling.models.impl.model.ModelClassConstructor;
import org.apache.sling.models.spi.AcceptsNullName;
import org.apache.sling.models.spi.DisposalCallback;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.ModelValidation;
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

@Component(metatype = true, immediate = true,
        label = "Apache Sling Model Adapter Factory")
@Service(value = ModelFactory.class)
@References({
    @Reference(
        name = "injector",
        referenceInterface = Injector.class,
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy = ReferencePolicy.DYNAMIC)
})
@SuppressWarnings("deprecation")
public class ModelAdapterFactory implements AdapterFactory, Runnable, ModelFactory {

    private static class DisposalCallbackRegistryImpl implements DisposalCallbackRegistry {

        private List<DisposalCallback> callbacks = new ArrayList<DisposalCallback>();

        @Override
        public void addDisposalCallback(@Nonnull DisposalCallback callback) {
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
        java.lang.ref.Reference<?> ref = queue.poll();
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


    private final @Nonnull ConcurrentMap<String, RankedServices<Injector>> injectors = new ConcurrentHashMap<String, RankedServices<Injector>>();
    private final @Nonnull RankedServices<Injector> sortedInjectors = new RankedServices<Injector>();

    @Reference(name = "injectAnnotationProcessorFactory", referenceInterface = InjectAnnotationProcessorFactory.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final @Nonnull RankedServices<InjectAnnotationProcessorFactory> injectAnnotationProcessorFactories = new RankedServices<InjectAnnotationProcessorFactory>();

    @Reference(name = "injectAnnotationProcessorFactory2", referenceInterface = InjectAnnotationProcessorFactory2.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final @Nonnull RankedServices<InjectAnnotationProcessorFactory2> injectAnnotationProcessorFactories2 = new RankedServices<InjectAnnotationProcessorFactory2>();

    @Reference(name = "staticInjectAnnotationProcessorFactory", referenceInterface = StaticInjectAnnotationProcessorFactory.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final @Nonnull RankedServices<StaticInjectAnnotationProcessorFactory> staticInjectAnnotationProcessorFactories = new RankedServices<StaticInjectAnnotationProcessorFactory>();

    @Reference(name = "implementationPicker", referenceInterface = ImplementationPicker.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final @Nonnull RankedServices<ImplementationPicker> implementationPickers = new RankedServices<ImplementationPicker>();
    
    // bind the service with the highest priority (if a new one comes in this service gets restarted)
    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY, policyOption=ReferencePolicyOption.GREEDY)
    private ModelValidation modelValidation = null;

    ModelPackageBundleListener listener;

    final AdapterImplementations adapterImplementations = new AdapterImplementations();

    private ServiceRegistration jobRegistration;

    private ServiceRegistration configPrinterRegistration;

    // Use threadlocal to count recursive invocations and break recursing if a max. limit is reached (to avoid cyclic dependencies)
    private ThreadLocal<ThreadInvocationCounter> invocationCountThreadLocal;

    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        Result<AdapterType> result = internalCreateModel(adaptable, type);
        if (!result.wasSuccessful()) {
            log.warn("Could not adapt to model", result.getThrowable());
            return null;
        } else {
            return result.getValue();
        }
    }

    @Override
    public @Nonnull <ModelType> ModelType createModel(@Nonnull Object adaptable, @Nonnull Class<ModelType> type) throws MissingElementsException,
            InvalidAdaptableException, ValidationException, InvalidModelException {
        Result<ModelType> result = internalCreateModel(adaptable, type);
        if (!result.wasSuccessful()) {
            throw result.getThrowable();
        }
        return result.getValue();
    }

    @Override
    public boolean canCreateFromAdaptable(@Nonnull Object adaptable, @Nonnull Class<?> modelClass) throws ModelClassException {
        return internalCanCreateFromAdaptable(adaptable, modelClass);
    }

    private boolean internalCanCreateFromAdaptable(Object adaptable, Class<?> requestedType) throws ModelClassException {
        try {
            ModelClass<?> modelClass = getImplementationTypeForAdapterType(requestedType, adaptable);
            Class<?>[] declaredAdaptable = modelClass.getModelAnnotation().adaptables();
            for (Class<?> clazz : declaredAdaptable) {
                if (clazz.isInstance(adaptable)) {
                    return true;
                }
            }
        } catch (ModelClassException e) {
            log.debug("Could not find implementation for given type " + requestedType + ". Probably forgot either the model annotation or it was not registered as adapter factory (yet)", e);
            return false;
        }
        return false;
    }

    @Override
    @Deprecated
    public boolean isModelClass(@Nonnull Object adaptable, @Nonnull Class<?> requestedType) {
        try {
            getImplementationTypeForAdapterType(requestedType, adaptable);
        } catch (ModelClassException e) {
            log.debug("Could not find implementation for given adaptable. Probably forgot either the model annotation or it was not registered as adapter factory (yet)", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean isModelClass(@Nonnull Class<?> type) {
        return this.adapterImplementations.isModelClass(type);
    }

    /**
     * 
     * @param requestedType the adapter type
     * @param adaptable the adaptable
     * @return the implementation type to use for the desired model type or null if there is none registered
     * @see <a
     *      href="http://sling.apache.org/documentation/bundles/models.html#specifying-an-alternate-adapter-class-since-sling-models-110">Specifying
     *      an Alternate Adapter Class</a>
     */
    private <ModelType> ModelClass<ModelType> getImplementationTypeForAdapterType(Class<ModelType> requestedType, Object adaptable) {
        // lookup ModelClass wrapper for implementation type
        // additionally check if a different implementation class was registered for this adapter type
        // the adapter implementation is initially filled by the ModelPackageBundleList
        ModelClass<ModelType> modelClass = this.adapterImplementations.lookup(requestedType, adaptable);
        if (modelClass != null) {
            log.debug("Using implementation type {} for requested adapter type {}", modelClass, requestedType);
            return modelClass;
        }
        // throw exception here
        throw new ModelClassException("Could not yet find an adapter factory for the model " + requestedType + " from adaptable " + adaptable.getClass());
    }

    @SuppressWarnings("unchecked")
    private <ModelType> Result<ModelType> internalCreateModel(Object adaptable, Class<ModelType> requestedType) {
        Result<ModelType> result;
        ThreadInvocationCounter threadInvocationCounter = invocationCountThreadLocal.get();
        if (threadInvocationCounter.isMaximumReached()) {
            String msg = String.format("Adapting %s to %s failed, too much recursive invocations (>=%s).",
                    adaptable, requestedType, threadInvocationCounter.maxRecursionDepth);
            return new Result<ModelType>(new ModelClassException(msg));
        }
        threadInvocationCounter.increase();
        try {
            // check if a different implementation class was registered for this adapter type
            ModelClass<ModelType> modelClass = getImplementationTypeForAdapterType(requestedType, adaptable);

            if (!modelClass.hasModelAnnotation()) {
                String msg = String.format("Provided Adapter class does not have a Model annotation: %s", modelClass.getType());
                return new Result<ModelType>(new ModelClassException(msg));
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
                String msg = String.format("Adaptables (%s) are not acceptable for the model class: %s", StringUtils.join(declaredAdaptable), modelClass.getType());
                return new Result<ModelType>(new InvalidAdaptableException(msg)); 
            } else {
                RuntimeException t = validateModel(adaptable, modelClass.getType(), modelAnnotation);
                if (t != null) {
                    return new Result<ModelType>(t);
                }
                if (modelClass.getType().isInterface()) {
                    Result<InvocationHandler> handlerResult = createInvocationHandler(adaptable, modelClass);
                    if (handlerResult.wasSuccessful()) {
                        ModelType model = (ModelType) Proxy.newProxyInstance(modelClass.getType().getClassLoader(), new Class<?>[] { modelClass.getType() }, handlerResult.getValue());
                        result = new Result<ModelType>(model);
                    } else {
                        return new Result<ModelType>(handlerResult.getThrowable());
                    }
                } else {
                    try {
                        result = createObject(adaptable, modelClass);
                    } catch (Exception e) {
                        String msg = String.format("Unable to create model %s", modelClass.getType());
                        return new Result<ModelType>(new ModelClassException(msg, e));
                    }
                }
            }
            return result;
        } finally {
            threadInvocationCounter.decrease();
        }
    }
    
    private <ModelType> RuntimeException validateModel(Object adaptable, Class<ModelType> modelType, Model modelAnnotation) {
        if (modelAnnotation.validation() != ValidationStrategy.DISABLED) {
            if (modelValidation == null) {
                return new ValidationException("No active service for ModelValidation found, therefore no validation can be performed.");
            }
            return modelValidation.validate(adaptable, modelType, modelAnnotation.validation() == ValidationStrategy.REQUIRED);
        }
        return null;
    }

    private interface InjectCallback {
        /**
         * Is called each time when the given value should be injected into the given element
         * @param element
         * @param value
         * @return an InjectionResult
         */
        RuntimeException inject(InjectableElement element, Object value);
    }

    private class SetFieldCallback implements InjectCallback {

        private final Object object;

        private SetFieldCallback(Object object) {
            this.object = object;
        }

        @Override
        public RuntimeException inject(InjectableElement element, Object value) {
            return setField((InjectableField) element, object, value);
        }
    }

    private class SetMethodsCallback implements InjectCallback {

        private final Map<Method, Object> methods;

        private SetMethodsCallback( Map<Method, Object> methods) {
            this.methods = methods;
        }

        @Override
        public RuntimeException inject(InjectableElement element, Object value) {
            return setMethod((InjectableMethod) element, methods, value);
        }
    }

    private class SetConstructorParameterCallback implements InjectCallback {

        private final List<Object> parameterValues;

        private SetConstructorParameterCallback(List<Object> parameterValues) {
            this.parameterValues = parameterValues;
        }

        @Override
        public RuntimeException inject(InjectableElement element, Object value) {
            return setConstructorParameter((ConstructorParameter)element, parameterValues, value);
        }
    }

    private
    @CheckForNull
    RuntimeException injectElement(final InjectableElement element, final Object adaptable,
                                   final @Nonnull DisposalCallbackRegistry registry, final InjectCallback callback) {

        InjectAnnotationProcessor annotationProcessor = null;
        String source = element.getSource();
        boolean wasInjectionSuccessful = false;

        // find an appropriate annotation processor
        for (InjectAnnotationProcessorFactory2 factory : injectAnnotationProcessorFactories2) {
            annotationProcessor = factory.createAnnotationProcessor(adaptable, element.getAnnotatedElement());
            if (annotationProcessor != null) {
                break;
            }
        }
        if (annotationProcessor == null) {
            for (InjectAnnotationProcessorFactory factory : injectAnnotationProcessorFactories) {
                annotationProcessor = factory.createAnnotationProcessor(adaptable, element.getAnnotatedElement());
                if (annotationProcessor != null) {
                    break;
                }
            }
        }

        String name = getName(element, annotationProcessor);
        Object injectionAdaptable = getAdaptable(adaptable, element, annotationProcessor);

        RuntimeException lastInjectionException = null;
        if (injectionAdaptable != null) {
            
            // prepare the set of injectors to process. if a source is given only use injectors with this name.
            final RankedServices<Injector> injectorsToProcess;
            if (StringUtils.isEmpty(source)) {
                injectorsToProcess = sortedInjectors;
            }
            else {
                injectorsToProcess = injectors.get(source);
                if (injectorsToProcess == null) {
                    throw new IllegalArgumentException("No Sling Models Injector registered for source '" + source + "'.");
                }
            }
            
            // find the right injector
            for (Injector injector : injectorsToProcess) {
                if (name != null || injector instanceof AcceptsNullName) {
                    Object value = injector.getValue(injectionAdaptable, name, element.getType(), element.getAnnotatedElement(), registry);
                    if (value != null) {
                        lastInjectionException = callback.inject(element, value);
                        if (lastInjectionException == null) {
                            wasInjectionSuccessful = true;
                            break;
                        }
                    }
                }
            }
        }
        // if injection failed, use default
        if (!wasInjectionSuccessful) {
            Result<Boolean> defaultInjectionResult = injectDefaultValue(element, annotationProcessor, callback);
            if (defaultInjectionResult.wasSuccessful()) {
                // log previous injection error, if there was any
                if (lastInjectionException != null) {
                    log.debug("Although falling back to default value worked, injection into {} failed because of: " + lastInjectionException.getMessage(), element.getAnnotatedElement(), lastInjectionException);
                }
                wasInjectionSuccessful = defaultInjectionResult.getValue();
            } else {
                return defaultInjectionResult.getThrowable();
            }
        }

        // if default is not set, check if mandatory
        if (!wasInjectionSuccessful) {
            if (element.isOptional(annotationProcessor)) {
                // log previous injection error, if there was any
                if (lastInjectionException != null) {
                    log.debug("Injection into optional element {} failed because of: " + lastInjectionException.getMessage(), element.getAnnotatedElement(), lastInjectionException);
                }
                if (element.isPrimitive()) {
                    RuntimeException throwable = injectPrimitiveInitialValue(element, callback);
                    if (throwable != null) {
                        return throwable;
                    }
                }
            } else {
                if (lastInjectionException != null) {
                    return lastInjectionException;
                } else {
                    return new ModelClassException("No injector returned a non-null value!");
                }
            }
        }
        return null;
    }

    private <ModelType> Result<InvocationHandler> createInvocationHandler(final Object adaptable, final ModelClass<ModelType> modelClass) {
        InjectableMethod[] injectableMethods = modelClass.getInjectableMethods();
        final Map<Method, Object> methods = new HashMap<Method, Object>();
        SetMethodsCallback callback = new SetMethodsCallback(methods);
        MapBackedInvocationHandler handler = new MapBackedInvocationHandler(methods);

        DisposalCallbackRegistryImpl registry = new DisposalCallbackRegistryImpl();
        registerCallbackRegistry(handler, registry);

        MissingElementsException missingElements = new MissingElementsException("Could not create all mandatory methods for interface of model " + modelClass);
        for (InjectableMethod method : injectableMethods) {
            RuntimeException t = injectElement(method, adaptable, registry, callback);
            if (t != null) {
                missingElements.addMissingElementExceptions(new MissingElementException(method.getAnnotatedElement(), t));
            }
        }
        registry.seal();
        if (!missingElements.isEmpty()) {
            return new Result<InvocationHandler>(missingElements);
        }
        return new Result<InvocationHandler>(handler);
    }

    private void registerCallbackRegistry(Object object, DisposalCallbackRegistryImpl registry) {
        PhantomReference<Object> reference = new PhantomReference<Object>(object, queue);
        disposalCallbacks.put(reference, registry);
    }

    private <ModelType> Result<ModelType> createObject(final Object adaptable, final ModelClass<ModelType> modelClass)
            throws InstantiationException, InvocationTargetException, IllegalAccessException {
        DisposalCallbackRegistryImpl registry = new DisposalCallbackRegistryImpl();

        ModelClassConstructor<ModelType> constructorToUse = getBestMatchingConstructor(adaptable, modelClass);
        if (constructorToUse == null) {
            return new Result<ModelType>(new ModelClassException("Unable to find a useable constructor for model " + modelClass.getType()));
        }

        final ModelType object;
        if (constructorToUse.getConstructor().getParameterTypes().length == 0) {
            // no parameters for constructor injection? instantiate it right away
            object = constructorToUse.getConstructor().newInstance();
        } else {
            // instantiate with constructor injection
            // if this fails, make sure resources that may be claimed by injectors are cleared up again
            try {
                Result<ModelType> result = newInstanceWithConstructorInjection(constructorToUse, adaptable, modelClass, registry);
                if (!result.wasSuccessful()) {
                    registry.onDisposed();
                    return result;
                } else {
                    object = result.getValue();
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

        InjectableField[] injectableFields = modelClass.getInjectableFields();
        MissingElementsException missingElements = new MissingElementsException("Could not inject all required fields into " + modelClass.getType());
        for (InjectableField field : injectableFields) {
            RuntimeException t = injectElement(field, adaptable, registry, callback);
            if (t != null) {
                missingElements.addMissingElementExceptions(new MissingElementException(field.getAnnotatedElement(), t));
            }
        }

        registry.seal();
        if (!missingElements.isEmpty()) {
            return new Result<ModelType>(missingElements);
        }
        try {
            invokePostConstruct(object);
        } catch (InvocationTargetException e) {
            return new Result<ModelType>(new PostConstructException("Post-construct method has thrown an exception for model " + modelClass.getType(), e.getCause()));
        } catch (IllegalAccessException e) {
            new Result<ModelType>(new ModelClassException("Could not call post-construct method for model " + modelClass.getType(), e));
        }
        return new Result<ModelType>(object);
    }

    /**
     * Gets best matching constructor for constructor injection - or default constructor if none is found.
     * @param adaptable Adaptable instance
     * @param type Model type
     * @return Constructor or null if none found
     */
    @SuppressWarnings("unchecked")
    private <ModelType> ModelClassConstructor<ModelType> getBestMatchingConstructor(Object adaptable, ModelClass<ModelType> type) {
        ModelClassConstructor<ModelType>[] constructors = type.getConstructors();

        for (ModelClassConstructor<ModelType> constructor : constructors) {
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

    private <ModelType> Result<ModelType> newInstanceWithConstructorInjection(final ModelClassConstructor<ModelType> constructor, final Object adaptable,
            final ModelClass<ModelType> modelClass, final DisposalCallbackRegistry registry)
            throws InstantiationException, InvocationTargetException, IllegalAccessException {
        ConstructorParameter[] parameters = constructor.getConstructorParameters();

        List<Object> paramValues = new ArrayList<Object>(Arrays.asList(new Object[parameters.length]));
        InjectCallback callback = new SetConstructorParameterCallback(paramValues);

        MissingElementsException missingElements = new MissingElementsException("Required constructor parameters were not able to be injected on model " + modelClass.getType());
        for (int i = 0; i < parameters.length; i++) {
            RuntimeException t = injectElement(parameters[i], adaptable, registry, callback);
            if (t != null) {
                missingElements.addMissingElementExceptions(new MissingElementException(parameters[i].getAnnotatedElement(), t));
            }
        }
        if (!missingElements.isEmpty()) {
            return new Result<ModelType>(missingElements);
        }
        return new Result<ModelType>(constructor.getConstructor().newInstance(paramValues.toArray(new Object[paramValues.size()])));
    }

    private Result<Boolean> injectDefaultValue(InjectableElement point, InjectAnnotationProcessor processor,
            InjectCallback callback) {
        if (processor != null) {
            if (processor.hasDefault()) {
                RuntimeException t = callback.inject(point, processor.getDefault());
                if (t == null) {
                    return new Result<Boolean>(Boolean.TRUE);
                } else {
                    return new Result<Boolean>(t);
                }
            }
        }

        Object value = point.getDefaultValue();
        if (value != null) {
            RuntimeException t = callback.inject(point, value);
            if (t == null) {
                return new Result<Boolean>(Boolean.TRUE);
            } else {
                return new Result<Boolean>(t);
            }
        }
        else {
            return new Result<Boolean>(Boolean.FALSE);
        }
    }

    /**
     * Injects the default initial value for the given primitive class which
     * cannot be null (e.g. int = 0, boolean = false).
     * 
     * @param point Annotated element
     * @param callback Inject callback
     */
    private RuntimeException injectPrimitiveInitialValue(InjectableElement point, InjectCallback callback) {
        Type primitiveType = ReflectionUtil.mapWrapperClasses(point.getType());
        Object value = null;
        if (primitiveType == int.class) {
            value = 0;
        } else if (primitiveType == long.class) {
            value = 0L;
        } else if (primitiveType == boolean.class) {
            value = Boolean.FALSE;
        } else if (primitiveType == double.class) {
            value = 0.0d;
        } else if (primitiveType == float.class) {
            value = 0.0f;
        } else if (primitiveType == short.class) {
            value = (short) 0;
        } else if (primitiveType == byte.class) {
            value = (byte) 0;
        } else if (primitiveType == char.class) {
            value = '\u0000';
        }
        if (value != null) {
            return callback.inject(point, value);
        } else {
            return new ModelClassException(String.format("Unknown primitive type %s", primitiveType.toString()));
        }
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

    private RuntimeException setField(InjectableField injectableField, Object createdObject, Object value) {
        Field field = injectableField.getField();
        Result<Object> result = adaptIfNecessary(value, field.getType(), field.getGenericType());
        if (result.wasSuccessful()) {
            boolean accessible = field.isAccessible();
            try {
                if (!accessible) {
                    field.setAccessible(true);
                }
                field.set(createdObject, result.getValue());
            } catch (Exception e) {
                return new ModelClassException("Could not inject field due to reflection issues", e);
            } finally {
                if (!accessible) {
                    field.setAccessible(false);
                }
            }
            return null;
        } else {
            return result.getThrowable();
        }
    }

    private RuntimeException setMethod(InjectableMethod injectableMethod, Map<Method, Object> methods, Object value) {
        Method method = injectableMethod.getMethod();
        Result<Object> result = adaptIfNecessary(value, method.getReturnType(), method.getGenericReturnType());
        if (result.wasSuccessful()) {
            methods.put(method, result.getValue());
            return null;
        } else {
            return result.getThrowable();
        }
    }

    private RuntimeException setConstructorParameter(ConstructorParameter constructorParameter, List<Object> parameterValues, Object value) {
        if (constructorParameter.getParameterType() instanceof Class<?>) {
            Result<Object> result = adaptIfNecessary(value, (Class<?>) constructorParameter.getParameterType(), constructorParameter.getGenericType());
            if (result.wasSuccessful() ) {
                parameterValues.set(constructorParameter.getParameterIndex(), result.getValue());
                return null;
            } else {
                return result.getThrowable();
            }
        } else {
            return new ModelClassException(String.format("Constructor parameter with index %d is not a class!", constructorParameter.getParameterIndex()));
        }
    }

    private Result<Object> adaptIfNecessary(final Object value, final Class<?> type, final Type genericType) {
        final Object adaptedValue;
        if (!isAcceptableType(type, genericType, value)) {
            if (isModelClass(value, type) && canCreateFromAdaptable(value, type)) {
                Result<?> result = internalCreateModel(value, type);
                if (result.wasSuccessful()) {
                    adaptedValue = result.getValue();
                } else {
                    return new Result<Object>(new ModelClassException(
                        String.format("Could not create model from %s: %s", value.getClass(), result.getThrowable().getMessage()),
                        result.getThrowable()));
                }
            } else if (value instanceof Adaptable) {
                adaptedValue = ((Adaptable) value).adaptTo(type);
                if (adaptedValue == null) {
                    return new Result<Object>(new ModelClassException(String.format("Could not adapt from %s to %s", value.getClass(), type)));
                } 
            } else if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                if (value instanceof Collection &&
                        (type.equals(Collection.class) || type.equals(List.class)) &&
                        parameterizedType.getActualTypeArguments().length == 1) {
                    List<Object> result = new ArrayList<Object>();
                    for (Object valueObject : (Collection<?>) value) {
                        if (valueObject instanceof Adaptable) {
                            Object adapted = ((Adaptable) valueObject).adaptTo((Class<?>) parameterizedType.getActualTypeArguments()[0]);
                            if (adapted != null) {
                                result.add(adapted);
                            } else {
                                return new Result<Object>(new ModelClassException(
                                    String.format("Could not adapt from %s to %s within the collection", value.getClass(), type)));
                            }
                        }
                    }
                    adaptedValue = result;
                } else {
                    return new Result<Object>(new ModelClassException(String.format("%s is neither a parametrized Collection or List",
                        type)));
                }
            } else {
                return new Result<Object>(new ModelClassException(
                    String.format("Could not adapt from %s to %s, because this class is not adaptable!", value.getClass(), type)));
            }
            return new Result<Object>(adaptedValue);
        } else {
            return new Result<Object>(value);
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
        properties.put("scheduler.period", 30L);

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
        RankedServices<Injector> newRankedServices = new RankedServices<Injector>();
        RankedServices<Injector> injectorsPerInjectorName = injectors.putIfAbsent(injector.getName(), newRankedServices);
        if (injectorsPerInjectorName == null) {
            injectorsPerInjectorName = newRankedServices;
        }
        injectorsPerInjectorName.bind(injector, props);
        sortedInjectors.bind(injector, props);
    }

    protected void unbindInjector(final Injector injector, final Map<String, Object> props) {
        RankedServices<Injector> injectorsPerInjectorName = injectors.get(injector.getName());
        if (injectorsPerInjectorName != null) {
            injectorsPerInjectorName.unbind(injector, props);
        }
        sortedInjectors.unbind(injector, props);
    }

    protected void bindInjectAnnotationProcessorFactory(final InjectAnnotationProcessorFactory factory, final Map<String, Object> props) {
        injectAnnotationProcessorFactories.bind(factory, props);
    }

    protected void unbindInjectAnnotationProcessorFactory(final InjectAnnotationProcessorFactory factory, final Map<String, Object> props) {
        injectAnnotationProcessorFactories.unbind(factory, props);
    }

    protected void bindInjectAnnotationProcessorFactory2(final InjectAnnotationProcessorFactory2 factory, final Map<String, Object> props) {
        injectAnnotationProcessorFactories2.bind(factory, props);
    }

    protected void unbindInjectAnnotationProcessorFactory2(final InjectAnnotationProcessorFactory2 factory, final Map<String, Object> props) {
        injectAnnotationProcessorFactories2.unbind(factory, props);
    }

    protected void bindStaticInjectAnnotationProcessorFactory(final StaticInjectAnnotationProcessorFactory factory, final Map<String, Object> props) {
        synchronized (staticInjectAnnotationProcessorFactories) {
            staticInjectAnnotationProcessorFactories.bind(factory, props);
            this.adapterImplementations.setStaticInjectAnnotationProcessorFactories(staticInjectAnnotationProcessorFactories.get());
        }
    }

    protected void unbindStaticInjectAnnotationProcessorFactory(final StaticInjectAnnotationProcessorFactory factory, final Map<String, Object> props) {
        synchronized (staticInjectAnnotationProcessorFactories) {
            staticInjectAnnotationProcessorFactories.unbind(factory, props);
            this.adapterImplementations.setStaticInjectAnnotationProcessorFactories(staticInjectAnnotationProcessorFactories.get());
        }
    }

    protected void bindImplementationPicker(final ImplementationPicker implementationPicker, final Map<String, Object> props) {
        synchronized (implementationPickers) {
            implementationPickers.bind(implementationPicker, props);
            this.adapterImplementations.setImplementationPickers(implementationPickers.get());
        }
    }

    protected void unbindImplementationPicker(final ImplementationPicker implementationPicker, final Map<String, Object> props) {
        synchronized (implementationPickers) {
            implementationPickers.unbind(implementationPicker, props);
            this.adapterImplementations.setImplementationPickers(implementationPickers.get());
        }
    }

    @Nonnull Collection<Injector> getInjectors() {
        return sortedInjectors.get();
    }

    @Nonnull Collection<InjectAnnotationProcessorFactory> getInjectAnnotationProcessorFactories() {
        return injectAnnotationProcessorFactories.get();
    }

    @Nonnull Collection<InjectAnnotationProcessorFactory2> getInjectAnnotationProcessorFactories2() {
        return injectAnnotationProcessorFactories2.get();
    }

    @Nonnull Collection<StaticInjectAnnotationProcessorFactory> getStaticInjectAnnotationProcessorFactories() {
        return staticInjectAnnotationProcessorFactories.get();
    }

    @Nonnull ImplementationPicker[] getImplementationPickers() {
        return adapterImplementations.getImplementationPickers();
    }

    @Override
    public boolean isModelAvailableForRequest(@Nonnull SlingHttpServletRequest request) {
        return adapterImplementations.getModelClassForRequest(request) != null;
    }

    @Override
    public boolean isModelAvailableForResource(@Nonnull Resource resource) {
        return adapterImplementations.getModelClassForResource(resource) != null;
    }

    @Override
    public Object getModelFromResource(Resource resource) {
        Class<?> clazz = this.adapterImplementations.getModelClassForResource(resource);
        if (clazz == null) {
            throw new ModelClassException("Could find model registered for resource type: " + resource.getResourceType());
        }
        return handleBoundModelResult(internalCreateModel(resource, clazz));
    }

    @Override
    public Object getModelFromRequest(SlingHttpServletRequest request) {
        Class<?> clazz = this.adapterImplementations.getModelClassForRequest(request);
        if (clazz == null) {
            throw new ModelClassException("Could find model registered for request path: " + request.getServletPath());
        }
        return handleBoundModelResult(internalCreateModel(request, clazz));
    }

    private Object handleBoundModelResult(Result<?> result) {
        if (!result.wasSuccessful()) {
            throw result.getThrowable();
        } else {
            return result.getValue();
        }
    }

}

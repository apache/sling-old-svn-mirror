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
package org.apache.sling.models.impl.model;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import javax.inject.Named;

import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.Required;
import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.impl.ModelAdapterFactory;
import org.apache.sling.models.impl.ReflectionUtil;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
abstract class AbstractInjectableElement implements InjectableElement {
    
    private final AnnotatedElement element;
    private final Type type;
    private final String name;
    private final String source;
    private final String via;
    private final boolean hasDefaultValue;
    private final Object defaultValue;
    private final boolean isOptional;
    private final boolean isRequired;
    private final DefaultInjectionStrategy injectionStrategy;
    private final DefaultInjectionStrategy defaultInjectionStrategy;
    
    private static final Logger log = LoggerFactory.getLogger(ModelAdapterFactory.class);
    
    public AbstractInjectableElement(AnnotatedElement element, Type type, String defaultName,
            StaticInjectAnnotationProcessorFactory[] processorFactories, DefaultInjectionStrategy defaultInjectionStrategy) {
        this.element = element;
        this.type = type;
        InjectAnnotationProcessor2 annotationProcessor = getAnnotationProcessor(element, processorFactories);
        this.name = getName(element, defaultName, annotationProcessor);
        this.source = getSource(element);
        this.via = getVia(element, annotationProcessor);
        this.hasDefaultValue = getHasDefaultValue(element, annotationProcessor);
        this.defaultValue = getDefaultValue(element, type, annotationProcessor);
        this.isOptional = getOptional(element, annotationProcessor);
        this.isRequired = getRequired(element, annotationProcessor);
        this.injectionStrategy = getInjectionStrategy(element, annotationProcessor, defaultInjectionStrategy);
        this.defaultInjectionStrategy = defaultInjectionStrategy;
    }
    
    private static InjectAnnotationProcessor2 getAnnotationProcessor(AnnotatedElement element, StaticInjectAnnotationProcessorFactory[] processorFactories) {
        for (StaticInjectAnnotationProcessorFactory processorFactory : processorFactories) {
            InjectAnnotationProcessor2 annotationProcessor = processorFactory.createAnnotationProcessor(element);
            if (annotationProcessor != null) {
                return annotationProcessor;
            }
        }
        return null;
    }
    
    private static String getName(AnnotatedElement element, String defaultName, InjectAnnotationProcessor2 annotationProcessor) {
        String name = null;
        if (annotationProcessor != null) {
            name = annotationProcessor.getName();
        }
        if (name == null) {
            Named namedAnnotation = element.getAnnotation(Named.class);
            if (namedAnnotation != null) {
                name = namedAnnotation.value();
            }
            else {
                name = defaultName;
            }
        }
        return name;
    }

    private static String getSource(AnnotatedElement element) {
        Source source = ReflectionUtil.getAnnotation(element, Source.class);
        if (source != null) {
            return source.value();
        }
        return null;
    }
    
    private static String getVia(AnnotatedElement element, InjectAnnotationProcessor2 annotationProcessor) {
        String via = null;
        if (annotationProcessor != null) {
            via = annotationProcessor.getVia();
        }
        if (via == null) {
            Via viaAnnotation = element.getAnnotation(Via.class);
            if (viaAnnotation != null) {
                via = viaAnnotation.value();
            }
        }
        return via;
    }

    private static boolean getHasDefaultValue(AnnotatedElement element, InjectAnnotationProcessor2 annotationProcessor) {
        if (annotationProcessor != null) {
            return annotationProcessor.hasDefault();
        }
        return element.isAnnotationPresent(Default.class);
    }

    private static Object getDefaultValue(AnnotatedElement element, Type type, InjectAnnotationProcessor2 annotationProcessor) {
        if (annotationProcessor != null && annotationProcessor.hasDefault()) {
            return annotationProcessor.getDefault();
        }
        
        Default defaultAnnotation = element.getAnnotation(Default.class);
        if (defaultAnnotation == null) {
            return null;
        }

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
                }
            }
        } else {
            log.warn("Cannot provide default for {}", type);
        }
        return value;
    }

    private static boolean getOptional(AnnotatedElement element, InjectAnnotationProcessor annotationProcessor) {
        if (annotationProcessor != null) {
            Boolean optional = annotationProcessor.isOptional();
            if (optional != null) {
                return optional.booleanValue();
            }
        }
        return element.isAnnotationPresent(Optional.class);
    }
    
    private static boolean getRequired(AnnotatedElement element, InjectAnnotationProcessor annotationProcessor) {
        // do not evaluate the injector-specific annotation (those are only considered for optional)
        // even setting optional=false will not make an attribute mandatory
        return element.isAnnotationPresent(Required.class);
    }
    
    private static DefaultInjectionStrategy getInjectionStrategy(AnnotatedElement element, InjectAnnotationProcessor annotationProcessor, DefaultInjectionStrategy defaultInjectionStrategy) {
        if (annotationProcessor != null) {
            if (annotationProcessor instanceof InjectAnnotationProcessor2) {
                switch (((InjectAnnotationProcessor2)annotationProcessor).getInjectionStrategy()) {
                    case OPTIONAL:
                        return DefaultInjectionStrategy.OPTIONAL;
                    case REQUIRED:
                        return DefaultInjectionStrategy.REQUIRED;
                    case DEFAULT:
                        break;
                }
            }
        }
        return defaultInjectionStrategy;
    }
    
    @Override
    public final AnnotatedElement getAnnotatedElement() {
        return this.element;
    }
    
    @Override
    public final Type getType() {
        return type;
    }

    @Override
    public final String getName() {
        return this.name;
    }
    
    @Override
    public String getSource() {
        return this.source;
    }

    @Override
    public String getVia() {
        return this.via;
    }
    
    @Override
    public boolean hasDefaultValue() {
        return this.hasDefaultValue;
    }

    @Override
    public Object getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public boolean isOptional(InjectAnnotationProcessor annotationProcessor) {
        DefaultInjectionStrategy injectionStrategy = this.injectionStrategy;
        boolean isOptional = this.isOptional;
        boolean isRequired = this.isRequired;
        
        // evaluate annotationProcessor (which depends on the adapter) 
        if (annotationProcessor != null) {
            isOptional = getOptional(getAnnotatedElement(), annotationProcessor);
            isRequired = getRequired(getAnnotatedElement(), annotationProcessor);
            injectionStrategy = getInjectionStrategy(element, annotationProcessor, defaultInjectionStrategy);
        }
        if (injectionStrategy == DefaultInjectionStrategy.REQUIRED) {
            return isOptional;
        } else {
            return !isRequired;
        }
    }

}

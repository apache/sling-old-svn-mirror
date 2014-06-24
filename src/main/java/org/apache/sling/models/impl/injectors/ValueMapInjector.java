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
package org.apache.sling.models.impl.injectors;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.AbstractInjectAnnotationProcessor;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 2000)
public class ValueMapInjector implements Injector, InjectAnnotationProcessorFactory {

    private static final Logger log = LoggerFactory.getLogger(ValueMapInjector.class);

    @Override
    public String getName() {
        return "valuemap";
    }

    public Object getValue(Object adaptable, String name, Type type, AnnotatedElement element,
            DisposalCallbackRegistry callbackRegistry) {
        ValueMap map = getMap(adaptable);
        if (map == null) {
            return null;
        } else if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            try {
                return map.get(name, clazz);
            } catch (ClassCastException e) {
                // handle case of primitive/wrapper arrays
                if (clazz.isArray()) {
                    Class<?> componentType = clazz.getComponentType();
                    if (componentType.isPrimitive()) {
                        Class<?> wrapper = ClassUtils.primitiveToWrapper(componentType);
                        if (wrapper != componentType) {
                            Object wrapperArray = map.get(name, Array.newInstance(wrapper, 0).getClass());
                            if (wrapperArray != null) {
                                return unwrapArray(wrapperArray, componentType);
                            }
                        }
                    } else {
                        Class<?> primitiveType = ClassUtils.wrapperToPrimitive(componentType);
                        if (primitiveType != componentType) {
                            Object primitiveArray = map.get(name, Array.newInstance(primitiveType, 0).getClass());
                            if (primitiveArray != null) {
                                return wrapArray(primitiveArray, componentType);
                            }
                        }
                    }
                }
                return null;
            }
        } else if (type instanceof ParameterizedType) {
            // list support
            ParameterizedType pType = (ParameterizedType) type;
            if (pType.getActualTypeArguments().length != 1) {
                return null;
            }
            Class<?> collectionType = (Class<?>) pType.getRawType();
            if (!(collectionType.equals(Collection.class) || collectionType.equals(List.class))) {
                return null;
            }

            Class<?> itemType = (Class<?>) pType.getActualTypeArguments()[0];
            Object array = map.get(name, Array.newInstance(itemType, 0).getClass());
            if (array == null) {
                return null;

            }

            return Arrays.asList((Object[]) array);
        } else {
            log.debug("ValueMapInjector doesn't support non-class types {}", type);
            return null;
        }
    }

    private ValueMap getMap(Object adaptable) {
        if (adaptable instanceof ValueMap) {
            return (ValueMap) adaptable;
        } else if (adaptable instanceof Adaptable) {
            ValueMap map = ((Adaptable) adaptable).adaptTo(ValueMap.class);
            return map;
        } else {
            return null;
        }
    }

    private Object unwrapArray(Object wrapperArray, Class<?> primitiveType) {
        int length = Array.getLength(wrapperArray);
        Object primitiveArray = Array.newInstance(primitiveType, length);
        for (int i = 0; i < length; i++) {
            Array.set(primitiveArray, i, Array.get(wrapperArray, i));
        }
        return primitiveArray;
    }

    private Object wrapArray(Object primitiveArray, Class<?> wrapperType) {
        int length = Array.getLength(primitiveArray);
        Object wrapperArray = Array.newInstance(wrapperType, length);
        for (int i = 0; i < length; i++) {
            Array.set(wrapperArray, i, Array.get(primitiveArray, i));
        }
        return wrapperArray;
    }

    @Override
    public InjectAnnotationProcessor createAnnotationProcessor(Object adaptable, AnnotatedElement element) {
        // check if the element has the expected annotation
        ValueMapValue annotation = element.getAnnotation(ValueMapValue.class);
        if (annotation != null) {
            return new ValueAnnotationProcessor(annotation, adaptable);
        }
        return null;
    }

    private static class ValueAnnotationProcessor extends AbstractInjectAnnotationProcessor {

        private final ValueMapValue annotation;

        private final Object adaptable;

        public ValueAnnotationProcessor(ValueMapValue annotation, Object adaptable) {
            this.annotation = annotation;
            this.adaptable = adaptable;
        }

        @Override
        public String getName() {
            // since null is not allowed as default value in annotations, the empty string means, the default should be
            // used!
            if (annotation.name().isEmpty()) {
                return null;
            }
            return annotation.name();
        }

        @Override
        public String getVia() {
            if (StringUtils.isNotBlank(annotation.via())) {
                return annotation.via();
            }
            // automatically go via resource, if this is the httprequest
            if (adaptable instanceof SlingHttpServletRequest) {
                return "resource";
            } else {
                return null;
            }
        }

        @Override
        public Boolean isOptional() {
            return annotation.optional();
        }
    }
}

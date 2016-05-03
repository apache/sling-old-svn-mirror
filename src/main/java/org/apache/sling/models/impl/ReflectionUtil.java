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
package org.apache.sling.models.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.ClassUtils;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotation;

/**
 * Helper methods for inspecting classes via reflection.
 */
public final class ReflectionUtil {

    private ReflectionUtil() {
        // static methods only
    }

    public static List<Field> collectInjectableFields(Class<?> type) {
        List<Field> result = new ArrayList<Field>();
        while (type != null) {
            Field[] fields = type.getDeclaredFields();
            addAnnotated(fields, result);
            type = type.getSuperclass();
        }
        return result;
    }

    public static List<Method> collectInjectableMethods(Class<?> type) {
        List<Method> result = new ArrayList<Method>();
        while (type != null) {
            Method[] methods = type.getDeclaredMethods();
            addAnnotated(methods, result);
            addAnnotatedMethodsFromInterfaces(type, result);
            type = type.getSuperclass();
        }
        return result;

    }

    private static void addAnnotatedMethodsFromInterfaces(Class<?> type, List<Method> result) {
        for (Class<?> iface : type.getInterfaces()) {
            Method[] methods = iface.getDeclaredMethods();
            addAnnotated(methods, result);
            addAnnotatedMethodsFromInterfaces(iface, result);
        }
    }

    public static <T extends AnnotatedElement> void addAnnotated(T[] elements, List<T> set) {
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

    /**
     * Get an annotation from either the element itself or on any of the
     * element's annotations (meta-annotations).
     * 
     * @param element the element
     * @param annotationClass the annotation class
     * @return the found annotation or null
     */
    public static <T extends Annotation> T getAnnotation(AnnotatedElement element, Class<T> annotationClass) {
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

    public static Type mapPrimitiveClasses(Type type) {
        if (type instanceof Class<?>) {
            return ClassUtils.primitiveToWrapper((Class<?>) type);
        } else {
            return type;
        }
    }

    public static Type mapWrapperClasses(Type type) {
        if (type instanceof Class<?>) {
            return ClassUtils.wrapperToPrimitive((Class<?>) type);
        } else {
            return type;
        }
    }

}

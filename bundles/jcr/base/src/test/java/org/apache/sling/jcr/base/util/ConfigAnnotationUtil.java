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
package org.apache.sling.jcr.base.util;

import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/** TODO move this to a testing utilities module? */
public class ConfigAnnotationUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigAnnotationUtil.class);

    @SuppressWarnings("unchecked")
    public static <T> T fromDictionary(final Class<T> clazz, final Dictionary<String, ?> properties)
            throws ConfigurationException {
        return (T)Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{ clazz },
                new Handler(copyAndVerify(properties, clazz)));
    }

    private static class Handler implements InvocationHandler {

        private Dictionary<String, ?> properties;

        private Handler(final Dictionary<String, ?> properties) {
            this.properties = properties;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final String methodName = method.getName();
            if ("toString".equals(methodName)) {
                return proxy.getClass().getName() + "[" + properties.toString() + "]";
            }

            final String propertyName = toPropertyName(methodName);
            Object value;
            if (properties != null && (value = properties.get(propertyName)) != null) {
                return value;
            } else {
                return method.getDefaultValue();
            }
        }
    }

    private static String toPropertyName(final String methodName) {
        return methodName.replace('_', '.');
    }

    private static String toMethodName(final String propertyName) {
        return propertyName.replace('.', '_');
    }

    private static <T> Dictionary<String, ?> copyAndVerify(final Dictionary<String, ?> properties, final Class<T> clazz)
            throws ConfigurationException {
        if (properties == null) {
            return null;
        }

        final Hashtable<String, Object> copy = new Hashtable<>();
        final Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            final String propertyName = keys.nextElement();
            final Object value = properties.get(propertyName);
            verifyValueType(clazz, propertyName, value);
            copy.put(propertyName, value);
        }
        return copy;
    }

    private static <T> void verifyValueType(final Class<T> clazz, final String propertyName, final Object value)
            throws ConfigurationException {
        final Method method = getDeclaredMethodByName(clazz, toMethodName(propertyName));
        if (method != null) {
            final Class<?> returnType = method.getReturnType();
            final Class<?> valueClass = value.getClass();
            if (!isAssignable(returnType, valueClass)) {
                LOG.error("Invalid value type for {} ({} instead of {})", propertyName, valueClass, returnType);
                throw new ConfigurationException(propertyName,
                        "Value of incorrect type " + valueClass.getName() + " instead of " + returnType.getName());
            }
        }
    }

    private static boolean isAssignable(final Class<?> type, final Class<?> superType) {
        final Class<?> wrappedType = primitiveToWrapper(type);
        final Class<?> wrappedSuperType = primitiveToWrapper(superType);
        return wrappedType.isAssignableFrom(wrappedSuperType);
    }

    private static Method getDeclaredMethodByName(final Class<?> clazz, final String methodName) {
        final Method[] declaredMethods = clazz.getDeclaredMethods();
        Method method = null;
        for (final Method declaredMethod : declaredMethods) {
            if (declaredMethod.getName().equals(methodName)) {
                method = declaredMethod;
                break;
            }
        }
        return method;
    }

    private static Class<?> primitiveToWrapper(Class<?> clazz) {
        if (primitiveToWrapper.containsKey(clazz)) {
            return primitiveToWrapper.get(clazz);
        } else {
            return clazz;
        }
    }

    private static final Map<Class<?>, Class<?>> primitiveToWrapper = new HashMap<Class<?>, Class<?>>();
    static {
        primitiveToWrapper.put(Boolean.TYPE, Boolean.class);
        primitiveToWrapper.put(Byte.TYPE, Byte.class);
        primitiveToWrapper.put(Character.TYPE, Character.class);
        primitiveToWrapper.put(Short.TYPE, Short.class);
        primitiveToWrapper.put(Integer.TYPE, Integer.class);
        primitiveToWrapper.put(Long.TYPE, Long.class);
        primitiveToWrapper.put(Double.TYPE, Double.class);
        primitiveToWrapper.put(Float.TYPE, Float.class);
    }
}

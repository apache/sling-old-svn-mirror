/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.scripting.sightly.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenderUtils.class);

    public static final String TO_STRING_METHOD = "toString";
    public static final String PROPERTY_ACCESS = "resolveProperty";
    public static final String COLLECTION_COERCE = "toCollection";
    public static final String NUMERIC_COERCE = "toNumber";
    public static final String STRING_COERCE = "toString";
    public static final String BOOLEAN_COERCE = "toBoolean";

    /**
     * Checks if an object is a {@link Collection} or is backed by one.
     * @param target the target object
     * @return {@code true} if the {@code target} is a collection or is backed by one, {@code false} otherwise
     */
    public static boolean isCollection(Object target) {
        return (target instanceof Collection) || (target instanceof Object[])
                || (target instanceof Iterable)
                || (target instanceof Iterator);
    }

    /**
     * Coerce the object to a numeric value
     *
     * @param object - the target object
     * @return - the numeric representation
     */
    public static Number toNumber(Object object) {
        if (object instanceof Number) {
            return (Number) object;
        }
        return 0;
    }

    /**
     * Convert the given object to a string.
     *
     * @param target - the target object
     * @return - the string representation of the object
     */
    public static String toString(Object target) {
        return objectToString(target);
    }

    /**
     * Convert the given object to a boolean value
     *
     * @param object - the target object
     * @return - the boolean representation of that object
     */
    public static boolean toBoolean(Object object) {
        return toBooleanInternal(object);
    }

    /**
     * Force the conversion of the object to a collection
     *
     * @param object - the target object
     * @return the collection representation of the object
     */
    public static Collection<Object> toCollection(Object object) {
        return obtainCollection(object);
    }

    /**
     * Force the conversion of the target object to a map
     *
     * @param object - the target object
     * @return - a map representation of the object. Default is an empty map
     */
    @SuppressWarnings("unchecked")
    public static Map toMap(Object object) {
        if (object instanceof Map) {
            return (Map) object;
        } else if (object instanceof Record) {
            Map<String, Object> map = new HashMap<String, Object>();
            Record record = (Record) object;
            Set<String> properties = record.getPropertyNames();
            for (String property : properties) {
                map.put(property, record.getProperty(property));
            }
            return map;
        }
        return Collections.emptyMap();
    }

    /**
     * Resolve a property of a target object and return its value. The property can
     * be either an index or a name
     *
     * @param target - the target object
     * @param property - the property to be resolved
     * @return - the value of the property
     */
    public static Object resolveProperty(Object target, Object property) {
        Object resolved;
        if (property instanceof Number) {
            resolved = getIndex(target, ((Number) property).intValue());
        } else {
            resolved = getProperty(target, property);
        }
        return resolved;
    }

    private static Object getProperty(Object target, Object propertyObj) {
        String property = toString(propertyObj);
        if (StringUtils.isEmpty(property)) {
            throw new IllegalArgumentException("Invalid property name");
        }
        if (target == null) {
            return null;
        }
        Object result = null;
        if (target instanceof Map) {
            result = getMapProperty((Map) target, property);
        }
        if (result == null && target instanceof Record) {
            result = ((Record) target).getProperty(property);
        }
        if (result == null) {
            result = getObjectProperty(target, property);
        }
        if (result == null && target instanceof Adaptable) {
            result = getValueMapProperty(((Adaptable) target).adaptTo(ValueMap.class), property);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> obtainCollection(Object obj) {
        if (obj == null) {
            return Collections.emptyList();
        }
        if (obj instanceof Object[]) {
            return Arrays.asList((Object[]) obj);
        }
        if (obj instanceof Collection) {
            return (Collection<Object>) obj;
        }
        if (obj instanceof Map) {
            return ((Map) obj).keySet();
        }
        if (obj instanceof Record) {
            return ((Record) obj).getPropertyNames();
        }
        if (obj instanceof Enumeration) {
            return Collections.list((Enumeration<Object>) obj);
        }
        if (obj instanceof Iterator) {
            return fromIterator((Iterator<Object>) obj);
        }
        if (obj instanceof Iterable) {
            Iterable iterable = (Iterable) obj;
            return fromIterator(iterable.iterator());
        }
        if (obj instanceof String || obj instanceof Number) {
            Collection list = new ArrayList();
            list.add(obj);
            return list;
        }
        return Collections.emptyList();
    }

    private static Collection<Object> fromIterator(Iterator<Object> iterator) {
        ArrayList<Object> result = new ArrayList<Object>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    private static boolean toBooleanInternal(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof Number) {
            Number number = (Number) obj;
            //todo should we consider precision issues?
            return !(number.doubleValue() == 0.0);
        }

        String s = obj.toString().trim();
        if ("".equals(s)) {
            return false;
        } else if ("true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s)) {
            return Boolean.parseBoolean(s);
        }

        if (obj instanceof Collection) {
            return ((Collection) obj).size() > 0;
        }

        if (obj instanceof Map) {
            return ((Map) obj).size() > 0;
        }

        if (obj instanceof Iterable<?>) {
            return ((Iterable<?>) obj).iterator().hasNext();
        }

        if (obj instanceof Iterator<?>) {
            return ((Iterator<?>) obj).hasNext();
        }

        return !(obj instanceof Object[]) || ((Object[]) obj).length > 0;
    }

    private static Object getIndex(Object obj, int index) {
        if (obj instanceof Map) {
            Map map = (Map) obj;
            if (map.containsKey(index)) {
                return map.get(index);
            }
        }
        Collection collection = toCollection(obj);
        if (collection instanceof List) {
            return getIndexSafe((List) collection, index);
        }
        return null;
    }

    private static Object getIndexSafe(List list, int index) {
        if (index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    private static Object getValueMapProperty(ValueMap valueMap, String property) {
        if (valueMap == null) {
            return null;
        }
        return valueMap.get(property);
    }

    private static Object getMapProperty(Map map, String property) {
        return map.get(property);
    }

    private static Object getObjectProperty(Object obj, String property) {
        Object result = getObjectNoArgMethod(obj, property);
        if (result == null) {
            result = getField(obj, property);
        }
        return result;
    }

    private static Object getField(Object obj, String property) {
        if (obj instanceof Object[] && "length".equals(property)) {
            // Working around this limitation: http://docs.oracle.com/javase/7/docs/api/java/lang/Class.html#getFields%28%29
            return ((Object[]) obj).length;
        }
        Class<?> cls = obj.getClass();
        try {
            Field field = cls.getDeclaredField(property);
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object getObjectNoArgMethod(Object obj, String property) {
        Class<?> cls = obj.getClass();
        Method method = findMethod(cls, property);
        if (method != null) {
            method = extractMethodInheritanceChain(cls, method);
            try {
                return method.invoke(obj);
            } catch (Exception e) {
                LOGGER.error("Cannot access method " + property + " on object " + obj.toString(), e);
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> cls, String baseName) {
        Method[] publicMethods = cls.getMethods();
        String capitalized = StringUtils.capitalize(baseName);
        for (Method m : publicMethods) {
            if (m.getParameterTypes().length == 0) {
                String methodName = m.getName();
                if (baseName.equals(methodName)
                        || ("get" + capitalized).equals(methodName)
                        || ("is" + capitalized).equals(methodName)) {

                    // this method is good, check whether allowed
                    if (isMethodAllowed(m)) {
                        return m;
                    }

                    // method would match but is not allowed, abort
                    break;
                }
            }
        }

        return null;
    }

    private static boolean isMethodAllowed(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        //methods of the Object.class are forbidden (except toString, which is allowed)
        return declaringClass != Object.class || TO_STRING_METHOD.equals(method.getName());
    }


    private static String objectToString(Object obj) {
        String output = "";
        if (obj != null) {
            if (obj instanceof String) {
                output = (String) obj;
            } else if (isPrimitive(obj)) {
                output = obj.toString();
            } else {
                Collection<?> col = obtainCollection(obj);
                if (col != null) {
                    output = collectionToString(col);
                }
            }
        }
        return output;
    }

    private static final Set<Class<?>> primitiveClasses = primitiveClasses();

    public static boolean isPrimitive(Object obj) {
        return primitiveClasses.contains(obj.getClass());
    }

    private static String collectionToString(Collection<?> col) {
        StringBuilder builder = new StringBuilder();
        String prefix = "";
        for (Object o : col) {
            builder.append(prefix).append(objectToString(o));
            prefix = ",";
        }
        return builder.toString();
    }

    private static Set<Class<?>> primitiveClasses() {
        Set<Class<?>> set = new HashSet<Class<?>>();
        set.add(Boolean.class);
        set.add(Character.class);
        set.add(Byte.class);
        set.add(Short.class);
        set.add(Integer.class);
        set.add(Long.class);
        set.add(Float.class);
        set.add(Double.class);
        set.add(Void.class);
        return set;
    }

    private static Method extractMethodInheritanceChain(Class type, Method m) {
        if (m == null || Modifier.isPublic(type.getModifiers())) {
            return m;
        }
        Class[] inf = type.getInterfaces();
        Method mp;
        for (Class<?> iface : inf) {
            try {
                mp = iface.getMethod(m.getName(), m.getParameterTypes());
                mp = extractMethodInheritanceChain(mp.getDeclaringClass(), mp);
                if (mp != null) {
                    return mp;
                }
            } catch (NoSuchMethodException e) {
                // do nothing
            }
        }
        Class<?> sup = type.getSuperclass();
        if (sup != null) {
            try {
                mp = sup.getMethod(m.getName(), m.getParameterTypes());
                mp = extractMethodInheritanceChain(mp.getDeclaringClass(), mp);
                if (mp != null) {
                    return mp;
                }
            } catch (NoSuchMethodException e) {
                // do nothing
            }
        }
        return null;
    }
}

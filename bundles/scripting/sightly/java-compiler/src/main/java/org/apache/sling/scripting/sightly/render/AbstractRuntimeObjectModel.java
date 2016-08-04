/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.render;

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
import org.apache.sling.scripting.sightly.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default abstract implementation of {@link RuntimeObjectModel}.
 */
public abstract class AbstractRuntimeObjectModel implements RuntimeObjectModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRuntimeObjectModel.class);

    /**
     * A {@link Set} that stores all the supported primitive classes.
     */
    public static final Set<Class<?>> PRIMITIVE_CLASSES = Collections.unmodifiableSet(new HashSet<Class<?>>() {{
        add(Boolean.class);
        add(Boolean.class);
        add(Character.class);
        add(Byte.class);
        add(Short.class);
        add(Integer.class);
        add(Long.class);
        add(Float.class);
        add(Double.class);
        add(Void.class);
    }});

    public static final String TO_STRING_METHOD = "toString";

    @Override
    public boolean isPrimitive(Object obj) {
        return PRIMITIVE_CLASSES.contains(obj.getClass());
    }

    @Override
    public boolean isCollection(Object target) {
        return (target instanceof Collection) || (target instanceof Object[]) || (target instanceof Iterable) ||
                (target instanceof Iterator);
    }

    @Override
    public Object resolveProperty(Object target, Object property) {
        Object resolved;
        if (property instanceof Number) {
            resolved = getIndex(target, ((Number) property).intValue());
        } else {
            resolved = getProperty(target, property);
        }
        return resolved;
    }

    @Override
    public boolean toBoolean(Object object) {
        return toBooleanInternal(object);
    }

    @Override
    public Number toNumber(Object object) {
        if (object instanceof Number) {
            return (Number) object;
        }
        return 0;
    }

    @Override
    public String toString(Object target) {
        return objectToString(target);
    }

    @Override
    public Collection<Object> toCollection(Object object) {
        return obtainCollection(object);
    }

    @Override
    public Map toMap(Object object) {
        if (object instanceof Map) {
            return (Map) object;
        } else if (object instanceof Record) {
            Map<String, Object> map = new HashMap<>();
            Record record = (Record) object;
            @SuppressWarnings("unchecked")
            Set<String> properties = record.getPropertyNames();
            for (String property : properties) {
                map.put(property, record.getProperty(property));
            }
            return map;
        }
        return Collections.emptyMap();
    }

    protected String objectToString(Object obj) {
        String output = "";
        if (obj != null) {
            if (obj instanceof String) {
                output = (String) obj;
            } else if (isPrimitive(obj)) {
                output = obj.toString();
            } else if (obj instanceof Enum) {
                return ((Enum) obj).name();
            } else {
                Collection<?> col = obtainCollection(obj);
                if (col != null) {
                    output = collectionToString(col);
                }
            }
        }
        return output;
    }

    protected Object getProperty(Object target, Object propertyObj) {
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
        return result;
    }

    @SuppressWarnings("unchecked")
    protected Collection<Object> obtainCollection(Object obj) {
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

    protected String collectionToString(Collection<?> col) {
        StringBuilder builder = new StringBuilder();
        String prefix = "";
        for (Object o : col) {
            builder.append(prefix).append(objectToString(o));
            prefix = ",";
        }
        return builder.toString();
    }

    protected Collection<Object> fromIterator(Iterator<Object> iterator) {
        ArrayList<Object> result = new ArrayList<>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    protected boolean toBooleanInternal(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof Number) {
            Number number = (Number) obj;
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

    protected Object getIndex(Object obj, int index) {
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

    protected Object getIndexSafe(List list, int index) {
        if (index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    protected Object getMapProperty(Map map, String property) {
        return map.get(property);
    }

    protected Object getObjectProperty(Object obj, String property) {
        Object result = getObjectNoArgMethod(obj, property);
        if (result == null) {
            result = getField(obj, property);
        }
        return result;
    }

    protected static Object getField(Object obj, String property) {
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

    protected Object getObjectNoArgMethod(Object obj, String property) {
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

    protected static Method findMethod(Class<?> cls, String baseName) {
        Method[] publicMethods = cls.getMethods();
        String capitalized = StringUtils.capitalize(baseName);
        for (Method m : publicMethods) {
            if (m.getParameterTypes().length == 0) {
                String methodName = m.getName();
                if (baseName.equals(methodName) || ("get" + capitalized).equals(methodName) || ("is" + capitalized).equals(methodName)) {
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

    protected static boolean isMethodAllowed(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        //methods of the Object.class are forbidden (except toString, which is allowed)
        return declaringClass != Object.class || TO_STRING_METHOD.equals(method.getName());
    }

    protected Method extractMethodInheritanceChain(Class type, Method m) {
        if (m == null || Modifier.isPublic(type.getModifiers())) {
            return m;
        }
        Class[] iFaces = type.getInterfaces();
        Method mp;
        for (Class<?> iFace : iFaces) {
            mp = getClassMethod(iFace, m);
            if (mp != null) {
                return mp;
            }
        }
        return getClassMethod(type.getSuperclass(), m);
    }

    protected Method getClassMethod(Class<?> clazz, Method m) {
        Method mp;
        try {
            mp = clazz.getMethod(m.getName(), m.getParameterTypes());
            mp = extractMethodInheritanceChain(mp.getDeclaringClass(), mp);
            if (mp != null) {
                return mp;
            }
        } catch (NoSuchMethodException e) {
            // do nothing
        }
        return null;
    }

}


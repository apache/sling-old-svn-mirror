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

package org.apache.sling.scripting.sightly.common;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.ValueMap;

import org.apache.sling.scripting.sightly.api.ObjectModel;
import org.apache.sling.scripting.sightly.api.Record;
import org.apache.sling.scripting.sightly.api.SightlyRenderException;

/**
 * Default implementation for the object model
 */
@Component
@Service(ObjectModel.class)
public class ObjectModelImpl implements ObjectModel {

    private static final String TO_STRING_METHOD = "toString";

    @Override
    public Object resolveProperty(Object target, Object property) {
        if (property instanceof Number) {
            return getIndex(target, ((Number) property).intValue());
        }
        return getProperty(target, property);
    }

    @Override
    public String coerceToString(Object target) {
        return objectToString(target);
    }

    @Override
    public boolean coerceToBoolean(Object object) {
        return toBoolean(object);
    }

    @Override
    public Collection<Object> coerceToCollection(Object object) {
        return obtainCollection(object);
    }

    @Override
    public Map coerceToMap(Object object) {
        if (object instanceof Map) {
            return (Map) object;
        }
        return Collections.emptyMap();
    }


    @Override
    public Number coerceNumeric(Object object) {
        if (object instanceof Number) {
            return (Number) object;
        }
        return 0;
    }

    @Override
    public boolean strictEq(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() == ((Number) right).doubleValue();
        }
        if (left instanceof String && right instanceof String) {
            return left.equals(right);
        }
        if (left instanceof Boolean && right instanceof Boolean) {
            return left.equals(right);
        }
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            Object notNull = (left != null) ? left : right;
            if (notNull instanceof String || notNull instanceof Boolean || notNull instanceof Number) {
                return false;
            }
        }
        throw new UnsupportedOperationException("Invalid types in comparison. Equality is supported for String, Number & Boolean types");
    }

    public boolean lt(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() < ((Number) right).doubleValue();
        }
        throw new UnsupportedOperationException("Invalid types in comparison. Comparison is supported for Number types only");
    }


    public boolean leq(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() <= ((Number) right).doubleValue();
        }
        throw new UnsupportedOperationException("Invalid types in comparison. Comparison is supported for Number types only");
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> obtainCollection(Object obj) {
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
            return ((Record) obj).properties();
        }
        if (obj instanceof Enumeration) {
            return Collections.list((Enumeration<Object>) obj);
        }
        if (obj instanceof Iterator) {
            return fromIterator((Iterator<Object>) obj);
        }
        return Collections.emptyList();
    }

    private Collection<Object> fromIterator(Iterator<Object> iterator) {
        ArrayList<Object> result = new ArrayList<Object>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    private boolean toBoolean(Object obj) {
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

        if (obj instanceof Object[]) {
            return ((Object[]) obj).length > 0;
        }

        return true;
    }

    private Object getProperty(Object target, Object propertyObj) {
        String property = coerceToString(propertyObj);
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
            result = ((Record) target).get(property);
        }
        if (result == null) {
            result = getObjectProperty(target, property);
        }
        if (result == null && target instanceof Adaptable) {
            result = getValueMapProperty(((Adaptable) target).adaptTo(ValueMap.class), property);
        }
        return result;
    }

    private Object getIndex(Object obj, int index) {
        if (obj instanceof Map) {
            Map map = (Map) obj;
            if (map.containsKey(index)) {
                return map.get(index);
            }
        }
        Collection collection = coerceToCollection(obj);
        if (collection instanceof List) {
            return getIndexSafe((List) collection, index);
        }
        return null;
    }

    private Object getIndexSafe(List list, int index) {
        if (index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    private Object getValueMapProperty(ValueMap valueMap, String property) {
        if (valueMap == null) {
            return null;
        }
        return valueMap.get(property);
    }

    private Object getMapProperty(Map map, String property) {
        return map.get(property);
    }

    private Object getObjectProperty(Object obj, String property) {
        Object result = getObjectNoArgMethod(obj, property);
        if (result != null) return result;
        return getField(obj, property);
    }

    private Object getField(Object obj, String property) {
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

    private Object getObjectNoArgMethod(Object obj, String property) {
        Class<?> cls = obj.getClass();
        Method method = findMethod(cls, property);
        if (method != null) {
            try {
                method = extractMethodInheritanceChain(cls, method);
                return method.invoke(obj);
            } catch (Exception e) {
                throw new SightlyRenderException(e);
            }
        }
        return null;
    }

    private Method findMethod(Class<?> cls, String baseName) {
        Method method;
        String capitalized = StringUtils.capitalize(baseName);
        method = tryMethod(cls, "get" + capitalized);
        if (method != null) return method;
        method = tryMethod(cls, "is" + capitalized);
        if (method != null) return method;
        method = tryMethod(cls, baseName);
        return method;
    }


    private Method tryMethod(Class<?> cls, String name) {
        try {
            Method m = cls.getMethod(name);
            Class<?> declaringClass = m.getDeclaringClass();
            return (isMethodAllowed(m)) ? m : null;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private boolean isMethodAllowed(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        //methods of the Object.class are forbidden (except toString, which is allowed)
        return declaringClass != Object.class || TO_STRING_METHOD.equals(method.getName());
    }


    private String objectToString(Object obj) {
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

    private boolean isPrimitive(Object obj) {
        return primitiveClasses.contains(obj.getClass());
    }

    private String collectionToString(Collection<?> col) {
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
                mp = iface.getMethod(m.getName(), (Class[]) m.getParameterTypes());
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
                mp = sup.getMethod(m.getName(), (Class[]) m.getParameterTypes());
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

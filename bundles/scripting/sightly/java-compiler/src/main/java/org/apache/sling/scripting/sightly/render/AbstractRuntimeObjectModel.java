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

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.scripting.sightly.Record;
import org.apache.sling.scripting.sightly.compiler.util.ObjectModel;

/**
 * Default abstract implementation of {@link RuntimeObjectModel}.
 */
public abstract class AbstractRuntimeObjectModel implements RuntimeObjectModel {

    /**
     * A {@link Set} that stores all the supported primitive classes.
     */
    public static final Set<Class<?>> PRIMITIVE_CLASSES = ObjectModel.PRIMITIVE_CLASSES;

    public static final String TO_STRING_METHOD = "toString";

    @Override
    public boolean isPrimitive(Object obj) {
        return ObjectModel.isPrimitive(obj);
    }

    @Override
    public boolean isDate(Object target) {
        return (target instanceof Date || target instanceof Calendar);
    }

    @Override
    public boolean isNumber(Object target) {
        if (target == null) {
            return false;
        }
        if (target instanceof Number) {
            return true;
        }
        String value = toString(target);
        return NumberUtils.isCreatable(value);
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
            resolved = ObjectModel.getIndex(target, ((Number) property).intValue());
        } else {
            resolved = getProperty(target, property);
        }
        return resolved;
    }

    @Override
    public boolean toBoolean(Object object) {
        return ObjectModel.toBoolean(object);
    }

    @Override
    public Number toNumber(Object object) {
        return ObjectModel.toNumber(object);
    }

    @Override
    public Date toDate(Object object) {
        if (object instanceof Date) {
            return (Date)object;
        } else if (object instanceof Calendar) {
            return ((Calendar)object).getTime();
        }
        return null;
    }

    @Override
    public String toString(Object target) {
        return ObjectModel.toString(target);
    }

    @Override
    public Collection<Object> toCollection(Object object) {
        if (object instanceof Record) {
            return ((Record) object).getPropertyNames();
        }
        return ObjectModel.toCollection(object);
    }

    @Override
    public Map toMap(Object object) {
        if (object instanceof Map) {
            return (Map) object;
        } else if (object instanceof Record) {
            Map<String, Object> map = new HashMap<>();
            Record record = (Record) object;
            Set<String> properties = record.getPropertyNames();
            for (String property : properties) {
                map.put(property, record.getProperty(property));
            }
            return map;
        }
        return Collections.emptyMap();
    }

    protected Object getProperty(Object target, Object propertyObj) {
        String property = ObjectModel.toString(propertyObj);
        Object result = null;
        if (target instanceof Record) {
            result = ((Record) target).getProperty(property);
        }
        if (result == null) {
            result = ObjectModel.resolveProperty(target, property);
        }
        return result;
    }

    /**
     * @deprecated see {@link ObjectModel#toCollection(Object)}
     */
    @Deprecated
    protected Collection<Object> obtainCollection(Object obj) {
        return ObjectModel.toCollection(obj);
    }

    /**
     * @deprecated see {@link ObjectModel#toString(Object)}
     */
    @Deprecated
    protected String objectToString(Object obj) {
        return ObjectModel.toString(obj);
    }

    /**
     * @deprecated see {@link ObjectModel#collectionToString(Collection)}
     */
    @Deprecated
    protected String collectionToString(Collection<?> col) {
        return ObjectModel.collectionToString(col);
    }

    /**
     * @deprecated see {@link ObjectModel#fromIterator(Iterator)}
     */
    @Deprecated
    protected Collection<Object> fromIterator(Iterator<Object> iterator) {
        return ObjectModel.fromIterator(iterator);
    }

    /**
     * @deprecated see {@link ObjectModel#toBoolean(Object)}
     */
    @Deprecated
    protected boolean toBooleanInternal(Object obj) {
        return ObjectModel.toBoolean(obj);
    }

    /**
     * @deprecated see {@link ObjectModel#getIndex(Object, int)}
     */
    @Deprecated
    protected Object getIndex(Object obj, int index) {
        return ObjectModel.getIndex(obj, index);
    }

    /**
     * @deprecated see {@link ObjectModel#getIndex(Object, int)}
     */
    @Deprecated
    protected Object getIndexSafe(List list, int index) {
        return ObjectModel.getIndex(list, index);
    }

    /**
     * @deprecated use {@link Map#get(Object)}
     */
    @Deprecated
    protected Object getMapProperty(Map map, String property) {
        return map.get(property);
    }

    /**
     * @deprecated see {@link ObjectModel#resolveProperty(Object, Object)}
     */
    @Deprecated
    protected Object getObjectProperty(Object obj, String property) {
        return ObjectModel.resolveProperty(obj, property);
    }

    /**
     * @deprecated see {@link ObjectModel#getField(Object, String)}
     */
    @Deprecated
    protected static Object getField(Object obj, String property) {
        return ObjectModel.getField(obj, property);
    }

    /**
     * @deprecated see {@link ObjectModel#invokeBeanMethod(Object, String)}
     */
    @Deprecated
    protected Object getObjectNoArgMethod(Object obj, String property) {
        return ObjectModel.invokeBeanMethod(obj, property);
    }

    /**
     * @deprecated see {@link ObjectModel#findBeanMethod(Class, String)}
     */
    @Deprecated
    protected static Method findMethod(Class<?> cls, String baseName) {
        return ObjectModel.findBeanMethod(cls, baseName);
    }

    /**
     * @deprecated see {@link ObjectModel#isMethodAllowed(Method)}
     */
    @Deprecated
    protected static boolean isMethodAllowed(Method method) {
       return ObjectModel.isMethodAllowed(method);
    }

    /**
     * @deprecated see {@link ObjectModel#findBeanMethod(Class, String)} (Class, Method)}
     */
    @Deprecated
    protected Method extractMethodInheritanceChain(Class type, Method m) {
        return ObjectModel.findBeanMethod(type, m.getName());
    }

    /**
     * @deprecated see {@link ObjectModel#findBeanMethod(Class, String)} (Class, Method)}
     */
    @Deprecated
    protected Method getClassMethod(Class<?> clazz, Method m) {
        return ObjectModel.findBeanMethod(clazz, m.getName());
    }

}

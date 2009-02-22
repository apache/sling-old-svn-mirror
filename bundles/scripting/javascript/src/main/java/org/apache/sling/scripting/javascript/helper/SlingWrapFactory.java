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
package org.apache.sling.scripting.javascript.helper;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlingWrapFactory extends WrapFactory {

    /** List of classes that must not be wrapped (added for SLING-382) */
    private static final Class<?>[] EXCLUDED_CLASSES = {};

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<Class<?>, String> wrappers = new HashMap<Class<?>, String>();

    /**
     * @param cx the current Context for this thread
     * @param scope the scope of the executing script
     * @param javaObject the object to be wrapped
     * @param staticType type hint. If security restrictions prevent to wrap
     *            object based on its class, staticType will be used instead.
     * @return the wrapped value which shall not be null
     */
    @SuppressWarnings("unchecked")
    @Override
    public Scriptable wrapAsJavaObject(Context cx, Scriptable scope,
            Object javaObject, Class staticType) {

        Scriptable result = null;
        try {
            String hostObjectName = getHostObjectName(staticType);

            if (hostObjectName == null) {
                hostObjectName = getHostObjectName(javaObject.getClass());
            }

            if (hostObjectName != null) {
                result = cx.newObject(scope, hostObjectName,
                    new Object[] { javaObject });
            }
        } catch (Exception e) {
            log.warn("Cannot Wrap " + javaObject, e);
        }

        if(result==null) {
            result = super.wrapAsJavaObject(cx, scope, javaObject, staticType);
        }

        return result;
    }

    private String getHostObjectName(Class<?> javaClass) {
        if(javaClass==null || isExcluded(javaClass)) {
            return null;
        }
        String hostObjectName = wrappers.get(javaClass);
        if (hostObjectName == null) {
            // before SLING-383 the superclass was tested first,
            // but for Version and VersionHistory this would get 
            // a Node wrapper, that's not what we want
            final Class<?>[] javaInterfaces = javaClass.getInterfaces();
            for (int i = 0; i < javaInterfaces.length && hostObjectName == null; i++) {
                hostObjectName = getHostObjectName(javaInterfaces[i]);
            }

            if (hostObjectName == null) {
                hostObjectName = getHostObjectName(javaClass.getSuperclass());
            }
        }

        return hostObjectName;
    }

    /*
     * Is this class in the excluded class  list?
     */
    private boolean isExcluded(Class<?> javaClass) {
        for (Class<?> type : EXCLUDED_CLASSES) {
            if (type.isAssignableFrom(javaClass)) {
                return true;
            }
        }
        return false;
    }

    public void registerWrapper(Class<?> javaClass, String hostObjectName) {
        wrappers.put(javaClass, hostObjectName);
    }

    public void unregisterWrapper(Class<?> javaClass) {
        wrappers.remove(javaClass);
    }
}

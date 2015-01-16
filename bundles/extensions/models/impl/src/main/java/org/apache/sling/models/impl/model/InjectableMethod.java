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

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.impl.ReflectionUtil;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;

public class InjectableMethod extends AbstractInjectableElement {
    
    private final Method method;
    private final Type genericReturnType;

    public InjectableMethod(Method method, StaticInjectAnnotationProcessorFactory[] processorFactories, DefaultInjectionStrategy defaultInjectionStrategy) {
        super(method, ReflectionUtil.mapPrimitiveClasses(method.getGenericReturnType()), getDefaultName(method), processorFactories, defaultInjectionStrategy);
        this.method = method;
        this.genericReturnType = method.getGenericReturnType();
    }

    public Method getMethod() {
        return method;
    }

    /**
     * @return Generic return type of method (may be primitive)
     */
    public Type getGenericReturnType() {
        return this.genericReturnType;
    }

    public boolean isPrimitive() {
        return getType() != this.genericReturnType;
    }

    private static String getDefaultName(Method method) {
        String methodName = method.getName();
        if (methodName.startsWith("get")) {
            return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        } else if (methodName.startsWith("is")) {
            return methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
        } else {
            return methodName;
        }
    }

}

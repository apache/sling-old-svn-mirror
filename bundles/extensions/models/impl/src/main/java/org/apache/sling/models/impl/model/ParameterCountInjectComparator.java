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

import java.lang.reflect.Constructor;
import java.util.Comparator;

import javax.inject.Inject;

/**
 * Comparator which sorts constructors by the number of parameters
 * in reverse order (most params to least params).
 * If two constructors have the same number of parameters constructors with
 * @Inject annotation are sorted first.
 */
class ParameterCountInjectComparator implements Comparator<Constructor<?>> {

    @Override
    public int compare(Constructor<?> o1, Constructor<?> o2) {
        int result = compareParameterCount(o2.getParameterTypes().length, o1.getParameterTypes().length);
        if (result==0) {
            return compareInjectAnnotation(o1, o2);
        }
        else {
            return result;
        }
    }

    private int compareParameterCount(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    private int compareInjectAnnotation(Constructor<?> o1, Constructor<?> o2) {
        boolean hasInject1 = o1.isAnnotationPresent(Inject.class);
        boolean hasInject2 = o2.isAnnotationPresent(Inject.class);
        if (hasInject1 && !hasInject2) {
            return -1;
        }
        else if (hasInject2 && !hasInject1) {
            return 1;
        }
        else {
            return 0;
        }
    }
    
}
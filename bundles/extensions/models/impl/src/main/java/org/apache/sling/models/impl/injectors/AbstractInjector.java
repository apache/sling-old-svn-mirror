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
package org.apache.sling.models.impl.injectors;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

/**
 * Abstract base class for injectors to consolidate common functionality.
 */
abstract class AbstractInjector {

    protected ResourceResolver getResourceResolver(Object adaptable) {
        ResourceResolver resolver = null;
        if (adaptable instanceof Resource) {
            resolver = ((Resource) adaptable).getResourceResolver();
        } else if (adaptable instanceof SlingHttpServletRequest) {
            resolver = ((SlingHttpServletRequest) adaptable).getResourceResolver();
        }
        return resolver;
    }

    protected ValueMap getValueMap(Object adaptable) {
        if (adaptable instanceof ValueMap) {
            return (ValueMap) adaptable;
        } else if (adaptable instanceof Adaptable) {
            ValueMap map = ((Adaptable) adaptable).adaptTo(ValueMap.class);
            return map;
        } else {
            return null;
        }
    }
    
    protected boolean isDeclaredTypeCollection(Type declaredType) {
        boolean isCollection = false;
        if (declaredType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) declaredType;
            Class<?> collectionType = (Class<?>) type.getRawType();
            isCollection = collectionType.equals(Collection.class)
                    || collectionType.equals(List.class);
        }
        return isCollection;
    }

}

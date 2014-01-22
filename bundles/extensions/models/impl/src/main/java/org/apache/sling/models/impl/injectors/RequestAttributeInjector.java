/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.models.impl.injectors;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import javax.servlet.ServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service
public class RequestAttributeInjector implements Injector {

    private static final Logger log = LoggerFactory.getLogger(RequestAttributeInjector.class);
    
    @Override
    public String getName() {
        return "request-attributes";
    }

    @Override
    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element, DisposalCallbackRegistry callbackRegistry) {
        if (!(adaptable instanceof ServletRequest)) {
            return null;
        } else if (declaredType instanceof Class<?>) {
            Class<?> clazz = (Class<?>) declaredType;
            Object attribute = ((ServletRequest)adaptable).getAttribute(name);
            if (clazz.isInstance(attribute)) {
                return attribute;
            } else {
                return null;
            }
        } else {
            log.debug("BindingsInjector doesn't support non-class type {}", declaredType);
            return null;
        }
    }

}

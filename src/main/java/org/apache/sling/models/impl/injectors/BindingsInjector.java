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
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 1000)
public class BindingsInjector implements Injector {

    private static final Logger log = LoggerFactory.getLogger(BindingsInjector.class);

    @Override
    public String getName() {
        return "script-bindings";
    }

    private static Object getValue(SlingBindings bindings, String name, Class<?> type) {
        Object value = bindings.get(name);
        if (type.isInstance(value)) {
            return value;
        } else {
            return null;
        }
    }

    public Object getValue(Object adaptable, String name, Type type, AnnotatedElement element, DisposalCallbackRegistry callbackRegistry) {
        SlingBindings bindings = getBindings(adaptable);
        if (bindings == null) {
            return null;
        }
        if (type instanceof Class<?>) {
            return getValue(bindings, name, (Class<?>) type);
        } else {
            log.debug("BindingsInjector doesn't support non-class type {}", type);
            return null;
        }

    }

    private SlingBindings getBindings(Object adaptable) {
        if (adaptable instanceof ServletRequest) {
            ServletRequest request = (ServletRequest) adaptable;
            return (SlingBindings) request.getAttribute(SlingBindings.class.getName());
        } else {
            return null;
        }
    }

}

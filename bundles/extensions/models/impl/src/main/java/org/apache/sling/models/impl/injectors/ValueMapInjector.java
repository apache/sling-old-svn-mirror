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

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.AbstractInjectAnnotationProcessor;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 2000)
public class ValueMapInjector implements InjectAnnotationProcessorFactory, Injector {

    private static final Logger log = LoggerFactory.getLogger(ValueMapInjector.class);

    @Override
    public String getName() {
        return "valuemap";
    }

    public Object getValue(Object adaptable, String name, Type type, AnnotatedElement element,
            DisposalCallbackRegistry callbackRegistry) {
        ValueMap map = getMap(adaptable);
        if (map == null) {
            return null;
        } else if (type instanceof Class<?>) {
            return map.get(name, (Class<?>) type);
        } else {
            log.debug("ValueMapInjector doesn't support non-class types {}", type);
            return null;
        }
    }

    private ValueMap getMap(Object adaptable) {
        if (adaptable instanceof ValueMap) {
            return (ValueMap) adaptable;
        } else if (adaptable instanceof Adaptable) {
            ValueMap map = ((Adaptable) adaptable).adaptTo(ValueMap.class);
            return map;
        } else {
            return null;
        }
    }

    @Override
    public InjectAnnotationProcessor createAnnotationProcessor(Object adaptable, AnnotatedElement element) {
        // check if the element has the expected annotation
        ValueMapValue annotation = element.getAnnotation(ValueMapValue.class);
        if (annotation != null) {
            return new ValueAnnotationProcessor(annotation, adaptable);
        }
        return null;
    }

    private static class ValueAnnotationProcessor extends AbstractInjectAnnotationProcessor {

        private final ValueMapValue annotation;

        private final Object adaptable;

        public ValueAnnotationProcessor(ValueMapValue annotation, Object adaptable) {
            this.annotation = annotation;
            this.adaptable = adaptable;
        }

        @Override
        public String getName() {
            // since null is not allowed as default value in annotations, the empty string means, the default should be
            // used!
            if (annotation.name().isEmpty()) {
                return null;
            }
            return annotation.name();
        }

        @Override
        public String getVia() {
            if (StringUtils.isNotBlank(annotation.via())) {
                return annotation.via();
            }
            // automatically go via resource, if this is the httprequest
            if (adaptable instanceof SlingHttpServletRequest) {
                return "resource";
            } else {
                return null;
            }
        }

        @Override
        public Boolean isOptional() {
            return annotation.optional();
        }
    }
}

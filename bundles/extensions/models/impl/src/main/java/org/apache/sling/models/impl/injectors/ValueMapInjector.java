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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.injectorspecific.Value;
import org.apache.sling.models.impl.annotationprocessors.ValueAnnotationProcessor;
import org.apache.sling.models.spi.ModelAnnotationProcessor;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.ModelAnnotationProcessorFactory;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 2000)
public class ValueMapInjector implements ModelAnnotationProcessorFactory, Injector {

    private static final Logger log = LoggerFactory.getLogger(ValueMapInjector.class);
    
    @Override
    public String getName() {
        return "valuemap";
    }

    public Object getValue(Object adaptable, String name, Type type, AnnotatedElement element, DisposalCallbackRegistry callbackRegistry) {
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
    public ModelAnnotationProcessor createAnnotationProcessor(Object adaptable,
	    AnnotatedElement element) {
	// check if the element has the expected annotation
	Value annotation = element.getAnnotation(Value.class);
	if (annotation != null) {
	    return new ValueAnnotationProcessor(annotation, adaptable);
	}
	return null;
    }
}

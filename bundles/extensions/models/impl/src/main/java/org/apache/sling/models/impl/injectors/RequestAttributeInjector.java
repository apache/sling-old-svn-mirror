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

import javax.annotation.Nonnull;
import javax.servlet.ServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.AbstractInjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.osgi.framework.Constants;

@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 4000)
public class RequestAttributeInjector implements Injector, StaticInjectAnnotationProcessorFactory {

    @Override
    public @Nonnull String getName() {
        return "request-attributes";
    }

    @Override
    public Object getValue(@Nonnull Object adaptable, String name, @Nonnull Type declaredType, @Nonnull AnnotatedElement element,
            @Nonnull DisposalCallbackRegistry callbackRegistry) {
        if (!(adaptable instanceof ServletRequest)) {
            return null;
        } else {
            return ((ServletRequest) adaptable).getAttribute(name);
        }
    }

    @Override
    public InjectAnnotationProcessor2 createAnnotationProcessor(AnnotatedElement element) {
        // check if the element has the expected annotation
        RequestAttribute annotation = element.getAnnotation(RequestAttribute.class);
        if (annotation != null) {
            return new RequestAttributeAnnotationProcessor(annotation);
        }
        return null;
    }

    private static class RequestAttributeAnnotationProcessor extends AbstractInjectAnnotationProcessor2 {

        private final RequestAttribute annotation;

        public RequestAttributeAnnotationProcessor(RequestAttribute annotation) {
            this.annotation = annotation;
        }

        @Override
        public InjectionStrategy getInjectionStrategy() {
            return annotation.injectionStrategy();
        }
        
        @Override
        @SuppressWarnings("deprecation")
        public Boolean isOptional() {
            return annotation.optional();
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
    }

    
}

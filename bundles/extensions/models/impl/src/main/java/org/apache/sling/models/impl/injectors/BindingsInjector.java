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

import org.apache.commons.lang.ObjectUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.ValuePreparer;
import org.apache.sling.models.spi.injectorspecific.AbstractInjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.osgi.framework.Constants;

@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 1000)
public class BindingsInjector implements Injector, StaticInjectAnnotationProcessorFactory, ValuePreparer {

    @Override
    public @Nonnull String getName() {
        return "script-bindings";
    }

    public Object getValue(@Nonnull Object adaptable, String name, @Nonnull Type type, @Nonnull AnnotatedElement element,
            @Nonnull DisposalCallbackRegistry callbackRegistry) {
        if (adaptable == ObjectUtils.NULL) {
            return null;
        }
        SlingBindings bindings = getBindings(adaptable);
        if (bindings == null) {
            return null;
        }
        return bindings.get(name);
    }

    private SlingBindings getBindings(Object adaptable) {
        if (adaptable instanceof SlingBindings) {
            return (SlingBindings) adaptable;
        } else if (adaptable instanceof ServletRequest) {
            ServletRequest request = (ServletRequest) adaptable;
            return (SlingBindings) request.getAttribute(SlingBindings.class.getName());
        } else {
            return null;
        }
    }

    @Override
    public InjectAnnotationProcessor2 createAnnotationProcessor(AnnotatedElement element) {
        // check if the element has the expected annotation
        ScriptVariable annotation = element.getAnnotation(ScriptVariable.class);
        if (annotation != null) {
            return new ScriptVariableAnnotationProcessor(annotation);
        }
        return null;
    }

    @Override
    public Object prepareValue(Object adaptable) {
        Object prepared = getBindings(adaptable);
        return prepared != null ? prepared : ObjectUtils.NULL;
    }

    private static class ScriptVariableAnnotationProcessor extends AbstractInjectAnnotationProcessor2 {

        private final ScriptVariable annotation;

        public ScriptVariableAnnotationProcessor(ScriptVariable annotation) {
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

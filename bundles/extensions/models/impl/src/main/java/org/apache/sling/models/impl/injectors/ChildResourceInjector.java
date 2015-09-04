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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.AbstractInjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory2;
import org.osgi.framework.Constants;

@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 3000)
public class ChildResourceInjector extends AbstractInjector implements Injector, InjectAnnotationProcessorFactory2 {

    @Override
    public @Nonnull String getName() {
        return "child-resources";
    }

    @Override
    public Object getValue(@Nonnull Object adaptable, String name, @Nonnull Type declaredType, @Nonnull AnnotatedElement element,
            @Nonnull DisposalCallbackRegistry callbackRegistry) {
        if (adaptable instanceof Resource) {
            Resource child = ((Resource) adaptable).getChild(name);
            if (child != null) {
                return getValue(child, declaredType);
            }
        }
        return null;
    }

    private Object getValue(Resource adaptable, Type declaredType) {
        if (declaredType instanceof Class) {
            return adaptable;
        } else if (isDeclaredTypeCollection(declaredType)) {
            return getResultList(adaptable, declaredType);
        } else {
            return null;
        }
    }

    private Object getResultList(Resource resource, Type declaredType) {
       List<Resource> result = new ArrayList<Resource>();
       Class<?> type = getActualType((ParameterizedType) declaredType);
       if (type != null && resource != null) {
           Iterator<Resource> children = resource.listChildren();
           while (children.hasNext()) {
               result.add(children.next());
           }
       }
       return result;
   }

   private Class<?> getActualType(ParameterizedType declaredType) {
       Type[] types = declaredType.getActualTypeArguments();
       if (types != null && types.length > 0) {
           return (Class<?>) types[0];
       }
       return null;
   }


    @Override
    public InjectAnnotationProcessor2 createAnnotationProcessor(Object adaptable, AnnotatedElement element) {
        // check if the element has the expected annotation
        ChildResource annotation = element.getAnnotation(ChildResource.class);
        if (annotation != null) {
            return new ChildResourceAnnotationProcessor(annotation, adaptable);
        }
        return null;
    }

    private static class ChildResourceAnnotationProcessor extends AbstractInjectAnnotationProcessor2 {

        private final ChildResource annotation;
        private final Object adaptable;

        public ChildResourceAnnotationProcessor(ChildResource annotation, Object adaptable) {
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
        public InjectionStrategy getInjectionStrategy() {
            return annotation.injectionStrategy();
        }

        @Override
        @SuppressWarnings("deprecation")
        public Boolean isOptional() {
            return annotation.optional();
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
    }

}

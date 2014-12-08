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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Path;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ResourcePath;
import org.apache.sling.models.spi.AcceptsNullName;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.AbstractInjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.osgi.framework.Constants;

@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 2500)
public class ResourcePathInjector extends AbstractInjector implements Injector, AcceptsNullName,
        StaticInjectAnnotationProcessorFactory {

    @Override
    public String getName() {
        return "resource-path";
    }

    @Override
    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element,
            DisposalCallbackRegistry callbackRegistry) {
        String[] resourcePaths = null;
        Path pathAnnotation = element.getAnnotation(Path.class);
        ResourcePath resourcePathAnnotation = element.getAnnotation(ResourcePath.class);
        if (pathAnnotation != null) {
            resourcePaths = getPathsFromAnnotation(pathAnnotation);
        } else if (resourcePathAnnotation != null) {
            resourcePaths = getPathsFromAnnotation(resourcePathAnnotation);
            // try the valuemap
        }
        if (ArrayUtils.isEmpty(resourcePaths)) {
            ValueMap map = getValueMap(adaptable);
            if (map != null) {
                resourcePaths = map.get(name, String[].class);
            }
        }
        if(ArrayUtils.isEmpty(resourcePaths)){
            //could not find a path to inject
            return null;
        }
        
        ResourceResolver resolver = getResourceResolver(adaptable);
        if(resolver==null){
            return null;
        }
        List<Resource> resources = getResources(resolver, resourcePaths);

        if (resources.isEmpty()) {
            return null;
        }
        // unwrap if necessary
        if (isDeclaredTypeCollection(declaredType)) {
            return resources;
        } else {
            // TODO: maybe thrown an exception is size>1 ?
            return resources.get(0);
        }

    }

    private List<Resource> getResources(ResourceResolver resolver, String[] paths) {
        List<Resource> resources = new ArrayList<Resource>();
        for (String path : paths) {
            Resource resource = resolver.getResource(path);
            if (resource != null) {
                resources.add(resource);
            }
        }
        return resources;
    }

    /**
     * obtains the paths from any of the two possible annotations
     * 
     * @param annotation
     * @return
     */
    private String[] getPathsFromAnnotation(Annotation annotation) {
        String[] resourcePaths = null;

        if (annotation instanceof Path) {
            Path pathAnnotation = (Path) annotation;
            if (StringUtils.isNotEmpty(pathAnnotation.value())) {
                resourcePaths = new String[] { pathAnnotation.value() };
            } else {
                resourcePaths = pathAnnotation.paths();
            }
        } else if (annotation instanceof ResourcePath) {
            ResourcePath resourcePathAnnotation = (ResourcePath) annotation;
            if (StringUtils.isNotEmpty(resourcePathAnnotation.path())) {
                resourcePaths = new String[] { resourcePathAnnotation.path() };
            } else {
                resourcePaths = resourcePathAnnotation.paths();
            }

        }

        return resourcePaths;
    }

    @Override
    public InjectAnnotationProcessor2 createAnnotationProcessor(AnnotatedElement element) {
        // check if the element has the expected annotation
        ResourcePath annotation = element.getAnnotation(ResourcePath.class);
        if (annotation != null) {
            return new ResourcePathAnnotationProcessor(annotation);
        }
        return null;
    }

    private static class ResourcePathAnnotationProcessor extends AbstractInjectAnnotationProcessor2 {

        private final ResourcePath annotation;

        public ResourcePathAnnotationProcessor(ResourcePath annotation) {
            this.annotation = annotation;
        }

        @Override
        public String getName() {
            // since null is not allowed as default value in annotations, the
            // empty string means, the default should be
            // used!
            if (annotation.name().isEmpty()) {
                return null;
            }
            return annotation.name();
        }

        @Override
        public Boolean isOptional() {
            return annotation.optional();
        }

        @Override
        public InjectionStrategy getInjectionStrategy() {
            return annotation.injectionStrategy();
        }
    }

}

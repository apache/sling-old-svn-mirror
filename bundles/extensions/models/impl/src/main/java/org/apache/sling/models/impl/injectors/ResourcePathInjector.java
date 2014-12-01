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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
public class ResourcePathInjector extends AbstractInjector implements Injector,
		AcceptsNullName, StaticInjectAnnotationProcessorFactory {

	@Override
	public String getName() {
		return "resource-path";
	}

	@Override
	public Object getValue(Object adaptable, String name, Type declaredType,
			AnnotatedElement element, DisposalCallbackRegistry callbackRegistry) {
		String resourcePath = null;
		Path pathAnnotation = element.getAnnotation(Path.class);
		if (pathAnnotation != null) {
			resourcePath = pathAnnotation.value();
		} else {
			ResourcePath resourcePathAnnotation = element
					.getAnnotation(ResourcePath.class);
			if (resourcePathAnnotation != null) {
				resourcePath = resourcePathAnnotation.path();
				if (resourcePath.isEmpty()) {
					resourcePath = null;
				}
			}
		}
		if (resourcePath != null) {
			ResourceResolver resolver = getResourceResolver(adaptable);
			if (resolver != null) {
				return resolver.getResource(resourcePath);
			}
		} else if (name != null) {
			// try to get from value map

			if (isDeclaredTypeCollection(declaredType)) {
				return getResourceList(name, adaptable);
			} else {
				return getSingleResource(name, adaptable);

			}

		}

		return null;
	}

	private List<Resource> getResourceList(String name, Object adaptable) {
		List<Resource> result = new ArrayList<Resource>();
		ValueMap map = getValueMap(adaptable);
		String[] resourcePaths = map.get(name, String[].class);
		ResourceResolver resolver = getResourceResolver(adaptable);
		if (resolver == null) {
			return null;
		}
		for (String resourcePath : resourcePaths) {
			result.add(resolver.getResource(resourcePath));
		}
		return result;

	}

	private Resource getSingleResource(String name, Object adaptable) {
		ValueMap map = getValueMap(adaptable);
		if (map == null) {
			return null;
		}
		String resourcePath = map.get(name, String.class);
		if (resourcePath != null) {
			ResourceResolver resolver = getResourceResolver(adaptable);
			if (resolver != null) {
				return resolver.getResource(resourcePath);
			}
		}
		return null;

	}

	@Override
	public InjectAnnotationProcessor2 createAnnotationProcessor(
			AnnotatedElement element) {
		// check if the element has the expected annotation
		ResourcePath annotation = element.getAnnotation(ResourcePath.class);
		if (annotation != null) {
			return new ResourcePathAnnotationProcessor(annotation);
		}
		return null;
	}

	private static class ResourcePathAnnotationProcessor extends
			AbstractInjectAnnotationProcessor2 {

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

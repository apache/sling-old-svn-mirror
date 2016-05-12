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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import javax.annotation.Nonnull;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapObject;
import org.apache.sling.models.spi.AcceptsNullName;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.AbstractInjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Injects a custom type object for a value map value that can be mapped to by calling its constructor 
 * (if there is one with one parameter that matches the type that is returned by the value map for the property name).
 */
@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 3000)
public class ValueMapObjectInjector extends AbstractInjector implements Injector, StaticInjectAnnotationProcessorFactory, AcceptsNullName {
    private static final Logger LOG = LoggerFactory.getLogger(ValueMapObjectInjector.class);

    @Override
    public @Nonnull String getName() {
        return "valuemap-object";
    }

    public Object getValue(@Nonnull Object adaptable, String name, @Nonnull Type type, @Nonnull AnnotatedElement element,
            @Nonnull DisposalCallbackRegistry callbackRegistry) {

    	// it's probably better to restrict to Annotation usages (otherwise the below reflection calls will be made for a 
    	// lot of "currentPage", "resource", etc. injections where this injector is not relevant), same thing is done for @Self    	
    	if (!element.isAnnotationPresent(ValueMapObject.class)) {
    		return null;
    	}
    	
    	// AbstractInjector.getValueMap() does not "unfold" request to resource, doing this here to make it work. 
    	// Should it be done in AbstractInjector.getValueMap()?
		if(adaptable instanceof SlingHttpServletRequest) {
			adaptable = ((SlingHttpServletRequest) adaptable).getResource();
		}
    	
    	ValueMap valueMap = getValueMap(adaptable);
    	if(valueMap==null) {
    		return null; 
    	}
    	
    	Object object = valueMap.get(name);
    	if(object == null) {
    		return null; 
    	}
    	
    	Class<?> objectType = object.getClass();
    	Class<?> clazz = (Class<?>) type;
		Class<?>[] contructorParameters = new Class<?>[] {objectType};
    	
    	Object valueToInject = null;
    	Constructor<?> constructor = null;
    	try {
			constructor = clazz.getConstructor(contructorParameters);
			valueToInject = constructor.newInstance(new Object[]{object});
		} catch (NoSuchMethodException e) {
			LOG.debug("@PlainObject: Constructor with parameter {} not found in class {}", objectType, clazz);
		} catch (SecurityException e) {
			LOG.debug("@PlainObject: Reflection not allowed", e);
		} catch (IllegalArgumentException e) {
			LOG.debug("@PlainObject: Impossible (invocation arguments will always match found contructur with above code)");
		} catch (InstantiationException e) {
			LOG.debug("@PlainObject: Exception while invoking {}", constructor, e);
		} catch (IllegalAccessException e) {
			LOG.debug("@PlainObject: Reflection not allowed", e);
		} catch (InvocationTargetException e) {
			LOG.debug("@PlainObject: Exception while invoking {}", constructor, e);
		} 
    	
        return valueToInject;
    }

    @Override
    public InjectAnnotationProcessor2 createAnnotationProcessor(AnnotatedElement element) {
    	ValueMapObject annotation = element.getAnnotation(ValueMapObject.class);
        if (annotation != null) {
            return new ValueMapObjectAnnotationProcessor(annotation);
        }
        return null;
    }

    private static class ValueMapObjectAnnotationProcessor extends AbstractInjectAnnotationProcessor2 {

        private final ValueMapObject annotation;

        public ValueMapObjectAnnotationProcessor(ValueMapObject annotation) {
            this.annotation = annotation;
        }

        @Override
        public InjectionStrategy getInjectionStrategy() {
            return annotation.injectionStrategy();
        }
        
    }

}

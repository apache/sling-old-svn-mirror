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
package org.apache.sling.models.impl;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.spi.ImplementationPicker;
import org.osgi.framework.Constants;

@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = (Integer.MAX_VALUE - 1))
public class ResourceTypeBasedResourcePicker implements ImplementationPicker {

    @Override
    public Class<?> pick(@Nonnull Class<?> adapterType, @Nonnull Class<?>[] implementationsTypes, @Nonnull Object adaptable) {
        final Resource resource = findResource(adaptable);
        if (resource == null) {
            return null;
        }

        Map<String, Class<?>> implementationsByRT = mapByResourceType(implementationsTypes);
        return AdapterImplementations.getModelClassForResource(resource, implementationsByRT);
    }

    private Resource findResource(Object adaptable) {
        if (adaptable instanceof Resource) {
            return (Resource) adaptable;
        } else if (adaptable instanceof SlingHttpServletRequest) {
            return ((SlingHttpServletRequest) adaptable).getResource();
        } else {
            return null;
        }
    }

    private Map<String, Class<?>> mapByResourceType(Class<?>[] implementationTypes) {
        Map<String, Class<?>> retval = new HashMap<String, Class<?>>(implementationTypes.length);

        for (Class<?> clazz : implementationTypes) {
            Model modelAnnotation = clazz.getAnnotation(Model.class);
            // this really should always be non-null at this point, but just in case...
            if (modelAnnotation != null) {
                String[] resourceTypes = modelAnnotation.resourceType();
                for (String resourceType : resourceTypes) {
                    if (!retval.containsKey(resourceType)) {
                        retval.put(resourceType, clazz);
                    }
                }
            }
        }
        return retval;
    }

}

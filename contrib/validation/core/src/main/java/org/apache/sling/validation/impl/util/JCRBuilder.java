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
package org.apache.sling.validation.impl.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.validation.api.ChildResource;
import org.apache.sling.validation.api.ResourceProperty;
import org.apache.sling.validation.api.Type;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.api.ValidatorLookupService;
import org.apache.sling.validation.impl.ChildResourceImpl;
import org.apache.sling.validation.impl.Constants;
import org.apache.sling.validation.impl.ResourcePropertyImpl;

/**
 * Helps building validation related objects from JCR content trees.
 */
public class JCRBuilder {

    /**
     * Creates a set of the properties that a resource is expected to have, together with the associated validators.
     *
     * @param vls                the {@link ValidatorLookupService}
     * @param propertiesResource the resource identifying the properties node from a validation model's structure
     * @return a set of properties or an empty set if no properties are defined
     * @see ResourceProperty
     */
    public static Set<ResourceProperty> buildProperties(ValidatorLookupService vls, Resource propertiesResource) {
        Set<ResourceProperty> properties = new HashSet<ResourceProperty>();
        if (propertiesResource != null) {
            for (Resource property : propertiesResource.getChildren()) {
                String fieldName = property.getName();
                ValueMap propertyValueMap = property.adaptTo(ValueMap.class);
                Type type = Type.getType(propertyValueMap.get(Constants.PROPERTY_TYPE, String.class));
                Boolean propertyMultiple = PropertiesUtil.toBoolean(propertyValueMap.get(Constants.PROPERTY_MULTIPLE), false);
                Resource validators = property.getChild(Constants.VALIDATORS);
                Map<Validator, Map<String, String>> validatorsMap = new HashMap<Validator, Map<String, String>>();
                if (validators != null) {
                    Iterator<Resource> validatorsIterator = validators.listChildren();
                    while (validatorsIterator.hasNext()) {
                        Resource validator = validatorsIterator.next();
                        ValueMap validatorProperties = validator.adaptTo(ValueMap.class);
                        String validatorName = validator.getName();
                        Validator v = vls.getValidator(validatorName);
                        String[] validatorArguments = validatorProperties.get(Constants.VALIDATOR_ARGUMENTS, String[].class);
                        Map<String, String> validatorArgumentsMap = new HashMap<String, String>();
                        if (validatorArguments != null) {
                            for (String arg : validatorArguments) {
                                String[] keyValuePair = arg.split("=");
                                if (keyValuePair.length != 2) {
                                    continue;
                                }
                                validatorArgumentsMap.put(keyValuePair[0], keyValuePair[1]);
                            }
                        }
                        validatorsMap.put(v, validatorArgumentsMap);
                    }
                }
                ResourceProperty f = new ResourcePropertyImpl(fieldName, type, propertyMultiple, validatorsMap);
                properties.add(f);
            }
        }
        return properties;
    }

    /**
     * Searches children resources from a {@code modelResource}, starting from the {@code rootResource}. If one needs all the children
     * resources of a model, then the {@code modelResource} and the {@code rootResource} should be identical.
     *
     * @param modelResource          the resource describing a {@link org.apache.sling.validation.api.ValidationModel}
     * @param rootResource           the model's resource from which to search for children (this resource has to have a {@link
     *                               Constants#CHILDREN} node directly underneath it)
     * @param validatorLookupService the {@link ValidatorLookupService}
     * @return a list of all the children resources; the list will be empty if there are no children resources
     */
    public static List<ChildResource> buildChildren(Resource modelResource, Resource rootResource,
                                                    ValidatorLookupService validatorLookupService) {
        List<ChildResource> children = new ArrayList<ChildResource>();
        Resource childrenResource = rootResource.getChild(Constants.CHILDREN);
        if (childrenResource != null) {
            for (Resource child : childrenResource.getChildren()) {
                ChildResource childResource = new ChildResourceImpl(modelResource, child, validatorLookupService);
                children.add(childResource);
                children.addAll(buildChildren(modelResource, child, validatorLookupService));
            }
        }
        return children;
    }
}

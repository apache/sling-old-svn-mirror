/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.validation.impl.model.MergedValidationModel;
import org.apache.sling.validation.impl.util.Trie;
import org.apache.sling.validation.model.ValidationModel;
import org.apache.sling.validation.model.spi.ValidationModelProvider;
import org.apache.sling.validation.spi.Validator;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.FieldOption;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Retrieves the most appropriate model (the one with the longest matching applicablePath) from any of the
 * {@link ValidationModelProvider}s. Also implements a cache of all previously retrieved models. */
@Component
public class ValidationModelRetrieverImpl implements ValidationModelRetriever {

    public static final String CACHE_INVALIDATION_EVENT_TOPIC = "org/apache/sling/validation/cache/INVALIDATE";

    /** 
     * Map of validation providers (key=service properties), Declarative Services 1.3 takes care that the list is ordered according to {@link ServiceReference#compareTo(Object)}.
     * Highest ranked service is the last one in the list.
     * 
     * @see OSGi R6 Comp, 112.3.8.1
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY, fieldOption = FieldOption.REPLACE)
    protected volatile List<ValidationModelProvider> modelProviders;

    /** List of all known validators (key=classname of validator) */
    @Nonnull
    Map<String, Validator<?>> validators = new ConcurrentHashMap<>();

    @Nonnull
    Map<String, ServiceReference<Validator<?>>> validatorServiceReferences = new ConcurrentHashMap<>();

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    private static final Logger LOG = LoggerFactory.getLogger(ValidationModelRetrieverImpl.class);

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.validation.impl.ValidationModelRetriever#getModels(java.lang.String, java.lang.String)
     */
    @CheckForNull
    public ValidationModel getModel(@Nonnull String resourceType, String resourcePath, boolean considerResourceSuperTypeModels) {
        // first get model for exactly the requested resource type
        ValidationModel baseModel = getModel(resourceType, resourcePath);
        String currentResourceType = resourceType;
        if (considerResourceSuperTypeModels) {
            Collection<ValidationModel> modelsToMerge = new ArrayList<ValidationModel>();
            ResourceResolver resourceResolver = null;
            try {
                resourceResolver = resourceResolverFactory.getServiceResourceResolver(null);
                while ((currentResourceType = resourceResolver.getParentResourceType(currentResourceType)) != null) {
                    ValidationModel modelToMerge = getModel(currentResourceType, resourcePath);
                    if (modelToMerge != null) {
                        if (baseModel == null) {
                            baseModel = modelToMerge;
                        } else {
                            modelsToMerge.add(modelToMerge);
                        }
                    }
                }
                if (!modelsToMerge.isEmpty()) {
                    return new MergedValidationModel(baseModel, modelsToMerge.toArray(new ValidationModel[modelsToMerge
                            .size()]));
                }
            } catch (LoginException e) {
                throw new IllegalStateException("Could not get service resource resolver", e);
            } finally {
                if (resourceResolver != null) {
                    resourceResolver.close();
                }
            }
        }
        return baseModel;
    }

    private @CheckForNull ValidationModel getModel(@Nonnull String resourceType, String resourcePath) {
        ValidationModel model = null;
        Trie<ValidationModel> modelsForResourceType = fillTrieForResourceType(resourceType);

        model = modelsForResourceType.getElementForLongestMatchingKey(resourcePath).getValue();
        if (model == null && !modelsForResourceType.isEmpty()) {
            LOG.warn("Although model for resource type {} is available, it is not allowed for path {}", resourceType,
                    resourcePath);
        }
        return model;
    }

    private @Nonnull Trie<ValidationModel> fillTrieForResourceType(@Nonnull String resourceType) {
        // create a new (empty) trie
        Trie<ValidationModel> modelsForResourceType = new Trie<ValidationModel>();

        // fill trie with data from model providers (all models for the given resource type, independent of resource path)
        // lowest ranked model provider inserts first (i.e. higher ranked should overwrite)
        for (ValidationModelProvider modelProvider : modelProviders) {
            for (ValidationModel model : modelProvider.getModels(resourceType, validators)) {
                for (String applicablePath : model.getApplicablePaths()) {
                    modelsForResourceType.insert(applicablePath, model);
                }
            }
        }
        return modelsForResourceType;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
    protected void addValidator(Validator<?> validator, Map<String, Object> properties, ServiceReference<Validator<?>> serviceReference) {
        String validatorId = getValidatorIdFromServiceProperties(properties, validator, serviceReference);
        if (validators.containsKey(validatorId)) {
            ServiceReference<Validator<?>> existingServiceReference = validatorServiceReferences.get(validatorId);
            if (existingServiceReference == null) {
                throw new IllegalStateException("Could not find service reference for validator with id " + validatorId);
            }
            if (serviceReference.compareTo(existingServiceReference) == 1) {
                LOG.info("Overwriting already existing validator {} from bundle {} with validator {} from bundle {},"
                        + " because it has the same id '{}' and a higher service ranking",
                        validators.get(validatorId), existingServiceReference.getBundle().getBundleId(), validator,
                        serviceReference.getBundle().getBundleId(), validatorId);
                validators.put(validatorId, validator);
                validatorServiceReferences.put(validatorId, serviceReference);
            } else {
                LOG.info(
                        "A Validator for the same id '{}' is already registered with class '{}' from bundle {} and has a higher service ranking",
                        validatorId, validators.get(validatorId), existingServiceReference.getBundle().getBundleId());
            }
        } else {
            validators.put(validatorId, validator);
            validatorServiceReferences.put(validatorId, serviceReference);
        }
    }

    // no need for an unbind method for validators, as those are static, i.e. component is deactivated first
    @Activate
    protected void activate() {
        LOG.info("Starting service...");
    }

    private String getValidatorIdFromServiceProperties(Map<String, Object> properties, Validator<?> validator,
            ServiceReference<Validator<?>> serviceReference) {
        Object object = properties.get(Validator.PROPERTY_VALIDATOR_ID);
        if (object == null) {
            throw new IllegalArgumentException("Validator '" + validator.getClass().getName() + "' provided from bundle "
                    + serviceReference.getBundle().getBundleId() +
                    " is lacking the mandatory service property " + Validator.PROPERTY_VALIDATOR_ID);
        }
        if (!(object instanceof String)) {
            throw new IllegalArgumentException("Validator '" + validator.getClass().getName() + "' provided from bundle "
                    + serviceReference.getBundle().getBundleId() +
                    " is providing the mandatory service property " + Validator.PROPERTY_VALIDATOR_ID + " with the wrong type "
                    + object.getClass() + " (must be of type String)");
        }
        return (String) object;
    }

}

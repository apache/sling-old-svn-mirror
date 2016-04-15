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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.RankedServices;
import org.apache.sling.validation.impl.model.MergedValidationModel;
import org.apache.sling.validation.impl.util.Trie;
import org.apache.sling.validation.model.ValidationModel;
import org.apache.sling.validation.model.spi.ValidationModelProvider;
import org.apache.sling.validation.spi.Validator;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retrieves the most appropriate model (the one with the longest matching applicablePath) from any of the
 * {@link ValidationModelProvider}s. Also implements a cache of all previously retrieved models.
 *
 */
@Service
@Component
public class ValidationModelRetrieverImpl implements ValidationModelRetriever, EventHandler {

    @Property(name = EventConstants.EVENT_TOPIC)
    public static final String CACHE_INVALIDATION_EVENT_TOPIC = "org/apache/sling/validation/cache/INVALIDATE";

    /**
     * Map of known validation models (key=validated resourceType, value=trie of ValidationModels sorted by their
     * allowed paths)
     */
    protected Map<String, Trie<ValidationModel>> validationModelsCache = new ConcurrentHashMap<String, Trie<ValidationModel>>();

    /** Map of validation providers (key=service properties) */
    @Reference(name = "modelProvider", referenceInterface = ValidationModelProvider.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
    private RankedServices<ValidationModelProvider> modelProviders = new RankedServices<ValidationModelProvider>();

    /**
     * List of all known validators (key=classname of validator)
     */
    @Reference(name = "validator", referenceInterface = Validator.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
    @Nonnull Map<String, Validator<?>> validators = new ConcurrentHashMap<String, Validator<?>>();

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private static final Logger LOG = LoggerFactory.getLogger(ValidationModelRetrieverImpl.class);

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.validation.impl.ValidationModelRetriever#getModels(java.lang.String, java.lang.String)
     */
    @Override
    public @CheckForNull ValidationModel getModel(@Nonnull String resourceType, String resourcePath,
            boolean considerResourceSuperTypeModels) {
        try {
            ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            try {
                return getModel(resourceType, resourcePath, considerResourceSuperTypeModels, resourceResolver);
            } finally {
                resourceResolver.close();
            }
        } catch (LoginException e) {
            throw new IllegalStateException(
                    "Could not retrieve models, because the administrative resource resolver could not be acquired", e);
        }
    }

    @CheckForNull
    ValidationModel getModel(@Nonnull String resourceType, String resourcePath,
            boolean considerResourceSuperTypeModels, @Nonnull ResourceResolver resourceResolver) {
        // first get model for exactly the requested resource type
        ValidationModel baseModel = getModel(resourceType, resourcePath, resourceResolver);
        String currentResourceType = resourceType;
        if (considerResourceSuperTypeModels) {
            Collection<ValidationModel> modelsToMerge = new ArrayList<ValidationModel>();
            while ((currentResourceType = resourceResolver.getParentResourceType(currentResourceType)) != null) {
                ValidationModel modelToMerge = getModel(currentResourceType, resourcePath, resourceResolver);
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
        }
        return baseModel;
    }

    private @CheckForNull ValidationModel getModel(@Nonnull String resourceType, String resourcePath,
            @Nonnull ResourceResolver resourceResolver) {
        ValidationModel model = null;
        Trie<ValidationModel> modelsForResourceType = validationModelsCache.get(resourceType);
        if (modelsForResourceType == null) {
            modelsForResourceType = fillTrieForResourceType(resourceType, resourceResolver);
        }
        model = modelsForResourceType.getElementForLongestMatchingKey(resourcePath).getValue();
        if (model == null && !modelsForResourceType.isEmpty()) {
            LOG.warn("Although model for resource type {} is available, it is not allowed for path {}", resourceType,
                    resourcePath);
        }
        return model;
    }

    private synchronized @Nonnull Trie<ValidationModel> fillTrieForResourceType(@Nonnull String resourceType,
            @Nonnull ResourceResolver resourceResolver) {
        Trie<ValidationModel> modelsForResourceType = validationModelsCache.get(resourceType);
        // use double-checked locking (http://en.wikipedia.org/wiki/Double-checked_locking)
        if (modelsForResourceType == null) {
            // create a new (empty) trie
            modelsForResourceType = new Trie<ValidationModel>();
            validationModelsCache.put(resourceType, modelsForResourceType);

            // fill trie with data from model providers (all models for the given resource type, independent of resource
            // path)
            for (ValidationModelProvider modelProvider : modelProviders) {
                for (ValidationModel model : modelProvider.getModels(resourceType, validators, resourceResolver)) {
                    for (String applicablePath : model.getApplicablePaths()) {
                        modelsForResourceType.insert(applicablePath, model);
                    }
                }
            }
        }
        return modelsForResourceType;
    }

    protected void bindModelProvider(ValidationModelProvider modelProvider, Map<String, Object> props) {
        modelProviders.bind(modelProvider, props);
        LOG.debug("Invalidating models cache because new model provider '{}' available", modelProvider);
        validationModelsCache.clear();
    }

    protected void unbindModelProvider(ValidationModelProvider modelProvider, Map<String, Object> props) {
        modelProviders.unbind(modelProvider, props);
        LOG.debug("Invalidating models cache because model provider '{}' is no longer available", modelProvider);
        validationModelsCache.clear();
    }

    protected void bindValidator(Validator<?> validator) {
        if (validators.put(validator.getClass().getName(), validator) != null) {
            LOG.debug("Validator with the name '{}' has been registered in the system already and was now overwritten",
                    validator.getClass().getName());
        }
    }

    protected void unbindValidator(Validator<?> validator) {
        // also remove references to all validators in the cache
        validator = validators.remove(validator.getClass().getName());
        if (validator != null) {
            LOG.debug("Invalidating models cache because validator {} is no longer available", validator);
            validationModelsCache.clear();
        }
    }

    @Override
    public void handleEvent(Event event) {
        validationModelsCache.clear();
        LOG.debug("Models cache invalidated");
    }

}

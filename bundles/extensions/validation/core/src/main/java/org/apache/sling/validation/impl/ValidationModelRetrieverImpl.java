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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.RankedServices;
import org.apache.sling.validation.api.ValidationModel;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.api.spi.ValidationModelCache;
import org.apache.sling.validation.api.spi.ValidationModelProvider;
import org.apache.sling.validation.impl.util.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Retrieves the most appropriate (the one with the longest matching applicablePath) model from any of the {@link ValidationModelProvider}s.
 * Also implements a cache of all previously retrieved models.
 *
 */
@Service
@Component
public class ValidationModelRetrieverImpl implements ValidationModelRetriever, ValidationModelCache {

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
    Map<String, Validator<?>> validators = new ConcurrentHashMap<String, Validator<?>>();

    private static final Logger LOG = LoggerFactory.getLogger(ValidationModelRetrieverImpl.class);

    /*
     * (non-Javadoc)
     * @see org.apache.sling.validation.impl.ValidationModelRetriever#getModel(java.lang.String, java.lang.String)
     */
    @Override
    public @CheckForNull ValidationModel getModel(@Nonnull String resourceType, String resourcePath) {
        ValidationModel model = null;
        Trie<ValidationModel> modelsForResourceType = validationModelsCache.get(resourceType);
        if (modelsForResourceType == null) {
            modelsForResourceType = fillTrieForResourceType(resourceType);
        }
        model = modelsForResourceType.getElementForLongestMatchingKey(resourcePath).getValue();
        if (model == null && !modelsForResourceType.isEmpty()) {
            LOG.warn("Although model for resource type {} is available, it is not allowed for path {}",
                    resourceType, resourcePath);
        }
        return model;
    }
    
    private synchronized @Nonnull Trie<ValidationModel> fillTrieForResourceType(@Nonnull String resourceType) {
        Trie<ValidationModel> modelsForResourceType = validationModelsCache.get(resourceType);
        // use double-checked locking (http://en.wikipedia.org/wiki/Double-checked_locking)
        if (modelsForResourceType == null) {
            // create a new (empty) trie
            modelsForResourceType = new Trie<ValidationModel>();
            validationModelsCache.put(resourceType, modelsForResourceType);

            // fill trie with data from model providers (all models for the given resource type, independent of resource path)
            for (ValidationModelProvider modelProvider : modelProviders) {
                for (ValidationModel model : modelProvider.getModel(resourceType, validators)) {
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
    public void invalidate() {
        validationModelsCache.clear();
        LOG.debug("Models cache invalidated");
    }
}

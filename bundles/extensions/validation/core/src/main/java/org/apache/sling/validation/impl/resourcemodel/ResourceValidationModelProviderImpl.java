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
package org.apache.sling.validation.impl.resourcemodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.validation.api.ChildResource;
import org.apache.sling.validation.api.ResourceProperty;
import org.apache.sling.validation.api.ValidationModel;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.api.spi.ValidationModelCache;
import org.apache.sling.validation.api.spi.ValidationModelProvider;
import org.apache.sling.validation.impl.Constants;
import org.apache.sling.validation.impl.ValidationModelRetrieverImpl;
import org.apache.sling.validation.impl.util.ResourceValidationBuilder;
import org.apache.sling.validation.impl.util.Trie;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//the event handler is dynamic and registered in the activate method
@Service(value = ValidationModelProvider.class)
@Component
public class ResourceValidationModelProviderImpl implements ValidationModelProvider, EventHandler {

    static final String MODEL_XPATH_QUERY = "/jcr:root%s/" + Constants.MODELS_HOME
            + "/*[@sling:resourceType=\"%s\" and @%s=\"%s\"]";
    static final String[] TOPICS = { SlingConstants.TOPIC_RESOURCE_REMOVED, SlingConstants.TOPIC_RESOURCE_CHANGED,
            SlingConstants.TOPIC_RESOURCE_ADDED };

    @Reference
    private ResourceResolverFactory rrf = null;

    @Reference
    private ValidationModelCache cache;

    @Reference
    private static final Logger LOG = LoggerFactory.getLogger(ValidationModelRetrieverImpl.class);

    @Reference
    private ThreadPoolManager tpm = null;

    private ThreadPool threadPool;

    private ServiceRegistration eventHandlerRegistration;

    @Activate
    protected void activate(ComponentContext componentContext) {
        threadPool = tpm.get("Validation Service Thread Pool");
        ResourceResolver rr = null;
        try {
            rr = rrf.getAdministrativeResourceResolver(null);
        } catch (LoginException e) {
            LOG.error("Cannot obtain a resource resolver.");
        }
        if (rr != null) {
            StringBuilder sb = new StringBuilder("(");
            String[] searchPaths = rr.getSearchPath();
            if (searchPaths.length > 1) {
                sb.append("|");
            }
            for (String searchPath : searchPaths) {
                if (searchPath.endsWith("/")) {
                    searchPath = searchPath.substring(0, searchPath.length() - 1);
                }
                String path = searchPath + "/*" + Constants.MODELS_HOME;
                sb.append("(path=").append(path).append("*)");
            }
            sb.append(")");
            Dictionary<String, Object> eventHandlerProperties = new Hashtable<String, Object>();
            eventHandlerProperties.put(EventConstants.EVENT_TOPIC, TOPICS);
            eventHandlerProperties.put(EventConstants.EVENT_FILTER, sb.toString());
            eventHandlerRegistration = componentContext.getBundleContext().registerService(
                    EventHandler.class.getName(), this, eventHandlerProperties);
            LOG.debug("Registered event handler for validation models in {}", sb.toString());
            rr.close();
        } else {
            LOG.warn("Null resource resolver. Cannot apply path filtering for event processing. Skipping registering this service as an "
                    + "EventHandler");
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        if (threadPool != null) {
            tpm.release(threadPool);
        }
        if (eventHandlerRegistration != null) {
            eventHandlerRegistration.unregister();
            eventHandlerRegistration = null;
        }
    }

    @Override
    public void handleEvent(Event event) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                cache.invalidate();
            }
        };
        threadPool.execute(task);
    }

    /**
     * Searches for valid validation models in the JCR repository for a certain resource type. All validation models
     * will be returned in a {@link Trie} data structure for easy retrieval of the models using their
     * {@code applicable paths} as trie keys.
     * <p/>
     * A valid content-tree {@code ValidationModel} has the following structure:
     * 
     * <pre>
     * validationModel
     *      &#064;validatedResourceType
     *      &#064;applicablePaths = [path1,path2,...] (optional)
     *      &#064;sling:resourceType = sling/validation/model
     *      fields
     *          field1
     *              &#064;fieldType
     *              validators
     *                  validator1
     *                      &#064;validatorArguments = [key=value,key=value...] (optional)
     *                  validatorN
     *                      #064;validatorArguments = [key=value,key=value...] (optional)
     *          fieldN
     *              &#064;fieldType
     *              validators
     *                  validator1
     *                  &#064;validatorArguments = [key=value,key=value...] (optional)
     * </pre>
     *
     * @param relativeResourceType
     *            {@inheritDoc}
     * @param validatorsMap
     *            {@inheritDoc}
     * @return a {@link Trie} with the validation models; an empty trie if no model is found
     */
    @Override
    public @Nonnull Collection<ValidationModel> getModel(@Nonnull String relativeResourceType,
            @Nonnull Map<String, Validator<?>> validatorsMap) {
        ResourceResolver rr = null;
        ResourceValidationModel vm;
        Collection<ValidationModel> validationModels = new ArrayList<ValidationModel>();
        try {
            rr = rrf.getAdministrativeResourceResolver(null);
            String[] searchPaths = rr.getSearchPath();
            for (String searchPath : searchPaths) {
                final String queryString = String.format(MODEL_XPATH_QUERY, searchPath,
                        Constants.VALIDATION_MODEL_RESOURCE_TYPE, Constants.VALIDATED_RESOURCE_TYPE,
                        relativeResourceType);
                Iterator<Resource> models = rr.findResources(queryString, "xpath");
                while (models.hasNext()) {
                    Resource model = models.next();
                    LOG.info("Found validation model resource {}.", model.getPath());
                    String jcrPath = model.getPath();
                    try {
                        ValueMap validationModelProperties = model.adaptTo(ValueMap.class);
                        String[] applicablePaths = PropertiesUtil.toStringArray(validationModelProperties.get(
                                Constants.APPLICABLE_PATHS, String[].class));
                        Resource r = model.getChild(Constants.PROPERTIES);
                        Set<ResourceProperty> resourceProperties = ResourceValidationBuilder.buildProperties(
                                validatorsMap, r);
                        List<ChildResource> children = ResourceValidationBuilder.buildChildren(model, model,
                                validatorsMap);
                        if (resourceProperties.isEmpty() && children.isEmpty()) {
                            throw new IllegalArgumentException("Neither children nor properties set.");
                        } else {
                            vm = new ResourceValidationModel(jcrPath, resourceProperties, relativeResourceType,
                                    applicablePaths, children);
                            validationModels.add(vm);
                        }
                    } catch (IllegalArgumentException e) {
                        throw new IllegalStateException("Found invalid validation model in '" + jcrPath + "': "
                                + e.getMessage(), e);
                    }
                }
                if (validationModels.isEmpty()) {
                    // do not continue to search in other search paths if some results were already found!
                    // earlier search paths overlay lower search paths (/apps wins over /libs)
                    // the applicable content paths do not matter here!
                    break;
                }
            }
        } catch (LoginException e) {
            throw new IllegalStateException("Unable to obtain a resource resolver.", e);
        } finally {
            if (rr != null) {
                rr.close();
            }
        }
        return validationModels;
    }

}

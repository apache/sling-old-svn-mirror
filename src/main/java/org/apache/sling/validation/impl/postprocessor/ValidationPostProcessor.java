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
package org.apache.sling.validation.impl.postprocessor;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.ValidationService;
import org.apache.sling.validation.model.ValidationModel;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Designate(ocd = ValidationPostProcessorConfiguration.class)
public class ValidationPostProcessor implements SlingPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationPostProcessor.class);

    private ValidationPostProcessorConfiguration configuration;

    @Reference
    protected ValidationService validationService;

    protected void activate(ValidationPostProcessorConfiguration configuration) {
        this.configuration = configuration;
    }

    private boolean enabledForPath(String path) {
        // this might be null in case the property is not set (https://osgi.org/bugzilla/show_bug.cgi?id=208)
        String[] enabledPathPrefixes = configuration.enabledForPathPrefix();
        if (enabledPathPrefixes == null) {
            return false;
        }
        for (String enabledPathPrefix : enabledPathPrefixes) {
            if (path.startsWith(enabledPathPrefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void process(SlingHttpServletRequest request, List<Modification> changes) throws Exception {
        // is this globally disabled?
        if (configuration.disabled()) {
            LOG.debug("ValidationPostProcessor globally disabled!");
            return;
        }
        
        String path = request.getResource().getPath();
        if (enabledForPath(path)) {
            LOG.debug("ValidationPostProcessor is enabled for path {}", path);
        } else {
            LOG.debug("ValidationPostProcessor is not enabled for path {}", path);
            return;
        }

        // request.getResource() contains the old resource (might even be the non-existing one), 
        // therefore retrieve the transient new resource at the same path
        Resource newResource = request.getResourceResolver().getResource(request.getResource().getPath());
        if (newResource == null) {
            LOG.debug("Could not find new/modified resource at {} to validate", request.getResource().getPath());
            return;
        }
        // get model for resource type
        ValidationModel model = validationService.getValidationModel(newResource, configuration.considerResourceSuperTypes());
        if (model == null) {
            if (configuration.failForMissingValidationModels()) {
                throw new IllegalStateException("Could not find validation model for resource type " + newResource.getResourceType());
            } else {
                LOG.debug("Could not find validation model for resource type {} -> skip validation", newResource.getResourceType());
                return;
            }
        }
        ValidationResult validationResult = validationService.validate(newResource, model);
        if (!validationResult.isValid()) {
            throw new InvalidResourcePostProcessorException(validationResult, request.getResourceBundle(null));
        } else {
            LOG.debug("Successfully validated modified/created resource at '{}'", request.getResource().getPath());
        }
    }

}

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
package org.apache.sling.validation.testservices;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.servlets.post.AbstractPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.validation.api.ValidationModel;
import org.apache.sling.validation.api.ValidationResult;
import org.apache.sling.validation.api.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.List;

@Component()
@Service(PostOperation.class)
@Properties({
        @Property(
                name = PostOperation.PROP_OPERATION_NAME,
                value = "validation",
                propertyPrivate = true
        )
})
public class ValidationPostOperation extends AbstractPostOperation {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationPostOperation.class);

    @Reference
    private ValidationService validationService;

    @Override
    protected void doRun(SlingHttpServletRequest request, PostResponse response, List<Modification> changes) throws RepositoryException {
        if (response instanceof ValidationPostResponse) {
            ValidationPostResponse vpr = (ValidationPostResponse) response;
            ValueMap requestParameters = request.adaptTo(ValueMap.class);
            String[] resourceTypeValues = requestParameters.get("sling:resourceType", String[].class);
            String resourceType = null;
            if (resourceTypeValues != null && resourceTypeValues.length > 0) {
                resourceType = resourceTypeValues[0];
            }
            if (resourceType != null && !"".equals(resourceType)) {
                String resourcePath = request.getRequestPathInfo().getResourcePath();
                ValidationModel vm = validationService.getValidationModel(resourceType, resourcePath);
                if (vm != null) {
                    ValidationResult vr = validationService.validate(requestParameters, vm);
                    vpr.setValidationResult(vr);
                } else {
                    LOG.error("No validation model for resourceType {} and resourcePath {} ", resourceType, resourcePath);
                }
            }
        }
    }
}

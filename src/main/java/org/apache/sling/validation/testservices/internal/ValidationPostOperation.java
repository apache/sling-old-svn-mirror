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
package org.apache.sling.validation.testservices.internal;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlets.post.AbstractPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.ValidationService;
import org.apache.sling.validation.model.ValidationModel;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = PostOperation.class,
    property = {
        PostOperation.PROP_OPERATION_NAME + "=validation"
    }
)
public class ValidationPostOperation extends AbstractPostOperation {

    private final Logger logger = LoggerFactory.getLogger(ValidationPostOperation.class);

    @Reference
    private ValidationService validationService;

    @Override
    protected void doRun(SlingHttpServletRequest request, PostResponse response, List<Modification> changes) {
        if (response instanceof ValidationPostResponse) {
            final Map<String, Object> base = new LinkedHashMap<>();
            final ValueMapDecorator valueMap = new ValueMapDecorator(base);
            final Enumeration<String> names = request.getParameterNames();
            while (names.hasMoreElements()) {
                final String name = names.nextElement();
                valueMap.put(name, request.getRequestParameter(name).getString());
            }

            final String resourceType = request.getRequestParameter("sling:resourceType").getString();
            if (resourceType != null && !"".equals(resourceType)) {
                final String resourcePath = request.getRequestPathInfo().getResourcePath();
                final ValidationModel validationModel = validationService.getValidationModel(resourceType, resourcePath, false);
                if (validationModel != null) {
                    final ValidationResult validationResult = validationService.validate(valueMap, validationModel);
                    final ValidationPostResponse validationPostResponse = (ValidationPostResponse) response;
                    validationPostResponse.setValidationResult(validationResult);
                } else {
                    logger.error("No validation model for resourceType {} and resourcePath {} ", resourceType, resourcePath);
                }
            } else {
                logger.error("resource type is empty");
            }
        }
    }

}

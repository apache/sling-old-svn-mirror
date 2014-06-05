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
package org.apache.sling.scripting.thymeleaf;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.input.ReaderInputStream;
import org.thymeleaf.TemplateProcessingParameters;
import org.thymeleaf.context.IContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.resourceresolver.IResourceResolver;
import org.thymeleaf.util.Validate;

public class SlingResourceResolver implements IResourceResolver {

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public InputStream getResourceAsStream(final TemplateProcessingParameters templateProcessingParameters, final String resourceName) {

        Validate.notNull(templateProcessingParameters, "Template Processing Parameters cannot be null");

        final IContext context = templateProcessingParameters.getContext();
        if (context instanceof SlingContext) {
            final SlingContext slingContext = (SlingContext) context;
            return new ReaderInputStream(slingContext.getReader(), StandardCharsets.UTF_8);
        } else {
            throw new TemplateProcessingException("Cannot handle context: " + context.getClass().getName());
        }
    }

}

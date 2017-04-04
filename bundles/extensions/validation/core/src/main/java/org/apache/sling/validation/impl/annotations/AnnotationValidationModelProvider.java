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
package org.apache.sling.validation.impl.annotations;

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.sling.validation.model.ValidationModel;
import org.apache.sling.validation.model.spi.ValidationModelProvider;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "Apache Sling Validation Annotation Based Model Provider", service = ValidationModelProvider.class)
public class AnnotationValidationModelProvider implements ValidationModelProvider {

    private static final Logger log = LoggerFactory.getLogger(AnnotationValidationModelProvider.class);

    ValidationPackageBundleListener listener;

    final ValidationModelImplementations validationModelImplementations = new ValidationModelImplementations();

    @Activate
    @Modified
    protected void activate(final ComponentContext ctx) {
        this.listener = new ValidationPackageBundleListener(ctx.getBundleContext(), this.validationModelImplementations);

    }

    @Override
    public List<ValidationModel> getValidationModels(@Nonnull String relativeResourceType) throws IllegalStateException {
        log.info("getValidationModels called");
        List<ValidationModel> list = validationModelImplementations.getValidationModelsByResourceType(relativeResourceType);
        return list;

    }

    @Deactivate
    protected void deactivate() {
        this.listener.unregisterAll();
        this.validationModelImplementations.removeAll();
    }


}

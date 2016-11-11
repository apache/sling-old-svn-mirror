package org.apache.sling.installer.factory.model.impl;/*
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


import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.apache.sling.provisioning.model.Configuration;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.Section;
import org.apache.sling.provisioning.model.Traceable;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.apache.sling.provisioning.model.io.ModelWriter;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This transformer detects a file with the ending ".model" containing
 * a provisioning model.
 */
@Component(service = ResourceTransformer.class)
public class ModelTransformer implements ResourceTransformer {

    public static final String TYPE_PROV_MODEL = "provisioningmodel";

    public static final String ATTR_MODEL = "model";

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public TransformationResult[] transform(final RegisteredResource resource) {
        if ( resource.getType().equals(InstallableResource.TYPE_FILE) && resource.getURL().endsWith(".model") ) {
            try ( final Reader reader = new InputStreamReader(resource.getInputStream(), "UTF-8") ) {
                final Model model = ModelReader.read(reader, resource.getURL());

                Map<Traceable, String> errors = ModelUtility.validate(model);
                if ( errors == null ) {
                    try {
                        final Model effectiveModel = ModelUtility.getEffectiveModel(model);

                        errors = validate(effectiveModel);
                        if ( errors == null ) {

                            final Feature f = effectiveModel.getFeatures().get(0);
                            final TransformationResult tr = new TransformationResult();
                            tr.setId(f.getName());
                            tr.setResourceType(TYPE_PROV_MODEL);
                            tr.setVersion(new Version(f.getVersion()));

                            try ( final StringWriter sw = new StringWriter()) {
                                ModelWriter.write(sw, effectiveModel);
                                tr.setAttributes(Collections.singletonMap(ATTR_MODEL, (Object)sw.toString()));
                            }
                            return new TransformationResult[] {tr};
                        }
                    } catch ( final IllegalArgumentException iae ) {
                        errors = Collections.singletonMap((Traceable)model, iae.getMessage());
                    }
                }
                if ( errors != null ) {
                    logger.warn("Errors during parsing model at {} : {}", resource.getURL(), errors.values());
                }

            } catch ( final IOException ioe) {
                logger.info("Unable to read model from " + resource.getURL(), ioe);
            }
        }
        return null;
    }

    private Map<Traceable, String> validate(final Model effectiveModel) {
        if ( effectiveModel.getFeatures().size() != 1 ) {
            return Collections.singletonMap((Traceable)effectiveModel, "Model should only contain a single feature.");
        }
        final Feature feature = effectiveModel.getFeatures().get(0);
        if ( feature.isSpecial() ) {
            return Collections.singletonMap((Traceable)feature, "Feature must not be special.");
        }
        if ( feature.getVersion() == null ) {
            return Collections.singletonMap((Traceable)feature, "Feature must have a version.");
        }
        for(final Section section : feature.getAdditionalSections()) {
            if ( !"repoinit".equals(section.getName()) ) {
                return Collections.singletonMap((Traceable)section, "Additional section not supported.");
            }
        }
        for(final RunMode mode : feature.getRunModes()) {
            if ( mode.isSpecial() ) {
                return Collections.singletonMap((Traceable)mode, "RunMode must not be special.");
            }
            for(final Configuration cfg : mode.getConfigurations()) {
                if ( cfg.isSpecial() ) {
                    return Collections.singletonMap((Traceable)cfg, "Configuration must not be special.");
                }
            }
        }

        return null;
    }

}

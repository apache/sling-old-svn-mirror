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


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.Traceable;
import org.apache.sling.provisioning.model.io.ModelArchiveReader;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.apache.sling.provisioning.model.io.ModelWriter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
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

    public static final String ATTR_FEATURE_INDEX = "feature";

    public static final String ATTR_BASE_PATH = "path";

    public static final String ATTR_FEATURE_NAME = "name";

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private BundleContext bundleContext;

    @Activate
    private void activate(final BundleContext bc) {
        this.bundleContext = bc;
    }

    @Override
    public TransformationResult[] transform(final RegisteredResource resource) {
        Model model = null;
        File baseDir = null;
        if ( resource.getType().equals(InstallableResource.TYPE_FILE) && resource.getURL().endsWith(".model") ) {
            try ( final Reader reader = new InputStreamReader(resource.getInputStream(), "UTF-8") ) {
                model = ModelReader.read(reader, resource.getURL());
            } catch ( final IOException ioe) {
                logger.info("Unable to read model from " + resource.getURL(), ioe);
            }
        }
        if ( resource.getType().equals(InstallableResource.TYPE_FILE) && resource.getURL().endsWith(".mar") ) {
            baseDir = this.bundleContext.getDataFile("");
            try ( final InputStream is = resource.getInputStream() ) {

                model = ModelArchiveReader.read(is, new ModelArchiveReader.ArtifactConsumer() {
                    @Override
                    public void consume(final Artifact artifact, final InputStream is) throws IOException {
                        // nothing to do, install task does extraction
                    }
                });
            } catch ( final IOException ioe) {
                logger.info("Unable to read model from " + resource.getURL(), ioe);
            }
        }
        if ( model != null ) {
            Map<Traceable, String> errors = ModelUtility.validate(model);
            if ( errors == null ) {
                try {
                    final Model effectiveModel = ModelUtility.getEffectiveModel(model);

                    errors = ModelUtility.validateIncludingVersion(effectiveModel);
                    if ( errors == null ) {

                        String modelTxt = null;
                        try ( final StringWriter sw = new StringWriter()) {
                            ModelWriter.write(sw, effectiveModel);
                            modelTxt = sw.toString();
                        } catch ( final IOException ioe) {
                            logger.info("Unable to read model from " + resource.getURL(), ioe);
                        }

                        if ( modelTxt != null ) {
                            final TransformationResult[] result = new TransformationResult[effectiveModel.getFeatures().size()];
                            int index = 0;
                            for(final Feature f : effectiveModel.getFeatures()) {

                                final TransformationResult tr = new TransformationResult();
                                tr.setResourceType(TYPE_PROV_MODEL);
                                tr.setId(f.getName());
                                tr.setVersion(new Version(f.getVersion()));

                                final Map<String, Object> attributes = new HashMap<>();
                                attributes.put(ATTR_MODEL, modelTxt);
                                attributes.put(ATTR_FEATURE_INDEX, index);
                                attributes.put(ATTR_FEATURE_NAME, f.getName() + "-" + f.getVersion());
                                if ( baseDir != null ) {
                                    final File dir = new File(baseDir, f.getName() + "-" + f.getVersion());
                                    attributes.put(ATTR_BASE_PATH, dir.getAbsolutePath());
                                }
                                tr.setAttributes(attributes);

                                result[index] = tr;
                                index++;
                            }
                            return result;
                        }
                    }
                } catch ( final IllegalArgumentException iae ) {
                    errors = Collections.singletonMap((Traceable)model, iae.getMessage());
                }
            }
            if ( errors != null ) {
                logger.warn("Errors during parsing model at {} : {}", resource.getURL(), errors.values());
            }
        }
        return null;
    }
}

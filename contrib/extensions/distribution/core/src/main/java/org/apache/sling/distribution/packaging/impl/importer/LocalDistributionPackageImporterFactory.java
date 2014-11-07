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
package org.apache.sling.distribution.packaging.impl.importer;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.component.DistributionComponentFactory;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.packaging.DistributionPackageImportException;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.distribution.packaging.DistributionPackageImporter} implementation which imports a FileVault
 * based {@link org.apache.sling.distribution.packaging.DistributionPackage} locally.
 */
@Component(label = "Sling Distribution - Local Package Importer Factory",
        metatype = true,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
@Service(value = DistributionPackageImporter.class)
public class LocalDistributionPackageImporterFactory implements DistributionPackageImporter {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(value = DistributionComponentFactory.PACKAGE_IMPORTER_LOCAL, propertyPrivate = true)
    private static final String TYPE = DistributionComponentFactory.COMPONENT_TYPE;

    @Property
    private static final String NAME = DistributionComponentFactory.COMPONENT_NAME;


    @Property(label = "Package Builder Properties", cardinality = 100)
    public static final String PACKAGE_BUILDER = DistributionComponentFactory.COMPONENT_PACKAGE_BUILDER;

    @Reference
    private DistributionComponentFactory componentFactory;

    DistributionPackageImporter importer;

    @Activate
    public void activate(Map<String, Object> config) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.putAll(config);
        String[] packageBuilderProperties = PropertiesUtil.toStringArray(config.get(PACKAGE_BUILDER));
        properties.put(PACKAGE_BUILDER, SettingsUtils.parseLines(packageBuilderProperties));

        importer = componentFactory.createComponent(DistributionPackageImporter.class, properties, null);
    }


    public boolean importPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionPackageImportException {
        return importer.importPackage(resourceResolver, distributionPackage);
    }

    public DistributionPackage importStream(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionPackageImportException {
        return importer.importStream(resourceResolver, stream);
    }

}

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
package org.apache.sling.distribution.serialization.impl;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.impl.FileDistributionPackageBuilder;
import org.apache.sling.distribution.packaging.impl.ResourceDistributionPackageBuilder;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;

/**
 * A factory for package builders
 */
@Component(metatype = true,
        label = "Apache Sling Distribution Packaging - Package Builder Factory",
        description = "OSGi configuration for package builders",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
@Service(DistributionPackageBuilder.class)
@Property(name = "webconsole.configurationFactory.nameHint", value = "Builder name: {name}")
public class DistributionPackageBuilderFactory implements DistributionPackageBuilder {

    /**
     * name of this package builder.
     */
    @Property(label = "Name", description = "The name of the package builder.")
    private static final String NAME = DistributionComponentConstants.PN_NAME;

    /**
     * type of this package builder.
     */
    @Property(options = {
            @PropertyOption(name = "resource",
                    value = "resource packages"
            ),
            @PropertyOption(name = "file",
                    value = "file packages"
            )},
            value = "resource", label = "type", description = "The persistence type used by this package builder")
    private static final String PERSISTENCE = DistributionComponentConstants.PN_TYPE;

    @Property(name = "format.target", label = "Content Serializer", description = "The target reference for the DistributionSerializationFormat used to (de)serialize packages, " +
            "e.g. use target=(name=...) to bind to services by name.", value = SettingsUtils.COMPONENT_NAME_DEFAULT)
    @Reference(name = "format")
    private DistributionContentSerializer contentSerializer;


    /**
     * Temp file folder
     */
    @Property(label = "Temp Filesystem Folder", description = "The filesystem folder where the temporary files should be saved.")
    private static final String TEMP_FS_FOLDER = "tempFsFolder";

    // 128K
    private static final int DEFAULT_FILE_THRESHOLD_VALUE = 1024000;

    @Property(
        label="File threshold (in bytes)",
        description = "Once the data reaches the configurable size value, buffering to memory switches to file buffering.",
        intValue = DEFAULT_FILE_THRESHOLD_VALUE
    )
    public static final String FILE_THRESHOLD = "fileThreshold";

    private DistributionPackageBuilder packageBuilder;

    @Activate
    public void activate(Map<String, Object> config) {

        String persistenceType = PropertiesUtil.toString(config.get(PERSISTENCE), null);
        String tempFsFolder = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(TEMP_FS_FOLDER), null));


        if ("file".equals(persistenceType)) {
            packageBuilder = new FileDistributionPackageBuilder(contentSerializer.getName(), contentSerializer, tempFsFolder);
        } else {
            final int fileThreshold = PropertiesUtil.toInteger(config.get(FILE_THRESHOLD), DEFAULT_FILE_THRESHOLD_VALUE);
            packageBuilder = new ResourceDistributionPackageBuilder(contentSerializer.getName(), contentSerializer, tempFsFolder, fileThreshold);
        }


    }

    public String getType() {
        return packageBuilder.getType();
    }

    @Nonnull
    public DistributionPackage createPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest request) throws DistributionException {
        return packageBuilder.createPackage(resourceResolver, request);
    }

    @Nonnull
    public DistributionPackage readPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionException {
        return packageBuilder.readPackage(resourceResolver, stream);
    }

    @CheckForNull
    public DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String id) throws DistributionException {
        return packageBuilder.getPackage(resourceResolver, id);
    }

    public boolean installPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionException {
        return packageBuilder.installPackage(resourceResolver, distributionPackage);
    }

    @Nonnull
    @Override
    public DistributionPackageInfo installPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionException {
        return packageBuilder.installPackage(resourceResolver, stream);
    }
}

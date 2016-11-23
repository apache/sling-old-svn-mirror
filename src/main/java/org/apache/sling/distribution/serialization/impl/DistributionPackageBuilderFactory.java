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

import java.io.InputStream;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.monitor.impl.MonitoringDistributionPackageBuilder;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.impl.FileDistributionPackageBuilder;
import org.apache.sling.distribution.packaging.impl.ResourceDistributionPackageBuilder;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.util.impl.FileBackedMemoryOutputStream.MemoryUnit;
import org.osgi.framework.BundleContext;

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

    // 1M
    private static final int DEFAULT_FILE_THRESHOLD_VALUE = 1;

    @Property(
        label="File threshold",
        description = "Once the data reaches the configurable size value, buffering to memory switches to file buffering.",
        intValue = DEFAULT_FILE_THRESHOLD_VALUE
    )
    public static final String FILE_THRESHOLD = "fileThreshold";

    private static final String DEFAULT_MEMORY_UNIT = "MEGA_BYTES";

    @Property(
        label = "The memory unit for the file threshold",
        description = "The memory unit for the file threshold, Megabytes by default",
        value = DEFAULT_MEMORY_UNIT,
        options = {
            @PropertyOption(name = "BYTES", value = "Bytes"),
            @PropertyOption(name = "KILO_BYTES", value = "Kilobytes"),
            @PropertyOption(name = "MEGA_BYTES", value = "Megabytes"),
            @PropertyOption(name = "GIGA_BYTES", value = "Gigabytes")
        }
    )
    private static final String MEMORY_UNIT = "memoryUnit";

    private static final boolean DEFAULT_USE_OFF_HEAP_MEMORY = false;

    @Property(
        label="Flag to enable/disable the off-heap memory",
        description = "Flag to enable/disable the off-heap memory, false by default",
        boolValue = DEFAULT_USE_OFF_HEAP_MEMORY
    )
    public static final String USE_OFF_HEAP_MEMORY = "useOffHeapMemory";

    private static final String DEFAULT_DIGEST_ALGORITHM = "NONE";

    @Property(
        label = "The digest algorithm to calculate the package checksum",
        description = "The digest algorithm to calculate the package checksum, Megabytes by default",
        value = DEFAULT_DIGEST_ALGORITHM,
        options = {
            @PropertyOption(name = DEFAULT_DIGEST_ALGORITHM, value = "Do not send digest"),
            @PropertyOption(name = "MD2", value = "md2"),
            @PropertyOption(name = "MD5", value = "md5"),
            @PropertyOption(name = "SHA-1", value = "sha1"),
            @PropertyOption(name = "SHA-256", value = "sha256"),
            @PropertyOption(name = "SHA-384", value = "sha384"),
            @PropertyOption(name = "SHA-512", value = "sha512")
        }
    )
    private static final String DIGEST_ALGORITHM = "digestAlgorithm";

    private static final int DEFAULT_MONITORING_QUEUE_SIZE = 100;

    @Property(
        label="The number of items for monitoring distribution packages creation/installation",
        description = "The number of items for monitoring distribution packages creation/installation, 100 by default",
        intValue = DEFAULT_MONITORING_QUEUE_SIZE
    )
    private static final String MONITORING_QUEUE_SIZE = "monitoringQueueSize";

    /**
     * Package node filters
     */
    @Property(label = "Package Node Filters", description = "The package node path filters. Filter format: path|+include|-exclude", cardinality = 100)
    private static final String PACKAGE_FILTERS = "package.filters";

    /**
     * Package property filters
     */
    @Property(label = "Package Property Filters", description = "The package property path filters. Filter format: path|+include|-exclude",
            unbounded = PropertyUnbounded.ARRAY, value = {})
    private static final String PROPERTY_FILTERS = "property.filters";

    private MonitoringDistributionPackageBuilder packageBuilder;

    @Activate
    public void activate(BundleContext context,
                         Map<String, Object> config) {

        String[] nodeFilters = SettingsUtils.removeEmptyEntries(PropertiesUtil.toStringArray(config.get(PACKAGE_FILTERS), null));
        String[] propertyFilters = SettingsUtils.removeEmptyEntries(PropertiesUtil.toStringArray(config.get(PROPERTY_FILTERS), null));
        String persistenceType = PropertiesUtil.toString(config.get(PERSISTENCE), null);
        String tempFsFolder = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(TEMP_FS_FOLDER), null));
        String digestAlgorithm = PropertiesUtil.toString(config.get(DIGEST_ALGORITHM), DEFAULT_DIGEST_ALGORITHM);
        if (DEFAULT_DIGEST_ALGORITHM.equals(digestAlgorithm)) {
            digestAlgorithm = null;
        }

        DistributionPackageBuilder wrapped;
        if ("file".equals(persistenceType)) {
            wrapped = new FileDistributionPackageBuilder(contentSerializer.getName(), contentSerializer, tempFsFolder, digestAlgorithm, nodeFilters, propertyFilters);
        } else {
            final int fileThreshold = PropertiesUtil.toInteger(config.get(FILE_THRESHOLD), DEFAULT_FILE_THRESHOLD_VALUE);
            String memoryUnitName = PropertiesUtil.toString(config.get(MEMORY_UNIT), DEFAULT_MEMORY_UNIT);
            final MemoryUnit memoryUnit = MemoryUnit.valueOf(memoryUnitName);
            final boolean useOffHeapMemory = PropertiesUtil.toBoolean(config.get(USE_OFF_HEAP_MEMORY), DEFAULT_USE_OFF_HEAP_MEMORY);
            wrapped = new ResourceDistributionPackageBuilder(contentSerializer.getName(), contentSerializer, tempFsFolder, fileThreshold, memoryUnit, useOffHeapMemory, digestAlgorithm, nodeFilters, propertyFilters);
        }

        int monitoringQueueSize = PropertiesUtil.toInteger(config.get(MONITORING_QUEUE_SIZE), DEFAULT_MONITORING_QUEUE_SIZE);
        packageBuilder = new MonitoringDistributionPackageBuilder(monitoringQueueSize, wrapped, context);
    }

    @Deactivate
    public void deactivate() {
        packageBuilder.clear();
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

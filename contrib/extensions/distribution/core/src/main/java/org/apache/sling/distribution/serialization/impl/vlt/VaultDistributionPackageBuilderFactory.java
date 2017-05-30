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
package org.apache.sling.distribution.serialization.impl.vlt;

import org.apache.felix.scr.annotations.*;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.Scheduler;
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
import org.apache.sling.distribution.packaging.impl.ResourceDistributionPackageCleanup;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.util.impl.FileBackedMemoryOutputStream.MemoryUnit;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

/**
 * A package builder for Apache Jackrabbit FileVault based implementations.
 */
@Component(metatype = true,
        label = "Apache Sling Distribution Packaging - Vault Package Builder Factory",
        description = "OSGi configuration for vault package builders",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
@Service(DistributionPackageBuilder.class)
@Property(name = "webconsole.configurationFactory.nameHint", value = "Builder name: {name}")
public class VaultDistributionPackageBuilderFactory implements DistributionPackageBuilder {

    /**
     * name of this package builder.
     */
    @Property(label = "Name", description = "The name of the package builder.")
    private static final String NAME = DistributionComponentConstants.PN_NAME;


    /**
     * type of this package builder.
     */
    @Property(options = {
            @PropertyOption(name = "jcrvlt",
                    value = "jcr packages"
            ),
            @PropertyOption(name = "filevlt",
                    value = "file packages"
            )},
            value = "jcrvlt", label = "type", description = "The type of this package builder")
    private static final String TYPE = DistributionComponentConstants.PN_TYPE;


    /**
     * import mode property for file vault package builder
     */
    @Property(label = "Import Mode", description = "The vlt import mode for created packages.")
    private static final String IMPORT_MODE = "importMode";

    /**
     * ACL handling property for file vault package builder
     */
    @Property(label = "Acl Handling", description = "The vlt acl handling mode for created packages.")
    private static final String ACL_HANDLING = "aclHandling";

    /**
     * Package roots
     */
    @Property(label = "Package Roots", description = "The package roots to be used for created packages. (this is useful for assembling packages with an user that cannot read above the package root)")
    private static final String PACKAGE_ROOTS = "package.roots";

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

    /**
     * Temp file folder
     */
    @Property(label = "Temp Filesystem Folder", description = "The filesystem folder where the temporary files should be saved.")
    private static final String TEMP_FS_FOLDER = "tempFsFolder";

    @Property(label = "Use Binary References", description = "If activated, it avoids sending binaries in the distribution package.", boolValue = false)
    public static final String USE_BINARY_REFERENCES = "useBinaryReferences";

    @Property(label = "Autosave threshold", description = "The value after which autosave is triggered for intermediate changes.", intValue = -1)
    public static final String AUTOSAVE_THRESHOLD = "autoSaveThreshold";

    private static final long DEFAULT_PACKAGE_CLEANUP_DELAY = 60L;

    @Property(
            label = "The delay in seconds between two runs of the cleanup phase for resource persisted packages.",
            description = "The resource persisted packages are cleaned up periodically (asynchronously) since SLING-6503." +
                    "The delay between two runs of the cleanup phase can be configured with this setting. 60 seconds by default",
            longValue = DEFAULT_PACKAGE_CLEANUP_DELAY
    )
    private static final String PACKAGE_CLEANUP_DELAY = "cleanupDelay";

    // 1M
    private static final int DEFAULT_FILE_THRESHOLD_VALUE = 1;

    @Property(
            label = "File threshold (in bytes)",
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
    private static final String MEMORY_UNIT = "MEGA_BYTES";

    private static final boolean DEFAULT_USE_OFF_HEAP_MEMORY = false;

    @Property(
            label = "Flag to enable/disable the off-heap memory",
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

    private static final int DEFAULT_MONITORING_QUEUE_SIZE = 0;

    @Property(
            label = "The number of items for monitoring distribution packages creation/installation",
            description = "The number of items for monitoring distribution packages creation/installation, 100 by default",
            intValue = DEFAULT_MONITORING_QUEUE_SIZE
    )
    private static final String MONITORING_QUEUE_SIZE = "monitoringQueueSize";

    @Reference
    private Packaging packaging;

    @Reference
    private ResourceResolverFactory resolverFactory;

    private ServiceRegistration packageCleanup = null;

    private MonitoringDistributionPackageBuilder packageBuilder;


    @Activate
    public void activate(BundleContext context, Map<String, Object> config) {

        String name = PropertiesUtil.toString(config.get(NAME), null);
        String type = PropertiesUtil.toString(config.get(TYPE), null);
        String importModeString = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(IMPORT_MODE), null));
        String aclHandlingString = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(ACL_HANDLING), null));

        String[] packageRoots = SettingsUtils.removeEmptyEntries(PropertiesUtil.toStringArray(config.get(PACKAGE_ROOTS), null));
        String[] packageNodeFilters = SettingsUtils.removeEmptyEntries(PropertiesUtil.toStringArray(config.get(PACKAGE_FILTERS), null));
        String[] packagePropertyFilters = SettingsUtils.removeEmptyEntries(PropertiesUtil.toStringArray(config.get(PROPERTY_FILTERS), null));

        long cleanupDelay = PropertiesUtil.toLong(config.get(PACKAGE_CLEANUP_DELAY), DEFAULT_PACKAGE_CLEANUP_DELAY);

        String tempFsFolder = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(TEMP_FS_FOLDER), null));
        boolean useBinaryReferences = PropertiesUtil.toBoolean(config.get(USE_BINARY_REFERENCES), false);
        int autosaveThreshold = PropertiesUtil.toInteger(config.get(AUTOSAVE_THRESHOLD), -1);

        String digestAlgorithm = PropertiesUtil.toString(config.get(DIGEST_ALGORITHM), DEFAULT_DIGEST_ALGORITHM);
        if (DEFAULT_DIGEST_ALGORITHM.equals(digestAlgorithm)) {
            digestAlgorithm = null;
        }

        ImportMode importMode = null;
        if (importModeString != null) {
            importMode = ImportMode.valueOf(importModeString.trim());
        }

        AccessControlHandling aclHandling = null;
        if (aclHandlingString != null) {
            aclHandling = AccessControlHandling.valueOf(aclHandlingString.trim());
        }

        DistributionContentSerializer contentSerializer = new FileVaultContentSerializer(name, packaging, importMode, aclHandling,
                packageRoots, packageNodeFilters, packagePropertyFilters, useBinaryReferences, autosaveThreshold);

        DistributionPackageBuilder wrapped;
        if ("filevlt".equals(type)) {
            wrapped = new FileDistributionPackageBuilder(name, contentSerializer, tempFsFolder, digestAlgorithm, packageNodeFilters, packagePropertyFilters);
        } else {
            final int fileThreshold = PropertiesUtil.toInteger(config.get(FILE_THRESHOLD), DEFAULT_FILE_THRESHOLD_VALUE);
            String memoryUnitName = PropertiesUtil.toString(config.get(MEMORY_UNIT), DEFAULT_MEMORY_UNIT);
            final MemoryUnit memoryUnit = MemoryUnit.valueOf(memoryUnitName);
            final boolean useOffHeapMemory = PropertiesUtil.toBoolean(config.get(USE_OFF_HEAP_MEMORY), DEFAULT_USE_OFF_HEAP_MEMORY);
            ResourceDistributionPackageBuilder resourceDistributionPackageBuilder = new ResourceDistributionPackageBuilder(contentSerializer.getName(), contentSerializer, tempFsFolder, fileThreshold, memoryUnit, useOffHeapMemory, digestAlgorithm, packageNodeFilters, packagePropertyFilters);
            Runnable cleanup = new ResourceDistributionPackageCleanup(resolverFactory, resourceDistributionPackageBuilder);
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(Scheduler.PROPERTY_SCHEDULER_CONCURRENT, false);
            props.put(Scheduler.PROPERTY_SCHEDULER_PERIOD, cleanupDelay);
            packageCleanup = context.registerService(Runnable.class.getName(), cleanup, props);
            wrapped = resourceDistributionPackageBuilder;
        }

        int monitoringQueueSize = PropertiesUtil.toInteger(config.get(MONITORING_QUEUE_SIZE), DEFAULT_MONITORING_QUEUE_SIZE);
        packageBuilder = new MonitoringDistributionPackageBuilder(monitoringQueueSize, wrapped, context);
    }

    @Deactivate
    public void deactivate() {
        packageBuilder.clear();
        if (packageCleanup != null) {
            packageCleanup.unregister();
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

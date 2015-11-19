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
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.serialization.impl.ResourceSharedDistributionPackageBuilder;

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
public class VaultDistributionPackageBuilderFactory implements DistributionPackageBuilder {

    /**
     * name of this package builder.
     */
    @Property(label = "Name", description = "The name of the package builder.")
    public static final String NAME = DistributionComponentConstants.PN_NAME;


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
    public static final String TYPE = DistributionComponentConstants.PN_TYPE;


    /**
     * import mode property for file vault package builder
     */
    @Property(label = "Import Mode", description = "The vlt import mode for created packages.")
    public static final String IMPORT_MODE = "importMode";

    /**
     * ACL handling property for file vault package builder
     */
    @Property(label = "Acl Handling", description = "The vlt acl handling mode for created packages.")
    public static final String ACL_HANDLING = "aclHandling";

    /**
     * Package roots
     */
    @Property(label = "Package Roots", description = "The package roots to be used for created packages. (this is useful for assembling packages with an user that cannot read above the package root)")
    public static final String PACKAGE_ROOTS = "package.roots";

    /**
     * Package filters
     */
    @Property(label = "Package Filters", description = "The package path filters. Filter format: path|+include|-exclude", cardinality = 100)
    public static final String PACKAGE_FILTERS = "package.filters";


    /**
     * Temp file folder
     */
    @Property(label = "Temp Filesystem Folder", description = "The filesystem folder where the temporary files should be saved.")
    public static final String TEMP_FS_FOLDER = "tempFsFolder";

    @Reference
    private Packaging packaging;

    private DistributionPackageBuilder packageBuilder;


    @Activate
    public void activate(Map<String, Object> config) {

        String name = PropertiesUtil.toString(config.get(NAME), null);
        String type = PropertiesUtil.toString(config.get(TYPE), null);
        String importModeString = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(IMPORT_MODE), null));
        String aclHandlingString = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(ACL_HANDLING), null));

        String[] packageRoots = SettingsUtils.removeEmptyEntries(PropertiesUtil.toStringArray(config.get(PACKAGE_ROOTS), null));
        String[] packageFilters = SettingsUtils.removeEmptyEntries(PropertiesUtil.toStringArray(config.get(PACKAGE_FILTERS), null));

        String tempFsFolder = SettingsUtils.removeEmptyEntry(PropertiesUtil.toString(config.get(TEMP_FS_FOLDER), null));

        ImportMode importMode = null;
        if (importModeString != null) {
            importMode = ImportMode.valueOf(importModeString.trim());
        }

        AccessControlHandling aclHandling = null;
        if (aclHandlingString != null) {
            aclHandling = AccessControlHandling.valueOf(aclHandlingString.trim());
        }

        if ("filevlt".equals(type)) {
            packageBuilder = new ResourceSharedDistributionPackageBuilder(new FileVaultDistributionPackageBuilder(name, packaging, importMode, aclHandling, packageRoots, packageFilters, tempFsFolder));
        } else {
            packageBuilder = new ResourceSharedDistributionPackageBuilder(new JcrVaultDistributionPackageBuilder(name, packaging, importMode, aclHandling, packageRoots, packageFilters, tempFsFolder));
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
}

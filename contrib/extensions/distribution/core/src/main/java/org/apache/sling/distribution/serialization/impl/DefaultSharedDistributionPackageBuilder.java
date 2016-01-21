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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DistributionPackageBuilder} for {@link SharedDistributionPackage}s
 */
public class DefaultSharedDistributionPackageBuilder implements DistributionPackageBuilder {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String PN_ORIGINAL_ID = "original.package.id";
    private static final String PN_ORIGINAL_REQUEST_TYPE = "original.package.request.type";
    private static final String PN_ORIGINAL_PATHS = "original.package.paths";

    private static final String PACKAGE_NAME_PREFIX = "distrpackage";
    private final String sharedPackagesRoot;
    private final String type;

    private final DistributionPackageBuilder distributionPackageBuilder;

    // use a global repolock for syncing access to the shared package root
    // TODO: this can be finegrained when we will allow configurable package roots
    private static final Object repolock = new Object();

    public DefaultSharedDistributionPackageBuilder(DistributionPackageBuilder distributionPackageExporter) {
        this.distributionPackageBuilder = distributionPackageExporter;
        this.type = distributionPackageBuilder.getType();
        this.sharedPackagesRoot = AbstractDistributionPackage.PACKAGES_ROOT + "/" + type + "/shared";
    }

    public String getType() {
        return type;
    }

    @Nonnull
    public DistributionPackage createPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest request) throws DistributionException {
        DistributionPackage distributionPackage = distributionPackageBuilder.createPackage(resourceResolver, request);

        String packageName = null;
        log.debug("create {}", distributionPackage.getId());

        try {
            packageName = generateNameFromId(resourceResolver, distributionPackage);

        } catch (PersistenceException e) {
            DistributionPackageUtils.deleteSafely(distributionPackage);
            throw new DistributionException(e);
        }

        String packagePath = getPathFromName(packageName);
        DistributionPackage sharedDistributionPackage = new DefaultSharedDistributionPackage(repolock, resourceResolver, packageName, packagePath, distributionPackage);

        log.debug("created shared package {} for {}", sharedDistributionPackage.getId(), distributionPackage.getId());
        return sharedDistributionPackage;
    }

    @Nonnull
    @CheckForNull
    public DistributionPackage readPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionException {
        DistributionPackage distributionPackage = distributionPackageBuilder.readPackage(resourceResolver, stream);

        log.debug("read shared package {}", distributionPackage);

        String packageName;
        try {
            packageName = generateNameFromId(resourceResolver, distributionPackage);

        } catch (PersistenceException e) {
            DistributionPackageUtils.deleteSafely(distributionPackage);
            throw new DistributionException(e);
        }

        String packagePath = getPathFromName(packageName);

        DistributionPackage sharedDistributionPackage = new DefaultSharedDistributionPackage(repolock, resourceResolver, packageName, packagePath, distributionPackage);

        log.debug("created shared package {} for {}", sharedDistributionPackage.getId(), distributionPackage.getId());
        return sharedDistributionPackage;
    }

    @CheckForNull
    public DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String distributionPackageId) throws DistributionException {
        String packageName = distributionPackageId;
        String originalPackageId = retrieveIdFromName(resourceResolver, packageName);

        log.debug("getting package {} {}", packageName, originalPackageId);

        if (originalPackageId == null) {
            return null;
        }

        DistributionPackage distributionPackage = distributionPackageBuilder.getPackage(resourceResolver, originalPackageId);

        log.debug("obtained package {}", distributionPackage);

        if (distributionPackage == null) {
            return null;
        }

        String packagePath = getPathFromName(packageName);

        return new DefaultSharedDistributionPackage(repolock, resourceResolver, packageName, packagePath, distributionPackage);
    }

    public boolean installPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionException {
        if (!(distributionPackage instanceof DefaultSharedDistributionPackage)) {
            return false;
        }

        DefaultSharedDistributionPackage sharedistributionPackage = (DefaultSharedDistributionPackage) distributionPackage;

        DistributionPackage originalPackage = sharedistributionPackage.getPackage();
        return distributionPackageBuilder.installPackage(resourceResolver, originalPackage);
    }

    private String generateNameFromId(ResourceResolver resourceResolver, DistributionPackage distributionPackage) throws PersistenceException {

        String name = PACKAGE_NAME_PREFIX + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PN_ORIGINAL_ID, distributionPackage.getId());

        // save the info just for debugging purposes
        if (distributionPackage.getInfo().getRequestType() != null) {
            properties.put(PN_ORIGINAL_REQUEST_TYPE, distributionPackage.getInfo().getRequestType().toString());

        }
        if (distributionPackage.getInfo().getPaths() != null) {
            properties.put(PN_ORIGINAL_PATHS, distributionPackage.getInfo().getPaths());
        }

        String packagePath = getPathFromName(name);

        synchronized (repolock) {
            Resource resource = ResourceUtil.getOrCreateResource(resourceResolver, packagePath,
                    "sling:Folder", "sling:Folder", false);

            ModifiableValueMap valueMap = resource.adaptTo(ModifiableValueMap.class);
            valueMap.putAll(properties);

            resourceResolver.create(resource, DefaultSharedDistributionPackage.REFERENCE_ROOT_NODE,
                    Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object) "sling:Folder"));

            resourceResolver.commit();
        }

        return name;
    }

    private String getPathFromName(String name) {
        return sharedPackagesRoot + "/" + name;
    }

    private String retrieveIdFromName(ResourceResolver resourceResolver, String name) {
        String packagePath = getPathFromName(name);

        Resource resource = resourceResolver.getResource(packagePath);

        if (resource == null) {
            return null;
        }

        ValueMap properties = resource.adaptTo(ValueMap.class);

        if (properties == null) {
            return null;
        }

        return properties.get(PN_ORIGINAL_ID, null);
    }
}

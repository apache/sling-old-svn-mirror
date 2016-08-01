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

package org.apache.sling.distribution.packaging.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.impl.vlt.VltUtils;
import org.apache.sling.distribution.util.impl.FileBackedMemoryOutputStream;
import org.apache.sling.distribution.util.impl.FileBackedMemoryOutputStream.MemoryUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ResourceDistributionPackageBuilder extends AbstractDistributionPackageBuilder {
    private static final String PREFIX_PATH = "/var/sling/distribution/packages/";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String packagesPath;
    private final File tempDirectory;
    private final DistributionContentSerializer distributionContentSerializer;
    private final int fileThreshold;
    private final MemoryUnit memoryUnit;
    private final boolean useOffHeapMemory;

    public ResourceDistributionPackageBuilder(String type,
                                              DistributionContentSerializer distributionContentSerializer,
                                              String tempFilesFolder,
                                              int fileThreshold,
                                              MemoryUnit memoryUnit,
                                              boolean useOffHeapMemory) {
        super(type);
        this.distributionContentSerializer = distributionContentSerializer;
        this.packagesPath = PREFIX_PATH + type + "/data";
        this.tempDirectory = VltUtils.getTempFolder(tempFilesFolder);
        this.fileThreshold = fileThreshold;
        this.memoryUnit = memoryUnit;
        this.useOffHeapMemory = useOffHeapMemory;
    }

    @Override
    protected DistributionPackage createPackageForAdd(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest request) throws DistributionException {
        DistributionPackage distributionPackage;

        FileBackedMemoryOutputStream outputStream = null;
        try {
            try {
                outputStream = new FileBackedMemoryOutputStream(fileThreshold, memoryUnit, useOffHeapMemory, tempDirectory, "distrpck-create-", "." + getType());
                distributionContentSerializer.exportToStream(resourceResolver, request, outputStream);
                outputStream.flush();
            } finally {
                IOUtils.closeQuietly(outputStream);
            }

            Resource packagesRoot = DistributionPackageUtils.getPackagesRoot(resourceResolver, packagesPath);

            InputStream inputStream = null;
            Resource packageResource = null;

            try {
                inputStream = outputStream.openWrittenDataInputStream();

                packageResource = uploadStream(packagesRoot, inputStream, outputStream.size());
            } finally {
                IOUtils.closeQuietly(inputStream);
            }

            distributionPackage = new ResourceDistributionPackage(packageResource, getType(), resourceResolver);
        } catch (IOException e) {
            throw new DistributionException(e);
        } finally {
            if (outputStream != null) {
                outputStream.clean();
            }
        }
        return distributionPackage;
    }

    @Override
    protected DistributionPackage readPackageInternal(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream inputStream)
            throws DistributionException {
        try {
            Resource packagesRoot = DistributionPackageUtils.getPackagesRoot(resourceResolver, packagesPath);

            Resource packageResource = uploadStream(packagesRoot, inputStream, -1);
            return new ResourceDistributionPackage(packageResource, getType(), resourceResolver);
        } catch (PersistenceException e) {
            throw new DistributionException(e);
        }
    }

    @Override
    protected boolean installPackageInternal(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream inputStream)
            throws DistributionException {
        try {
            distributionContentSerializer.importFromStream(resourceResolver, inputStream);
            return true;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    protected DistributionPackage getPackageInternal(@Nonnull ResourceResolver resourceResolver, @Nonnull String id) {
        Resource resource = resourceResolver.getResource(id);
        if (resource != null) {
            return new ResourceDistributionPackage(resource, getType(), resourceResolver);
        } else {
            return null;
        }
    }


    Resource uploadStream(Resource parent, InputStream stream, long size) throws PersistenceException {

        String name;
        log.debug("uploading stream");
        if (size == -1) {
            // stable id
            Map<String, Object> info = new HashMap<String, Object>();
            DistributionPackageUtils.readInfo(stream, info);
            log.info("read header {}", info);
            Object remoteId = info.get(DistributionPackageUtils.PROPERTY_REMOTE_PACKAGE_ID);
            if (remoteId != null) {
                name = remoteId.toString();
                if (name.contains("/")) {
                    name = name.substring(name.lastIndexOf('/') + 1);
                }
                log.info("preserving remote id {}", name);
            } else {
                name = "dstrpck-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString();
                log.info("generating a new id {}", name);
            }
        } else {
            name = "dstrpck-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString();
        }

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, "sling:Folder");
        props.put("type", getType());

        if (size != -1) {
            props.put("size", size);
        }

        ResourceResolver resourceResolver = parent.getResourceResolver();

        Resource r = resourceResolver.getResource(parent, name);
        if (r != null) {
            resourceResolver.delete(r);
        }

        Resource resource = resourceResolver.create(parent, name, props);
        try {
            DistributionPackageUtils.uploadStream(resource, stream);
        } catch (RepositoryException e) {
            throw new PersistenceException("cannot upload stream", e);

        }

        resourceResolver.commit();

        return resource;
    }
}

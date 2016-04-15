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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.impl.vlt.VltUtils;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class ResourceDistributionPackageBuilder extends AbstractDistributionPackageBuilder {
    private static final String PREFIX_PATH = "/var/sling/distribution/packages/";

    private final String packagesPath;
    private final File tempDirectory;
    private final DistributionContentSerializer distributionContentSerializer;

    public ResourceDistributionPackageBuilder(String type, DistributionContentSerializer distributionContentSerializer, String tempFilesFolder) {
        super(type);
        this.distributionContentSerializer = distributionContentSerializer;
        this.packagesPath = PREFIX_PATH + type + "/data";
        this.tempDirectory = VltUtils.getTempFolder(tempFilesFolder);

    }

    @Override
    protected DistributionPackage createPackageForAdd(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest request) throws DistributionException {
        DistributionPackage distributionPackage;
        // TODO : write to file if size > threshold

        File file = null;
        try {
            file = File.createTempFile("distrpck-create-" + System.nanoTime(),  "." + getType(), tempDirectory);

            OutputStream outputStream = null;

            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(file));
                distributionContentSerializer.exportToStream(resourceResolver, request, outputStream);
                outputStream.flush();
            } finally {
                IOUtils.closeQuietly(outputStream);
            }


            Resource packagesRoot = DistributionPackageUtils.getPackagesRoot(resourceResolver, packagesPath);

            InputStream inputStream = null;
            Resource packageResource = null;

            try {
                inputStream = new BufferedInputStream(new FileInputStream(file));

                packageResource = uploadStream(packagesRoot, inputStream, file.length());
            } finally {
                IOUtils.closeQuietly(inputStream);
            }

            distributionPackage = new ResourceDistributionPackage(packageResource, getType(), resourceResolver);
        } catch (IOException e) {
            throw new DistributionException(e);
        } finally {
            FileUtils.deleteQuietly(file);
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
    protected boolean installPackageInternal(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream  inputStream)
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
        return new ResourceDistributionPackage(resourceResolver.getResource(id), getType(), resourceResolver);
    }


    Resource uploadStream(Resource parent, InputStream stream, long size) throws PersistenceException {
        String name = "dstrpck-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, "sling:Folder");
        props.put("type", getType());

        if (size != -1) {
            props.put("size", size);
        }

        Resource resource = parent.getResourceResolver().create(parent, name, props);
        try {
            DistributionPackageUtils.uploadStream(resource, stream);
        } catch (RepositoryException e) {
            throw new PersistenceException("cannot upload stream", e);

        }

        parent.getResourceResolver().commit();

        return resource;
    }
}

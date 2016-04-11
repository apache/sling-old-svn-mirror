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

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.impl.vlt.VltUtils;

import javax.annotation.Nonnull;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class FileDistributionPackageBuilder extends AbstractDistributionPackageBuilder  {

    private final File tempDirectory;
    private final DistributionContentSerializer distributionContentSerializer;

    public FileDistributionPackageBuilder(String type, DistributionContentSerializer distributionContentSerializer, String tempFilesFolder) {
        super(type);
        this.distributionContentSerializer = distributionContentSerializer;
        this.tempDirectory = VltUtils.getTempFolder(tempFilesFolder);

    }

    @Override
    protected DistributionPackage createPackageForAdd(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest request) throws DistributionException {
        DistributionPackage distributionPackage;
        OutputStream outputStream = null;

        try {
            File file = File.createTempFile("distrpck-create-" + System.nanoTime(),  "." + getType(), tempDirectory);
            outputStream = new BufferedOutputStream(new FileOutputStream(file));

            distributionContentSerializer.exportToStream(resourceResolver, request, outputStream);
            outputStream.flush();

            distributionPackage = new FileDistributionPackage(file, getType());
        } catch (IOException e) {
            throw new DistributionException(e);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        return distributionPackage;
    }

    @Override
    protected DistributionPackage readPackageInternal(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream)
            throws DistributionException {
        DistributionPackage distributionPackage;

        OutputStream outputStream = null;
        try {
            File file = File.createTempFile("distrpck-read-" + System.nanoTime(), "." + getType(), tempDirectory);
            outputStream = new BufferedOutputStream(new FileOutputStream(file));

            IOUtils.copy(stream, outputStream);
            outputStream.flush();

            distributionPackage = new FileDistributionPackage(file, getType());
        } catch (Exception e) {
            throw new DistributionException(e);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        return distributionPackage;
    }

    @Override
    protected boolean installPackageInternal(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage)
            throws DistributionException {
        InputStream inputStream = null;
        try {
            inputStream = distributionPackage.createInputStream();
            distributionContentSerializer.importFromStream(resourceResolver, inputStream);
            return true;
        } catch (IOException e) {
            throw new DistributionException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    protected DistributionPackage getPackageInternal(@Nonnull ResourceResolver resourceResolver, @Nonnull String id) {
        return new FileDistributionPackage(new File(id), getType());
    }
}

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

import static org.apache.sling.distribution.util.impl.DigestUtils.openDigestOutputStream;
import static org.apache.sling.distribution.util.impl.DigestUtils.readDigestMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.impl.vlt.VltUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DistributionPackageBuilder} based on files.
 */
public class FileDistributionPackageBuilder extends AbstractDistributionPackageBuilder {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final File tempDirectory;
    private final String digestAlgorithm;
    private final DistributionContentSerializer distributionContentSerializer;

    public FileDistributionPackageBuilder(String type,
                                          DistributionContentSerializer distributionContentSerializer,
                                          String tempFilesFolder,
                                          String digestAlgorithm) {
        super(type);
        this.distributionContentSerializer = distributionContentSerializer;
        this.tempDirectory = VltUtils.getTempFolder(tempFilesFolder);
        this.digestAlgorithm = digestAlgorithm;
    }

    @Override
    protected DistributionPackage createPackageForAdd(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest request) throws DistributionException {
        DistributionPackage distributionPackage;
        OutputStream outputStream = null;
        String digestMessage = null;

        final File file;

        try {
            file = File.createTempFile("distrpck-create-" + System.nanoTime(), "." + getType(), tempDirectory);
            if (digestAlgorithm != null) {
                outputStream = openDigestOutputStream(new FileOutputStream(file), digestAlgorithm);
            } else {
                outputStream = new FileOutputStream(file);
            }

            distributionContentSerializer.exportToStream(resourceResolver, request, outputStream);
            outputStream.flush();

            if (digestAlgorithm != null) {
                digestMessage = readDigestMessage((DigestOutputStream) outputStream);
            }
            distributionPackage = new FileDistributionPackage(file, getType(), digestAlgorithm, digestMessage);
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
        final File file;
        DigestOutputStream outputStream = null;
        try {
            String name;
            // stable id
            Map<String, Object> info = new HashMap<String, Object>();
            DistributionPackageUtils.readInfo(stream, info);
            Object remoteId = info.get(DistributionPackageUtils.PROPERTY_REMOTE_PACKAGE_ID);
            if (remoteId != null) {
                name = remoteId.toString();
                log.debug("preserving remote id {}", name);
            } else {
                name = "distrpck-read-" + System.nanoTime();
                log.debug("generating a new id {}", name);
            }
            file = File.createTempFile(name, "." + getType(), tempDirectory);
            outputStream = openDigestOutputStream(new FileOutputStream(file), digestAlgorithm);

            IOUtils.copy(stream, outputStream);
            outputStream.flush();

            String digestMessage = readDigestMessage(outputStream);
            distributionPackage = new FileDistributionPackage(file, getType(), digestAlgorithm, digestMessage);
        } catch (Exception e) {
            throw new DistributionException(e);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        return distributionPackage;
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
        return new FileDistributionPackage(new File(id), getType(), null, null);
    }
}

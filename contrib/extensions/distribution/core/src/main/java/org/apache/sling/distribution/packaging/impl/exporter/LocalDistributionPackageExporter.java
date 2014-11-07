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
package org.apache.sling.distribution.packaging.impl.exporter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExportException;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.serialization.DistributionPackageBuildingException;

/**
 * {@link org.apache.sling.distribution.packaging.DistributionPackageExporter} implementation which creates a FileVault based
 * {@link org.apache.sling.distribution.packaging.DistributionPackage} locally.
 */
public class LocalDistributionPackageExporter implements DistributionPackageExporter {

    private DistributionPackageBuilder packageBuilder;

    public LocalDistributionPackageExporter(DistributionPackageBuilder packageBuilder) {
        this.packageBuilder = packageBuilder;
    }

    @Nonnull
    public List<DistributionPackage> exportPackages(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest) throws DistributionPackageExportException {
        List<DistributionPackage> result = new ArrayList<DistributionPackage>();

        DistributionPackage createdPackage;
        try {
            createdPackage = packageBuilder.createPackage(resourceResolver, distributionRequest);
        } catch (DistributionPackageBuildingException e) {
            throw new DistributionPackageExportException(e);
        }
        if (createdPackage != null) {
            result.add(createdPackage);
        }
        return result;
    }

    public DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String distributionPackageId) {
        return packageBuilder.getPackage(resourceResolver, distributionPackageId);
    }
}

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
package org.apache.sling.distribution.packaging;


import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.List;

import aQute.bnd.annotation.ConsumerType;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.serialization.DistributionPackage;

/**
 * A {@link DistributionPackageExporter ) is responsible of exporting
 * {@link DistributionPackage }s to be then imported by a {@link org.apache.sling.distribution.agent.DistributionAgent }
 * (via a {@link DistributionPackageImporter }).
 * <p/>
 * Exporting a {@link DistributionPackage } means obtaining that package by either
 * directly creating it by bundling local Sling resources together or retrieving it from a remote endpoint, e.g. by
 * executing an HTTP POST request on another Sling instance exposing already created packages (for remotely changed resources).
 */
@ConsumerType
public interface DistributionPackageExporter {

    /**
     * Exports the {@link DistributionPackage}s built from the
     * passed {@link org.apache.sling.distribution.DistributionRequest}.
     *
     * @param resourceResolver    - the resource resolver used to export the packages, for example a 'local' exporter
     *                            will use the resource resolver to read the content and assemble the binary in a certain
     *                            location in the repository while a 'remote' exporter will use the resolver just to
     *                            store the binary of the remotely fetched packages in the repository.
     * @param distributionRequest - the request containing the needed information for content to be exported
     * @return a {@link java.util.List} of {@link DistributionPackage}s
     */
    @Nonnull
    List<DistributionPackage> exportPackages(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest) throws DistributionException;

    /**
     * Retrieves a {@link DistributionPackage} given its identifier, if it already exists.
     * This will be used for example to get already created (and cached) packages that were not yet distributed to the
     * target instance.
     *
     * @param resourceResolver      - the resource resolver use to obtain the package.
     * @param distributionPackageId - the {@link DistributionPackage#getId() id of the package} to be retrieved
     * @return a {@link DistributionPackage} if available, {@code null} otherwise
     */
    @CheckForNull
    DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String distributionPackageId) throws DistributionException;
}

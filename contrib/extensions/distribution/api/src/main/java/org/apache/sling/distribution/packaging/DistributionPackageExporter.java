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
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.component.DistributionComponent;

/**
 * A {@link DistributionPackageExporter ) is responsible of exporting
 * {@link DistributionPackage }s to be then imported by a {@link org.apache.sling.distribution.agent.DistributionAgent }
 * (via a {@link DistributionPackageImporter }).
 */
@ConsumerType
public interface DistributionPackageExporter extends DistributionComponent {

    /**
     * Exports the {@link DistributionPackage}s built from the
     * passed {@link org.apache.sling.distribution.communication.DistributionRequest}.
     *
     * @param resourceResolver   - the resource resolver used to export the packages
     * @param distributionRequest - the request containing the information about which content is to be exported
     * @return a <code>List</code> of {@link DistributionPackage}s
     */
    @Nonnull
    List<DistributionPackage> exportPackages(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest) throws DistributionPackageExportException;

    /**
     * Retrieves a {@link DistributionPackage} given its 'id', if it already exists.
     *
     * @param resourceResolver     - the resource resolver use to obtain the package.
     * @param distributionPackageId - the id of the package to be retrieved
     * @return a {@link DistributionPackage} if available, <code>null</code> otherwise
     */
    @CheckForNull
    DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String distributionPackageId);
}

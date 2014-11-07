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
package org.apache.sling.distribution.serialization;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.InputStream;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;

/**
 * A builder for {@link org.apache.sling.distribution.packaging.DistributionPackage}s
 */
public interface DistributionPackageBuilder {

    /**
     * creates a {@link org.apache.sling.distribution.packaging.DistributionPackage} for a specific {@link org.apache.sling.distribution.communication.DistributionRequest}
     *
     * @param resourceResolver the resource resolver used to access the resources to be packaged
     * @param request          the {@link org.apache.sling.distribution.communication.DistributionRequest} to create the package for
     * @return a {@link org.apache.sling.distribution.packaging.DistributionPackage} or <code>null</code> if it could not be created
     * @throws DistributionPackageBuildingException if any error occurs while creating the package, or if the resource resolver is not authorized to do that
     */
    @CheckForNull
    DistributionPackage createPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest request) throws DistributionPackageBuildingException;

    /**
     * reads a stream and tries to convert it to a {@link org.apache.sling.distribution.packaging.DistributionPackage} this provider can read and install
     *
     * @param resourceResolver resource resolver used to store the eventually created package
     * @param stream           the {@link InputStream} of the package to read
     * @return a {@link org.apache.sling.distribution.packaging.DistributionPackage} if it can read it from the stream
     * @throws DistributionPackageReadingException when the stream cannot be read as a {@link org.apache.sling.distribution.packaging.DistributionPackage}
     */
    @CheckForNull
    DistributionPackage readPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionPackageReadingException;

    /**
     * get an already created (and saved into the repository) {@link org.apache.sling.distribution.packaging.DistributionPackage} by its id
     *
     * @param resourceResolver resource resolver used to access the package with the given id
     * @param id               the unique identifier of an already created {@link org.apache.sling.distribution.packaging.DistributionPackage}
     * @return a {@link org.apache.sling.distribution.packaging.DistributionPackage} if one with such an id exists, <code>null</code> otherwise
     */
    @CheckForNull
    DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String id);

    /**
     * Installs the given distributionPackage into the repository
     *
     * @param resourceResolver   the resource resolver used to install the packaged resources
     * @param distributionPackage the distribution package to install
     * @return <code>true</code> if the package was installed successfully
     * @throws DistributionPackageReadingException
     */
    boolean installPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionPackageReadingException;

}

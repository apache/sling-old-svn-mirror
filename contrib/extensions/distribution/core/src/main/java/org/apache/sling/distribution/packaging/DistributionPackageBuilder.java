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
import java.io.InputStream;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;

/**
 * A builder for {@link DistributionPackage}s
 */
@ProviderType
public interface DistributionPackageBuilder {

    /**
     * returns the type of a package. Only packages of this type will be accepted by the package builder.
     * @return the package type.
     */
    String getType();

    /**
     * creates a {@link DistributionPackage} for a specific {@link org.apache.sling.distribution.DistributionRequest}
     *
     * @param resourceResolver the resource resolver used to access the resources to be packaged
     * @param request          the {@link org.apache.sling.distribution.DistributionRequest} to create the package for
     * @return a {@link DistributionPackage} or <code>null</code> if it could not be created
     * @throws org.apache.sling.distribution.common.DistributionException if any error occurs while creating the package, or if the resource resolver is not authorized to do that
     */
    @Nonnull
    DistributionPackage createPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest request) throws DistributionException;

    /**
     * reads a stream and tries to convert it to a {@link DistributionPackage} this provider can read and install
     *
     * @param resourceResolver resource resolver used to store the eventually created package
     * @param stream           the {@link InputStream} of the package to read
     * @return a {@link DistributionPackage} if it can read it from the stream
     * @throws DistributionException when the stream cannot be read as a {@link DistributionPackage}
     */
    @Nonnull
    DistributionPackage readPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionException;

    /**
     * get an already created (and saved into the repository) {@link DistributionPackage} by its id
     *
     * @param resourceResolver resource resolver used to access the package with the given id
     * @param id               the unique identifier of an already created {@link DistributionPackage}
     * @return a {@link DistributionPackage} if one with such an id exists, <code>null</code> otherwise
     * @throws DistributionException when the stream the package with that id cannot be retrieved
     */
    @CheckForNull
    DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String id) throws DistributionException;

    /**
     * Installs the given distributionPackage into the repository
     *
     * @param resourceResolver   the resource resolver used to install the packaged resources
     * @param distributionPackage the distribution package to install
     * @return <code>true</code> if the package was installed successfully
     * @throws DistributionException
     */
    boolean installPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionException;


    /**
     * install a stream and returns the associated to a {@link DistributionPackageInfo} this provider can read and install
     *
     * @param resourceResolver resource resolver used to store the eventually created package
     * @param stream           the {@link InputStream} of the package to read
     * @return a {@link DistributionPackage} if it can read it from the stream
     * @throws DistributionException when the stream cannot be read as a {@link DistributionPackage}
     */
    @Nonnull
    DistributionPackageInfo installPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionException;

}

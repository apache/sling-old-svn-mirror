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
package org.apache.sling.replication.serialization;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.InputStream;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;

/**
 * A builder for {@link org.apache.sling.replication.packaging.ReplicationPackage}s
 */
public interface ReplicationPackageBuilder {

    /**
     * creates a {@link org.apache.sling.replication.packaging.ReplicationPackage} for a specific {@link ReplicationRequest}
     *
     * @param resourceResolver the resource resolver used to access the resources to be packaged
     * @param request          the {@link org.apache.sling.replication.communication.ReplicationRequest} to create the package for
     * @return a {@link org.apache.sling.replication.packaging.ReplicationPackage} or <code>null</code> if it could not be created
     * @throws ReplicationPackageBuildingException if any error occurs while creating the package, or if the resource resolver is not authorized to do that
     */
    @CheckForNull
    ReplicationPackage createPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull ReplicationRequest request) throws ReplicationPackageBuildingException;

    /**
     * reads a stream and tries to convert it to a {@link ReplicationPackage} this provider can read and install
     *
     * @param resourceResolver resource resolver used to store the eventually created package
     * @param stream           the {@link InputStream} of the package to read
     * @return a {@link ReplicationPackage} if it can read it from the stream
     * @throws ReplicationPackageReadingException when the stream cannot be read as a {@link ReplicationPackage}
     */
    @CheckForNull
    ReplicationPackage readPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws ReplicationPackageReadingException;

    /**
     * get an already created (and saved into the repository) {@link ReplicationPackage} by its id
     *
     * @param resourceResolver resource resolver used to access the package with the given id
     * @param id               the unique identifier of an already created {@link ReplicationPackage}
     * @return a {@link ReplicationPackage} if one with such an id exists, <code>null</code> otherwise
     */
    @CheckForNull
    ReplicationPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String id);

    /**
     * Installs the given replicationPackage into the repository
     *
     * @param resourceResolver   the resource resolver used to install the packaged resources
     * @param replicationPackage the replication package to install
     * @return <code>true</code> if the package was installed successfully
     * @throws ReplicationPackageReadingException
     */
    boolean installPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull ReplicationPackage replicationPackage) throws ReplicationPackageReadingException;

}

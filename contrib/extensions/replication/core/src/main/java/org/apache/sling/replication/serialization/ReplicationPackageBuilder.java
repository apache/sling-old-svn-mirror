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

import java.io.InputStream;

import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;

/**
 * A builder for {@link org.apache.sling.replication.packaging.ReplicationPackage}s
 */
public interface ReplicationPackageBuilder {

    /**
     * creates a {@link org.apache.sling.replication.packaging.ReplicationPackage} for a specific {@link ReplicationRequest}
     *
     * @param request the {@link ReplicationRequest} to create the package for
     * @return a {@link org.apache.sling.replication.packaging.ReplicationPackage}
     * @throws ReplicationPackageBuildingException
     */
    ReplicationPackage createPackage(ReplicationRequest request) throws ReplicationPackageBuildingException;

    /**
     * reads a stream and tries to convert it to a {@link ReplicationPackage} this provider can read and install
     *
     * @param stream the {@link InputStream} of the package to read
     * @return a {@link ReplicationPackage} if it can read it from the stream
     * @throws ReplicationPackageReadingException when the stream cannot be read as a {@link ReplicationPackage}
     */
    ReplicationPackage readPackage(InputStream stream) throws ReplicationPackageReadingException;

    /**
     * get an already created (and saved into the repository) {@link ReplicationPackage} by its id
     *
     * @param id a <code>String</code> representing the unique identifier of an already created {@link ReplicationPackage}
     * @return a {@link ReplicationPackage} if one with such an id exists, <code>null</code> otherwise
     */
    ReplicationPackage getPackage(String id);

    /**
     * Installs the given replicationPackage into the repository
     *
     * @param replicationPackage the replication package to install
     * @return <code>true</code> if the package was installed successfully
     * @throws ReplicationPackageReadingException
     */
    boolean installPackage(ReplicationPackage replicationPackage) throws ReplicationPackageReadingException;

}

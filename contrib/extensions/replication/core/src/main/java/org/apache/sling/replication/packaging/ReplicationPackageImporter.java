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
package org.apache.sling.replication.packaging;

import java.io.InputStream;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.serialization.ReplicationPackageReadingException;

/**
 * A {@link org.apache.sling.replication.packaging.ReplicationPackageImporter} is responsible for importing
 * {@link org.apache.sling.replication.packaging.ReplicationPackage}s into the resource tree.
 */
public interface ReplicationPackageImporter {
    /**
     * Imports the given replication package
     *
     * @param replicationPackage - the package to be imported
     * @return <code>true</code> if the import succeeded, <code>false</code> otherwise
     */
    boolean importPackage(ResourceResolver resourceResolver, ReplicationPackage replicationPackage) throws ReplicationPackageReadingException;

    /**
     * reads a stream and tries to convert it to a {@link ReplicationPackage} this provider can read and install
     *
     * @param stream the {@link InputStream} of the package to read
     * @return a {@link ReplicationPackage} if it can read it from the stream
     * @throws ReplicationPackageReadingException when the stream cannot be read as a {@link ReplicationPackage}
     */
    ReplicationPackage readPackage(ResourceResolver resourceResolver, InputStream stream) throws ReplicationPackageReadingException;

}

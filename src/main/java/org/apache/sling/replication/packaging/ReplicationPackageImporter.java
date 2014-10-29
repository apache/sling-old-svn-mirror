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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.InputStream;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.component.ReplicationComponent;

/**
 * A {@link org.apache.sling.replication.packaging.ReplicationPackageImporter} is responsible for importing
 * {@link org.apache.sling.replication.packaging.ReplicationPackage}s into the resource tree.
 */
public interface ReplicationPackageImporter extends ReplicationComponent {

    /**
     * Imports the given replication package into the underlying system
     *
     * @param resourceResolver   - the resource resolver used to import the resources
     * @param replicationPackage - the package to be imported
     * @return <code>true</code> if the import succeeded, <code>false</code> otherwise
     * @throws ReplicationPackageImportException if any error occurs during import
     */
    boolean importPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull ReplicationPackage replicationPackage) throws ReplicationPackageImportException;

    /**
     * Tries to convert an {@link java.io.InputStream} to a {@link ReplicationPackage} and then imports it into the underlying system
     *
     * @param resourceResolver - the resource resolver used to read the package
     * @param stream           the {@link InputStream} of the package to be converted and installed
     * @return a {@link ReplicationPackage} if the stream can converted into it and installed
     * @throws ReplicationPackageImportException when the stream cannot be read as a {@link ReplicationPackage} and installed
     */
    @CheckForNull
    ReplicationPackage importStream(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws ReplicationPackageImportException;

}

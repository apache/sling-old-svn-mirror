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

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

import aQute.bnd.annotation.ProviderType;

/**
 * A distribution package
 */
@ProviderType
public interface DistributionPackage {

    /**
     * get package id. the id is a unique string that can be used to identify the package
     *
     * @return the package id
     */
    @Nonnull
    String getId();

    /**
     * get the type of package
     *
     * @return the package type
     */
    @Nonnull
    String getType();

    /**
     * creates a package stream.
     * a new stream is created for each call and it is the caller's obligation to close the stream.
     *
     * @return an {@link InputStream}
     * @throws IOException
     */
    @Nonnull
    InputStream createInputStream() throws IOException;


    /**
     *
     * @return the size in bytes
     */
    long getSize();

    /**
     * closes all resources associated with this instance
     */
    void close();

    /**
     * releases all resources associated with this package
     */
    void delete();

    /**
     * gets an additional info holder for this package.
     * The additional info object contains control information rather than content information.
     * For example info.origin can be used to skip distributing back to the originating endpoint.
     *
     * @return the associated metadata to this package
     */
    @Nonnull
    DistributionPackageInfo getInfo();

}

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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * A replication package
 */
public interface ReplicationPackage extends Serializable {

    /**
     * get package id. the id is a unique string that can be used to retrieve
     * the package from an {@link ReplicationPackageExporter}
     *
     * @return the package id
     */
    String getId();

    /**
     * get the paths covered by this package
     *
     * @return an array of paths
     */
    String[] getPaths();

    /**
     * get the action this package is used for
     *
     * @return the action
     */
    String getAction();

    /**
     * get the type of package
     *
     * @return the package type
     */
    String getType();

    /**
     * creates a package stream.
     * a new stream is created for each call and it is the caller's obligation to close the stream.
     *
     * @return an {@link InputStream}
     * @throws IOException
     */
    InputStream createInputStream() throws IOException;

    /**
     * get package stream length
     *
     * @return the package length
     */
    long getLength();

    /**
     * releases all resources associated with this package
     */
    void delete();


    /**
     * gets an additional info holder for this package.
     * The additional info object contains control information rather than content information.
     * For example info.origin can be used to skip replicating back to the originating endpoint.
     * It should not be be serialized between instances as its main purpose is to allow
     * inter component communication on the same instance.
     * @return
     */
    ReplicationPackageInfo getInfo();

}

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

import java.io.InputStream;
import java.io.OutputStream;

import aQute.bnd.annotation.ConsumerType;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.common.DistributionException;

/**
 * A content serializer used to convert distribution payloads to and from binary streams.
 */
@ConsumerType
public interface DistributionContentSerializer {

    /**
     * extracts the resources identified by the given request into the given outputStream
     * @param resourceResolver the resource resolver used to access resources to export
     * @param exportOptions export options
     * @param outputStream the output stream
     * @throws DistributionException if extraction fails for some reason
     */
    void exportToStream(ResourceResolver resourceResolver, DistributionExportOptions exportOptions,
                        OutputStream outputStream) throws DistributionException;

    /**
     * imports the given stream
     * @param resourceResolver the resource resolver used to write resources
     * @param inputStream the stream to import
     * @throws DistributionException if importing fails for some reason
     */
    void importFromStream(ResourceResolver resourceResolver, InputStream inputStream) throws DistributionException;

    /**
     * retrieve the name of this content serializer
     * @return the name of this content serializer
     */
    String getName();

    /**
     * whether or not this {@link DistributionContentSerializer} can build package filters for including / excluding
     * certain resources / attributes directly from a {@link org.apache.sling.distribution.DistributionRequest}
     * @return {@code true} if it can build filters from a request, {@code false} otherwise
     */
    boolean isRequestFiltering();
}

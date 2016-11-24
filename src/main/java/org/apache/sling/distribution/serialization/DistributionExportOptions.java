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
import javax.annotation.Nullable;
import java.io.OutputStream;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;

/**
 * Export options used to identify and process the resources to be extracted by {@link
 * DistributionContentSerializer#exportToStream(ResourceResolver, DistributionExportOptions, OutputStream)}.
 */
public final class DistributionExportOptions {

    private final DistributionRequest request;
    private final DistributionExportFilter filter;

    public DistributionExportOptions(@Nullable DistributionRequest request, @Nullable DistributionExportFilter filter) {
        this.request = request;
        this.filter = filter;
    }

    /**
     * get the distribution request
     * @return the distribution request
     */
    @CheckForNull
    public DistributionRequest getRequest() {
        return request;
    }

    /**
     * get the export filter
     * @return the export filter
     */
    @CheckForNull
    public DistributionExportFilter getFilter() {
        return filter;
    }
}

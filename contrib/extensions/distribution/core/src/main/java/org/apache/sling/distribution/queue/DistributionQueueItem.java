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
package org.apache.sling.distribution.queue;

import javax.annotation.Nonnull;
import java.util.Map;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.distribution.serialization.DistributionPackage;

/**
 * An item in a {@link DistributionQueue}
 * This is basically a proxy to {@link DistributionPackage} designed to avoid having
 * to keep the package {@link DistributionPackage#createInputStream() stream} into
 * the queues.
 */
public class DistributionQueueItem extends ValueMapDecorator implements ValueMap {

    private final String packageId;

    public DistributionQueueItem(@Nonnull String packageId, Map<String, Object> base) {
        super(base);
        this.packageId = packageId;
    }

    @Nonnull
    public String getPackageId() {
        return packageId;
    }


    @Override
    public String toString() {
        return "DistributionQueueItem{" +
                "id='" + packageId + '\'' +
                ", info=" + super.toString() +
                '}';
    }
}

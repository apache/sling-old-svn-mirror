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
package org.apache.sling.distribution.packaging.impl;

import javax.annotation.Nonnull;

import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;

/**
 * abstract implementation of a {@link DistributionPackage}
 */
public abstract class AbstractDistributionPackage implements SharedDistributionPackage {

    public static final String PACKAGES_ROOT = "/var/sling/distribution/packages";

    private final DistributionPackageInfo info;
    private final String id;

    protected AbstractDistributionPackage(String id, String type) {
        this.id = id;
        this.info = new DistributionPackageInfo(type);
    }

    @Nonnull
    public DistributionPackageInfo getInfo() {
        return info;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getType() {
        return info.getType();
    }

}

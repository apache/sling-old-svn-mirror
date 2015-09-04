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

package org.apache.sling.distribution.serialization.impl;


import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

/**
 * Package wrapper meant to be extended and provide just simple overridden methods.
 */
public  class DistributionPackageWrapper implements DistributionPackage {

    protected final DistributionPackage distributionPackage;

    protected DistributionPackageWrapper(DistributionPackage distributionPackage) {

        this.distributionPackage = distributionPackage;
    }

    @Nonnull
    public String getId() {
        return distributionPackage.getId();
    }

    @Nonnull
    public String getType() {
        return distributionPackage.getId();
    }

    @Nonnull
    public InputStream createInputStream() throws IOException {
        return distributionPackage.createInputStream();
    }

    public void close() {
        distributionPackage.close();
    }

    public void delete() {
        distributionPackage.delete();
    }

    @Nonnull
    public DistributionPackageInfo getInfo() {
        return distributionPackage.getInfo();
    }
}

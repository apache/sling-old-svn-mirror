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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.sling.distribution.packaging.DistributionPackage;

/**
 * A reference package wraps an actual {@link DistributionPackage} providing a reference to it
 * by its {@link DistributionPackage#getId()}.
 */
public class ReferencePackage extends AbstractDistributionPackage {
    static final String REFERENCE_PREFIX = "reference-";
    private final DistributionPackage distributionPackage;
    private final String reference;

    public ReferencePackage(DistributionPackage distributionPackage) {
        super(REFERENCE_PREFIX + distributionPackage.getId(), distributionPackage.getType());
        this.distributionPackage = distributionPackage;
        this.reference = REFERENCE_PREFIX + distributionPackage.getId();
        getInfo().putAll(distributionPackage.getInfo());
        getInfo().put("isReference", true);
    }

    @Override
    public void acquire(@Nonnull String... holderNames) {
        if (distributionPackage instanceof AbstractDistributionPackage) {
            ((AbstractDistributionPackage) distributionPackage).acquire(holderNames);
        }
    }

    @Override
    public void release(@Nonnull String... holderNames) {
        if (distributionPackage instanceof AbstractDistributionPackage) {
            ((AbstractDistributionPackage) distributionPackage).release(holderNames);
        }
    }

    @Nonnull
    @Override
    public InputStream createInputStream() throws IOException {
        return new ByteArrayInputStream(reference.getBytes());
    }

    @Override
    public long getSize() {
        return getId().length();
    }

    @Override
    public void close() {
        distributionPackage.close();
    }

    @Override
    public void delete() {
        distributionPackage.delete();
    }

    @Nonnull
    @Override
    public String getType() {
        return getInfo().getType();
    }

    @Nonnull
    @Override
    public String getId() {
        return reference;
    }

    public static boolean isReference(String string) {
        return string.startsWith(REFERENCE_PREFIX);
    }

    @CheckForNull
    public static String idFromReference(String reference) {
        return isReference(reference) ? reference.substring(REFERENCE_PREFIX.length()) : null;
    }
}

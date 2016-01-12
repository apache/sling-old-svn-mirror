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
package org.apache.sling.distribution.serialization.impl.vlt;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageInfo;
import org.apache.sling.distribution.serialization.impl.AbstractDistributionPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a FileVault {@link DistributionPackage}
 */
public class FileVaultDistributionPackage extends AbstractDistributionPackage implements DistributionPackage {

    Logger log = LoggerFactory.getLogger(FileVaultDistributionPackage.class);

    private final VaultPackage pkg;

    public FileVaultDistributionPackage(String type, VaultPackage pkg) {
        super(pkg.getFile().getAbsolutePath(), type);
        this.pkg = pkg;
        String[] paths = VltUtils.getPaths(pkg.getMetaInf());

        this.getInfo().put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, paths);
        this.getInfo().put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, DistributionRequestType.ADD);
    }

    @Nonnull
    public InputStream createInputStream() throws IOException {
        return new FileInputStream(pkg.getFile());
    }

    @Override
    public long getSize() {
        return pkg.getFile().length();
    }

    public void close() {
        pkg.close();
    }

    public void delete() {
        try {
            VltUtils.deletePackage(pkg);
        } catch (Throwable e) {
            log.error("cannot delete file", e);
        }
    }

    @Override
    public String toString() {
        return "FileVaultDistributionPackage{" +
                "id='" + getId() + '\'' +
                ", pkg=" + pkg +
                '}';
    }
}

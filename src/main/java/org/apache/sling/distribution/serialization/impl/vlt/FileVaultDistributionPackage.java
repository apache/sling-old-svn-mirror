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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.serialization.impl.AbstractDistributionPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a FileVault {@link org.apache.sling.distribution.packaging.DistributionPackage}
 */
public class FileVaultDistributionPackage extends AbstractDistributionPackage implements DistributionPackage {

    Logger log = LoggerFactory.getLogger(FileVaultDistributionPackage.class);

    private static final long serialVersionUID = 1L;

    private final String id;

    private final String type;
    private final VaultPackage pkg;

    public FileVaultDistributionPackage(String type, VaultPackage pkg) {
        this.type = type;
        this.pkg = pkg;
        String[] paths = VltUtils.getPaths(pkg.getMetaInf());
        this.getInfo().setPaths(paths);
        this.getInfo().setRequestType(DistributionRequestType.ADD);
        this.id = pkg.getFile().getAbsolutePath();
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public InputStream createInputStream() throws IOException {
        return new FileInputStream(pkg.getFile());
    }

    @Nonnull
    public String getType() {
        return type;
    }

    public void close() {
        pkg.close();
    }

    public void delete() {
        try {
            File file = pkg.getFile();

            close();

            if (file.exists()) {
                file.delete();
            }
        } catch (Throwable e) {
            log.error("cannot delete file", e);
        }
    }

    @Override
    public String toString() {
        return "FileVaultDistributionPackage{" +
                "id='" + id + '\'' +
                ", pkg=" + pkg +
                '}';
    }
}

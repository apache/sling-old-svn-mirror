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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DistributionPackage} based on a {@link File}.
 */
public class FileDistributionPackage extends AbstractDistributionPackage {
    private final Logger log = LoggerFactory.getLogger(getClass());


    private final File file;

    public FileDistributionPackage(@Nonnull File file, @Nonnull String type) {
        super(file.getAbsolutePath(), type);
        this.file = file;

        this.getInfo().put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, DistributionRequestType.ADD);
    }


    @Nonnull
    public InputStream createInputStream() throws IOException {
        return new PackageInputStream(file);
    }

    @Override
    public long getSize() {
        return file.length();
    }

    public void close() {
        // do nothing
    }

    public void delete() {
        FileUtils.deleteQuietly(file);
        FileUtils.deleteQuietly(getStatusFile());
    }

    public File getFile() {
        return file;
    }

    @Override
    public void acquire(@Nonnull String[] holderNames) {
        try {
            DistributionPackageUtils.acquire(getStatusFile(), holderNames);
        } catch (IOException e) {
            log.error("cannot release package", e);
        }
    }

    @Override
    public void release(@Nonnull String[] holderNames) {
        try {
            boolean doDelete = DistributionPackageUtils.release(getStatusFile(), holderNames);

            if (doDelete) {
                delete();
            }
        } catch (IOException e) {
            log.error("cannot release package", e);
        }
    }


    File getStatusFile() {
        String statusFilePath = file.getAbsolutePath() + ".status";
        return new File(statusFilePath);
    }


    public class PackageInputStream extends BufferedInputStream {
        private final File file;

        public PackageInputStream(File file) throws IOException {
            super(FileUtils.openInputStream(file));

            this.file = file;
        }


        public File getFile() {
            return file;
        }
    }

}

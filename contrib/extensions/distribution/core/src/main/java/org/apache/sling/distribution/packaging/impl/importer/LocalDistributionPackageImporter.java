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
package org.apache.sling.distribution.packaging.impl.importer;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.event.DistributionEventType;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.packaging.DistributionPackageImportException;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.serialization.DistributionPackageReadingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.distribution.packaging.DistributionPackageImporter} implementation which imports a FileVault
 * based {@link org.apache.sling.distribution.packaging.DistributionPackage} locally.
 */

public class LocalDistributionPackageImporter implements DistributionPackageImporter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DistributionPackageBuilder packageBuilder;

    private final DistributionEventFactory distributionEventFactory;


    public LocalDistributionPackageImporter(DistributionPackageBuilder packageBuilder,
                                            DistributionEventFactory distributionEventFactory) {

        if (packageBuilder == null) {
            throw new IllegalArgumentException("A package builder is required");
        }

        if (distributionEventFactory == null) {
            throw new IllegalArgumentException("An event factory is required");
        }
        this.distributionEventFactory = distributionEventFactory;
        this.packageBuilder = packageBuilder;
    }


    public boolean importPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionPackageImportException {
        boolean success;
        try {
            success = packageBuilder.installPackage(resourceResolver, distributionPackage);

            if (success) {
                log.info("Distribution package read and installed for path(s) {}", Arrays.toString(distributionPackage.getPaths()));

                Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
                dictionary.put("distribution.action", distributionPackage.getAction());
                dictionary.put("distribution.path", distributionPackage.getPaths());
                distributionEventFactory.generateEvent(DistributionEventType.PACKAGE_INSTALLED, dictionary);

            } else {
                log.warn("could not read a distribution package");
            }
        } catch (Exception e) {
            log.error("cannot import a package from the given stream of type {}", distributionPackage.getType());
            throw new DistributionPackageImportException(e);
        }
        return success;
    }

    public DistributionPackage importStream(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionPackageImportException {
        try {
            DistributionPackage distributionPackage = packageBuilder.readPackage(resourceResolver, stream);
            if (importPackage(resourceResolver, distributionPackage)) {
                return distributionPackage;
            } else {
                throw new DistributionPackageImportException("could not import the package " + distributionPackage);
            }
        } catch (DistributionPackageReadingException e) {
            throw new DistributionPackageImportException("cannot read a package from the given stream", e);
        }
    }

}

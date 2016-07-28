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
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.event.DistributionEventTopics;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.packaging.impl.ReferencePackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.distribution.packaging.DistributionPackageImporter} implementation which imports a
 * {@link DistributionPackage} locally.
 */
public class LocalDistributionPackageImporter implements DistributionPackageImporter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DistributionPackageBuilder packageBuilder;

    private final DistributionEventFactory eventFactory;

    private final String name;

    public LocalDistributionPackageImporter(String name, DistributionEventFactory eventFactory, DistributionPackageBuilder packageBuilder) {

        if (packageBuilder == null) {
            throw new IllegalArgumentException("A package builder is required");
        }

        if (eventFactory == null) {
            throw new IllegalArgumentException("EventFactory is required");
        }

        if (name == null) {
            throw new IllegalArgumentException("An importer name is required");
        }

        this.packageBuilder = packageBuilder;
        this.eventFactory = eventFactory;
        this.name = name;
    }

    @Override
    public void importPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionException {
        boolean success = packageBuilder.installPackage(resourceResolver, distributionPackage);

        if (!success) {
            log.warn("could not install distribution package {}", distributionPackage.getId());
        }

        eventFactory.generatePackageEvent(DistributionEventTopics.IMPORTER_PACKAGE_IMPORTED, DistributionComponentKind.IMPORTER, name, distributionPackage.getInfo());
    }

    @Override
    @Nonnull
    public DistributionPackageInfo importStream(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionException {
        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
        Map<String, Object> headerInfo = new HashMap<String, Object>();
        DistributionPackageUtils.readInfo(stream, headerInfo);
        log.debug("header info: {}", headerInfo);

        Object o = headerInfo.get(DistributionPackageUtils.PROPERTY_REMOTE_PACKAGE_ID);
        String reference = o != null ? String.valueOf(o) : null;

        if (reference != null) {
            if (ReferencePackage.isReference(reference)) {
                String actualPackageId = ReferencePackage.idFromReference(reference);
                log.info("installing from reference {}", actualPackageId);
                DistributionPackage distributionPackage = packageBuilder.getPackage(resourceResolver, actualPackageId);
                if (distributionPackage != null) {
                    DistributionPackageInfo packageInfo = packageBuilder.installPackage(resourceResolver, stream);
                    log.info("package installed {}", packageInfo);
                    eventFactory.generatePackageEvent(DistributionEventTopics.IMPORTER_PACKAGE_IMPORTED, DistributionComponentKind.IMPORTER, name, packageInfo);
                    return distributionPackage.getInfo();
                } else {
                    throw new DistributionException("could not install package from reference " + actualPackageId);
                }
            } else {
                try {
                    stream.reset();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                DistributionPackageInfo packageInfo;
                Object rr = headerInfo.get("reference-required");
                boolean store = rr != null && Boolean.valueOf(rr.toString());
                if (store) {
                    log.debug("storing actual package");
                    DistributionPackage distributionPackage = packageBuilder.readPackage(resourceResolver, stream);
                    packageInfo = distributionPackage.getInfo();
                    log.info("package stored {}", packageInfo);
                } else {
                    packageInfo = packageBuilder.installPackage(resourceResolver, stream);
                    log.info("package installed {}", packageInfo);
                }
                eventFactory.generatePackageEvent(DistributionEventTopics.IMPORTER_PACKAGE_IMPORTED, DistributionComponentKind.IMPORTER, name, packageInfo);
                return packageInfo;
            }
        } else {
            DistributionPackageInfo packageInfo = packageBuilder.installPackage(resourceResolver, stream);
            log.info("package installed");
            eventFactory.generatePackageEvent(DistributionEventTopics.IMPORTER_PACKAGE_IMPORTED, DistributionComponentKind.IMPORTER, name, packageInfo);
            return packageInfo;
        }

    }

}

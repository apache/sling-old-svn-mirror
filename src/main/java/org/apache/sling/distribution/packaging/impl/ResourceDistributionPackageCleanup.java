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


import java.util.Iterator;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.common.DistributionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This runnable removes unreferenced {@link ResourceDistributionPackage} packages.
 * It is meant to be run periodically. See SLING-6503.
 */
public class ResourceDistributionPackageCleanup implements Runnable {

    /**
     * The default logger
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ResourceDistributionPackageBuilder packageBuilder;

    private final ResourceResolverFactory resolverFactory;

    public ResourceDistributionPackageCleanup(@Nonnull ResourceResolverFactory resolverFactory,
                                              @Nonnull ResourceDistributionPackageBuilder packageBuilder) {
        this.resolverFactory = resolverFactory;
        this.packageBuilder = packageBuilder;
    }

    public void run () {
        log.debug("Cleaning up {} packages", packageBuilder.getType());
        ResourceResolver serviceResolver = null;
        try {
            int deleted = 0, total = 0;
            serviceResolver = resolverFactory.getServiceResourceResolver(null);
            for (Iterator<ResourceDistributionPackage> pkgs = packageBuilder.getPackages(serviceResolver) ; pkgs.hasNext() ; total++) {
                ResourceDistributionPackage pkg = pkgs.next();
                if (pkg.disposable()) {
                    log.debug("Delete package {}", pkg.getId());
                    deleted++;
                    pkg.delete(false);
                } else {
                    log.debug("package {} is not disposable", pkg.getId());
                }
            }
            if (serviceResolver.hasChanges()) {
                serviceResolver.commit();
            }
            log.debug("Cleaned up {}/{} {} packages",
                    new Object[]{deleted, total, packageBuilder.getType()});
        } catch (LoginException e) {
            log.error("Failed to get distribution service resolver: {}", e.getMessage());
        } catch (DistributionException e) {
            log.error("Failed to get the list of packages", e);
        } catch (PersistenceException e) {
            log.error("Failed to delete disposable packages", e);
        } finally {
            if (serviceResolver != null && serviceResolver.isLive()) {
                serviceResolver.close();
            }
        }
    }
}

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

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSharedDistributionPackage implements SharedDistributionPackage {
    private final Logger log = LoggerFactory.getLogger(getClass());

    static final String REFERENCE_ROOT_NODE = "refs";

    private final ResourceResolver resourceResolver;
    private final String packagePath;
    private final DistributionPackage distributionPackage;
    private final String packageName;

    public DefaultSharedDistributionPackage(ResourceResolver resourceResolver, String packageName, String packagePath, DistributionPackage distributionPackage) {
        this.resourceResolver = resourceResolver;
        this.packageName = packageName;
        this.packagePath = packagePath;
        this.distributionPackage = distributionPackage;
    }

    public void acquire(@Nonnull String[] holderNames) {
        if (holderNames.length == 0) {
            throw new IllegalArgumentException("holder name cannot be null or empty");
        }

        try {
            createHolderResource(holderNames);

            log.debug("acquired package {} for holder {}", new Object[]{packagePath, Arrays.toString(holderNames)});

        } catch (PersistenceException e) {
            log.error("cannot acquire package", e);
        }
    }

    public void release(@Nonnull String[] holderNames) {

        if (holderNames.length == 0) {
            throw new IllegalArgumentException("holder name cannot be null or empty");
        }

        try {
            boolean doPackageDelete = deleteHolderResource(holderNames);


            if (doPackageDelete) {
                distributionPackage.delete();
            }

            if (resourceResolver.hasChanges()) {
                resourceResolver.commit();
            }

            log.debug("released package {} from holder {} delete {}", new Object[]{packagePath, Arrays.toString(holderNames), doPackageDelete});
        } catch (PersistenceException e) {
            log.error("cannot release package", e);
        }
    }


    @Nonnull
    public String getId() {
        return packageName;
    }

    @Nonnull
    public String getType() {
        return distributionPackage.getType();
    }

    @Nonnull
    public InputStream createInputStream() throws IOException {
        return distributionPackage.createInputStream();
    }

    @Override
    public long getSize() {
        return distributionPackage.getSize();
    }

    public void close() {
        distributionPackage.close();
    }

    public void delete() {

        try {
            deleteProxy();
        } catch (PersistenceException e) {
            log.error("cannot delete shared resource", e);
        }

        distributionPackage.delete();
    }

    @Nonnull
    public DistributionPackageInfo getInfo() {
        return distributionPackage.getInfo();
    }

    public DistributionPackage getPackage() {
        return distributionPackage;
    }


    private Resource getProxyResource() {
        String holderPath = packagePath;

        return resourceResolver.getResource(holderPath);
    }


    private Resource getHolderRootResource() {
        Resource resource = getProxyResource();

        Resource holderRoot = resource.getChild(REFERENCE_ROOT_NODE);
        if (holderRoot != null) {
            return holderRoot;
        }

        return null;
    }

    private void createHolderResource(String[] holderNames) throws PersistenceException {

        Resource holderRoot = getHolderRootResource();

        if (holderRoot == null) {
            return;
        }

        for (String holderName : holderNames) {
            Resource holder = holderRoot.getChild(holderName);

            if (holder != null) {
                return;
            }

            resourceResolver.create(holderRoot, holderName, Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object) "sling:Folder"));
        }

        resourceResolver.commit();
    }

    private boolean deleteHolderResource(String[] holderNames) throws PersistenceException {
        boolean doPackageDelete = false;
        Resource holderRoot = getHolderRootResource();

        if (holderRoot != null) {
            for (String holderName : holderNames) {
                Resource holder = holderRoot.getChild(holderName);

                if (holder != null) {
                    resourceResolver.delete(holder);
                }
            }
        }

        if (!holderRoot.hasChildren()) {
            Resource resource = getProxyResource();
            resourceResolver.delete(resource);
            doPackageDelete = true;
        }

        return doPackageDelete;
    }

    private void deleteProxy() throws PersistenceException {
        Resource resource = getProxyResource();
        resourceResolver.delete(resource);
        resourceResolver.commit();
    }


}

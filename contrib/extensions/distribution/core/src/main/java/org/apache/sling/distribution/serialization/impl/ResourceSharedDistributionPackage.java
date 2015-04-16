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
import java.util.Collections;

import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.SharedDistributionPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceSharedDistributionPackage implements SharedDistributionPackage {
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String  REFERENCE_ROOT_NODE = "refs";


    private final ResourceResolver resourceResolver;
    private final String packagePath;
    private final DistributionPackage distributionPackage;
    private final String packageName;

    public ResourceSharedDistributionPackage(ResourceResolver resourceResolver, String packageName, String packagePath, DistributionPackage distributionPackage) {
        this.resourceResolver = resourceResolver;
        this.packageName = packageName;
        this.packagePath = packagePath;
        this.distributionPackage = distributionPackage;
    }

    public void acquire(@Nonnull String holderName) {
        if (holderName.length() == 0) {
            throw new IllegalArgumentException("holder name cannot be null or empty");
        }
        
        try {
            createHolderResource(holderName);
        } catch (PersistenceException e) {
            log.error("cannot acquire package", e);
        }
    }

    public void release(@Nonnull String holderName) {

        if (holderName.length() == 0) {
            throw new IllegalArgumentException("holder name cannot be null or empty");
        }

        try {
            deleteHolderResource(holderName);

            Resource holderRoot = getHolderRootResource();
            if (!holderRoot.hasChildren()) {
                delete();
            }
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

    public void close() {
        distributionPackage.close();
    }

    public void delete() {
        Resource resource = getProxyResource();
        try {
            resourceResolver.delete(resource);
            resourceResolver.commit();
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

        resourceResolver.refresh();
        Resource resource = resourceResolver.getResource(holderPath);
        return resource;
    }




    private Resource getHolderRootResource()  {
        Resource resource = getProxyResource();

        Resource holderRoot = resource.getChild(REFERENCE_ROOT_NODE);
        if (holderRoot != null) {
            return holderRoot;
        }

        return null;
    }

    private void createHolderResource(String holderName) throws PersistenceException {
        Resource holderRoot = getHolderRootResource();

        if (holderRoot == null) {
            return;
        }

        Resource holder = holderRoot.getChild(holderName);

        if (holder != null) {
            return;
        }

        resourceResolver.create(holderRoot, holderName, Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object) "sling:Folder"));
        resourceResolver.commit();

    }

    private void deleteHolderResource(String holderName) throws PersistenceException {
        Resource holderRoot = getHolderRootResource();

        if (holderRoot == null) {
            return;
        }

        Resource holder = holderRoot.getChild(holderName);

        if (holder == null) {
            return;
        }

        resourceResolver.delete(holder);
        resourceResolver.commit();
    }

}

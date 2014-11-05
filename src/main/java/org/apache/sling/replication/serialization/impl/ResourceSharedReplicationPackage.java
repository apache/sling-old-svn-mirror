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
package org.apache.sling.replication.serialization.impl;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.*;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageInfo;
import org.apache.sling.replication.packaging.SharedReplicationPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

public class ResourceSharedReplicationPackage implements SharedReplicationPackage {
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String  REFERENCE_ROOT_NODE = "refs";
    private static final String  PN_REFERENCE_COUNT = "ref.count";


    private final ResourceResolver resourceResolver;
    private final String packagePath;
    private final ReplicationPackage replicationPackage;

    public ResourceSharedReplicationPackage(ResourceResolver resourceResolver, String packagePath, ReplicationPackage replicationPackage) {
        this.resourceResolver = resourceResolver;
        this.packagePath = packagePath;
        this.replicationPackage = replicationPackage;
    }

    public void acquire(@Nonnull String holderName) {
        if (holderName == null || holderName.length() == 0) {
            throw new IllegalArgumentException("holder name cannot be null or empty");
        }
        
        try {
            createHolderResource(holderName);
        } catch (PersistenceException e) {
            log.error("cannot acquire package", e);
        }
    }

    public void release(@Nonnull String holderName) {

        if (holderName == null || holderName.length() == 0) {
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
        return packagePath;
    }

    @Nonnull
    public String[] getPaths() {
        return replicationPackage.getPaths();
    }

    @Nonnull
    public String getAction() {
        return replicationPackage.getAction();
    }

    @Nonnull
    public String getType() {
        return replicationPackage.getType();
    }

    @Nonnull
    public InputStream createInputStream() throws IOException {
        return replicationPackage.createInputStream();
    }

    public long getLength() {
        return replicationPackage.getLength();
    }

    public void delete() {
        Resource resource = getProxyResource();
        try {
            resourceResolver.delete(resource);
            resourceResolver.commit();
        } catch (PersistenceException e) {
            log.error("cannot delete shared resource", e);
        }
        replicationPackage.delete();
    }

    @Nonnull
    public ReplicationPackageInfo getInfo() {
        return replicationPackage.getInfo();
    }

    public ReplicationPackage getPackage() {
        return replicationPackage;
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

        resourceResolver.create(holderRoot, holderName, Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object) "nt:unstructured"));
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

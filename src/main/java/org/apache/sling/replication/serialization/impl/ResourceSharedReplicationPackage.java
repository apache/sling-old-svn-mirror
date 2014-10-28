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

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageInfo;
import org.apache.sling.replication.packaging.SharedReplicationPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class ResourceSharedReplicationPackage implements SharedReplicationPackage {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final Object syncObject = new Object();


    private static final String  PN_REFERENCE_COUNT = "ref.count";

    private final ResourceResolver resourceResolver;
    private final String packagePath;
    private final ReplicationPackage replicationPackage;

    public ResourceSharedReplicationPackage(ResourceResolver resourceResolver, String packagePath, ReplicationPackage replicationPackage) {
        this.resourceResolver = resourceResolver;
        this.packagePath = packagePath;
        this.replicationPackage = replicationPackage;
    }

    public void acquire() {
        synchronized (syncObject) {
            Resource resource = getProxyResource();
            ModifiableValueMap valueMap = resource.adaptTo(ModifiableValueMap.class);
            int refCount = valueMap.get(PN_REFERENCE_COUNT, 0);
            refCount ++;
            valueMap.put(PN_REFERENCE_COUNT, refCount);

            try {
                resourceResolver.commit();
            } catch (PersistenceException e) {
                log.error("cannot release package", e);
            }
        }
    }

    public void release() {
        synchronized (syncObject) {
            Resource resource = getProxyResource();
            ModifiableValueMap valueMap = resource.adaptTo(ModifiableValueMap.class);
            int refCount = valueMap.get(PN_REFERENCE_COUNT, 0);
            refCount --;

            if (refCount > 0) {
                valueMap.put(PN_REFERENCE_COUNT, refCount);
            }
            else {
                delete();
            }

            try {
                resourceResolver.commit();
            } catch (PersistenceException e) {
                log.error("cannot release package", e);
            }
        }
    }



    public String getId() {
        return packagePath;
    }

    public String[] getPaths() {
        return replicationPackage.getPaths();
    }

    public String getAction() {
        return replicationPackage.getAction();
    }

    public String getType() {
        return replicationPackage.getType();
    }

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

    public ReplicationPackageInfo getInfo() {
        return replicationPackage.getInfo();
    }

    public ReplicationPackage getPackage() {
        return replicationPackage;
    }


    private Resource getProxyResource() {
        Resource resource = resourceResolver.getResource(packagePath);
        return resource;
    }

}

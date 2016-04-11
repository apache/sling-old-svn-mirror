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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.serialization.impl.vlt.VltUtils;
import org.apache.sling.distribution.util.DistributionJcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * base abstract implementation of a JCR based {@link org.apache.sling.distribution.serialization.DistributionPackageBuilder}
 */
public abstract class AbstractDistributionPackageBuilder implements DistributionPackageBuilder {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String type;

    protected AbstractDistributionPackageBuilder(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Nonnull
    public DistributionPackage createPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest request)
            throws DistributionException {
        DistributionPackage distributionPackage;

        request = VltUtils.sanitizeRequest(request);


        if (DistributionRequestType.ADD.equals(request.getRequestType())) {
            distributionPackage = createPackageForAdd(resourceResolver, request);
        } else if (DistributionRequestType.DELETE.equals(request.getRequestType())) {
            distributionPackage = new SimpleDistributionPackage(request, type);
        } else if (DistributionRequestType.PULL.equals(request.getRequestType())) {
            distributionPackage = new SimpleDistributionPackage(request, type);
        } else if (DistributionRequestType.TEST.equals(request.getRequestType())) {
            distributionPackage = new SimpleDistributionPackage(request, type);
        } else {
            throw new DistributionException("unknown action type " + request.getRequestType());
        }

        DistributionPackageUtils.fillInfo(distributionPackage.getInfo(), request);

        return distributionPackage;
    }

    @Nonnull
    public DistributionPackage readPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionException {

        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
        Map<String, Object> headerInfo = new HashMap<String, Object>();
        DistributionPackageUtils.readInfo(stream, headerInfo);

        DistributionPackage distributionPackage = SimpleDistributionPackage.fromStream(stream, type);

        stream.mark(-1);

        // not a simple package
        if (distributionPackage == null) {
            distributionPackage = readPackageInternal(resourceResolver, stream);
        }

        distributionPackage.getInfo().putAll(headerInfo);
        return distributionPackage;
    }

    public boolean installPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionException {

        DistributionRequestType actionType = distributionPackage.getInfo().getRequestType();

        if (!type.equals(distributionPackage.getType())) {
            throw new DistributionException("not supported package type" + distributionPackage.getType());
        }

        boolean installed = false;
        if (DistributionRequestType.DELETE.equals(actionType)) {
            installed = installDeletePackage(resourceResolver, distributionPackage);
        } else if (DistributionRequestType.TEST.equals(actionType)) {
            // do nothing for test packages
            installed = true;
        } else if (DistributionRequestType.ADD.equals(actionType)) {
            installed = installPackageInternal(resourceResolver, distributionPackage);
        }

        return installed;
    }

    private boolean installDeletePackage(@Nonnull ResourceResolver resourceResolver, @CheckForNull DistributionPackage distributionPackage) throws DistributionException {
        Session session = null;
        try {
            if (distributionPackage != null) {
                session = getSession(resourceResolver);
                for (String path : distributionPackage.getInfo().getPaths()) {
                    if (session.itemExists(path)) {
                        session.removeItem(path);
                    }
                }
                return true;
            }
        } catch (Exception e) {
            throw new DistributionException(e);
        } finally {
            ungetSession(session);
        }

        return false;
    }

    @CheckForNull
    public DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String id) {
        DistributionPackage distributionPackage = SimpleDistributionPackage.fromIdString(id, type);

        // not a simple package
        if (distributionPackage == null) {
            distributionPackage = getPackageInternal(resourceResolver, id);
        }
        return distributionPackage;
    }

    protected Session getSession(ResourceResolver resourceResolver) throws RepositoryException {
        Session session = resourceResolver.adaptTo(Session.class);
        if (session != null) {
            DistributionJcrUtils.setDoNotDistribute(session);
        } else {
            throw new RepositoryException("could not obtain a session from calling user " + resourceResolver.getUserID());
        }
        return session;
    }

    protected void ungetSession(Session session) {
        if (session != null) {
            try {
                if (session.hasPendingChanges()) {
                    session.save();
                }
            } catch (RepositoryException e) {
                log.debug("Cannot save session", e);
            }
        }
    }

    @CheckForNull
    protected abstract DistributionPackage createPackageForAdd(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest request)
            throws DistributionException;

    @CheckForNull
    protected abstract DistributionPackage readPackageInternal(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream)
            throws DistributionException;


    protected abstract boolean installPackageInternal(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage)
            throws DistributionException;

    @CheckForNull
    protected abstract DistributionPackage getPackageInternal(@Nonnull ResourceResolver resourceResolver, @Nonnull String id);

}

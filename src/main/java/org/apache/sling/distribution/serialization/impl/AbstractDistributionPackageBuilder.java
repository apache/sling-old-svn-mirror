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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.communication.DistributionActionType;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.event.DistributionEventType;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.serialization.DistributionPackageBuildingException;
import org.apache.sling.distribution.serialization.DistributionPackageReadingException;
import org.apache.sling.distribution.util.DistributionJcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * base abstract implementation of a JCR based {@link org.apache.sling.distribution.serialization.DistributionPackageBuilder}
 */
public abstract class AbstractDistributionPackageBuilder implements DistributionPackageBuilder {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String type;

    private final DistributionEventFactory distributionEventFactory;

    protected AbstractDistributionPackageBuilder(String type, DistributionEventFactory distributionEventFactory) {
        this.type = type;
        this.distributionEventFactory = distributionEventFactory;
    }

    @CheckForNull
    public DistributionPackage createPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest request)
            throws DistributionPackageBuildingException {
        DistributionPackage distributionPackage;
        if (DistributionActionType.ADD.equals(request.getActionType())) {
            distributionPackage = createPackageForAdd(resourceResolver, request);
        } else if (DistributionActionType.DELETE.equals(request.getActionType())) {
            distributionPackage = new VoidDistributionPackage(request, type);
        } else if (DistributionActionType.POLL.equals(request.getActionType())) {
            distributionPackage = new VoidDistributionPackage(request, type);
        } else {
            throw new DistributionPackageBuildingException("unknown action type "
                    + request.getActionType());
        }
        if (distributionPackage != null && distributionEventFactory != null) {
            Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
            dictionary.put("distribution.action", distributionPackage.getAction());
            dictionary.put("distribution.path", distributionPackage.getPaths());
            distributionEventFactory.generateEvent(DistributionEventType.PACKAGE_CREATED, dictionary);
        }
        return distributionPackage;
    }

    @CheckForNull
    public DistributionPackage readPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionPackageReadingException {
        DistributionPackage distributionPackage = null;
        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
        try {
            stream.mark(6);
            byte[] buffer = new byte[6];
            int bytesRead = stream.read(buffer, 0, 6);
            stream.reset();
            String s = new String(buffer, "UTF-8");
            log.info("read {} bytes as {}", bytesRead, s);

            if (bytesRead > 0 && buffer[0] > 0 && s.startsWith("DEL")) {
                distributionPackage = VoidDistributionPackage.fromStream(stream);
            }
        } catch (Exception e) {
            log.warn("cannot parse stream", e);
        }
        stream.mark(-1);
        if (distributionPackage == null) {
            distributionPackage = readPackageInternal(resourceResolver, stream);
        }
        return distributionPackage;
    }

    public boolean installPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionPackageReadingException {
        DistributionActionType actionType = DistributionActionType.fromName(distributionPackage.getAction());
        boolean installed;
        if (DistributionActionType.DELETE.equals(actionType)) {
            installed = installDeletePackage(resourceResolver, distributionPackage);
        } else {
            installed = installPackageInternal(resourceResolver, distributionPackage);
        }

        if (installed && distributionEventFactory != null) {
            Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
            dictionary.put("distribution.action", distributionPackage.getAction());
            dictionary.put("distribution.path", distributionPackage.getPaths());
            distributionEventFactory.generateEvent(DistributionEventType.PACKAGE_INSTALLED, dictionary);
        }

        return installed;
    }

    private boolean installDeletePackage(ResourceResolver resourceResolver, DistributionPackage distributionPackage) throws DistributionPackageReadingException {
        Session session = null;
        try {
            if (distributionPackage != null) {
                session = getSession(resourceResolver);
                for (String path : distributionPackage.getPaths()) {
                    if (session.itemExists(path)) {
                        session.removeItem(path);
                    }
                }
                session.save();
                return true;
            }
        } catch (Exception e) {
            throw new DistributionPackageReadingException(e);
        } finally {
            ungetSession(session);
        }

        return false;
    }

    public DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String id) {
        DistributionPackage distributionPackage = null;
        try {
            distributionPackage = VoidDistributionPackage.fromStream(new ByteArrayInputStream(id.getBytes("UTF-8")));
        } catch (IOException ex) {
            // not a void package
        }

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
               session.save();
           } catch (RepositoryException e) {
               log.debug("Cannot save session", e);
           }
       }
    }

    protected abstract DistributionPackage createPackageForAdd(ResourceResolver resourceResolver, DistributionRequest request)
            throws DistributionPackageBuildingException;


    protected abstract DistributionPackage readPackageInternal(ResourceResolver resourceResolver, InputStream stream)
            throws DistributionPackageReadingException;


    protected abstract boolean installPackageInternal(ResourceResolver resourceResolver, DistributionPackage distributionPackage)
            throws DistributionPackageReadingException;

    protected abstract DistributionPackage getPackageInternal(ResourceResolver resourceResolver, String id);

}

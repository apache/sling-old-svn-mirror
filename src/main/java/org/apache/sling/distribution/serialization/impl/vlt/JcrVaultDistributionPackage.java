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
package org.apache.sling.distribution.serialization.impl.vlt;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageInfo;
import org.apache.sling.distribution.serialization.impl.AbstractDistributionPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a JcrPackage based {@link DistributionPackage}
 */
public class JcrVaultDistributionPackage extends AbstractDistributionPackage implements DistributionPackage {

    private static final Logger log = LoggerFactory.getLogger(JcrVaultDistributionPackage.class);

    private final JcrPackage jcrPackage;
    private final Session session;

    public JcrVaultDistributionPackage(String type, JcrPackage jcrPackage, Session session) {
        super(getIdFromPackage(jcrPackage), type);
        this.jcrPackage = jcrPackage;
        this.session = session;
        String[] paths = new String[0];
        try {
            paths = VltUtils.getPaths(jcrPackage.getDefinition().getMetaInf());
        } catch (RepositoryException e) {
            log.error("cannot read paths", e);
        }

        this.getInfo().put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, paths);
        this.getInfo().put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, DistributionRequestType.ADD);

    }

    private static String getIdFromPackage(JcrPackage jcrPackage) {
        try {
            return jcrPackage.getPackage().getId().getName();
        } catch (Exception e) {
            log.warn("could not get package {}", jcrPackage, e);
        }

        return null;
    }

    @Nonnull
    public InputStream createInputStream() throws IOException {
        try {
            return jcrPackage.getData().getBinary().getStream();
        } catch (RepositoryException e) {
            log.error("Cannot create input stream", e);
            throw new IOException();
        }
    }

    @Override
    public long getSize() {
        return jcrPackage.getSize();
    }

    public void close() {
        jcrPackage.close();
    }

    public void delete() {
        try {
            VltUtils.deletePackage(jcrPackage);
            session.save();
        } catch (Throwable e) {
            log.error("Cannot delete package", e);
        }
    }

    JcrPackage getJcrPackage() {
        return jcrPackage;
    }
}

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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.ExportOptions;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.packaging.impl.FileDistributionPackage;
import org.apache.sling.distribution.util.DistributionJcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DistributionContentSerializer} based on Apache Jackrabbit FileVault
 */
public class FileVaultContentSerializer implements DistributionContentSerializer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    static final String TYPE = "filevault";
    private static final String VERSION = "0.0.1";
    private static final String PACKAGE_GROUP = "sling/distribution";

    private final Packaging packaging;
    private final ImportMode importMode;
    private final AccessControlHandling aclHandling;
    private final String[] packageRoots;
    private final int autosaveThreshold;
    private final TreeMap<String, List<String>> nodeFilters;
    private final TreeMap<String, List<String>> propertyFilters;
    private final boolean useBinaryReferences;
    private final String name;

    public FileVaultContentSerializer(String name, Packaging packaging, ImportMode importMode, AccessControlHandling aclHandling, String[] packageRoots,
                                      String[] nodeFilters, String[] propertyFilters, boolean useBinaryReferences, int autosaveThreshold) {
        this.name = name;
        this.packaging = packaging;
        this.importMode = importMode;
        this.aclHandling = aclHandling;
        this.packageRoots = packageRoots;
        this.autosaveThreshold = autosaveThreshold;
        this.nodeFilters = VltUtils.parseFilters(nodeFilters);
        this.propertyFilters = VltUtils.parseFilters(propertyFilters);
        this.useBinaryReferences = useBinaryReferences;
    }

    @Override
    public void exportToStream(ResourceResolver resourceResolver, DistributionRequest request, OutputStream outputStream) throws DistributionException {
        Session session = null;
        try {
            session = getSession(resourceResolver);
            String packageGroup = PACKAGE_GROUP;
            String packageName = TYPE + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID();

            WorkspaceFilter filter = VltUtils.createFilter(request, nodeFilters, propertyFilters);
            ExportOptions opts = VltUtils.getExportOptions(filter, packageRoots, packageGroup, packageName, VERSION, useBinaryReferences);

            log.debug("assembling package {} user {}", packageGroup + '/' + packageName + "-" + VERSION, resourceResolver.getUserID());

            packaging.getPackageManager().assemble(session, opts, outputStream);
        } catch (Exception e) {
            throw new DistributionException(e);
        } finally {
            ungetSession(session);
        }

    }

    @Override
    public void importFromStream(ResourceResolver resourceResolver, InputStream inputStream) throws DistributionException {

        Session session = null;
        OutputStream outputStream = null;
        File file = null;
        boolean isTmp = true;
        try {
            session = getSession(resourceResolver);
            ImportOptions importOptions = VltUtils.getImportOptions(aclHandling, importMode, autosaveThreshold);

            if (inputStream instanceof FileDistributionPackage.PackageInputStream) {
                file = ((FileDistributionPackage.PackageInputStream) inputStream).getFile();
                isTmp = false;
            } else {
                file = File.createTempFile("distrpck-tmp-" + System.nanoTime(), "." + TYPE);
            }

            outputStream = new BufferedOutputStream(new FileOutputStream(file));

            IOUtils.copy(inputStream, outputStream);
            IOUtils.closeQuietly(outputStream);

            PackageManager packageManager = packaging.getPackageManager();
            VaultPackage vaultPackage = packageManager.open(file);

            vaultPackage.extract(session, importOptions);

            vaultPackage.close();
        } catch (Exception e) {
            throw new DistributionException(e);
        } finally {
            IOUtils.closeQuietly(outputStream);
            if (isTmp) {
                FileUtils.deleteQuietly(file);
            }

            ungetSession(session);
        }

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
                log.error("Cannot save session", e);
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }
}

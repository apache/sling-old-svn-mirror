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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.UUID;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.fs.io.Importer;
import org.apache.jackrabbit.vault.fs.io.ZipStreamArchive;
import org.apache.jackrabbit.vault.packaging.ExportOptions;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.apache.sling.distribution.serialization.DistributionExportOptions;
import org.apache.sling.distribution.util.DistributionJcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DistributionContentSerializer} based on Apache Jackrabbit FileVault
 */
public class FileVaultContentSerializer implements DistributionContentSerializer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String TYPE = "filevault";
    private static final String VERSION = "0.0.1";
    private static final String PACKAGE_GROUP = "sling/distribution";

    /**
     * The custom <code>Path-Mapping</code> property.
     */
    private static final String PATH_MAPPING_PROPERTY = "Path-Mapping";

    private static final String MAPPING_SEPARATOR = "=";

    private static final String MAPPING_DELIMITER = ";";

    private final Packaging packaging;
    private final ImportMode importMode;
    private final AccessControlHandling aclHandling;
    private final String[] packageRoots;
    private final int autosaveThreshold;
    private final TreeMap<String, List<String>> nodeFilters;
    private final TreeMap<String, List<String>> propertyFilters;
    private final boolean useBinaryReferences;
    private final String name;
    private final Map<String, String> exportPathMapping;

    public FileVaultContentSerializer(String name, Packaging packaging, ImportMode importMode, AccessControlHandling aclHandling, String[] packageRoots,
                                      String[] nodeFilters, String[] propertyFilters, boolean useBinaryReferences, int autosaveThreshold,
                                      Map<String, String> exportPathMapping) {
        this.name = name;
        this.packaging = packaging;
        this.importMode = importMode;
        this.aclHandling = aclHandling;
        this.packageRoots = packageRoots;
        this.autosaveThreshold = autosaveThreshold;
        this.nodeFilters = VltUtils.parseFilters(nodeFilters);
        this.propertyFilters = VltUtils.parseFilters(propertyFilters);
        this.useBinaryReferences = useBinaryReferences;
        this.exportPathMapping = exportPathMapping;
    }

    @Override
    public void exportToStream(ResourceResolver resourceResolver, DistributionExportOptions exportOptions, OutputStream outputStream) throws DistributionException {
        Session session = null;
        try {
            session = getSession(resourceResolver);
            String packageGroup = PACKAGE_GROUP;
            String packageName = TYPE + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID();

            WorkspaceFilter filter = VltUtils.createFilter(exportOptions.getRequest(), nodeFilters, propertyFilters);
            ExportOptions opts = VltUtils.getExportOptions(filter, packageRoots, packageGroup, packageName, VERSION, useBinaryReferences, exportPathMapping);

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
        Archive archive = null;
        try {
            session = getSession(resourceResolver);
            ImportOptions importOptions = VltUtils.getImportOptions(aclHandling, importMode, autosaveThreshold);
            Importer importer = new Importer(importOptions);
            archive = new ZipStreamArchive(inputStream);
            archive.open(false);

            // retrieve the mapping
            MetaInf metaInf = archive.getMetaInf();
            if (metaInf != null) {
                Properties metaInfProperties = metaInf.getProperties();
                if (metaInfProperties != null) {
                    String pathsMappingProperty = metaInfProperties.getProperty(PATH_MAPPING_PROPERTY);

                    if (pathsMappingProperty != null && !pathsMappingProperty.isEmpty()) {
                        RegexpPathMapping pathMapping = new RegexpPathMapping();

                        StringTokenizer pathsMappingTokenizer = new StringTokenizer(pathsMappingProperty, MAPPING_DELIMITER);
                        while (pathsMappingTokenizer.hasMoreTokens()) {
                            String[] pathMappingHeader = pathsMappingTokenizer.nextToken().split(MAPPING_SEPARATOR);
                            pathMapping.addMapping(pathMappingHeader[0], pathMappingHeader[1]);
                        }

                        importOptions.setPathMapping(pathMapping);
                    }
                }
            }

            // now import the content
            importer.run(archive, session.getRootNode());
            if (importer.hasErrors() && importOptions.isStrict()) {
                throw new PackageException("Errors during import.");
            }
            if (session.hasPendingChanges()) {
                session.save();
            }
        } catch (Exception e) {
            throw new DistributionException(e);
        } finally {
            ungetSession(session);
            if (archive != null) {
                archive.close();
            }
        }

    }

    private Session getSession(ResourceResolver resourceResolver) throws RepositoryException {
        Session session = resourceResolver.adaptTo(Session.class);
        if (session != null) {
            DistributionJcrUtils.setDoNotDistribute(session);
        } else {
            throw new RepositoryException("could not obtain a session from calling user " + resourceResolver.getUserID());
        }
        return session;
    }

    private void ungetSession(Session session) {
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

    @Override
    public boolean isRequestFiltering() {
        return true;
    }
}

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
package org.apache.sling.replication.serialization.impl.vlt;

import javax.jcr.Session;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.config.DefaultMetaInf;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.ExportOptions;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.apache.sling.replication.serialization.ReplicationPackageReadingException;
import org.apache.sling.replication.serialization.impl.AbstractReplicationPackageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a {@link org.apache.sling.replication.serialization.ReplicationPackageBuilder} based on Apache Jackrabbit FileVault.
 * <p/>
 * Each {@link org.apache.sling.replication.packaging.ReplicationPackage} created by <code>FileVaultReplicationPackageBuilder</code> is
 * backed by a {@link org.apache.jackrabbit.vault.packaging.VaultPackage}. 
 */
public class FileVaultReplicationPackageBuilder extends AbstractReplicationPackageBuilder implements
        ReplicationPackageBuilder {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Packaging packaging;

    public static final String NAME = "vlt";

    public FileVaultReplicationPackageBuilder(Packaging packaging, ReplicationEventFactory replicationEventFactory) {
        super(NAME, replicationEventFactory);

        this.packaging = packaging;
    }

    @Override
    protected ReplicationPackage createPackageForAdd(ResourceResolver resourceResolver, ReplicationRequest request)
            throws ReplicationPackageBuildingException {
        Session session = null;
        try {
            session = getSession(resourceResolver);

            final String[] paths = request.getPaths();

            DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
            DefaultMetaInf inf = new DefaultMetaInf();
            ExportOptions opts = new ExportOptions();
            for (String path : paths) {
                filter.add(new PathFilterSet(path));
            }
            inf.setFilter(filter);

            Properties props = new Properties();
            String packageGroup = "sling/replication";
            props.setProperty(VaultPackage.NAME_GROUP, packageGroup);
            String packageName = String.valueOf(request.getTime());
            props.setProperty(VaultPackage.NAME_NAME, packageName);
            log.debug("assembling package {}", packageGroup + '/' + packageName);
            inf.setProperties(props);

            opts.setMetaInf(inf);
            opts.setRootPath("/");
            File tmpFile = File.createTempFile("rp-vlt-create-" + System.nanoTime(), ".zip");
            VaultPackage vaultPackage = packaging.getPackageManager().assemble(session, opts, tmpFile);
            return new FileVaultReplicationPackage(vaultPackage);
        } catch (Exception e) {
            throw new ReplicationPackageBuildingException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    @Override
    protected ReplicationPackage readPackageInternal(ResourceResolver resourceResolver, final InputStream stream)
            throws ReplicationPackageReadingException {
        log.debug("reading a stream");
        ReplicationPackage pkg = null;
        try {
            File tmpFile = File.createTempFile("rp-vlt-read-" + System.nanoTime(), ".zip");
            FileOutputStream fileStream = new FileOutputStream(tmpFile);
            IOUtils.copy(stream, fileStream);
            IOUtils.closeQuietly(fileStream);

            VaultPackage vaultPackage = packaging.getPackageManager().open(tmpFile);

            if (vaultPackage != null) {
                pkg = new FileVaultReplicationPackage(vaultPackage);
            } else {
                log.warn("stream could not be read as a vlt package");
            }

        } catch (Exception e) {
            log.error("could not read / install the package", e);
            throw new ReplicationPackageReadingException(e);
        }
        return pkg;
    }


    @Override
    protected ReplicationPackage getPackageInternal(ResourceResolver resourceResolver, String id) {
        ReplicationPackage replicationPackage = null;
        try {
            File file = new File(id);
            if (file.exists()) {
                VaultPackage pkg = packaging.getPackageManager().open(file);
                replicationPackage = new FileVaultReplicationPackage(pkg);
            }
        } catch (Exception e) {
            log.warn("could not find a package with id : {}", id);
        }
        return replicationPackage;
    }


    @Override
    public boolean installPackageInternal(ResourceResolver resourceResolver, ReplicationPackage replicationPackage) throws ReplicationPackageReadingException {
        log.debug("reading a replication package stream");

        Session session = null;
        try {
            session = getSession(resourceResolver);
            File file = new File(replicationPackage.getId());
            if (file.exists()) {
                VaultPackage pkg = packaging.getPackageManager().open(file);
                ImportOptions opts = new ImportOptions();
                // TODO : make it possible to expose the VLT ImportMode / ACLHandling in a generic way (from the ReplicationRequest?)
//                opts.setImportMode(ImportMode.MERGE);
                pkg.extract(session, opts);
                return true;
            }
        } catch (Exception e) {
            log.error("could not read / install the package", e);
            throw new ReplicationPackageReadingException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
        return false;
    }
}

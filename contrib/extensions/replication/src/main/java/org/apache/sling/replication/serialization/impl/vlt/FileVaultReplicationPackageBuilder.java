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

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.config.DefaultMetaInf;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.ExportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.sling.jcr.api.SlingRepository;

import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;

import org.apache.sling.replication.serialization.ReplicationPackageReadingException;
import org.apache.sling.replication.serialization.impl.AbstractReplicationPackageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a {@link ReplicationPackageBuilder} based on Apache Jackrabbit FileVault.
 * <p/>
 * Each {@link ReplicationPackage} created by <code>FileVaultReplicationPackageBuilder</code> is
 * backed by a {@link VaultPackage}. 
 */
@Component(metatype = false)
@Service(value = ReplicationPackageBuilder.class)
@Property(name = "name", value = FileVaultReplicationPackageBuilder.NAME)
public class FileVaultReplicationPackageBuilder extends AbstractReplicationPackageBuilder implements
        ReplicationPackageBuilder {

    public static final String NAME = "vlt";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private SlingRepository repository;

    @Reference
    private Packaging packaging;

    protected ReplicationPackage createPackageForAdd(ReplicationRequest request)
            throws ReplicationPackageBuildingException {
        Session session = null;
        try {
            // TODO : replace this by using Credentials
            session = getSession();

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
            if (log.isInfoEnabled()) {
                log.info("assembling package {}", packageGroup + '/' + packageName);
            }
            inf.setProperties(props);

            opts.setMetaInf(inf);
            opts.setRootPath("/");
            File tmpFile = File.createTempFile("vlt-rp-" + System.nanoTime(), ".zip");
            VaultPackage pkg = packaging.getPackageManager().assemble(session, opts, tmpFile);
            return new FileVaultReplicationPackage(pkg);
        } catch (Exception e) {
            throw new ReplicationPackageBuildingException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected Session getSession() throws RepositoryException {
        return repository.loginAdministrative(null);
    }

    @Override
    protected ReplicationPackage readPackageForAdd(final InputStream stream, boolean install)
            throws ReplicationPackageReadingException {
        if (log.isInfoEnabled()) {
            log.info("reading a stream {}", stream);
        }
        Session session = null;
        ReplicationPackage pkg = null;
        try {
            if (log.isInfoEnabled()) {
                log.info("reading package for addition");
            }
            // TODO : use proper Credentials here
            session = getSession();
            if (session != null) {
                final JcrPackage jcrPackage = packaging.getPackageManager(session).upload(stream, true,
                        false);
                if (install) {
                    jcrPackage.install(new ImportOptions());
                }
                pkg = new FileVaultReplicationPackage(jcrPackage.getPackage());
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("could not read / install the package", e);
            }
            throw new ReplicationPackageReadingException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
        return pkg;
    }

    public ReplicationPackage getPackage(String id) {
        ReplicationPackage replicationPackage = null;
        try {
            File file = new File(id);
            if (file.exists()) {
                VaultPackage pkg = packaging.getPackageManager().open(file);
                replicationPackage = new FileVaultReplicationPackage(pkg);
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.info("could not find a package with id : {}", id);
            }
        }
        return replicationPackage;
    }

}

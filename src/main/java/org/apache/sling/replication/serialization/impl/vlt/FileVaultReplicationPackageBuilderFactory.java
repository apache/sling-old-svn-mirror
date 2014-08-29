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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.config.DefaultMetaInf;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.ExportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.apache.sling.replication.serialization.ReplicationPackageReadingException;
import org.apache.sling.replication.serialization.impl.AbstractReplicationPackageBuilder;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a {@link ReplicationPackageBuilder} based on Apache Jackrabbit FileVault.
 * <p/>
 * Each {@link ReplicationPackage} created by <code>FileVaultReplicationPackageBuilder</code> is
 * backed by a {@link VaultPackage}. 
 */
@Component(metatype = true,
        label = "FileVault based Replication Package Builder",
        description = "OSGi configuration based PackageBuilder service factory",
        name = FileVaultReplicationPackageBuilderFactory.SERVICE_PID)
@Service(value = ReplicationPackageBuilder.class)
@Property(name = "name", value = FileVaultReplicationPackageBuilderFactory.NAME)
public class FileVaultReplicationPackageBuilderFactory  implements ReplicationPackageBuilder {

    static final String SERVICE_PID = "org.apache.sling.replication.serialization.impl.vlt.FileVaultReplicationPackageBuilder";

    public static final String NAME = "vlt";

    @Property
    public static final String USERNAME = "username";

    @Property
    public static final String PASSWORD = "password";

    @Reference
    private SlingRepository repository;

    @Reference
    private Packaging packaging;

    private FileVaultReplicationPackageBuilder packageBuilder;

    @Activate
    @Modified
    protected void activate(Map<String, Object> config) {
        packageBuilder = getInstance(config, repository, packaging);
    }

    public static FileVaultReplicationPackageBuilder getInstance(Map<String, Object> config,
                                                   SlingRepository repository, Packaging packaging) {
        String username = PropertiesUtil.toString(config.get(USERNAME), "").trim();
        String password = PropertiesUtil.toString(config.get(PASSWORD), "").trim();
        if (username.length() == 0 || password.length() == 0) {
            throw new IllegalArgumentException("Username and password cannot be empty");
        }
        return new FileVaultReplicationPackageBuilder(NAME, username, password, repository, packaging);

    }

    public ReplicationPackage createPackage(ReplicationRequest request) throws ReplicationPackageBuildingException {
        return packageBuilder.createPackage(request);
    }

    public ReplicationPackage readPackage(InputStream stream) throws ReplicationPackageReadingException {
        return packageBuilder.readPackage(stream);
    }

    public ReplicationPackage getPackage(String id) {
        return packageBuilder.getPackage(id);
    }

    public boolean installPackage(ReplicationPackage replicationPackage) throws ReplicationPackageReadingException {
        return packageBuilder.installPackage(replicationPackage);
    }
}

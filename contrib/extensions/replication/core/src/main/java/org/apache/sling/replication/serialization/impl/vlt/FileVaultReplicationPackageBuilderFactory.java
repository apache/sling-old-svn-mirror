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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a {@link ReplicationPackageBuilder} based on Apache Jackrabbit FileVault.
 * <p/>
 * Each {@link ReplicationPackage} created by <code>FileVaultReplicationPackageBuilder</code> is
 * backed by a {@link VaultPackage}. 
 */
@Component(metatype = true,
        immediate = true,
        label = "FileVault based Replication Package Builder",
        description = "OSGi configuration based PackageBuilder service factory",
        name = FileVaultReplicationPackageBuilderFactory.SERVICE_PID,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
public class FileVaultReplicationPackageBuilderFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    static final String SERVICE_PID = "org.apache.sling.replication.serialization.impl.vlt.FileVaultReplicationPackageBuilderFactory";

    @Property
    public static final String NAME = "name";

    @Property
    public static final String SERVICENAME = "servicename";

    @Reference
    private SlingRepository repository;

    @Reference
    private Packaging packaging;

    private ServiceRegistration builderReg;

    @Activate
    protected void activate(BundleContext context, Map<String, Object> config) {
        log.info("activating FileVault package builder with config {}", config);

        String name = PropertiesUtil.toString(config.get(NAME), "").trim();

        if (name.length() == 0) {
            throw new IllegalArgumentException("name must not be empty");
        }

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(NAME, name);

        ReplicationPackageBuilder replicationPackageBuilder = getInstance(config, repository, packaging);

        builderReg = context.registerService(ReplicationPackageBuilder.class.getName(), replicationPackageBuilder, props);
    }

    @Deactivate
    protected void deactivate() throws Exception {
        log.info("deactivating FileVault package builder");
        if (builderReg != null) {
            builderReg.unregister();
            builderReg = null;
        }
    }

    public static FileVaultReplicationPackageBuilder getInstance(Map<String, Object> config,
                                                                 SlingRepository repository, Packaging packaging) {


        String serviceName = PropertiesUtil.toString(config.get(SERVICENAME), "").trim();

        if (serviceName.length() == 0) {
            throw new IllegalArgumentException("Service Name cannot be empty");
        }

        return new FileVaultReplicationPackageBuilder(serviceName, repository, packaging);

    }


}

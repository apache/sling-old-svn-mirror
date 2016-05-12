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
package org.apache.sling.distribution.packaging.impl.importer;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.component.impl.DistributionComponentConstants;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.serialization.DistributionPackageInfo;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi configuration factory for {@link RepositoryDistributionPackageImporter}s.
 */
@Component(label = "Apache Sling Distribution Importer - Repository Package Importer Factory",
        metatype = true,
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE)
@Service(DistributionPackageImporter.class)
@Property(name="webconsole.configurationFactory.nameHint", value="Importer name: {name}")
public class RepositoryDistributionPackageImporterFactory implements DistributionPackageImporter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * name of this component.
     */
    @Property
    private static final String NAME = DistributionComponentConstants.PN_NAME;

    @Property(name = "service.name")
    private static String SERVICE_NAME;

    @Property(name = "path")
    private static String PATH;

    @Property(name = "privilege.name")
    private static String PRIVILEGE_NAME;

    @Reference
    private SlingRepository repository;

    private RepositoryDistributionPackageImporter importer;

    @Activate
    protected void activate(Map<String, Object> config) {

        importer = new RepositoryDistributionPackageImporter(repository,
                PropertiesUtil.toString(config.get(SERVICE_NAME), "admin"),
                PropertiesUtil.toString(config.get(PATH), "/var/sling/distribution/import"),
                PropertiesUtil.toString(config.get(PRIVILEGE_NAME), "jcr:read"));
    }

    public void importPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionException {
        importer.importPackage(resourceResolver, distributionPackage);

    }

    @Nonnull
    public DistributionPackageInfo importStream(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionException {
        return importer.importStream(resourceResolver, stream);
    }
}

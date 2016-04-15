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
package org.apache.sling.distribution.serialization.impl.kryo;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.serialization.DistributionContentSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link DistributionContentSerializer}s based on Kryo.
 */
@Component(metatype = true,
        label = "Apache Sling Distribution Packaging - Kryo Serialization Format Factory",
        description = "OSGi configuration for Kryo formatas",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
@Service(DistributionContentSerializer.class)
public class KryoDistributionContentSerializerFactory implements DistributionContentSerializer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * name of this package builder.
     */
    @Property(label = "Name", description = "The name of the package builder.")
    public static final String NAME = "name";

    private KryoContentSerializer format;

    @Activate
    public void activate(Map<String, Object> config) {

        String name = PropertiesUtil.toString(config.get(NAME), null);
        log.info("starting Kryo format {}", name);

        format = new KryoContentSerializer(name);
        log.info("started Kryo resource package builder");
    }


    @Override
    public void exportToStream(ResourceResolver resourceResolver, DistributionRequest request, OutputStream outputStream) throws DistributionException {
        format.exportToStream(resourceResolver, request, outputStream);
    }

    @Override
    public void importFromStream(ResourceResolver resourceResolver, InputStream stream) throws DistributionException {
        format.importFromStream(resourceResolver, stream);
    }

    @Override
    public String getName() {
        return format.getName();
    }
}

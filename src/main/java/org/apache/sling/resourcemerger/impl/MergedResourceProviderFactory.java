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
package org.apache.sling.resourcemerger.impl;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;

@Component(name = "Apache Sling Merged Resource Provider Factory",
           description = "This resource provider delivers merged resources based on the search paths.",
           metatype=true)
@Service(value = ResourceProviderFactory.class)
@Properties({
    @Property(name = ResourceProvider.ROOTS, value=MergedResourceProviderFactory.DEFAULT_ROOT,
            label="Root",
            description="The mount point of merged resources"),
    @Property(name = ResourceProvider.USE_RESOURCE_ACCESS_SECURITY, boolValue=true,
              label="Secure", description="If enabled additional access checks are performed")
})
/**
 * The <code>MergedResourceProviderFactory</code> creates merged resource
 * providers.
 */
public class MergedResourceProviderFactory implements ResourceProviderFactory {

    public static final String DEFAULT_ROOT = "/merged";

    private String mergeRootPath;

    /**
     * {@inheritDoc}
     */
    public ResourceProvider getResourceProvider(final Map<String, Object> stringObjectMap)
    throws LoginException {
        return new MergedResourceProvider(mergeRootPath);
    }

    /**
     * {@inheritDoc}
     */
    public ResourceProvider getAdministrativeResourceProvider(final Map<String, Object> stringObjectMap)
    throws LoginException {
        return new MergedResourceProvider(mergeRootPath);
    }

    @Activate
    protected void configure(final Map<String, Object> properties) {
        mergeRootPath = PropertiesUtil.toString(properties.get(ResourceProvider.ROOTS), DEFAULT_ROOT);
    }

}

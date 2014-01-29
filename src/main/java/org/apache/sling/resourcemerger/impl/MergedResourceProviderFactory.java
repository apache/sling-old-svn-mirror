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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;

@Component(metatype = false)
@Service(value = ResourceProviderFactory.class)
@Properties({
        @Property(name = ResourceProvider.ROOTS, value = {"/merge"}, propertyPrivate = true)
})
/**
 * The <code>MergedResourceProviderFactory</code> creates merged resource
 * providers.
 */
public class MergedResourceProviderFactory implements ResourceProviderFactory {

    private String mergeRootPath;

    /**
     * {@inheritDoc}
     */
    public ResourceProvider getResourceProvider(Map<String, Object> stringObjectMap) throws LoginException {
        return new MergedResourceProvider(mergeRootPath);
    }

    /**
     * {@inheritDoc}
     */
    public ResourceProvider getAdministrativeResourceProvider(Map<String, Object> stringObjectMap) throws LoginException {
        return new MergedResourceProvider(mergeRootPath);
    }

    @Activate
    private void configure(Map<String, ?> properties) {
        String[] mergeRootPaths = PropertiesUtil.toStringArray(properties.get(ResourceProvider.ROOTS), new String[0]);
        if (mergeRootPaths.length > 0) {
            mergeRootPath = mergeRootPaths[0];
        }
    }

}

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
package org.apache.sling.contextaware.config.impl;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.ConfigurationBuilder;
import org.apache.sling.contextaware.config.ConfigurationResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(service=AdapterFactory.class,
        property={
            AdapterFactory.ADAPTER_CLASSES + "=org.apache.sling.contextaware.config.ConfigurationBuilder",
            AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.resource.Resource"
        })
public class ConfigurationBuilderAdapterFactory implements AdapterFactory {

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private ConfigurationResolver resolver;

    @Override
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(final Object adaptable, final Class<AdapterType> type) {
        if (adaptable instanceof Resource && type == ConfigurationBuilder.class) {
            return (AdapterType) resolver.get((Resource) adaptable);
        }
        return null;
    }

}

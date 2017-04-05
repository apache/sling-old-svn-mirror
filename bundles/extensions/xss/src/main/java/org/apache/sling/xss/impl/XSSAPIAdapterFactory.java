/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or
 * more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the
 * Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 ******************************************************************************/
package org.apache.sling.xss.impl;

import javax.annotation.Nonnull;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter factory that adapts a {@link ResourceResolver} to a resourceResolver-specific
 * {@link XSSAPI} service.
 */
@Component(
        property = {
                Constants.SERVICE_DESCRIPTION + "=Adapter for the XSSAPI service.",
                AdapterFactory.ADAPTER_CLASSES + "=org.apache.sling.xss.XSSAPI",
                AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.resource.ResourceResolver",
                AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.SlingHttpServletRequest"
        }
)
public class XSSAPIAdapterFactory implements AdapterFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(XSSAPIAdapterFactory.class);

    @Reference
    XSSAPI xssApi;

    public <AdapterType> AdapterType getAdapter(@Nonnull Object adaptable, @Nonnull Class<AdapterType> type) {
        if (adaptable instanceof ResourceResolver) {
            return getAdapter((ResourceResolver) adaptable, type);
        } else if (adaptable instanceof SlingHttpServletRequest) {
            return getAdapter((SlingHttpServletRequest) adaptable, type);
        } else {
            LOGGER.warn("Unable to handle adaptable {}", adaptable.getClass().getName());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <AdapterType> AdapterType getAdapter(ResourceResolver resourceResolver, Class<AdapterType> type) {
        if (resourceResolver != null) {
            if (type == XSSAPI.class) {
                return (AdapterType) xssApi.getResourceResolverSpecificAPI(resourceResolver);
            }
        }
        LOGGER.error(String.format("Unable to adapt resourceResolver to type %s.", type.getName()));
        return null;
    }

    @SuppressWarnings("unchecked")
    private <AdapterType> AdapterType getAdapter(SlingHttpServletRequest request, Class<AdapterType> type) {
        if (request != null) {
            if (type == XSSAPI.class) {
                return (AdapterType) xssApi.getRequestSpecificAPI(request);
            }
        }
        LOGGER.error(String.format("Unable to adapt request to type %s.", type.getName()));
        return null;
    }
}

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
package org.apache.sling.scripting.thymeleaf.internal.resourceresolver;

import org.apache.sling.api.request.SlingRequestEvent;
import org.apache.sling.api.request.SlingRequestEvent.EventType;
import org.apache.sling.api.request.SlingRequestListener;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = {
        RequestScopedResourceResolverProvider.class,
        SlingRequestListener.class
    },
    immediate = true
)
public class RequestScopedResourceResolverProvider implements SlingRequestListener {

    private final ThreadLocal<DelegatingResourceResolver> threadLocal = new ThreadLocal<>();

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile ResourceResolverFactory resourceResolverFactory;

    @Reference(
        cardinality = ReferenceCardinality.MANDATORY,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile ServiceUserMapped serviceUserMapped;

    private final Logger logger = LoggerFactory.getLogger(RequestScopedResourceResolverProvider.class);

    public ResourceResolver getResourceResolver() {
        DelegatingResourceResolver resourceResolver = threadLocal.get();
        if (resourceResolver == null) {
            try {
                logger.debug("getting service resource resolver for thread {}", Thread.currentThread().getName());
                final ResourceResolver delegate = resourceResolverFactory.getServiceResourceResolver(null);
                resourceResolver = new DelegatingResourceResolver(delegate);
                logger.debug("setting service resource resolver {} for thread {}", resourceResolver, Thread.currentThread().getName());
                threadLocal.set(resourceResolver);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return resourceResolver;
    }

    @Override
    public void onEvent(final SlingRequestEvent slingRequestEvent) {
        if (EventType.EVENT_DESTROY.equals(slingRequestEvent.getType())) {
            final DelegatingResourceResolver resourceResolver = threadLocal.get();
            logger.debug("removing service resource resolver {} for thread {}", resourceResolver, Thread.currentThread().getName());
            threadLocal.remove();
            if (resourceResolver != null) {
                logger.debug("closing resource resolver {} for thread {}", resourceResolver, Thread.currentThread().getName());
                resourceResolver.closeInternal();
            }
        }
    }

}

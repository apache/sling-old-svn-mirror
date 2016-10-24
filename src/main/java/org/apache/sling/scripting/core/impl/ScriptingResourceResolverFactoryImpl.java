/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.core.impl;

import org.apache.sling.api.request.SlingRequestEvent;
import org.apache.sling.api.request.SlingRequestListener;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.scripting.api.ScriptingResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        name = "Apache Sling Scripting Resource Resolver Factory",
        service = {ScriptingResourceResolverFactory.class, SlingRequestListener.class},
        configurationPid = "org.apache.sling.scripting.core.impl.ScriptingResourceResolverFactoryImpl"
)
@Designate(
        ocd = ScriptingResourceResolverFactoryImpl.Configuration.class
)
public class ScriptingResourceResolverFactoryImpl implements ScriptingResourceResolverFactory, SlingRequestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptingResourceResolverFactoryImpl.class);

    private ThreadLocal<ScriptingResourceResolver> perThreadResourceResolver = new ThreadLocal<>();
    private boolean logStackTraceOnResolverClose;

    @Reference
    private ResourceResolverFactory rrf;

    @ObjectClassDefinition(
            name = "Apache Sling Scripting Resource Resolver Factory Configuration",
            description = "The Apache Sling Scripting Resource Resolver Factory can be used by scripting bundles to obtain resource " +
                    "resolvers for solving scripting dependencies."
    )
    @interface Configuration {

        @AttributeDefinition(
                name = "Log the stack trace call for ResourceResolver#close",
                description = "If enabled, all calls to ResourceResolver#close for the request-scoped resource resolvers will be logged " +
                        "with the full stack trace. Don't enable this setting on production systems."
        )
        boolean log_stacktrace_onclose() default false;


    }

    @Override
    public ResourceResolver getRequestScopedResourceResolver() {
        return perThreadResourceResolver.get();
    }

    @Override
    public ResourceResolver getResourceResolver() {
        try {
            return rrf.getServiceResourceResolver(null);
        } catch (LoginException e) {
            LOGGER.error("Unable to retrieve a scripting resource resolver.");
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onEvent(SlingRequestEvent sre) {
        if (sre.getType().equals(SlingRequestEvent.EventType.EVENT_INIT)) {
            try {
                ResourceResolver delegate = rrf.getServiceResourceResolver(null);
                this.perThreadResourceResolver.set(new ScriptingResourceResolver(logStackTraceOnResolverClose, delegate));
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Setting per thread resource resolver for thread {}.", Thread.currentThread().getName());
                }
            } catch (LoginException e) {
                LOGGER.error("Cannot create per thread resource resolver.", e);
            }
        } else if (sre.getType().equals(SlingRequestEvent.EventType.EVENT_DESTROY)) {
            ScriptingResourceResolver scriptingResourceResolver = perThreadResourceResolver.get();
            if (scriptingResourceResolver != null) {
                scriptingResourceResolver._close();
                perThreadResourceResolver.remove();
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Removing per thread resource resolver for thread {}.", Thread.currentThread().getName());
            }
        }
    }

    @Activate
    protected void activate(Configuration configuration) {
        logStackTraceOnResolverClose = configuration.log_stacktrace_onclose();
    }

}

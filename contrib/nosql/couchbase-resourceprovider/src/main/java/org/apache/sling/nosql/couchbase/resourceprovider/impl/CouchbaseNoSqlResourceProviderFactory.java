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
package org.apache.sling.nosql.couchbase.resourceprovider.impl;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.nosql.couchbase.client.CouchbaseClient;
import org.apache.sling.nosql.generic.adapter.NoSqlAdapter;
import org.apache.sling.nosql.generic.resource.AbstractNoSqlResourceProviderFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ResourceProviderFactory} implementation that uses couchbase as
 * persistence.
 */
@Component(immediate = true)
@Service(value = ResourceProviderFactory.class)
public class CouchbaseNoSqlResourceProviderFactory extends AbstractNoSqlResourceProviderFactory {

    @Property(label = "Couchbase Client ID", description = "ID referencing the matching couchbase client configuration and bucket for caching.", value = CouchbaseNoSqlResourceProviderFactory.COUCHBASE_CLIENT_ID_DEFAULT)
    static final String COUCHBASE_CLIENT_ID_PROPERTY = "couchbaseClientID";
    private static final String COUCHBASE_CLIENT_ID_DEFAULT = "caravan-resourceprovider-couchbase";

    @Property(label = "Cache Key Prefix", description = "Prefix for caching keys.", value = CouchbaseNoSqlResourceProviderFactory.CACHE_KEY_PREFIX_DEFAULT)
    static final String CACHE_KEY_PREFIX_PROPERTY = "cacheKeyPrefix";
    private static final String CACHE_KEY_PREFIX_DEFAULT = "sling-resource:";

    @Property(label = "Root paths", description = "Root paths for resource provider.", cardinality = Integer.MAX_VALUE)
    static final String PROVIDER_ROOTS_PROPERTY = ResourceProvider.ROOTS;

    @Reference
    private EventAdmin eventAdmin;

    private String couchbaseClientId;
    private ServiceReference couchbaseClientServiceReference;
    private NoSqlAdapter noSqlAdapter;

    private static final Logger log = LoggerFactory.getLogger(CouchbaseNoSqlResourceProviderFactory.class);

    @Activate
    private void activate(ComponentContext componentContext, Map<String, Object> config) {
        CouchbaseClient couchbaseClient = null;
        try {
            couchbaseClientId = PropertiesUtil.toString(config.get(COUCHBASE_CLIENT_ID_PROPERTY),
                    COUCHBASE_CLIENT_ID_DEFAULT);
            BundleContext bundleContext = componentContext.getBundleContext();
            ServiceReference[] serviceReferences = bundleContext.getServiceReferences(
                    CouchbaseClient.class.getName(), "(" + CouchbaseClient.CLIENT_ID_PROPERTY + "=" + couchbaseClientId + ")");
            if (serviceReferences.length == 1) {
                couchbaseClientServiceReference = serviceReferences[0];
                couchbaseClient = (CouchbaseClient)bundleContext.getService(couchbaseClientServiceReference);
            }
            else if (serviceReferences.length > 1) {
                log.error("Multiple couchbase clients registered for client id '{}', caching is disabled.",
                        couchbaseClientId);
            }
            else {
                log.error("No couchbase clients registered for client id '{}', caching is disabled.", couchbaseClientId);
            }
        }
        catch (InvalidSyntaxException ex) {
            log.error("Invalid service filter, couchbase caching is disabled.", ex);
        }

        String cacheKeyPrefix = PropertiesUtil
                .toString(config.get(CACHE_KEY_PREFIX_PROPERTY), CACHE_KEY_PREFIX_DEFAULT);
        noSqlAdapter = new CouchbaseNoSqlAdapter(couchbaseClient, cacheKeyPrefix);
    }

    @Deactivate
    private void deactivate(ComponentContext componentContext) {
        if (couchbaseClientServiceReference != null) {
            componentContext.getBundleContext().ungetService(couchbaseClientServiceReference);
        }
    }

    @Override
    protected NoSqlAdapter getNoSqlAdapter() {
        return noSqlAdapter;
    }

    @Override
    protected EventAdmin getEventAdmin() {
        return eventAdmin;
    }

}

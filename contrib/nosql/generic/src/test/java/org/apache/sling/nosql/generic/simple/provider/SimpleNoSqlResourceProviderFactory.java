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
package org.apache.sling.nosql.generic.simple.provider;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.QueriableResourceProvider;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.nosql.generic.adapter.NoSqlAdapter;
import org.apache.sling.nosql.generic.resource.AbstractNoSqlResourceProviderFactory;
import org.osgi.service.event.EventAdmin;

/**
 * Simple NoSQL resource provider factory based on {@link SimpleNoSqlAdapter} which just stores
 * the resource data in a hash map.
 */
@Component(configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, metatype = true)
@Service(value = ResourceProviderFactory.class)
@Properties({
    @Property(name = ResourceProvider.ROOTS, value = ""),
    @Property(name = QueriableResourceProvider.LANGUAGES, value = { "simple" })
})
public class SimpleNoSqlResourceProviderFactory extends AbstractNoSqlResourceProviderFactory {

    @Reference
    private EventAdmin eventAdmin;
    
    private NoSqlAdapter noSqlAdapter;
    
    @Activate
    protected void activate(final Map<String, Object> props) {
        noSqlAdapter = new SimpleNoSqlAdapter();
    }
    
    @Override
    protected NoSqlAdapter getNoSqlAdapter() {
        return noSqlAdapter;
    }

    @Override
    protected EventAdmin getEventAdmin() {
        return eventAdmin;
    }
    
    protected void bindEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    protected void unbindEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

}

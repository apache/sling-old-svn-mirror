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
package org.apache.sling.nosql.mongodb.resourceprovider.impl;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.nosql.generic.adapter.MetricsNoSqlAdapterWrapper;
import org.apache.sling.nosql.generic.adapter.NoSqlAdapter;
import org.apache.sling.nosql.generic.resource.AbstractNoSqlResourceProviderFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;

import aQute.bnd.annotation.component.Deactivate;

/**
 * {@link ResourceProviderFactory} implementation that uses MongoDB as persistence.
 */
@Component(immediate = true, metatype = true,
    name="org.apache.sling.nosql.mongodb.resourceprovider.MongoDBNoSqlResourceProviderFactory.factory.config",
    label = "Apache Sling NoSQL MongoDB Resource Provider Factory", 
    description = "Defines a resource provider factory with MongoDB persistence.", 
    configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Service(value = ResourceProviderFactory.class)
@Property(name = "webconsole.configurationFactory.nameHint", 
    value = "Root paths: {" + MongoDBNoSqlResourceProviderFactory.PROVIDER_ROOTS_PROPERTY + "}")
public final class MongoDBNoSqlResourceProviderFactory extends AbstractNoSqlResourceProviderFactory {

    @Property(label = "Root paths", description = "Root paths for resource provider.", cardinality = Integer.MAX_VALUE)
    static final String PROVIDER_ROOTS_PROPERTY = ResourceProvider.ROOTS;
    
    @Property(label = "Connection String",
            description = "MongoDB connection String. Example: 'localhost:27017,localhost:27018,localhost:27019'",
            value = MongoDBNoSqlResourceProviderFactory.CONNECTION_STRING_DEFAULT)
    static final String CONNECTION_STRING_PROPERTY = "connectionString";
    private static final String CONNECTION_STRING_DEFAULT = "localhost:27017";
    
    @Property(label = "Database",
            description = "MongoDB database to store resource data in.",
            value = MongoDBNoSqlResourceProviderFactory.DATABASE_DEFAULT)
    static final String DATABASE_PROPERTY = "database";
    private static final String DATABASE_DEFAULT = "sling";
    
    @Property(label = "Collection",
            description = "MongoDB collection to store resource data in.",
            value = MongoDBNoSqlResourceProviderFactory.COLLECTION_DEFAULT)
    static final String COLLECTION_PROPERTY = "collection";
    private static final String COLLECTION_DEFAULT = "resources";
    
    @Reference
    private EventAdmin eventAdmin;

    private MongoClient mongoClient;
    private NoSqlAdapter noSqlAdapter;

    @Activate
    private void activate(ComponentContext componentContext, Map<String, Object> config) {
        String connectionString = PropertiesUtil.toString(config.get(CONNECTION_STRING_PROPERTY), CONNECTION_STRING_DEFAULT);
        String database = PropertiesUtil.toString(config.get(DATABASE_PROPERTY), DATABASE_DEFAULT);
        String collection = PropertiesUtil.toString(config.get(COLLECTION_PROPERTY), COLLECTION_DEFAULT);
        
        mongoClient = new MongoClient(connectionString);
        NoSqlAdapter mongodbAdapter = new MongoDBNoSqlAdapter(mongoClient, database, collection);
        
        // enable call logging and metrics for {@link MongoDBNoSqlAdapter}
        noSqlAdapter = new MetricsNoSqlAdapterWrapper(mongodbAdapter, LoggerFactory.getLogger(MongoDBNoSqlAdapter.class));
    }
    
    @Deactivate
    private void deactivate() {
        if (mongoClient != null) {
            mongoClient.close();
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

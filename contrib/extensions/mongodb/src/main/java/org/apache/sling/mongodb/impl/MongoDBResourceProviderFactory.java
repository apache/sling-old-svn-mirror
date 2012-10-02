/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.mongodb.impl;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.Mongo;

/**
 * The MongoDB resource provider factory allows to provided resources stored
 * in MongoDB.
 */
@Component(label="%factory.name",
description="%factory.description",
configurationFactory=true,
policy=ConfigurationPolicy.REQUIRE,
metatype=true)
@Service(value=ResourceProviderFactory.class)
@Properties({
    @Property(name=ResourceProvider.ROOTS, value="/mongo")
})
public class MongoDBResourceProviderFactory implements ResourceProviderFactory {

    private static final String DEFAULT_HOST = "localhost";

    private static final int DEFAULT_PORT = 27017;

    private static final String DEFAULT_DB = "sling";

    @Property(value=DEFAULT_HOST)
    private static final String PROP_HOST = "host";

    @Property(intValue=DEFAULT_PORT)
    private static final String PROP_PORT = "port";

    @Property(value=DEFAULT_DB)
    private static final String PROP_DB = "db";

    @Property(unbounded=PropertyUnbounded.ARRAY, value="system.indexes")
    private static final String PROP_FILTER_COLLECTIONS = "filter.collections";

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The global context passed to each resource provider. */
    private MongoDBContext context;

    @Reference
    private EventAdmin eventAdmin;

    @Activate
    protected void activate(final Map<String, Object> props) throws Exception {
        final String[] roots = PropertiesUtil.toStringArray(props.get(ResourceProvider.ROOTS));
        if ( roots == null || roots.length == 0 ) {
            throw new Exception("Roots configuration is missing.");
        }
        if ( roots.length > 1 ) {
            throw new Exception("Only a single root should be configured.");
        }
        if ( roots[0] == null || roots[0].trim().length() == 0 ) {
            throw new Exception("Roots configuration is missing.");
        }
        final String host = PropertiesUtil.toString(props.get(PROP_HOST), DEFAULT_HOST);
        final int port = PropertiesUtil.toInteger(props.get(PROP_PORT), DEFAULT_PORT);
        final String db = PropertiesUtil.toString(props.get(PROP_DB), DEFAULT_DB);
        logger.info("Starting MongoDB resource provider with host={}, port={}, db={}",
                new Object[] {host, port, db});

        final Mongo m = new Mongo( host , port );
        final DB database = m.getDB( db );
        logger.info("Connected to database {}", database);

        this.context = new MongoDBContext(database,
                roots[0],
                PropertiesUtil.toStringArray(props.get(PROP_FILTER_COLLECTIONS)),
                this.eventAdmin);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceProviderFactory#getResourceProvider(java.util.Map)
     */
    public ResourceProvider getResourceProvider(final Map<String, Object> authenticationInfo) throws LoginException {
        // for now we allow anonymous access
        return new MongoDBResourceProvider(this.context);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceProviderFactory#getAdministrativeResourceProvider(java.util.Map)
     */
    public ResourceProvider getAdministrativeResourceProvider(final Map<String, Object> authenticationInfo) throws LoginException {
        // for now we allow anonymous access
        return new MongoDBResourceProvider(this.context);
    }
}

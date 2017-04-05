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
package org.apache.sling.nosql.generic.resource;

import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.nosql.generic.adapter.NoSqlAdapter;
import org.apache.sling.nosql.generic.resource.impl.NoSqlResourceProvider;
import org.osgi.service.event.EventAdmin;

import aQute.bnd.annotation.ConsumerType;

/**
 * Abstract implementation of resource provider factory.
 * NoSQL resource providers implement this, add their own configuration support and and provide the matching NoSQL adapter implementation.
 */
@ConsumerType
public abstract class AbstractNoSqlResourceProviderFactory implements ResourceProviderFactory {

    public final ResourceProvider getResourceProvider(Map<String, Object> authenticationInfo) throws LoginException {
        NoSqlAdapter adapter = getNoSqlAdapter();
        adapter.checkConnection();
        adapter.createIndexDefinitions();
        return new NoSqlResourceProvider(adapter, getEventAdmin());
    }

    public final ResourceProvider getAdministrativeResourceProvider(Map<String, Object> authenticationInfo) throws LoginException {
        return getResourceProvider(authenticationInfo);
    }
    
    protected abstract NoSqlAdapter getNoSqlAdapter();

    protected abstract EventAdmin getEventAdmin();

}

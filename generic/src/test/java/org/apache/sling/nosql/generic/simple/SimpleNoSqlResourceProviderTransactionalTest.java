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
package org.apache.sling.nosql.generic.simple;

import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.nosql.generic.resource.impl.AbstractNoSqlResourceProviderTransactionalTest;
import org.apache.sling.nosql.generic.simple.provider.SimpleNoSqlResourceProviderFactory;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test basic ResourceResolver and ValueMap with different data types.
 */
public class SimpleNoSqlResourceProviderTransactionalTest extends AbstractNoSqlResourceProviderTransactionalTest {
    
    private Resource testRoot;

    @Override
    protected void registerResourceProviderFactory() {
        context.registerInjectActivateService(new SimpleNoSqlResourceProviderFactory(), ImmutableMap.<String, Object>builder()
                .put(ResourceProvider.ROOTS, "/nosql-simple")
                .build());
    }

    @Override
    protected Resource testRoot() {
        if (this.testRoot == null) {
            try {
                Map<String, Object> props = new HashMap<String, Object>();
                props.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                final Resource root = context.resourceResolver().getResource("/");
                Resource noSqlRoot = context.resourceResolver().create(root, "nosql-simple", props);
                this.testRoot = context.resourceResolver().create(noSqlRoot, "test", props);
                context.resourceResolver().commit();
            }
            catch (PersistenceException ex) {
                throw new RuntimeException(ex);
            }
        }
        return this.testRoot;
    }

    @Test
    public void testGetInvalidPath() {
        assertNull(context.resourceResolver().getResource(testRoot().getPath() + "/invalid/1"));
    }
    
}

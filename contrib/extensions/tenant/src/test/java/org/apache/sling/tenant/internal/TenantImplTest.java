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
package org.apache.sling.tenant.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.tenant.Tenant;
import org.junit.Test;

public class TenantImplTest {

    private static final String t1 = "t1";

    private static final String t2 = "t2";

    private static final String pt1 = "/etc/tenants/" + t1;

    private static final String pt2 = "/etc/tenants/" + t2;

    private static final String n1 = "name1";

    private static final String n2 = "name2";

    private static final String d1 = "description1";

    private static final String d2 = "description2";

    private static final String p1 = "prop1";

    @SuppressWarnings("serial")
    private static final Set<String> propNamesDefault = new HashSet<String>() {
        {
            add(Tenant.PROP_NAME);
            add(Tenant.PROP_DESCRIPTION);
        }
    };

    @SuppressWarnings("serial")
    private static final Set<String> propNamesTest = new HashSet<String>() {
        {
            add(p1);
        }
    };

    @Test
    public void test_id() {
        Resource r = new MockResource(pt1, new HashMap<String, Object>());
        Tenant tenant1 = new TenantImpl(r);

        TestCase.assertEquals(t1, tenant1.getId());
        TestCase.assertNull(tenant1.getName());
        TestCase.assertNull(tenant1.getDescription());

        TestCase.assertFalse(tenant1.getPropertyNames().hasNext());

        TestCase.assertNull(tenant1.getProperty(Tenant.PROP_NAME));
        TestCase.assertNull(tenant1.getProperty(Tenant.PROP_DESCRIPTION));
        TestCase.assertNull(tenant1.getProperty(p1));
    }

    @Test
    public void test_name_description() {
        @SuppressWarnings("serial")
        Resource r = new MockResource(pt1, new HashMap<String, Object>() {
            {
                put(Tenant.PROP_NAME, n1);
                put(Tenant.PROP_DESCRIPTION, d1);
            }
        });
        Tenant tenant1 = new TenantImpl(r);

        TestCase.assertEquals(t1, tenant1.getId());
        TestCase.assertEquals(n1, tenant1.getName());
        TestCase.assertEquals(d1, tenant1.getDescription());

        Iterator<String> pi = tenant1.getPropertyNames();
        TestCase.assertTrue(propNamesDefault.contains(pi.next()));
        TestCase.assertTrue(propNamesDefault.contains(pi.next()));
        TestCase.assertFalse(pi.hasNext());

        TestCase.assertEquals(n1, tenant1.getProperty(Tenant.PROP_NAME));
        TestCase.assertEquals(d1, tenant1.getProperty(Tenant.PROP_DESCRIPTION));
        TestCase.assertNull(tenant1.getProperty(p1));

    }

    @Test
    public void test_property() {
        @SuppressWarnings("serial")
        Resource r = new MockResource(pt1, new HashMap<String, Object>() {
            {
                put(p1, p1);
            }
        });
        Tenant tenant1 = new TenantImpl(r);

        TestCase.assertEquals(t1, tenant1.getId());
        TestCase.assertNull(tenant1.getName());
        TestCase.assertNull(tenant1.getDescription());

        Iterator<String> pi = tenant1.getPropertyNames();
        TestCase.assertTrue(propNamesTest.contains(pi.next()));
        TestCase.assertFalse(pi.hasNext());

        TestCase.assertNull(tenant1.getProperty(Tenant.PROP_NAME));
        TestCase.assertNull(tenant1.getProperty(Tenant.PROP_DESCRIPTION));
        TestCase.assertEquals(p1, tenant1.getProperty(p1));
    }

    private static class MockResource extends AbstractResource {

        private final String path;

        private final Map<String, Object> props;

        MockResource(final String path, final Map<String, Object> props) {
            this.path = path;
            this.props = props;
        }

        public String getPath() {
            return path;
        }

        public String getResourceType() {
            return null;
        }

        public String getResourceSuperType() {
            return null;
        }

        public ResourceMetadata getResourceMetadata() {
            return null;
        }

        public ResourceResolver getResourceResolver() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            if (type == ValueMap.class) {
                return (AdapterType) new ValueMapDecorator(props);
            }

            return super.adaptTo(type);
        }
    }
}

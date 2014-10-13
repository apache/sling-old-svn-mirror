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
package org.apache.sling.testing.mock.sling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class MockAdapterManagerTest {

    @Test
    public void test() {
        AdaptableTest sampleObject = new AdaptableTest();
        assertNull(sampleObject.adaptTo(String.class));

        BundleContext bundleContext = MockOsgi.newBundleContext();
        MockSling.setAdapterManagerBundleContext(bundleContext);

        bundleContext.registerService(AdapterFactory.class.getName(), new AdapterFactory() {
            @SuppressWarnings("unchecked")
            @Override
            public <AdapterType> AdapterType getAdapter(final Object adaptable, final Class<AdapterType> type) {
                if (adaptable instanceof AdaptableTest && type.isAssignableFrom(String.class)) {
                    return (AdapterType) ((AdaptableTest) adaptable).toString();
                }
                return null;
            }
        }, null);

        sampleObject = new AdaptableTest();
        assertEquals("adaptedString", sampleObject.adaptTo(String.class));

        MockSling.clearAdapterManagerBundleContext();

        sampleObject = new AdaptableTest();
        assertNull(sampleObject.adaptTo(String.class));
    }

    private static class AdaptableTest extends SlingAdaptable {

        @Override
        public String toString() {
            return "adaptedString";
        }

    }

}

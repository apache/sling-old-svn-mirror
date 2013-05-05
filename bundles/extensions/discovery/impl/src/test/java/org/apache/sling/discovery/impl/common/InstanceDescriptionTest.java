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
package org.apache.sling.discovery.impl.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class InstanceDescriptionTest {

    @Test
    public void testConstructor() throws Exception {
        final String slingId = UUID.randomUUID().toString();

        final DefaultClusterViewImpl clusterView = null;
        final boolean isLeader = false;
        final boolean isOwn = false;
        final String theSlingId = null;
        final Map<String, String> properties = null;
        try {
            constructInstanceDescription(clusterView, isLeader, isOwn,
                    theSlingId, properties);
            fail("should have thrown an exception");
        } catch (Exception e) {
            // ok
        }
        try {
            constructInstanceDescription(null, false, false, "", null);
            fail("should have thrown an exception");
        } catch (Exception e) {
            // ok
        }
        try {
            constructInstanceDescription(null, false, false, slingId, null)
                    .setClusterView(null);
            fail("should have thrown an exception");
        } catch (Exception e) {
            // ok
        }
        try {
            constructInstanceDescription(null, false, false, slingId, null)
                    .setProperties(null);
            fail("should have thrown an exception");
        } catch (Exception e) {
            // ok
        }
        DefaultInstanceDescriptionImpl id = constructInstanceDescription(null,
                false, false, slingId, null);
        id.setClusterView(new DefaultClusterViewImpl(UUID.randomUUID()
                .toString()));
        try {
            id.setClusterView(new DefaultClusterViewImpl(UUID.randomUUID()
                    .toString()));
            fail("should have thrown an exception");
        } catch (Exception e) {
            // ok
        }

        assertEquals(slingId,
                constructInstanceDescription(null, false, false, slingId, null)
                        .getSlingId());
        assertEquals(true,
                constructInstanceDescription(null, true, false, slingId, null)
                        .isLeader());
        assertEquals(false,
                constructInstanceDescription(null, false, false, slingId, null)
                        .isLeader());
        assertEquals(false,
                constructInstanceDescription(null, false, false, slingId, null)
                        .isLocal());

    }

    @Test
    public void testNotOwnInstance() throws Exception {
        final String slingId = UUID.randomUUID().toString();
        assertEquals(true,
                constructInstanceDescription(null, false, true, slingId, null)
                        .isLocal());
    }

    @Test
    public void testPropertiesSetting() throws Exception {
        String slingId = UUID.randomUUID().toString();
        DefaultInstanceDescriptionImpl id = constructInstanceDescription(null,
                false, false, slingId, null);
        id.setProperties(new HashMap<String, String>());
        // it is actually ok to set the properties multiple times...
        id.setProperties(new HashMap<String, String>());
        id.setProperties(new HashMap<String, String>());
        id.setProperties(new HashMap<String, String>());
    }

    public DefaultInstanceDescriptionImpl constructInstanceDescription(
            final DefaultClusterViewImpl clusterView, final boolean isLeader,
            final boolean isOwn, final String theSlingId,
            final Map<String, String> properties) throws Exception {
        return new DefaultInstanceDescriptionImpl(clusterView, isLeader, isOwn,
                theSlingId, properties);
    }
}

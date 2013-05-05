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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.Test;

public class ClusterViewTest {

    @Test
    public void testConstructor() throws Exception {

        try {
            new DefaultClusterViewImpl(null);
            fail("should throw an exception");
        } catch (Exception e) {
            // ok
        }

        try {
            new DefaultClusterViewImpl("");
            fail("should throw an exception");
        } catch (Exception e) {
            // ok
        }

        final String slingId = UUID.randomUUID().toString();
        DefaultClusterViewImpl cv = new DefaultClusterViewImpl(slingId);
        assertEquals(slingId, cv.getId());

        try {
            cv.getInstances();
            fail("should throw an exception");
        } catch (Exception e) {
            // ok
        }
        try {
            cv.getLeader();
            fail("should throw an exception");
        } catch (Exception e) {
            // ok
        }

        try {
            cv.getInstances();
            fail("should complain that there were never any instances added...");
        } catch (Exception e) {
            // ok
        }
        DefaultInstanceDescriptionImpl id0 = new DefaultInstanceDescriptionImpl(
                null, false, false, UUID.randomUUID().toString(), null);
        try {
            cv.getInstances();
            fail("should complain that there were never any instances added...");
        } catch (Exception e) {
            // ok
        }
        cv.addInstanceDescription(id0);
        assertEquals(1, cv.getInstances().size());
        assertSame(id0, cv.getInstances().get(0));
        try {
            cv.addInstanceDescription(id0);
            fail("can only set clusterview once");
        } catch (Exception e) {
            // ok
        }

        assertEquals(1, cv.getInstances().size());
        DefaultInstanceDescriptionImpl id = new DefaultInstanceDescriptionImpl(
                cv, true, false, UUID.randomUUID().toString(), null);
        assertEquals(2, cv.getInstances().size());
        try {
            cv.addInstanceDescription(id);
            fail("can only set clusterview once - already set in constructor above");
        } catch (Exception e) {
            // ok
        }
        assertEquals(2, cv.getInstances().size());
        assertSame(id, cv.getInstances().get(1));
        assertSame(id, cv.getLeader());

        DefaultInstanceDescriptionImpl id2 = new DefaultInstanceDescriptionImpl(
                cv, false, false, UUID.randomUUID().toString(), null);
        try {
            cv.addInstanceDescription(id2);
            fail("can only set clusterview once - already set in constructor above");
        } catch (Exception e) {
            // ok
        }
        assertEquals(3, cv.getInstances().size());
        assertSame(id, cv.getLeader());

        try {
            cv.addInstanceDescription(id);
            fail("should throw an exception");
        } catch (Exception e) {
            // ok
        }
        try {
            cv.addInstanceDescription(id2);
            fail("should throw an exception");
        } catch (Exception e) {
            // ok
        }
        try {
            DefaultInstanceDescriptionImpl id3 = new DefaultInstanceDescriptionImpl(
                    cv, true, false, UUID.randomUUID().toString(), null);
            cv.addInstanceDescription(id3);
            fail("should throw an exception");
        } catch (Exception e) {
            // ok
        }
    }
}

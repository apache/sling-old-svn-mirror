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
package org.apache.sling.replication.serialization.impl;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Testcase for {@link VoidReplicationPackage}
 */
public class VoidReplicationPackageTest {

    @Test
    public void testCreatedAndReadPackagesEquality() throws Exception {
        ReplicationRequest request = new ReplicationRequest(123l, ReplicationActionType.DELETE, "/abc");
        VoidReplicationPackage createdPackage = new VoidReplicationPackage(request, "VOID");
        VoidReplicationPackage readPackage = VoidReplicationPackage.fromStream(new ByteArrayInputStream("DELETE:[/abc]:123:VOID".getBytes()));
        assertEquals(createdPackage.getId(), readPackage.getId());
        assertEquals(createdPackage.getAction(), readPackage.getAction());
        assertEquals(createdPackage.getType(), readPackage.getType());
        assertEquals(createdPackage.getLength(), readPackage.getLength());
        assertEquals(Arrays.toString(createdPackage.getPaths()), Arrays.toString(readPackage.getPaths()));
        assertTrue(IOUtils.contentEquals(createdPackage.createInputStream(), readPackage.createInputStream()));
    }
}

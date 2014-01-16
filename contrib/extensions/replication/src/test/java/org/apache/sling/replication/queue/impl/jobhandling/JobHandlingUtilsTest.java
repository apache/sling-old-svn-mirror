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
package org.apache.sling.replication.queue.impl.jobhandling;

import java.io.InputStream;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link JobHandlingUtils}
 */
public class JobHandlingUtilsTest {
    @Test
    public void testFullPropertiesFromPackageCreation() throws Exception {
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        InputStream stream = IOUtils.toInputStream("some text");
        when(replicationPackage.createInputStream()).thenReturn(stream);
        when(replicationPackage.getAction()).thenReturn("ADD");
        when(replicationPackage.getId()).thenReturn("an-id");
        when(replicationPackage.getLength()).thenReturn(10l);
        when(replicationPackage.getPaths()).thenReturn(new String[]{"/content", "/apps"});
        when(replicationPackage.getType()).thenReturn("vlt");
        Map<String,Object> fullPropertiesFromPackage = JobHandlingUtils.createFullPropertiesFromPackage(replicationPackage);
        assertNotNull(fullPropertiesFromPackage);
        assertEquals(6, fullPropertiesFromPackage.size());
        assertNotNull(fullPropertiesFromPackage.get("replication.package.paths"));
        assertNotNull(fullPropertiesFromPackage.get("replication.package.id"));
        assertNotNull(fullPropertiesFromPackage.get("replication.package.length"));
        assertNotNull(fullPropertiesFromPackage.get("replication.package.stream"));
        assertNotNull(fullPropertiesFromPackage.get("replication.package.type"));
        assertNotNull(fullPropertiesFromPackage.get("replication.package.action"));
    }

    @Test
    public void testIdPropertiesFromPackageCreation() throws Exception {
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        when(replicationPackage.getId()).thenReturn("an-id");
        Map<String,Object> idPropertiesFromPackage = JobHandlingUtils.createIdPropertiesFromPackage(replicationPackage);
        assertNotNull(idPropertiesFromPackage);
        assertEquals(1, idPropertiesFromPackage.size());
        assertNotNull(idPropertiesFromPackage.get("replication.package.id"));
    }
}

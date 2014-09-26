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
package org.apache.sling.replication.it;

import org.apache.http.protocol.HTTP;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.junit.Test;

import static org.apache.sling.replication.it.ReplicationUtils.assertExists;
import static org.apache.sling.replication.it.ReplicationUtils.assertNotExits;
import static org.apache.sling.replication.it.ReplicationUtils.createRandomNode;
import static org.apache.sling.replication.it.ReplicationUtils.doExport;
import static org.apache.sling.replication.it.ReplicationUtils.doImport;

public class ReplicationPackageExporterImporterTest extends ReplicationIntegrationTestBase  {

    @Test
    public void testAddExportImport() throws Exception {
        String nodePath = createRandomNode(publishClient, "/content/export_" + System.nanoTime());
        assertExists(publishClient, nodePath);

        String content = doExport(publish, "default", ReplicationActionType.ADD, nodePath);

        publishClient.delete(nodePath);
        assertNotExits(publishClient, nodePath);

        doImport(publish, "default", content.getBytes(HTTP.DEFAULT_CONTENT_CHARSET));
        assertExists(publishClient, nodePath);

    }

    @Test
    public void testDeleteExportImport() throws Exception {
        String nodePath = createRandomNode(publishClient, "/content/export_" + System.nanoTime());
        assertExists(publishClient, nodePath);

        String content = doExport(publish, "default", ReplicationActionType.DELETE, nodePath);

        doImport(publish, "default", content.getBytes(HTTP.DEFAULT_CONTENT_CHARSET));
        assertNotExits(publishClient, nodePath);
    }
}

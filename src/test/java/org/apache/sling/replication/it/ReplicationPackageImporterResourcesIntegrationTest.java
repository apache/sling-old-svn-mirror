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

import org.junit.Test;

import static org.apache.sling.replication.it.ReplicationUtils.assertExists;
import static org.apache.sling.replication.it.ReplicationUtils.assertResponseContains;
import static org.apache.sling.replication.it.ReplicationUtils.importerRootUrl;
import static org.apache.sling.replication.it.ReplicationUtils.importerUrl;

/**
 * Integration test for {@link org.apache.sling.replication.packaging.ReplicationPackageImporter} resources
 */
public class ReplicationPackageImporterResourcesIntegrationTest extends ReplicationIntegrationTestBase {

    @Test
    public void testImporterRootResource() throws Exception {
        String rootResource = importerRootUrl();
        assertExists(publishClient, rootResource);
        assertResponseContains(publish, rootResource,
                "sling:resourceType", "sling/replication/service/importer/list",
                "items", "default");
    }

    @Test
    public void testLocalImporterResource() throws Exception {
        String rootResource = importerUrl("default");
        assertExists(publishClient, rootResource);
        assertResponseContains(publish, rootResource,
                "sling:resourceType", "sling/replication/service/importer",
                "name", "default");
    }
}

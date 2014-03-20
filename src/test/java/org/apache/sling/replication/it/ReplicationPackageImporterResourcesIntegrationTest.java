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

/**
 * Integration test for {@link org.apache.sling.replication.serialization.ReplicationPackageImporter} resources
 */
public class ReplicationPackageImporterResourcesIntegrationTest extends ReplicationITBase {

    @Test
    public void testImporterRootResource() throws Exception {
        assertResourceExists("/libs/sling/replication/importer.json");
        assertJsonResponseEquals("/libs/sling/replication/importer.json",
                "{\"sling:resourceType\":\"replication/importers\",\"items\":[\"default\"]}");
    }

    @Test
    public void testDefaultImporterResource() throws Exception {
        String rootResource = "/libs/sling/replication/importer/default.json";
        assertResourceExists(rootResource);
        assertJsonResponseEquals(rootResource,
                "{\"sling:resourceType\":\"replication/importer\",\"name\":\"default\"}");
    }
}

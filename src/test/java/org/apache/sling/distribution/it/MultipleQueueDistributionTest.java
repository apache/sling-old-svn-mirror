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
package org.apache.sling.distribution.it;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.distribution.DistributionRequestType;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static org.apache.sling.distribution.it.DistributionUtils.assertExists;
import static org.apache.sling.distribution.it.DistributionUtils.assertPostResourceWithParameters;
import static org.apache.sling.distribution.it.DistributionUtils.createRandomNode;
import static org.apache.sling.distribution.it.DistributionUtils.distribute;
import static org.apache.sling.distribution.it.DistributionUtils.getResource;
import static org.apache.sling.distribution.it.DistributionUtils.queueUrl;

/**
 * Integration test for forward distribution
 */
public class MultipleQueueDistributionTest extends DistributionIntegrationTestBase {

    final static String DELETE_LIMIT = "100";

    @Test
    public void testQueues() throws Exception {

        JSONObject json = getResource(author, queueUrl("queue-multiple") + ".infinity");

        JSONArray items = json.getJSONArray("items");
        assertEquals("endpoint1", items.getString(0));
        assertEquals("endpoint2", items.getString(1));

        JSONObject endpoint1 = json.getJSONObject("endpoint1");
        assertEquals(true, endpoint1.getBoolean("empty"));

        JSONObject endpoint2 = json.getJSONObject("endpoint2");
        assertEquals(true, endpoint2.getBoolean("empty"));

    }

    @Test
    public void testDistributeQueues() throws Exception {

        String nodePath = createRandomNode(authorClient, "/content/forward_add_" + System.nanoTime());
        assertExists(authorClient, nodePath);


        // Add two items in both queues
        distribute(author, "queue-multiple", DistributionRequestType.ADD, nodePath);
        distribute(author, "queue-multiple", DistributionRequestType.DELETE, nodePath);

        JSONObject json = getResource(author, queueUrl("queue-multiple") + ".infinity");

        JSONArray items = json.getJSONArray("items");
        assertEquals("endpoint1", items.getString(0));
        assertEquals("endpoint2", items.getString(1));

        JSONObject queue = json.getJSONObject("endpoint1");
        assertEquals(false, queue.getBoolean("empty"));

        JSONArray queueItems = queue.getJSONArray("items");
        assertEquals(2, queueItems.length());
        assertEquals(2, queue.get("itemsCount"));
        String firstId = queueItems.getString(0);
        JSONObject queueItem = queue.getJSONObject(firstId);
        assertEquals("ADD", queueItem.getString("action"));


        String secondId = queueItems.getString(1);
        queueItem = queue.getJSONObject(secondId);
        assertEquals("DELETE", queueItem.getString("action"));

        queue = json.getJSONObject("endpoint2");
        queueItems = queue.getJSONArray("items");
        assertEquals(2, queueItems.length());
        assertEquals(2, queue.get("itemsCount"));


        // Delete second item from endpoint1
        assertPostResourceWithParameters(author, 200, queueUrl("queue-multiple") + "/endpoint1",
                "operation", "delete", "id", secondId);

        json = getResource(author, queueUrl("queue-multiple") + ".infinity");
        queue = json.getJSONObject("endpoint1");
        queueItems = queue.getJSONArray("items");
        assertEquals(1, queueItems.length());
        assertEquals(1, queue.get("itemsCount"));

        String id = queueItems.getString(0);
        assertEquals(firstId, id);

        queue = json.getJSONObject("endpoint2");
        queueItems = queue.getJSONArray("items");
        assertEquals(2, queueItems.length());
        assertEquals(2, queue.get("itemsCount"));

        // Delete 2 items from endpoint2
        assertPostResourceWithParameters(author, 200, queueUrl("queue-multiple") + "/endpoint2",
                "operation", "delete", "limit", "2");

        json = getResource(author, queueUrl("queue-multiple") + ".infinity");
        queue = json.getJSONObject("endpoint1");
        queueItems = queue.getJSONArray("items");
        assertEquals(1, queueItems.length());
        assertEquals(1, queue.get("itemsCount"));


        queue = json.getJSONObject("endpoint2");
        queueItems = queue.getJSONArray("items");
        assertEquals(0, queueItems.length());
        assertEquals(0, queue.get("itemsCount"));

    }


    @After
    public void clean() throws IOException {
        assertPostResourceWithParameters(author, 200, queueUrl("queue-multiple") + "/endpoint1",
                "operation", "delete", "limit", DELETE_LIMIT);

        assertPostResourceWithParameters(author, 200, queueUrl("queue-multiple") + "/endpoint2",
                "operation", "delete", "limit", DELETE_LIMIT);

    }

}

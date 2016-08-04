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

import org.apache.sling.distribution.DistributionRequestType;
import org.junit.Test;

import java.util.Map;

import static org.apache.sling.distribution.it.DistributionUtils.assertExists;
import static org.apache.sling.distribution.it.DistributionUtils.assertNotExists;
import static org.apache.sling.distribution.it.DistributionUtils.createRandomNode;
import static org.apache.sling.distribution.it.DistributionUtils.distribute;
import static org.apache.sling.distribution.it.DistributionUtils.distributeDeep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Integration test for forward distribution
 */
public class SelectiveForwardDistributionTest extends DistributionIntegrationTestBase {


    @Test
    public void testQueues() throws Exception {

        Map<String, Map<String, Object>> queues = DistributionUtils.getQueues(author, "publish-selective");
        assertEquals(2, queues.size());
    }

    @Test
    public void testAddContent() throws Exception {
        String nodePath1 = createRandomNode(authorClient, "/content/news/forward_add_" + System.nanoTime());
        String nodePath2 = createRandomNode(authorClient, "/content/forward_add_" + System.nanoTime());

        assertExists(authorClient, nodePath1);
        assertExists(authorClient, nodePath2);

        distribute(author, "publish-selective", DistributionRequestType.ADD, nodePath1);
        distribute(author, "publish-selective", DistributionRequestType.ADD, nodePath2);

        assertExists(publishClient, nodePath1);
        assertExists(publishClient, nodePath2);
    }

    @Test
    public void testDeleteContent() throws Exception {
        String nodePath1 = createRandomNode(publishClient, "/content/news/forward_del_" + System.nanoTime());
        String nodePath2 = createRandomNode(publishClient, "/content/forward_del_" + System.nanoTime());

        assertExists(publishClient, nodePath1);
        assertExists(publishClient, nodePath2);

        distribute(author, "publish-selective", DistributionRequestType.DELETE, nodePath1);
        distribute(author, "publish-selective", DistributionRequestType.DELETE, nodePath2);

        assertNotExists(publishClient, nodePath1);
        assertNotExists(publishClient, nodePath2);

    }
}

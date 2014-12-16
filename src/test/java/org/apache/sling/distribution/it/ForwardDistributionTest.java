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

import static org.apache.sling.distribution.it.DistributionUtils.assertExists;
import static org.apache.sling.distribution.it.DistributionUtils.assertNotExists;
import static org.apache.sling.distribution.it.DistributionUtils.createRandomNode;
import static org.apache.sling.distribution.it.DistributionUtils.distribute;
import static org.apache.sling.distribution.it.DistributionUtils.distributeDeep;

/**
 * Integration test for forward distribution
 */
public class ForwardDistributionTest extends DistributionIntegrationTestBase {

    @Test
    public void testAddContent() throws Exception {
        String nodePath = createRandomNode(authorClient, "/content/forward_add_" + System.nanoTime());
        assertExists(authorClient, nodePath);
        distribute(author, "publish", DistributionRequestType.ADD, nodePath);
        assertExists(publishClient, nodePath);
    }

    @Test
    public void testDeleteContent() throws Exception {
        String nodePath = createRandomNode(publishClient, "/content/forward_del_" + System.nanoTime());
        assertExists(publishClient, nodePath);
        distribute(author, "publish", DistributionRequestType.DELETE, nodePath);
        assertNotExists(publishClient, nodePath);
    }

    @Test
    public void testShallowAddTreeContent() throws Exception {
        String nodePath = createRandomNode(authorClient, "/content/forward_add_" + System.nanoTime());
        assertExists(authorClient, nodePath);

        String childPath = nodePath + "/child";
        authorClient.createNode(childPath);
        assertExists(authorClient, childPath);

        distribute(author, "publish", DistributionRequestType.ADD, nodePath);
        assertExists(publishClient, nodePath);
        assertNotExists(publishClient, childPath);
    }

    @Test
    public void testDeepAddTreeContent() throws Exception {
        String nodePath = createRandomNode(authorClient, "/content/forward_add_" + System.nanoTime());
        assertExists(authorClient, nodePath);

        String childPath = nodePath + "/child";
        authorClient.createNode(childPath);
        assertExists(authorClient, childPath);

        distributeDeep(author, "publish", DistributionRequestType.ADD, nodePath);
        assertExists(publishClient, nodePath);
        assertExists(publishClient, childPath);
    }


    @Test
    public void testShallowUpdateTreeContent() throws Exception {
        String nodePath = createRandomNode(authorClient, "/content/forward_add_" + System.nanoTime());
        assertExists(authorClient, nodePath);

        String child1Path = nodePath + "/child1";
        authorClient.createNode(child1Path);
        assertExists(authorClient, child1Path);

        String child2Path = nodePath + "/child2";

        publishClient.createNode(child2Path);
        assertExists(publishClient, child2Path);

        distribute(author, "publish", DistributionRequestType.ADD, child1Path);

        assertExists(publishClient, child1Path);
        assertExists(publishClient, child2Path);
    }


    @Test
    public void testDeepUpdateTreeContent() throws Exception {
        String nodePath = createRandomNode(authorClient, "/content/forward_add_" + System.nanoTime());
        assertExists(authorClient, nodePath);

        String child1Path = nodePath + "/child1";
        authorClient.createNode(child1Path);
        assertExists(authorClient, child1Path);

        String child2Path = nodePath + "/child2";

        publishClient.createNode(child2Path);
        assertExists(publishClient, child2Path);

        distributeDeep(author, "publish", DistributionRequestType.ADD, nodePath);
        assertExists(publishClient, nodePath);
        assertExists(publishClient, child1Path);
        assertExists(publishClient, child2Path);
    }

}

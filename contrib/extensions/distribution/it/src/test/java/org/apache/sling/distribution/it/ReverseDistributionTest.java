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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.apache.sling.distribution.it.DistributionUtils.assertExists;
import static org.apache.sling.distribution.it.DistributionUtils.assertNotExists;
import static org.apache.sling.distribution.it.DistributionUtils.createRandomNode;
import static org.apache.sling.distribution.it.DistributionUtils.distribute;

/**
 * Integration test for reverse distribution
 */
@RunWith(Parameterized.class)
public class ReverseDistributionTest extends DistributionIntegrationTestBase {


    private final String reverseAgent;

    @Parameterized.Parameters
    public static Collection<Object[]> generateData() {
        return Arrays.asList(new Object[][]{
                {"reverse"},
                {"impersonate-reverse"},
        });
    }

    public ReverseDistributionTest(String reverseAgent) {

        this.reverseAgent = reverseAgent;
    }


    @Test
    public void testAddContent() throws Exception {
        String nodePath = createRandomNode(publishClient, "/content/reverse_add_" + System.nanoTime());
        assertExists(publishClient, nodePath);
        distribute(publish, reverseAgent, DistributionRequestType.ADD, nodePath);
        assertExists(authorClient, nodePath);
    }

    @Test
    public void testDeleteContent() throws Exception {
        String nodePath = createRandomNode(authorClient, "/content/reverse_del_" + System.nanoTime());
        assertExists(authorClient, nodePath);
        distribute(publish, reverseAgent, DistributionRequestType.DELETE, nodePath);
        assertNotExists(authorClient, nodePath);
    }

    @Test
    public void testAddTwoContent() throws Exception {
        String nodePath1 = createRandomNode(publishClient, "/content/reverse_twoadd_" + System.nanoTime());
        assertExists(publishClient, nodePath1);
        String nodePath2 = createRandomNode(publishClient, "/content/reverse_twoadd_" + System.nanoTime());
        assertExists(publishClient, nodePath2);

        distribute(publish, reverseAgent, DistributionRequestType.ADD, nodePath1);
        distribute(publish, reverseAgent, DistributionRequestType.ADD, nodePath2);

        assertExists(authorClient, nodePath1);
        assertExists(authorClient, nodePath2);
    }

}

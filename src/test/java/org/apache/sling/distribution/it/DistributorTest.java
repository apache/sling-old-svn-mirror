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
import static org.apache.sling.distribution.it.DistributionUtils.assertPostResourceWithParameters;
import static org.apache.sling.distribution.it.DistributionUtils.createRandomNode;

public class DistributorTest extends DistributionIntegrationTestBase {

    @Test
    public void testAddContent() throws Exception {
        String nodePath = createRandomNode(authorClient, "/content/forward_add_" + System.nanoTime());
        assertExists(authorClient, nodePath);

        assertPostResourceWithParameters(author, 200, "/bin/test/distributor", "agent", "publish",
                "path", nodePath, "action", DistributionRequestType.ADD.name());
        assertExists(publishClient, nodePath);
    }
}

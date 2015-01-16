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

package org.apache.sling.distribution.it.triggers;


import org.apache.sling.distribution.it.DistributionIntegrationTestBase;
import org.junit.Ignore;
import org.junit.Test;

import static org.apache.sling.distribution.it.DistributionUtils.assertExists;
import static org.apache.sling.distribution.it.DistributionUtils.triggerUrl;

@Ignore
public class DistributionTriggerResourcesTest extends DistributionIntegrationTestBase {

    @Test
    public void testTestTriggersOnAuthor() throws Exception {
        String[] names = new String[]{
                "test-content-event",
                "test-remote-event",
                "test-distribute-event",
                "test-scheduled-event",
                "test-persisting-event"
        };
        for (String name : names) {
            assertExists(authorClient, triggerUrl(name));
        }
    }
}

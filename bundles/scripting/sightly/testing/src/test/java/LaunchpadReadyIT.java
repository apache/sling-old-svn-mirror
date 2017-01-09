/*******************************************************************************
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
 ******************************************************************************/

import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.junit.rules.SlingInstanceRule;
import org.junit.ClassRule;
import org.junit.Test;

public class LaunchpadReadyIT {

    @ClassRule
    public static final SlingInstanceRule SLING_INSTANCE_RULE = new SlingInstanceRule();

    @Test
    public void testLaunchpadReady() throws Exception {
        SlingClient client = SLING_INSTANCE_RULE.getAdminClient();
        client.waitUntilExists("/apps/sightly", 100, 100);
        client.waitUntilExists("/sightlytck", 100, 100);
    }

}

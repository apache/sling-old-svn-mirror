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
package org.apache.sling.distribution.queue.impl.simple;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.junit.Test;

import static org.junit.Assert.*;

public class QueueItemMapperTest {

    @Test
    public void testReadWriteQueueItem() throws Exception {
        QueueItemMapper mapper = new QueueItemMapper();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("string-1", "some string");
        properties.put("null", null);
        properties.put("long", 200L);
        properties.put("double", 300.3d);
        DistributionQueueItem expected = new DistributionQueueItem("packageId", properties);
        DistributionQueueItem actual = mapper.readQueueItem(mapper.writeQueueItem(expected));
        assertEquals(expected.getPackageId(), actual.getPackageId());
        for (Map.Entry<String, Object> entry : actual.entrySet()) {
            assertEquals(expected.get(entry.getKey()), entry.getValue());
        }
    }
}
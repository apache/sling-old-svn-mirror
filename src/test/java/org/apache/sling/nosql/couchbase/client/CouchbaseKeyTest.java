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
package org.apache.sling.nosql.couchbase.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class CouchbaseKeyTest {

    private static final String PREFIX = "prefix:";

    @Test
    public void testShortKey() {
        String key = CouchbaseKey.build("/short/key", PREFIX);
        assertEquals(PREFIX + "/short/key", key);
    }

    @Test
    public void testLongKey() {
        String key = CouchbaseKey.build("/long/key/" + StringUtils.repeat("/aaa", 500), PREFIX);
        assertTrue(StringUtils.startsWith(key, PREFIX + "/long/key/"));
        assertEquals(CouchbaseKey.MAX_KEY_LENGTH, key.length());
    }

}

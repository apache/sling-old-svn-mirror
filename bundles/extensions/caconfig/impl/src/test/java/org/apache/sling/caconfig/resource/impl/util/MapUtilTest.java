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
package org.apache.sling.caconfig.resource.impl.util;

import static org.apache.sling.caconfig.resource.impl.util.MapUtil.traceOutput;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class MapUtilTest {

    @Test
    public void testTraceOutput() {
        assertEquals("{}", traceOutput(ImmutableMap.<String,Object>of()));
        
        assertEquals("{prop1: 'aa', prop2: 5, prop3: true}", traceOutput(ImmutableMap.<String,Object>of(
                "prop1", "aa",
                "prop2", 5,
                "prop3", true
                )));

        assertEquals("{prop1: ['aa','bb'], prop2: [5,10], prop3: true}", traceOutput(ImmutableMap.<String,Object>of(
                "prop1", new String[] { "aa", "bb" },
                "prop2", new Integer[] { 5, 10 },
                "prop3", true
                )));
    }

}

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
package org.apache.sling.distribution.component.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class DistributionComponentUtilsTest {

    @Test
    public void testEmptySettingsTransform() {
        Map<String, Object> settings = new HashMap<String, Object>();

        DistributionComponentUtils utils = new DistributionComponentUtils();
        List<Map<String, Object>> result = utils.transformToOsgi("agent", "publish", settings);

        assertEquals(0, result.size());
    }

    @Test
    public void testFirstLevelTransform() {
        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put("type", "simple");

        settings.put("packageExporter", new HashMap<String, Object>());

        DistributionComponentUtils utils = new DistributionComponentUtils();
        List<Map<String, Object>> result = utils.transformToOsgi("agent", "publish", settings);

        assertEquals(1, result.size());
        assertEquals(6, result.get(0).size());
        assertTrue(result.get(0).containsKey("name"));
        assertTrue(result.get(0).containsKey("type"));
        assertTrue(result.get(0).containsKey("packageExporter.target"));
        assertEquals("(parent.ref.id=agent/publish)", result.get(0).get("packageExporter.target"));
    }

    @Test
    public void testTwoLevelTransform() {
        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put("type", "simple");
        settings.put("packageExporter", new HashMap<String, Object>());
        ((Map) settings.get("packageExporter")).put("kind", "exporter");
        ((Map) settings.get("packageExporter")).put("type", "remote");

        DistributionComponentUtils utils = new DistributionComponentUtils();
        List<Map<String, Object>> result = utils.transformToOsgi("agent", "publish", settings);

        assertEquals(2, result.size());

        {
            assertEquals(6, result.get(1).size());
            assertTrue(result.get(1).containsKey("kind"));
            assertTrue(result.get(1).containsKey("name"));
            assertTrue(result.get(1).containsKey("type"));
            assertTrue(result.get(1).containsKey("packageExporter.target"));
            assertEquals("(parent.ref.id=agent/publish)", result.get(1).get("packageExporter.target"));
        }

        {
            assertEquals(5, result.get(0).size());
        }
    }

    @Test
    public void testMultipleTargetTransform() {
        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put("type", "simple");
        settings.put("triggers", new HashMap<String, Object>());
        ((Map) settings.get("triggers")).put("type", "list");
        ((Map) settings.get("triggers")).put("kind", "trigger");


        Map trigger1Properties = new HashMap<String, Object>();
        trigger1Properties.put("type", "event");
        ((Map) settings.get("triggers")).put("trigger1", trigger1Properties);


        Map trigger2Properties = new HashMap<String, Object>();
        trigger2Properties.put("type", "event");
        ((Map) settings.get("triggers")).put("trigger2", trigger2Properties);



        DistributionComponentUtils utils = new DistributionComponentUtils();
        List<Map<String, Object>> result = utils.transformToOsgi("agent", "publish", settings);

        assertEquals(3, result.size());
        {
            String resultKey = "agent|simple|agent/publish";
            assertEquals(6, result.get(2).size());
            assertTrue(result.get(2).containsKey("name"));
            assertTrue(result.get(2).containsKey("type"));
            assertTrue(result.get(2).containsKey("triggers.target"));
            assertEquals("(parent.ref.id=agent/publish)", result.get(2).get("triggers.target"));
        }
    }
}

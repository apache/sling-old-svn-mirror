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

import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SettingsUtilsTest {

    @Test
    public void testParseMap() {
        String[] lines = new String[] {
                "packageExporter/exporterKey=exporterValue",
                "packageExporter/exporterMap[key1]=mapvalue1",
                "packageExporter/exporterBuilder/builderKey=builderValue",
                "packageExporter/endpointsArray[0]=http://abc.com",

                "mainKey=mainValue",
                "trigger[0]/propertyKey1=propertyValue1",
                "trigger[0]/propertyKey2=propertyValue2",
                "slashKey=http://aaa.com"
        };
        Map<String, Object> result = SettingsUtils.parseLines(lines);

        assertEquals("result map size", 4, result.size());

        assertTrue(result.containsKey("packageExporter"));
        Object packageExporterProperties = result.get("packageExporter");
        assertTrue(packageExporterProperties instanceof Map);
        @SuppressWarnings("unchecked") // type is known
        Map<String, Object> packageExporterMap = (Map<String, Object>) packageExporterProperties;

        assertTrue(packageExporterMap.containsKey("exporterKey"));
        assertEquals("exporterValue", packageExporterMap.get("exporterKey"));
        assertTrue(packageExporterMap.containsKey("exporterBuilder"));
        assertTrue(packageExporterMap.get("exporterBuilder") instanceof Map);
        @SuppressWarnings("unchecked") // type is known
        Map<String, Object> exporterBuilderMap = (Map<String, Object>) packageExporterMap.get("exporterBuilder");
        assertEquals("builderValue", exporterBuilderMap.get("builderKey"));
        assertTrue(packageExporterMap.containsKey("exporterMap"));
        assertTrue(packageExporterMap.get("exporterMap") instanceof Map);
        @SuppressWarnings("unchecked") // type is known
        Map<String, Object> exporterMap =  (Map<String, Object>) packageExporterMap.get("exporterMap");
        assertEquals("mapvalue1", exporterMap.get("key1"));

        assertTrue(packageExporterMap.containsKey("endpointsArray"));
        assertTrue(packageExporterMap.get("endpointsArray") instanceof List);
        @SuppressWarnings("unchecked") // type is known
        List<String> endpointsArray = (List<String>) packageExporterMap.get("endpointsArray");
        assertEquals("http://abc.com", endpointsArray.get(0));

        assertTrue(result.containsKey("mainKey"));

        assertTrue(result.containsKey("trigger"));
        assertTrue(result.get("trigger") instanceof List);

        @SuppressWarnings("unchecked") // type is known
        List<Map<String, Object>> triggersList = (List<Map<String, Object>>) result.get("trigger");
        assertNotNull(triggersList.get(0));
        Map<String, Object> trigger0Map = triggersList.get(0);
        assertEquals("propertyValue1", trigger0Map.get("propertyKey1"));
        assertEquals("propertyValue2", trigger0Map.get("propertyKey2"));

        assertTrue(result.containsKey("slashKey"));
    }
}

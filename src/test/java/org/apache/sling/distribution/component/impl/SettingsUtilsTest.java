package org.apache.sling.distribution.component.impl;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
        Map<String, Object> packageExporterMap = (Map) packageExporterProperties;

        assertTrue(packageExporterMap.containsKey("exporterKey"));
        assertEquals("exporterValue", packageExporterMap.get("exporterKey"));
        assertTrue(packageExporterMap.containsKey("exporterBuilder"));
        assertTrue(packageExporterMap.get("exporterBuilder") instanceof Map);
        Map exporterBuilderMap = (Map) packageExporterMap.get("exporterBuilder");
        assertEquals("builderValue", exporterBuilderMap.get("builderKey"));
        assertTrue(packageExporterMap.containsKey("exporterMap"));
        assertTrue(packageExporterMap.get("exporterMap") instanceof Map);
        Map exporterMap =  (Map) packageExporterMap.get("exporterMap");
        assertEquals("mapvalue1", exporterMap.get("key1"));

        assertTrue(packageExporterMap.containsKey("endpointsArray"));
        assertTrue(packageExporterMap.get("endpointsArray") instanceof List);
        assertEquals("http://abc.com", ((List)packageExporterMap.get("endpointsArray")).get(0));

        assertTrue(result.containsKey("mainKey"));

        assertTrue(result.containsKey("trigger"));
        assertTrue(result.get("trigger") instanceof List);

        List<Map> triggersList = (List<Map>) result.get("trigger");
        assertTrue(triggersList.get(0) instanceof Map);
        Map trigger0Map = triggersList.get(0);
        assertEquals("propertyValue1", trigger0Map.get("propertyKey1"));
        assertEquals("propertyValue2", trigger0Map.get("propertyKey2"));

        assertTrue(result.containsKey("slashKey"));
    }
}

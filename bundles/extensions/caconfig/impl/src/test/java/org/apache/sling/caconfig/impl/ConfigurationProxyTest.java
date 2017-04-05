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
package org.apache.sling.caconfig.impl;

import static org.apache.sling.caconfig.example.AllTypesDefaults.BOOL_DEFAULT;
import static org.apache.sling.caconfig.example.AllTypesDefaults.BOOL_DEFAULT_2;
import static org.apache.sling.caconfig.example.AllTypesDefaults.DOUBLE_DEFAULT;
import static org.apache.sling.caconfig.example.AllTypesDefaults.DOUBLE_DEFAULT_2;
import static org.apache.sling.caconfig.example.AllTypesDefaults.INT_DEFAULT;
import static org.apache.sling.caconfig.example.AllTypesDefaults.INT_DEFAULT_2;
import static org.apache.sling.caconfig.example.AllTypesDefaults.LONG_DEFAULT;
import static org.apache.sling.caconfig.example.AllTypesDefaults.LONG_DEFAULT_2;
import static org.apache.sling.caconfig.example.AllTypesDefaults.STRING_DEFAULT;
import static org.apache.sling.caconfig.example.AllTypesDefaults.STRING_DEFAULT_2;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationResolveException;
import org.apache.sling.caconfig.example.AllTypesConfig;
import org.apache.sling.caconfig.example.IllegalTypesConfig;
import org.apache.sling.caconfig.example.ListConfig;
import org.apache.sling.caconfig.example.NestedConfig;
import org.apache.sling.caconfig.example.SimpleConfig;
import org.apache.sling.caconfig.example.SpecialNamesConfig;
import org.apache.sling.caconfig.example.WithoutAnnotationConfig;
import org.apache.sling.caconfig.impl.ConfigurationProxy.ChildResolver;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

public class ConfigurationProxyTest {
    
    @Rule
    public SlingContext context = new SlingContext();
    
    @Test
    public void testNonExistingConfig_AllTypes() {
        AllTypesConfig cfg = get(null, AllTypesConfig.class);

        assertNull(cfg.stringParam());
        assertEquals(STRING_DEFAULT, cfg.stringParamWithDefault());
        assertEquals(0, cfg.intParam());
        assertEquals(INT_DEFAULT, cfg.intParamWithDefault());
        assertEquals(0L, cfg.longParam());
        assertEquals(LONG_DEFAULT, cfg.longParamWithDefault());
        assertEquals(0d, cfg.doubleParam(), 0.001d);
        assertEquals(DOUBLE_DEFAULT, cfg.doubleParamWithDefault(), 0.001d);
        assertEquals(false, cfg.boolParam());
        assertEquals(BOOL_DEFAULT, cfg.boolParamWithDefault());

        assertArrayEquals(new String[0], cfg.stringArrayParam());
        assertArrayEquals(new String[] {STRING_DEFAULT,STRING_DEFAULT_2}, cfg.stringArrayParamWithDefault());
        assertArrayEquals(new int[0], cfg.intArrayParam());
        assertArrayEquals(new int[] {INT_DEFAULT,INT_DEFAULT_2}, cfg.intArrayParamWithDefault());
        assertArrayEquals(new long[0], cfg.longArrayParam());
        assertArrayEquals(new long[] {LONG_DEFAULT,LONG_DEFAULT_2}, cfg.longArrayParamWithDefault());
        assertArrayEquals(new double[0],cfg.doubleArrayParam(), 0.001d);
        assertArrayEquals(new double[] {DOUBLE_DEFAULT,DOUBLE_DEFAULT_2}, cfg.doubleArrayParamWithDefault(), 0.001d);
        assertArrayEquals(new boolean[0], cfg.boolArrayParam());
        assertArrayEquals(new boolean[] {BOOL_DEFAULT,BOOL_DEFAULT_2}, cfg.boolArrayParamWithDefault());
    }

    @Test
    public void testNonExistingConfig_Nested() {
        NestedConfig cfg = get(null, NestedConfig.class);

        assertNull(cfg.stringParam());
        
        SimpleConfig subConfig = cfg.subConfig();
        assertNull(subConfig.stringParam());
        assertEquals(5, subConfig.intParam());
        assertFalse(subConfig.boolParam());
        
        assertArrayEquals(new ListConfig[0], cfg.subListConfig());
    }

    @Test
    public void testConfig_AllTypes() {
        Resource resource = context.build().resource("/test",
                "stringParam", "configValue2",
                "intParam", 222,
                "longParam", 3456L,
                "doubleParam", 0.123d,
                "boolParam", true,
                "stringArrayParam", new String[] {STRING_DEFAULT_2,STRING_DEFAULT},
                "intArrayParam", new int[] {INT_DEFAULT_2},
                "longArrayParam", new long[] {LONG_DEFAULT_2,LONG_DEFAULT},
                "doubleArrayParam", new double[] {DOUBLE_DEFAULT_2},
                "boolArrayParam", new boolean[] {BOOL_DEFAULT_2,BOOL_DEFAULT})
                .getCurrentParent();
        AllTypesConfig cfg = get(resource, AllTypesConfig.class);

        assertEquals("configValue2", cfg.stringParam());
        assertEquals(STRING_DEFAULT, cfg.stringParamWithDefault());
        assertEquals(222, cfg.intParam());
        assertEquals(INT_DEFAULT, cfg.intParamWithDefault());
        assertEquals(3456L, cfg.longParam());
        assertEquals(LONG_DEFAULT, cfg.longParamWithDefault());
        assertEquals(0.123d, cfg.doubleParam(), 0.001d);
        assertEquals(DOUBLE_DEFAULT, cfg.doubleParamWithDefault(), 0.001d);
        assertEquals(true, cfg.boolParam());
        assertEquals(BOOL_DEFAULT, cfg.boolParamWithDefault());

        assertArrayEquals(new String[] {STRING_DEFAULT_2,STRING_DEFAULT}, cfg.stringArrayParam());
        assertArrayEquals(new String[] {STRING_DEFAULT,STRING_DEFAULT_2}, cfg.stringArrayParamWithDefault());
        assertArrayEquals(new int[] {INT_DEFAULT_2}, cfg.intArrayParam());
        assertArrayEquals(new int[] { INT_DEFAULT, INT_DEFAULT_2}, cfg.intArrayParamWithDefault());
        assertArrayEquals(new long[] {LONG_DEFAULT_2,LONG_DEFAULT}, cfg.longArrayParam());
        assertArrayEquals(new long[] {LONG_DEFAULT,LONG_DEFAULT_2 }, cfg.longArrayParamWithDefault());
        assertArrayEquals(new double[] {DOUBLE_DEFAULT_2}, cfg.doubleArrayParam(), 0.001d);
        assertArrayEquals(new double[] {DOUBLE_DEFAULT,DOUBLE_DEFAULT_2}, cfg.doubleArrayParamWithDefault(), 0.001d);
        assertArrayEquals(new boolean[] {BOOL_DEFAULT_2,BOOL_DEFAULT}, cfg.boolArrayParam());
        assertArrayEquals(new boolean[] {BOOL_DEFAULT,BOOL_DEFAULT_2}, cfg.boolArrayParamWithDefault());
    }

    @Test
    public void testConfig_SpecialNames() {
        Resource resource = context.build()
                .resource("/test",  "stringParam", "configValue2", "int_Param", 222, "bool.Param", true)
                .getCurrentParent();
        SpecialNamesConfig cfg = get(resource, SpecialNamesConfig.class);

        assertEquals("configValue2", cfg.$stringParam());
        assertEquals(222, cfg.int__Param());
        assertEquals(true, cfg.bool_Param());
    }

    @Test
    public void testConfig_Nested() {
        context.build().resource("/test", "stringParam", "v1")
            .resource("/test/subConfig", "stringParam", "v2", "intParam", 444, "boolParam", true)
            .resource("/test/subListConfig/1", "stringParam", "v3.1")
            .resource("/test/subListConfig/2", "stringParam", "v3.2")
            .resource("/test/subListConfig/3", "stringParam", "v3.3")
            .resource("/test/subConfigWithoutAnnotation", "stringParam", "v4");

        Resource resource = context.resourceResolver().getResource("/test");
        
        NestedConfig cfg = get(resource, NestedConfig.class);

        assertEquals("v1", cfg.stringParam());
        
        SimpleConfig subConfig = cfg.subConfig();
        assertEquals("v2", subConfig.stringParam());
        assertEquals(444, subConfig.intParam());
        assertEquals(true, subConfig.boolParam());
        
        ListConfig[] listConfig = cfg.subListConfig();
        assertEquals(3, listConfig.length);
        assertEquals("v3.1", listConfig[0].stringParam());
        assertEquals("v3.2", listConfig[1].stringParam());
        assertEquals("v3.3", listConfig[2].stringParam());

        WithoutAnnotationConfig subConfigWithoutAnnotation = cfg.subConfigWithoutAnnotation();
        assertEquals("v4", subConfigWithoutAnnotation.stringParam());
    }

    @Test(expected=ConfigurationResolveException.class)
    public void testInvalidClassConversion() {
        // test with class not supported for configuration mapping
        get(null, Rectangle2D.class);
    }

    @Test(expected=ConfigurationResolveException.class)
    public void testIllegalTypes_Class() {
        IllegalTypesConfig cfg = get(null, IllegalTypesConfig.class);
        cfg.clazz();
    }
    
    @Test(expected=ConfigurationResolveException.class)
    public void testIllegalTypes_Byte() {
        IllegalTypesConfig cfg = get(null, IllegalTypesConfig.class);
        cfg.byteSingle();
    }
    
    @Test(expected=ConfigurationResolveException.class)
    public void testIllegalTypes_ByteArray() {
        IllegalTypesConfig cfg = get(null, IllegalTypesConfig.class);
        cfg.byteArray();
    }
    
    @Test(expected=ConfigurationResolveException.class)
    public void testIllegalTypes_Short() {
        IllegalTypesConfig cfg = get(null, IllegalTypesConfig.class);
        cfg.shortSingle();
    }
    
    @Test(expected=ConfigurationResolveException.class)
    public void testIllegalTypes_ShortArray() {
        IllegalTypesConfig cfg = get(null, IllegalTypesConfig.class);
        cfg.shortArray();
    }
    
    @Test(expected=ConfigurationResolveException.class)
    public void testIllegalTypes_Float() {
        IllegalTypesConfig cfg = get(null, IllegalTypesConfig.class);
        cfg.floatSingle();
    }
    
    @Test(expected=ConfigurationResolveException.class)
    public void testIllegalTypes_FloatArray() {
        IllegalTypesConfig cfg = get(null, IllegalTypesConfig.class);
        cfg.floatArray();
    }
    
    @Test(expected=ConfigurationResolveException.class)
    public void testIllegalTypes_Char() {
        IllegalTypesConfig cfg = get(null, IllegalTypesConfig.class);
        cfg.charSingle();
    }
    
    @Test(expected=ConfigurationResolveException.class)
    public void testIllegalTypes_CharArray() {
        IllegalTypesConfig cfg = get(null, IllegalTypesConfig.class);
        cfg.charArray();
    }
    
    private <T> T get(Resource resource, Class<T> clazz) {
        return ConfigurationProxy.get(resource, clazz, childResolver(resource));
    }
    
    // simulate simple child resolver without involving ConfigurationResolver implementation
    private ChildResolver childResolver(final Resource resource) {
        return new ChildResolver() {
            @Override
            public <T> T getChild(String configName, Class<T> clazz) {
                Resource child = resource!=null ? resource.getChild(configName) : null;
                return ConfigurationProxy.get(child, clazz, childResolver(child));
            }
            @Override
            public <T> Collection<T> getChildren(String configName, Class<T> clazz) {
                List<T> collection = new ArrayList<>();
                Resource childParent = resource!=null ? resource.getChild(configName) : null;
                if (childParent != null) {
                    for (Resource child : childParent.getChildren()) {
                        T result = ConfigurationProxy.get(child, clazz, childResolver(child));
                        if (result != null) {
                            collection.add(result);
                        }
                    }
                }
                return collection;
            }
        };
    }
    
}

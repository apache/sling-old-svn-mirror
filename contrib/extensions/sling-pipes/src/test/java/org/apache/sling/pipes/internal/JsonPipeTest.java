/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.pipes.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.pipes.AbstractPipeTest;
import org.junit.Before;
import org.junit.Test;

/**
 * testing json pipe with anonymous yahoo meteo API
 */
public class JsonPipeTest extends AbstractPipeTest {
    public static final String CONF = "/content/json/conf/weather";
    public static final String ARRAY = "/content/json/conf/array";

    @Before
    public void setup() {
        super.setup();
        context.load().json("/json.json", "/content/json");
    }

    @Test
    public void testPipedJson() throws Exception{
        Iterator<Resource> outputs = getOutput(CONF);
        outputs.next();
        Resource result = outputs.next();
        context.resourceResolver().commit();
        ValueMap properties = result.adaptTo(ValueMap.class);
        assertTrue("There should be a Paris property", properties.containsKey("Paris"));
        assertTrue("There should be a Bucharest property", properties.containsKey("Bucharest"));
    }

    @Test
    public void testPipedArray() throws Exception {
        Iterator<Resource> outputs = getOutput(ARRAY);
        Resource first = outputs.next();
        Resource second = outputs.next();
        Resource third = outputs.next();
        assertFalse("there should be only three elements", outputs.hasNext());
        assertEquals("first resource should be one", "/content/json/array/one", first.getPath());
        assertEquals("second resource should be two", "/content/json/array/two", second.getPath());
        assertEquals("third resource should be three", "/content/json/array/three", third.getPath());
    }
}

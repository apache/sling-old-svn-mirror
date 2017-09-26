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
import org.apache.sling.pipes.PipeBuilder;
import org.junit.Before;
import org.junit.Test;

import javax.json.JsonObject;

/**
 * testing json pipe with anonymous yahoo meteo API
 */
public class JsonPipeTest extends AbstractPipeTest {
    public static final String CONTENT_JSON = "/content/json";
    public static final String CONF = CONTENT_JSON + "/conf/weather";
    public static final String ARRAY = CONTENT_JSON + "/conf/array";
    public static final String JSON_DUMP = CONTENT_JSON + "/jsonDump";
    public static final String CONTENT_ARRAY = CONTENT_JSON + "/array";

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

    protected void testArray(Iterator<Resource> outputs){
        Resource first = outputs.next();
        Resource second = outputs.next();
        Resource third = outputs.next();
        assertFalse("there should be only three elements", outputs.hasNext());
        assertEquals("first resource should be one", "/content/json/array/one", first.getPath());
        assertEquals("second resource should be two", "/content/json/array/two", second.getPath());
        assertEquals("third resource should be three", "/content/json/array/three", third.getPath());
    }

    @Test
    public void testPipedArray() throws Exception {
        testArray(getOutput(ARRAY));
    }

    @Test
    public void testSimpleJsonPath() throws Exception {
        testJsonPath("{'size':2, 'items':[{'test':'one'}, {'test':'two'}]}", "$.items");
        testJsonPath("[['foo','bar'],[{'test':'one'}, {'test':'two'}]]", "$[1]");
    }
    @Test
    public void testNestedJsonPath()  throws Exception {
        testJsonPath("{'arrays':[['foo','bar'],[{'test':'one'}, {'test':'two'}]]}", "$.arrays[1]");
        testJsonPath("{'objects':{'items':[{'test':'one'}, {'test':'two'}]}}", "$.objects.items");
    }

    protected void testJsonPath(String json, String valuePath) throws Exception {
        assertEquals("there should be 2 results for valuePath " + valuePath, 2, plumber.newPipe(context.resourceResolver())
                .echo("/content/fruits")
                .json(json).with("valuePath", valuePath).name("json")
                .echo("/content/json/array/${json.test}")
                .run().size());
    }
}
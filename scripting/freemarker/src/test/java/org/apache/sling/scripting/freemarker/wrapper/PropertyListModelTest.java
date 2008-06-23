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
package org.apache.sling.scripting.freemarker.wrapper;

import org.apache.sling.scripting.freemarker.FreemarkerTestBase;

import javax.jcr.Value;

/**
 * Test freemarker property list model.
 */
public class PropertyListModelTest extends FreemarkerTestBase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        rootNode.setProperty("text", new String[] {
            "Test-1-" + System.currentTimeMillis(),
            "Test-2-" + System.currentTimeMillis(),
            "Test-3-" + System.currentTimeMillis()
        });
    }

    public void testGetByIndex() throws Exception {
        Value[] values = rootNode.getProperty("text").getValues();
        assertEquals(values[0].getString(), freemarker.evalToString("${node.@text[0]}"));
        assertEquals(values[1].getString(), freemarker.evalToString("${node.@text[1]}"));
        assertEquals(values[2].getString(), freemarker.evalToString("${node.@text[2]}"));
    }

    public void testGetSize() throws Exception {
        assertEquals("3", freemarker.evalToString("${node.@text?size}"));
    }

    public void testIteration() throws Exception {
        String expect = "";
        for (Value value : rootNode.getProperty("text").getValues()) {
            expect += value.getString();
        }
        assertEquals(expect, freemarker.evalToString("<#list node.@text as text>${text}</#list>"));
    }

}

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

import org.apache.commons.collections.IteratorUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.pipes.AbstractPipeTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.cglib.core.CollectionUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Testing traverse pipes and its different configurations on the same resource tree
 */
public class TraversePipeTest extends AbstractPipeTest {
    public static final String ROOT = "/content/traverse";
    public static final String CONF_ROOT = ROOT + "/pipes/";

    @Before
    public void setup() {
        super.setup();
        context.load().json("/traverse.json", ROOT);
    }

    @Test
    public void testDefault() throws Exception{
        assertListEquals(getResourceNameList("default"), "tree", "fruits", "apple", "banana", "vegetables", "leek", "carrot");
    }

    @Test
    public void testBreadth() throws Exception{
        assertListEquals(getResourceNameList("breadth"), "tree", "fruits", "vegetables", "apple", "banana", "leek", "carrot");
    }

    @Test
    public void testProperties() throws Exception{
        Set<String> properties = new HashSet<String>(getResourceNameList("properties"));
        assertTrue("should contains all properties ",
                properties.contains("jcr:primaryType")
                && properties.contains("jcr:description")
                && properties.contains("jcr:title")
                && properties.contains("color"));
    }

    @Test
    public void testSlim() throws Exception{
        assertListEquals(getResourceNameList("slim"), "slim", "tree", "test");
    }

    @Test
    @Ignore //for now nameGlobs is not implemented (see SLING-7089)
    public void testWhiteListProperties() throws Exception {
        List<String> colorList = CollectionUtils.transform(getResourceList("whiteListProperties"), o -> ((Resource)o).adaptTo(String.class));
        assertListEquals(colorList, "green", "yellow", "green", "orange");
    }

    @Test
    public void testDepthLimit() throws Exception{
        assertListEquals(getResourceNameList("depthLimit"), "tree", "fruits", "vegetables");
    }

    List<Resource> getResourceList(String pipeName){
        Iterator<Resource> output = getOutput(CONF_ROOT + pipeName);
        return IteratorUtils.toList(output);
    }
    List<String> getResourceNameList(String pipeName){
        return CollectionUtils.transform(getResourceList(pipeName), o -> ((Resource)o).getName());
    }

    private void assertListEquals(List<String> tested, String... expected){
        assertArrayEquals("arrays should be equals", expected, tested.toArray(new String[tested.size()]));
    }
}
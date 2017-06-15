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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.pipes.AbstractPipeTest;
import org.apache.sling.pipes.Pipe;
import org.junit.Before;
import org.junit.Test;

/**
 * test write
 */
public class WritePipeTest extends AbstractPipeTest {

    public static final String NN_PIPED = "piped";
    public static final String NN_VARIABLE_PIPED = "variablePipe";
    public static final String NN_SIMPLETREE = "simpleTree";

    @Before
    public void setup() {
        super.setup();
        context.load().json("/write.json", PATH_PIPE);
    }

    @Test
    public void testSimple() throws Exception {
        Resource confResource = context.resourceResolver().getResource(PATH_PIPE + "/" + NN_SIMPLE);
        Pipe pipe = plumber.getPipe(confResource);
        assertNotNull("pipe should be found", pipe);
        assertTrue("this pipe should be marked as content modifier", pipe.modifiesContent());
        pipe.getOutput();
        context.resourceResolver().commit();
        ValueMap properties =  context.resourceResolver().getResource("/content/fruits/apple").adaptTo(ValueMap.class);
        assertTrue("There should be hasSeed set to true", properties.get("hasSeed", false));
        assertArrayEquals("Colors should be correctly set", new String[]{"green", "red"}, properties.get("colors", String[].class));
        assertFalse("worm property should be gone (${null} conf)", properties.get("worm", false));
    }

    /**
     *
     * @param resource
     */
    public static void assertPiped(Resource resource) {
        ValueMap properties = resource.adaptTo(ValueMap.class);
        String[] array = new String[]{"cabbage","carrot"};
        assertArrayEquals("Second fruit should have been correctly instantiated & patched, added to the first", new String[]{"apple","banana"}, properties.get("fruits", String[].class));
        assertArrayEquals("Fixed mv should be there", array, properties.get("fixedVegetables", String[].class));
        assertArrayEquals("Expr fixed mv should there and computed", array, properties.get("computedVegetables", String[].class));
    }

    @Test
    public void testPiped() throws Exception {
        Pipe pipe = getPipe(PATH_PIPE + "/" + NN_PIPED);
        assertTrue("this pipe should be marked as content modifier", pipe.modifiesContent());
        Iterator<Resource> it = pipe.getOutput();
        assertTrue("There should be one result", it.hasNext());
        Resource resource = it.next();
        assertNotNull("The result should not be null", resource);
        assertEquals("The result should be the configured one in the piped write pipe", "/content/fruits", resource.getPath());
        context.resourceResolver().commit();
        ValueMap properties = resource.adaptTo(ValueMap.class);
        assertArrayEquals("First fruit should have been correctly instantiated & patched from nothing", new String[]{"apple"}, properties.get("fruits", String[].class));
        assertTrue("There should be a second result awaiting", it.hasNext());
        resource = it.next();
        assertNotNull("The second result should not be null", resource);
        assertEquals("The second result should be the configured one in the piped write pipe", "/content/fruits", resource.getPath());
        context.resourceResolver().commit();
        assertPiped(resource);
    }

    @Test
    public void testVariablePiped() throws Exception {
        Iterator<Resource> it = getOutput(PATH_PIPE + "/" + NN_VARIABLE_PIPED);
        Resource resource = it.next();
        assertEquals("path should be the one configured in first pipe", PATH_PIPE + "/" + NN_VARIABLE_PIPED + "/conf/fruit/conf/apple", resource.getPath());
        context.resourceResolver().commit();
        ValueMap properties = resource.adaptTo(ValueMap.class);
        assertEquals("Configured value should be written", "apple is a fruit and its color is green", properties.get("jcr:description", ""));
        assertEquals("Worm has been removed", "", properties.get("worm", ""));
        Resource archive = resource.getChild("archive/wasthereworm");
        assertNotNull("there is an archive of the worm value", archive);
        assertEquals("Worm value has been written at the same time", "true", archive.adaptTo(String.class));
    }

    @Test
    public void testSimpleTree() throws Exception {
        Pipe pipe = getPipe(PATH_PIPE + "/" + NN_SIMPLETREE);
        assertNotNull("pipe should be found", pipe);
        assertTrue("this pipe should be marked as content modifier", pipe.modifiesContent());
        pipe.getOutput();
        context.resourceResolver().commit();
        Resource appleResource = context.resourceResolver().getResource("/content/fruits/apple");
        ValueMap properties =  appleResource.adaptTo(ValueMap.class);
        assertTrue("There should be hasSeed set to true", properties.get("hasSeed", false));
        assertArrayEquals("Colors should be correctly set", new String[]{"green", "red"}, properties.get("colors", String[].class));
        Node appleNode = appleResource.adaptTo(Node.class);
        NodeIterator children = appleNode.getNodes();
        assertTrue("Apple node should have children", children.hasNext());
    }
}

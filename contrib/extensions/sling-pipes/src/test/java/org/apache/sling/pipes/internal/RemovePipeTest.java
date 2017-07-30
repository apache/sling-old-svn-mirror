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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import javax.jcr.Node;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.pipes.AbstractPipeTest;
import org.apache.sling.pipes.Pipe;
import org.junit.Before;
import org.junit.Test;

/**
 * testing removal
 */
public class RemovePipeTest extends AbstractPipeTest {

    @Before
    public void setUp() throws Exception {
        context.load().json("/remove.json", PATH_PIPE);
    }

    @Test
    public void testRemoveNode() throws Exception{
        ResourceResolver resolver = context.resourceResolver();
        Resource resource = resolver.getResource(PATH_PIPE + "/" + NN_SIMPLE);
        resource.adaptTo(Node.class).setProperty(Pipe.PN_PATH, PATH_BANANA);
        resolver.commit();
        Pipe pipe = plumber.getPipe(resource);
        Iterator<Resource> outputs = pipe.getOutput();
        outputs.next();
        resolver.commit();
        resolver.refresh();
        assertNotNull("Apple should still be here", resolver.getResource(PATH_APPLE));
        assertNull("Banana should not", resolver.getResource(PATH_BANANA));
    }

    @Test
    public void testRemoveProperty() throws Exception {
        String indexPath = PATH_FRUITS + "/index";
        ResourceResolver resolver = context.resourceResolver();
        Resource resource = resolver.getResource(PATH_PIPE + "/" + NN_SIMPLE);
        resource.adaptTo(Node.class).setProperty(Pipe.PN_PATH, indexPath);
        resolver.commit();
        Pipe pipe = plumber.getPipe(resource);
        Iterator<Resource> outputs = pipe.getOutput();
        outputs.next();
        resolver.commit();
        resolver.refresh();
        assertNull("Index property should not be here", resolver.getResource(indexPath));
        assertNotNull("Apple should still be here", resolver.getResource(PATH_APPLE));
        assertNotNull("Banana should still be here", resolver.getResource(PATH_BANANA));
    }

    @Test
    public void testComplexRemove() throws Exception {
        ResourceResolver resolver = context.resourceResolver();
        Resource resource = resolver.getResource(PATH_PIPE + "/" + NN_COMPLEX);
        Pipe pipe = plumber.getPipe(resource);
        assertTrue("resource should be solved", pipe.getOutput().hasNext());
        pipe.getOutput().next();
        resolver.commit();
        assertNull("isnota carrot should be removed", resolver.getResource(PATH_APPLE + "/isnota/carrot"));
        Resource isNotAPea = resolver.getResource(PATH_APPLE + "/isnota/pea");
        assertNotNull("isnota pea should not be removed", isNotAPea);
        assertFalse("isnota pea color property should not be here", isNotAPea.adaptTo(ValueMap.class).containsKey("color"));
    }
}

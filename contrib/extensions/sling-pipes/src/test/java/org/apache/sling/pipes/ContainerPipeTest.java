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
package org.apache.sling.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;

/**
 * testing container with dummy child pipes
 */
public class ContainerPipeTest extends AbstractPipeTest {

    public static final String NN_DUMMYTREE = "dummyTree";
    public static final String NN_OTHERTREE = "otherTree";
    public static final String NN_ROTTENTREE = "rottenTree";
    public static final String NN_ONEPIPE = "onePipeContainer";

    @Before
    public void setup() {
        super.setup();
        context.load().json("/container.json", PATH_PIPE);
    }

    @Test
    public void testDummyTree() throws Exception {
        ContainerPipe pipe = (ContainerPipe)getPipe(PATH_PIPE + "/" + NN_DUMMYTREE);
        assertNotNull("A container pipe should be built out from the given configuration", pipe);
        Iterator<Resource> resourceIterator = pipe.getOutput();
        assertTrue("There should be some results", resourceIterator.hasNext());
        Resource firstResource = resourceIterator.next();
        assertNotNull("First resource should not be null", firstResource);
        assertEquals("First resource should be instantiated path with apple & pea",
                PATH_FRUITS + "/apple/isnota/pea/buttheyhavesamecolor",
                firstResource.getPath());
        assertTrue("There should still be another item", resourceIterator.hasNext());
        Resource secondResource = resourceIterator.next();
        assertNotNull("Second resource should not be null", secondResource);
        assertEquals("Second resource should be instantiated path with apple & carrot",
                PATH_FRUITS + "/apple/isnota/carrot/andtheircolorisdifferent",
                secondResource.getPath());
        assertTrue("There should still be another item", resourceIterator.hasNext());
        Resource thirdResource = resourceIterator.next();
        assertNotNull("Third resource should not be null", thirdResource);
        assertEquals("Third resource should be instantiated path with banana & pea",
                PATH_FRUITS + "/banana/isnota/pea/andtheircolorisdifferent",
                thirdResource.getPath());
        assertTrue("There should still be another item", resourceIterator.hasNext());
        Resource fourthResource = resourceIterator.next();
        assertNotNull("Fourth resource should not be null", fourthResource);
        assertEquals("fourthResource resource should be instantiated path with banana & carrot",
                PATH_FRUITS + "/banana/isnota/carrot/andtheircolorisdifferent",
                fourthResource.getPath());
        assertFalse("There should be no more items", resourceIterator.hasNext());
    }

    @Test
    public void testOtherTree() throws Exception {
        ContainerPipe pipe = (ContainerPipe)getPipe(PATH_PIPE + "/" + NN_OTHERTREE);
        Iterator<Resource> resourceIterator = pipe.getOutput();
        assertTrue("There should be some results", resourceIterator.hasNext());
        Resource firstResource = resourceIterator.next();
        assertNotNull("First resource should not be null", firstResource);
        assertEquals("First resource should be instantiated path with apple & pea",
                PATH_FRUITS + "/apple/isnota/pea/buttheyhavesamecolor",
                firstResource.getPath());
        assertTrue("There should still be another item", resourceIterator.hasNext());
        Resource secondResource = resourceIterator.next();
        assertNotNull("Second resource should not be null", secondResource);
        assertEquals("Second resource should be instantiated path with banana & pea",
                PATH_FRUITS + "/banana/isnota/pea/andtheircolorisdifferent",
                secondResource.getPath());
        assertFalse("There should be no more items", resourceIterator.hasNext());
    }

    @Test
    public void testRottenTree() throws Exception {
        assertFalse("There shouldn't be any resource", getOutput(PATH_PIPE + "/" + NN_ROTTENTREE).hasNext());
    }

    @Test
    public void testOnePipe() throws Exception {
        assertTrue("There should be children", getOutput(PATH_PIPE + "/" + NN_ONEPIPE).hasNext());
    }
}
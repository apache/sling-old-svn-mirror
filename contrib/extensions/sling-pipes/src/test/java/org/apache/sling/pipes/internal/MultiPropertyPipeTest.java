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
import static org.junit.Assert.assertNotNull;

import java.util.Iterator;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.pipes.AbstractPipeTest;
import org.apache.sling.pipes.Pipe;
import org.junit.Before;
import org.junit.Test;

/**
 * testing normal case of MV Property Pipe
 */
public class MultiPropertyPipeTest extends AbstractPipeTest {

    @Before
    public void setup() throws PersistenceException {
        super.setup();
        context.load().json("/multiProperty.json", PATH_PIPE);
    }

    @Test
    public void testMV() throws Exception{
        Pipe pipe = getPipe(PATH_PIPE + "/working");
        Iterator<Resource> outputs = pipe.getOutput();
        Resource resource = outputs.next();
        assertNotNull(resource);
        assertEquals("Resource path should be the input", PATH_FRUITS + PN_INDEX, resource.getPath());
        String fruit = (String)pipe.getOutputBinding();
        assertEquals("First resource binding should be apple", "apple", fruit);
        resource = outputs.next();
        assertNotNull(resource);
        assertEquals("Resource path should be the input", PATH_FRUITS + PN_INDEX, resource.getPath());
        fruit = (String)pipe.getOutputBinding();
        assertEquals("Second resource binding should be banana", "banana", fruit);
    }

    @Test
    public void testNonWorkingMV() throws Exception{
        assertFalse("There should not be next for a non existing resource", getOutput(PATH_PIPE + "/typo").hasNext());
        assertFalse("There should not be next for a resource that is not a property", getOutput(PATH_PIPE + "/notProperty").hasNext());
        assertFalse("There should not be next for a property that is not multiple", getOutput(PATH_PIPE + "/notMultiple").hasNext());
    }
}

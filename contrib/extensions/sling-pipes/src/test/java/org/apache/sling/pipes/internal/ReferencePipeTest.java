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
import static org.junit.Assert.assertTrue;

import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.pipes.AbstractPipeTest;
import org.apache.sling.pipes.Pipe;
import org.apache.sling.pipes.ReferencePipe;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * testing references
 */
public class ReferencePipeTest  extends AbstractPipeTest {

    @Before
    public void setUp() throws Exception {
        context.load().json("/reference.json", PATH_PIPE);
    }

    @Test
    public void testSimple() throws Exception {
        testOneResource(PATH_PIPE + "/" + NN_SIMPLE, PATH_APPLE);
    }

    @Test
    public void testRefersFailure() throws Exception {
        assertFalse("Reference a pipe with no result shouldn't have any result", getOutput(PATH_PIPE + "/refersfailure").hasNext());
    }

    @Test
    public void testPathBinding() throws Exception {
        testOneResource(PATH_PIPE + "/testPathBinding", PATH_APPLE + "/isnota/carrot");
    }

    @Test
    public void testValueBinding() throws Exception {
        testOneResource(PATH_PIPE + "/isAppleWormy", PATH_APPLE);
    }

    @Test
    public void testBuilderWithAdditionalBinding() throws Exception {
        String newFruit = "watermelon";
        String newPath = PATH_FRUITS + "/" + newFruit;
        ResourceUtil.getOrCreateResource(context.resourceResolver(), newPath, "nt:unstructured", "nt:unstructured", true);
        Map bindings = new HashMap();
        bindings.put("fruit", newFruit);
        Pipe pipe = plumber.newPipe(context.resourceResolver()).echo(PATH_FRUITS + "/${fruit}").build();
        Set<String> paths = plumber.newPipe(context.resourceResolver())
                .pipe(ReferencePipe.RESOURCE_TYPE)
                .expr(pipe.getResource().getPath())
                .run(bindings);
        assertTrue("paths should contain new path", paths.contains(newPath));
    }
}
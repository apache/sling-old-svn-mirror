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

import org.apache.sling.api.resource.ValueMap;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class PipeBuilderTest extends AbstractPipeTest {
    @Test
    public void simpleBuild() throws Exception {
        PipeBuilder rmBuilder = plumber.getBuilder(context.resourceResolver());
        Pipe rmPipe = rmBuilder.echo(PATH_APPLE).rm().build();
        assertNotNull(" a pipe should be built", rmPipe);
        //we rebuild pipe out of created pipe path, execute it, and test correct output (= correct pipe built)
        testOneResource(rmPipe.getResource().getPath(), PATH_FRUITS);
    }

    @Test
    public void run() throws Exception {
        String lemonPath = "/content/fruits/lemon";
        PipeBuilder lemonBuilder = plumber.getBuilder(context.resourceResolver());
        Set<String> paths = lemonBuilder.mkdir(lemonPath).run();
        assertTrue("returned set should contain lemon path", paths.contains(lemonPath));
        assertNotNull("there should be a lemon created", context.resourceResolver().getResource(lemonPath));
    }

    @Test
    public void confBuild() throws Exception {
        PipeBuilder writeBuilder = plumber.getBuilder(context.resourceResolver());
        writeBuilder.echo(PATH_APPLE).write("tested", true, "working", true).run();
        ValueMap properties = context.resourceResolver().getResource(PATH_APPLE).adaptTo(ValueMap.class);
        assertTrue("properties should have been written", properties.get("tested", false) && properties.get("working", false));
    }

    @Test
    public void bindings() throws Exception {
        PipeBuilder defaultNames = plumber.getBuilder(context.resourceResolver());
        Set<String> paths = defaultNames
                .echo(PATH_FRUITS)
                .$("nt:unstructured")
                .filter("slingPipesFilter_test","${two.worm}")
                .$("nt:unstructured#isnota")
                .$("nt:unstructured").name("thing")
                .write("jcr:path", "${path.thing}").run();
        assertEquals("There should be only one resource", 2, paths.size());
        String pea = "/content/fruits/apple/isnota/pea";
        String carrot = "/content/fruits/apple/isnota/carrot";
        assertTrue("the paths should contain " + pea, paths.contains(pea));
        assertTrue("the paths should contain " + carrot, paths.contains(carrot));
        for (String path : paths){
            String writtenPath = context.resourceResolver().getResource(path).adaptTo(ValueMap.class).get("jcr:path", String.class);
            assertEquals("written path should be the same as actual path", path, writtenPath);
        }
    }

}
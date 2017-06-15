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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.pipes.dummies.DummyNull;
import org.apache.sling.pipes.dummies.DummySearch;
import org.apache.sling.pipes.internal.PlumberImpl;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;

/**
 * this abstract class for pipes implements a plumber with all registered pipes, plus some test ones, and give some paths,
 * it also provides a testing Sling Context, with some content.
 */
public class AbstractPipeTest {

    protected static final String PATH_PIPE = "/etc/pipe";
    protected static final String PATH_FRUITS = "/content/fruits";
    protected static final String PATH_BANANA = PATH_FRUITS + "/banana";
    protected static final String PATH_APPLE = PATH_FRUITS + "/apple";
    protected static final String NN_SIMPLE = "simple";
    protected static final String NN_COMPLEX = "complex";
    protected static final String PN_INDEX = "/index";

    protected Plumber plumber;

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    @Before
    public void setup(){
        PlumberImpl plumberImpl = new PlumberImpl();
        PlumberImpl.Configuration configuration = mock(PlumberImpl.Configuration.class);
        when(configuration.authorizedUsers()).thenReturn(new String[]{});
        when(configuration.serviceUser()).thenReturn(null);
        when(configuration.bufferSize()).thenReturn(PlumberImpl.DEFAULT_BUFFER_SIZE);
        plumberImpl.activate(configuration);
        plumberImpl.registerPipe("slingPipes/dummyNull", DummyNull.class);
        plumberImpl.registerPipe("slingPipes/dummySearch", DummySearch.class);
        plumber = plumberImpl;
        context.load().json("/fruits.json", PATH_FRUITS);
    }

    protected Pipe getPipe(String path){
        Resource resource = context.resourceResolver().getResource(path);
        return plumber.getPipe(resource);
    }

    protected Iterator<Resource> getOutput(String path){
        Pipe pipe = getPipe(path);
        assertNotNull("pipe should be found", pipe);
        return pipe.getOutput();
    }

    /**
     * tests given pipe (pipePath) outputs at least one resource, which path is resourcepath
     * @param pipePath
     * @param resourcePath
     */
    protected void testOneResource(String pipePath, String resourcePath){
        Iterator<Resource> it = getOutput(pipePath);
        assertTrue("pipe should have results", it.hasNext());
        assertEquals("return result should be the one expected", resourcePath, it.next().getPath());
    }
}

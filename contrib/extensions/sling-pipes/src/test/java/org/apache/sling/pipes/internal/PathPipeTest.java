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

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.pipes.AbstractPipeTest;
import org.apache.sling.pipes.Pipe;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Testing path pipe using pipe builder
 */
public class PathPipeTest extends AbstractPipeTest {

    private static final String WATERMELON = "watermelon";
    private static final String WATERMELON_FULL_PATH = PATH_FRUITS + "/" + WATERMELON;

    @Test
    public void modifiesContent() throws IllegalAccessException, PersistenceException {
        Pipe pipe = plumber.newPipe(context.resourceResolver())
                .mkdir(PATH_FRUITS + "/whatever")
                .build();
        assertTrue("path pipe should be considered as modifying the content", pipe.modifiesContent());
    }

    @Test
    public void getClassicOutput() throws Exception {
        ResourceResolver resolver = context.resourceResolver();
        plumber.newPipe(resolver).mkdir(WATERMELON_FULL_PATH).run();
        resolver.revert();
        assertNotNull("Resource should be here & saved", resolver.getResource(WATERMELON_FULL_PATH));
    }

    @Test
    public void getRelativePath() throws Exception {
        ResourceResolver resolver = context.resourceResolver();
        plumber.newPipe(resolver).echo(PATH_FRUITS).mkdir(WATERMELON).run();
        assertNotNull("Resource should be    here & saved", resolver.getResource(WATERMELON_FULL_PATH));
    }
}
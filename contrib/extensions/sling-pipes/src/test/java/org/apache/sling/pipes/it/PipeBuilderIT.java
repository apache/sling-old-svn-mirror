/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.pipes.it;

import java.util.Set;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.pipes.Pipe;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * testing explicitly the pipe builder
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class PipeBuilderIT extends PipesTestSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlumberTestIT.class);

    @Test
    public void traverseTest() throws Exception {
        final String CONTENT = "/content/traverse/test";
        try (ResourceResolver resolver = resolver()) {
            mkdir(resolver, CONTENT);
            Set<String> results = plumber.newPipe(resolver).echo(CONTENT).traverse().run();
            LOGGER.info("Following results are found {}", results);
            assertTrue("should contain former test", results.contains(CONTENT));
        }
    }

    @Test
    public void mvTest() throws Exception {
        final String CONTENT = "/content/mv/" + NN_TEST;
        final String TARGET = "/content/target";
        final String TARGET_PATH = TARGET + "/" + NN_TEST;
        try (ResourceResolver resolver = resolver()) {
            mkdir(resolver, CONTENT);
            mkdir(resolver, TARGET);
            Set<String> results = plumber.newPipe(resolver).echo(CONTENT).mv(TARGET_PATH).run();
            LOGGER.info("Following results are found {}", results);
            assertTrue("mv return should be the moved item", results.contains(TARGET_PATH));
        }
    }

    @Test
    public void xpathTest() throws Exception {
        final String ROOT = "/content/xpath";
        final int NB_ITEMS = 10;
        try (final ResourceResolver resolver = resolver()) {
            for (int i = 0; i < NB_ITEMS; i++) {
                final String path = String.format("%s/%s/%s", ROOT, i, NN_TEST);
                plumber.newPipe(resolver).mkdir(path).write("xpathTestStatus", "testing").run();
            }
            final String query = String.format("/jcr:root%s//element(*,nt:base)[@xpathTestStatus]", ROOT);
            final Set<String> results = plumber.newPipe(resolver).xpath(query).run();
            assertEquals("xpath query should return as many items as we wrote", NB_ITEMS, results.size());
        }
    }

    @Test
    public void referenceWithBindings() throws Exception {
        final String ROOT = "/content/reference";
        try (ResourceResolver resolver = resolver()) {
            Pipe pipe = plumber.newPipe(resolver).mkdir(ROOT + "/test-${testedBinding}").write("jcr:title","${testedBinding}").build();
            for (int i = 0; i < 10; i ++) {
                plumber.newPipe(resolver).ref(pipe.getResource().getPath()).runWith("testedBinding", i);
            }
            Set<String> results = plumber.newPipe(resolver).echo(ROOT).traverse().run();
            LOGGER.info("Following results are found {}", results);
            assertEquals("we should have root and implemented children", 11, results.size());
        }
    }
}

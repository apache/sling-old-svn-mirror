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

import org.apache.sling.pipes.dummies.DummyNull;
import org.apache.sling.pipes.dummies.DummySearch;
import org.apache.sling.pipes.impl.PlumberImpl;
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
    Plumber plumber;

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    @Before
    public void setup(){
        PlumberImpl plumberImpl = new PlumberImpl();
        plumberImpl.activate();
        plumberImpl.registerPipe("slingPipes/dummyNull", DummyNull.class);
        plumberImpl.registerPipe("slingPipes/dummySearch", DummySearch.class);
        plumber = plumberImpl;
        context.load().json("/fruits.json", PATH_FRUITS);
    }

}

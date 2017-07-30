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

import static org.junit.Assert.assertTrue;

import org.apache.sling.pipes.AbstractPipeTest;
import org.junit.Before;
import org.junit.Test;

/**
 * test the sling query pipe
 */
public class SlingQueryPipeTest extends AbstractPipeTest {

    @Before
    public void setup() {
        super.setup();
        context.load().json("/users.json", "/content/users");
        context.load().json("/slingQuery.json", PATH_PIPE);
    }

    @Test
    public void testChildren() throws Exception {
        assertTrue("this pipe should have an output", getOutput(PATH_PIPE + "/" + NN_SIMPLE).hasNext());
    }
}

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

import org.apache.sling.pipes.AbstractPipeTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class NotPipeTest extends AbstractPipeTest {

    @Before
    public void setUp() throws Exception {
        context.load().json("/reference.json", PATH_PIPE);
    }

    @Test
    public void testTrue() throws Exception {
        assertFalse("working referred pipe should make not pipe fail", getOutput(PATH_PIPE + "/not").hasNext());
    }

    @Test
    public void testFalse() throws Exception {
        //not working referred pipe should stream input of the not pipe
        testOneResource(PATH_PIPE + "/notfailure", PATH_APPLE);
    }
}

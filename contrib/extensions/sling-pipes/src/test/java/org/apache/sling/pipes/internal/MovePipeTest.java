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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.pipes.AbstractPipeTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.jcr.Session;
import java.util.Iterator;

/**
 * testing moving nodes & properties
 */
public class MovePipeTest extends AbstractPipeTest {

    static final String MOVENODE_PIPE = "/moveNode";
    static final String MOVEPROPERTY_PIPE = "/moveProperty";
    static final String MOVED_NODE_PATH = "/granny";
    static final String MOVED_PROPERTY_PATH = "/indexFruits";

    @Before
    public void setup() {
        super.setup();
        context.load().json("/move.json", PATH_PIPE);
    }

    @Ignore //move operation is not supported yet by MockSession
    @Test
    public void testMoveNode() throws Exception {
        Iterator<Resource> output = getOutput(PATH_PIPE + MOVENODE_PIPE);
        Assert.assertTrue(output.hasNext());
        output.next();
        Session session = context.resourceResolver().adaptTo(Session.class);
        session.save();
        Assert.assertTrue("new node path should exists", session.nodeExists(PATH_FRUITS + MOVED_NODE_PATH));
    }

    @Ignore //move operation is not supported yet by MockSession
    @Test
    public void testMoveProperty() throws Exception {
        Iterator<Resource> output = getOutput(PATH_PIPE + MOVEPROPERTY_PIPE);
        Assert.assertTrue(output.hasNext());
        output.next();
        Session session = context.resourceResolver().adaptTo(Session.class);
        session.save();
        Assert.assertTrue("new property path should exists", session.propertyExists(PATH_FRUITS + MOVED_NODE_PATH));
        Assert.assertFalse("old property path should not", session.propertyExists(PATH_FRUITS + PN_INDEX));
    }
}

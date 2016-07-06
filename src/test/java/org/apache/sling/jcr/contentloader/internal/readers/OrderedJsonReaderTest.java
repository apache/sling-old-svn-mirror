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
package org.apache.sling.jcr.contentloader.internal.readers;

import org.apache.sling.jcr.contentloader.internal.JsonReaderTest;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JMock;
import org.junit.runner.RunWith;

/**
 * testing specific ordered json import case:
 * - should work with all normal json cases,
 * - should work for specific ordered case
 */
@RunWith(JMock.class)
public class OrderedJsonReaderTest extends JsonReaderTest {

    @Override
    protected void setReader() {
        this.jsonReader = new OrderedJsonReader();
    }

    @org.junit.Test public void testTwoOrderedChildren() throws Exception {
        String json = "{ " +
                " 'SLING:ordered' : [" +
                        "{ 'SLING:name': c1}," +
                        "{ 'SLING:name': c2}" +
                    "]" +
                "}";
        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).createNode("c1", null, null); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
            allowing(creator).createNode("c2", null, null); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }
}

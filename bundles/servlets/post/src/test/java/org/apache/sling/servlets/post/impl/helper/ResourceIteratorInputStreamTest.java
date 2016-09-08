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

package org.apache.sling.servlets.post.impl.helper;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.sling.MockResource;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ieb on 06/09/2016.
 */
public class ResourceIteratorInputStreamTest {

    @Test
    public void test() throws IOException {
        List<Resource> resources = new ArrayList<Resource>();
        for (int i = 0; i < 10; i++ ) {
            final int initialState = i;
            final InputStream in = new InputStream() {
                private int state = initialState;
                @Override
                public int read() throws IOException {
                    return state--;
                }
            };
            resources.add(new MockResource(null,null,null){
                @Override
                public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
                    if (InputStream.class.equals(type)) {
                        return (AdapterType) in;
                    }
                    return super.adaptTo(type);
                }

                @Override
                public String getName() {
                    return "chunk-"+(initialState*100)+"-"+(((initialState+1)*100)-1);
                }
            });
        }
        ResourceIteratorInputStream resourceIteratorInputStream = new ResourceIteratorInputStream(resources.iterator());
        int expected = 0;
        int cycle = 0;
        for(int i = resourceIteratorInputStream.read(); i >= 0; i = resourceIteratorInputStream.read()) {
            Assert.assertEquals(expected, i);
            if ( expected == 0 ) {
                cycle++;
                expected=cycle;
            } else {
                expected--;
            }
        }
        Assert.assertEquals(10,cycle);
    }
}

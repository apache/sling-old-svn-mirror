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
package org.apache.sling.bgservlets.impl.nodestream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class NodeStreamPathTest {
    
    @Test
    public void testNullOnFirstCall() {
        final NodeStreamPath nsp = new NodeStreamPath();
        assertNull(nsp.getNodePath());
    }
    
    @Test
    public void testPaths() {
        final String [] data = {
            "1", "1",
            "9", "9",
            "99", "99",
            "100", "10/0",
            "199", "19/9",
            "200", "20/0",
            "1234", "12/34",
        };
        
        for(int i=0; i < data.length; i += 2) {
            final NodeStreamPath nsp = new NodeStreamPath();
            for(int j = 0; j < Integer.parseInt(data[i]); j++) {
                nsp.selectNextPath();
            }
            final String exp = data[i+1];
            assertEquals("At index " + i + ", expected " + exp, exp, nsp.getNodePath());
        }
    }
}

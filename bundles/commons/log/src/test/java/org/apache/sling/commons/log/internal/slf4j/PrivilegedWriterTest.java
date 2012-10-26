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

package org.apache.sling.commons.log.internal.slf4j;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PrivilegedWriterTest {

    @Mock
    private Writer mockWriter;

    public PrivilegedWriterTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPrivilegedWriter() throws IOException {
        StringWriter sw = new StringWriter();
        PrivilegedWriter p = new PrivilegedWriter(sw);
        char[] c = "Testing".toCharArray();
        p.write(c, 0, c.length);
        p.flush();
        p.close();
        Assert.assertEquals("Testing", sw.toString());
    }

    // Not certain how to test failures here. Mocking the Security manager doesnt work.

}

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
package org.apache.sling.servlets.post.impl.operations;

import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostResponse;

public class AbstractCreateOperationTest extends TestCase {

    private AbstractCreateOperation op = new AbstractCreateOperation() {

        @Override
        protected void doRun(SlingHttpServletRequest request,
                PostResponse response, List<Modification> changes) {
            // none here
        }
    };

    public void test_ignoreParameter() throws Exception {
        Method ip = getMethod("ignoreParameter", String.class);

        // default setup without matching regexp
        assertEquals(true, ip.invoke(op, "_charset_"));
        assertEquals(true, ip.invoke(op, ":operation"));
        assertEquals(false, ip.invoke(op, "j_username"));
        assertEquals(false, ip.invoke(op, "j_password"));
        assertEquals(false, ip.invoke(op, "some_random_j_name"));

        // setup: j_.*
        op.setIgnoredParameterNamePattern(Pattern.compile("j_.*"));
        assertEquals(true, ip.invoke(op, "_charset_"));
        assertEquals(true, ip.invoke(op, ":operation"));
        assertEquals(true, ip.invoke(op, "j_username"));
        assertEquals(true, ip.invoke(op, "j_password"));
        assertEquals(false, ip.invoke(op, "some_random_j_name"));

        // setup: .*j_.*
        op.setIgnoredParameterNamePattern(Pattern.compile(".*j_.*"));
        assertEquals(true, ip.invoke(op, "_charset_"));
        assertEquals(true, ip.invoke(op, ":operation"));
        assertEquals(true, ip.invoke(op, "j_username"));
        assertEquals(true, ip.invoke(op, "j_password"));
        assertEquals(true, ip.invoke(op, "some_random_j_name"));

        // setup: .+j_.*
        op.setIgnoredParameterNamePattern(Pattern.compile(".+j_.*"));
        assertEquals(true, ip.invoke(op, "_charset_"));
        assertEquals(true, ip.invoke(op, ":operation"));
        assertEquals(false, ip.invoke(op, "j_username"));
        assertEquals(false, ip.invoke(op, "j_password"));
        assertEquals(true, ip.invoke(op, "some_random_j_name"));
    }

    private Method getMethod(String name, Class... parameterTypes) {
        try {
            Method m = AbstractCreateOperation.class.getDeclaredMethod(name,
                parameterTypes);
            m.setAccessible(true);
            return m;
        } catch (Throwable t) {
            fail(t.toString());
            return null; // compiler wants this
        }
    }

}

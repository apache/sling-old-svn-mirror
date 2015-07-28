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
package org.apache.sling.bgservlets.impl.storage;

import static org.junit.Assert.fail;

import java.util.Hashtable;
import java.util.regex.Pattern;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

public class JobStorageImplTest {
    private JobStorageImpl storage;
    private final Mockery mockery = new Mockery(); 

    @Before
    public void setup() {
        storage = new JobStorageImpl();
        
        final Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("job.storage.path", "/var/test");
        
        final ComponentContext ctx = mockery.mock(ComponentContext.class);
        mockery.checking(new Expectations() {{
            allowing(ctx).getProperties();
            will(returnValue(props));
        }});
        
        storage.activate(ctx);
    }
    
    private void assertPath(String regex, String actual) {
        if(!Pattern.compile(regex).matcher(actual).matches()) {
            fail("Path " + actual + " does not match expected regex " + regex);
        }
    }
    
    @Test
    public void testNextPath() {
        assertPath("/var/test/2.*/1$", storage.getNextPath());
        assertPath("/var/test/2.*/2$", storage.getNextPath());
    }
}
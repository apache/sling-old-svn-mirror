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
package org.apache.sling.jcr.jcrinstall.osgi.impl;

import java.util.Dictionary;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ConfigResourceProcessorTest {
    
    private Mockery mockery;
    
    @org.junit.Before public void setup() {
        mockery = new Mockery();
    }
    
    @org.junit.Test public void testConfigPathProperty() throws Exception {
        final String path = "/foo/bar/config/somenode";
        final String data = "foo = bar";
        final MockInstallableData id = new MockInstallableData(path, data);
        final ConfigurationAdmin ca = mockery.mock(ConfigurationAdmin.class);
        final ConfigResourceProcessor p = new ConfigResourceProcessor(ca);
        final Configuration c = mockery.mock(Configuration.class);
        final String pid = "dummyConfigPid";
        
        final Matcher<Dictionary<?,?>> matcher = new BaseMatcher<Dictionary<?,?>> () {

            public boolean matches(Object item) {
                final Dictionary<?, ?> d = (Dictionary<?, ?>)item;
                boolean result = "bar".equals(d.get("foo"));
                result &= path.equals(d.get("_jcr_config_path"));
                return result;
            }

            public void describeTo(Description description) {
                description.appendText("Config Dictionary contains foo and _jcr_config_path properties");
            }
        };
        
        mockery.checking(new Expectations() {{
            allowing(ca).getConfiguration(with(any(String.class)), with(any(String.class)));
            will(returnValue(c));
            allowing(c).getPid();
            will(returnValue(pid));
            allowing(c).getBundleLocation();
            will(returnValue(null));
            allowing(c).update(with(matcher));
        }});
        
        p.installOrUpdate(path, null, id);
    }
}

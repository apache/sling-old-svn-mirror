/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.impl;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLogEntry;
import org.junit.Test;

public class HealthCheckMBeanTest {
    private final MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
    private boolean resultOk;
    private static final Map<String, String> info = new HashMap<String, String>();
    public static final String OBJECT_NAME = "org.apache.sling.testing:type=HealthCheckMBeanTest";
    
    static {
        info.put("foo",  "bar");
        info.put("Another entry",  "ok");
    }
    
    private HealthCheck testHealthCheck = new HealthCheck() {

        @Override
        public Result execute() {
            final Result result = new Result();
            if(resultOk) {
                result.log(ResultLogEntry.LT_DEBUG, "Nothing to report, result ok");
            } else {
                result.log(ResultLogEntry.LT_WARN, "Result is not ok!");
            }
            return result;
        }

        @Override
        public Map<String, String> getInfo() {
            return info;
        }
    };
    
    @Test
    public void testBean() throws Exception {
        final HealthCheckMBean mbean = new HealthCheckMBean(testHealthCheck);
        final ObjectName name = new ObjectName(OBJECT_NAME);
        jmxServer.registerMBean(mbean, name);
        try {
            JmxAttributeHealthCheckTest.assertJmxValue(OBJECT_NAME, "foo", "bar", true);
            JmxAttributeHealthCheckTest.assertJmxValue(OBJECT_NAME, "Another entry", "ok", true);
            
            resultOk = true;
            JmxAttributeHealthCheckTest.assertJmxValue(OBJECT_NAME, "ok", "true", true);
            
            resultOk = false;
            JmxAttributeHealthCheckTest.assertJmxValue(OBJECT_NAME, "ok", "true", false);
            
            JmxAttributeHealthCheckTest.assertJmxValue(OBJECT_NAME, "log", "contains message=Result is not ok!", true);
        } finally {
            jmxServer.unregisterMBean(name);
        }
    }

}
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
package org.apache.sling.contextaware.config.it.util;

import static org.junit.Assert.fail;

import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.testing.tools.sling.TimeoutsProvider;

public class WaitFor {
    /** Block until the specified set of serviceClasss are present, or timeout */
    public static void services(TeleporterRule teleporter, Class <?> ... serviceClasses) {
        for(Class<?> serviceClass : serviceClasses) {
            service(teleporter, serviceClass);
        }
    }
    
    /** Block until the specified serviceClass is present, or timeout */
    public static void service(TeleporterRule teleporter, Class <?> serviceClass) {
        final long timeout = System.currentTimeMillis() + TimeoutsProvider.getInstance().getTimeout(10000);
        while(System.currentTimeMillis() < timeout) {
            try {
                if(teleporter.getService(serviceClass) != null) {
                    return;
                }
            } catch(IllegalStateException ignore) {
            }
            
            try {
                Thread.sleep(50L);
            } catch(InterruptedException ignore) {
            }
        }
        fail("Timeout waiting for serviceClass " + serviceClass.getName());
    }
}

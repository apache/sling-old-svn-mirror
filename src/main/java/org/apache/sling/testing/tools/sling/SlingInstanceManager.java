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
package org.apache.sling.testing.tools.sling;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Helper class for running tests against multiple Sling instances,
 *  takes care of starting the Sling instances and waiting for them to be ready.
 */
public class SlingInstanceManager {
    private final Map<String, SlingInstance> slingTestInstances = new ConcurrentHashMap<String, SlingInstance>();

    public SlingInstanceManager(String... instanceNames) {
        this(System.getProperties(), instanceNames);
    }

    /** Get configuration but do not start server yet, that's done on demand */
    public SlingInstanceManager(Properties systemProperties, String... instanceNames) {
        if (instanceNames == null || instanceNames.length == 0) {
            instanceNames = new String [] { SlingInstanceState.DEFAULT_INSTANCE_NAME };
        }

        for (String instanceName : instanceNames) {
            Properties instanceProperties = removeInstancePrefix(systemProperties, instanceName);

            SlingInstanceState state = SlingInstanceState.getInstance(instanceName);
            SlingInstance instance = new SlingTestBase(state, instanceProperties);
            slingTestInstances.put(instanceName, instance);
        }
    }


    private Properties removeInstancePrefix(Properties properties, String instanceName) {
        Properties result = new Properties();
        for (Object propertyKey : properties.keySet()) {
            Object propertyValue = properties.get(propertyKey);

            if (propertyKey instanceof String) {
                String propertyName = (String) propertyKey;
                String instancePropertyName = null;
                if (propertyName.startsWith(instanceName + ".")) {
                    instancePropertyName = propertyName.substring(instanceName.length()+1);
                }

                if (instancePropertyName != null) {
                    result.put(instancePropertyName, propertyValue);
                }
                else if (!result.containsKey(propertyName)) {
                    result.put(propertyName, propertyValue);
                }
            }
            else {
                result.put(propertyKey, propertyValue);

            }
        }

        return result;
    }


    public SlingInstance getInstance(String instanceName) {
        return slingTestInstances.get(instanceName);
    }

    public Collection<SlingInstance> getInstances() {
        return slingTestInstances.values();
    }
}
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.tools.serversetup;

import java.util.Properties;

/** In general we just need a singleton ServerSetup, that
 *  uses System properties for its configuration - this class
 *  supplies that.
 */
public class ServerSetupSingleton {
    
    /** Property name of the ServerSetup class that we instantiate */
    public static final String CLASS_NAME_PROP = ServerSetup.PROP_NAME_PREFIX + ".class.name";
    
    private static ServerSetup instance;
    
    /** Create an instance based on the <code>@CLASS_NAME_PROP</code>
     *  property if needed and return it.
     *  
     *  @param config Ignored unless an instance is created
     */
    public static ServerSetup instance(Properties config) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if(instance == null) {
            synchronized (ServerSetupSingleton.class) {
                if(instance == null) {
                    final String className = config.getProperty(CLASS_NAME_PROP);
                    if(className == null) {
                        throw new IllegalArgumentException("Missing config property: " + CLASS_NAME_PROP);
                    }
                    instance = (ServerSetup)
                        ServerSetupSingleton.class.getClassLoader()
                        .loadClass(className)
                        .newInstance();
                    instance.setConfig(config);
                }
            }
        }
        return instance;
    }
    
    /** Same as no-parameter instance() method, but uses System properties
     *  to create its instance.
     */
    public static ServerSetup instance() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return instance(System.getProperties());
    }
}

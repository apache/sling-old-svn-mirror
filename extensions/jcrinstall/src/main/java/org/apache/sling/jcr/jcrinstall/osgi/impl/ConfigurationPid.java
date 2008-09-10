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

/** Builds configration PIDs out of filenames, examples:
 *      o.a.s.foo.bar.cfg -> pid = o.a.s.foo.bar
 *      o.a.s.foo.bar-a.cfg -> pid = o.a.s.foo.bar, factory pid = a 
 */
class ConfigurationPid {
    private final String configPid;
    private final String factoryPid;

    public ConfigurationPid(String path) {
        // cut off path and extension
        String pid = path;
        final int lastSlash = path.lastIndexOf('/');
        if(lastSlash >= 0) {
            pid = path.substring(lastSlash + 1);
        }
        final int lastDot = pid.lastIndexOf('.');
        if(lastDot >= 0) {
            pid = pid.substring(0, lastDot);
        }

        // split pid and factory pid alias
        int n = pid.indexOf('-');
        if (n > 0) {
            factoryPid = pid.substring(n + 1);
            configPid = pid.substring(0, n);
        } else {
            factoryPid = null;
            configPid = pid;
        }
    }
    
    @Override
    public String toString() {
        return "Configuration (configPid=" + configPid + ", factoryPid=" + factoryPid + ")";
    }

    public String getConfigPid() {
        return configPid;
    }

    public String getFactoryPid() {
        return factoryPid;
    }
}

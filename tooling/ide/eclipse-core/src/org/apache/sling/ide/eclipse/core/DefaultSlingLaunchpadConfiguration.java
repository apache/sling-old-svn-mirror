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
package org.apache.sling.ide.eclipse.core;

/**
 * The <tt>DefaultSlingLaunchpadConfiguration</tt> specifies reasonable defaults when
 * configuring a new Sling server instance.
 *
 */
public class DefaultSlingLaunchpadConfiguration implements ISlingLaunchpadConfiguration {

    public static final ISlingLaunchpadConfiguration INSTANCE = new DefaultSlingLaunchpadConfiguration();
    
    @Override
    public int getPort() {
        return 8080;
    }

    @Override
    public int getDebugPort() {
        return 30303;
    }

    @Override
    public String getContextPath() {
        return "/";
    }

    @Override
    public String getUsername() {
        return "admin";
    }

    @Override
    public String getPassword() {
        return "admin";
    }

    @Override
    public boolean bundleInstallLocally() {
        return true;
    }

    @Override
    public boolean resolveSourcesInDebugMode() {
        return true;
    }

}
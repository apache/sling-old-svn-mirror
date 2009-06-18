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
package org.apache.sling.launchpad.base.shared;

import java.util.Map;

/**
 * The <code>Launcher</code> interface is implemented by the delegate classes
 * inside the Launcher JAR and are used by the actual Main class or servlet to
 * configure and start the framework.
 */
public interface Launcher {

    /**
     * Sets the sling.home to be used for starting the framework. This method
     * must be called with a non-<code>null</code> argument before trying to
     * start the framework.
     */
    public void setSlingHome(String slingHome);

    /**
     * The {@link Notifiable} to notify on framework stop or update
     */
    public void setNotifiable(Notifiable notifiable);

    /**
     * The commandline provided from the standalone launch case.
     */
    public void setCommandLine(Map<String, String> args);

    /**
     * Starts the framework and returns <code>true</code> if successfull.
     */
    public boolean start();

    /**
     * Stops the framework. This method only returns when the framework has
     * actually been stopped. This method may be used by the main class or
     * servlet to initiate a shutdown of the framework.
     */
    public void stop();
}

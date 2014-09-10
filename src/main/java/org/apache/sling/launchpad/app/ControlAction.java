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
package org.apache.sling.launchpad.app;

/**
 * The <code>ControlAction</code> defines values to used as the action for the
 * Sling control with the {@link Main#doControlAction()} method.
 */
public enum ControlAction {

    /**
     * Indicates the Sling application should be started and a listener should
     * be installed to accept control commands.
     */
    START,

    /**
     * Indicates to connect to a running Sling application having installed a
     * listener and send that application the command to shutdown.
     */
    STOP,

    /**
     * Indicates to connect to a running Sling application having installed a
     * listener and ask that application about its state.
     */
    STATUS,

    /**
     * Indicates to connect to a running Sling application having installed a
     * listener and ask for a thread dump.
     */
    THREADS;

}

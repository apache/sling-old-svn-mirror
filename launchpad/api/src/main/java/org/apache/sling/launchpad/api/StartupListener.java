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
package org.apache.sling.launchpad.api;


/**
 * A startup listener receives events about the startup.
 *
 * On registration of this listener, the method {@link #inform(StartupMode, boolean)}
 * is called with the startup mode and whether the startup is already finished.
 *
 * If the startup is not finished at point of registration, the {@link #startupFinished(StartupMode)}
 * method will be called, after the inform method has been called once the startup is finished.
 *
 * If the startup is not finished, the {@link #startupProgress(float)} method might be called
 * to indicate the current startup progress. This method should only be used for informational
 * purposes.
 *
 * A listener waiting for the startup to finish, should act on both actions: a call
 * of the inform method with the second argument set to true or a call of the startupFinished
 * method. Whatever is called first can be used as indication.
 *
 * @since 1.1.0
 */
public interface StartupListener {

    /**
     * Informs the listener upon registration about the current state.
     * @param mode The startup mode
     * @param finished Whether the startup is already finished or not
     */
    void inform(StartupMode mode, boolean finished);

    /**
     * Notify finished startup.
     * @param The startup mode
     */
    void startupFinished(StartupMode mode);

    /**
     * Notify startup progress
     * @param ratio The current ratio
     */
    void startupProgress(final float ratio);
}

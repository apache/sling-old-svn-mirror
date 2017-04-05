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

package org.apache.sling.crankstart.launcher;

/**
 * Listens to startup information from the launcher thread.
 */
public interface LauncherListener {

    /**
     * Called when the launcher has finished loading all initial bundles reporting those that started and those that failed.
     * @param started the number started.
     * @param failed the number that failed.
     * @param length the total number of bundles.
     */
    void onStartup(int started, int failed, int length);

    /**
     * Called when the launcher thread begins to perform shutdown.
     */
    void onShutdown();
}

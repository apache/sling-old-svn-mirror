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
package org.apache.sling.installer.api.tasks;

/**
 * The retry handler should be informed by external services
 * whenever something in the system changed which might make
 * it worth to retry a failed installed.
 *
 * The installer retries all unprocessed resources whenever this
 * retry handler is notified to reschedule. For example a bundle
 * listener listening for updates/installs/removals of bundles
 * can inform the retry handler.
 *
 * @since 1.2
 */
public interface RetryHandler {

    /**
     * Schedule a retry.
     */
    void scheduleRetry();
}

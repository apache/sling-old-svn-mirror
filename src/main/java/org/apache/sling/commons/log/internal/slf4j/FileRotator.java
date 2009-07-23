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
package org.apache.sling.commons.log.internal.slf4j;

import java.io.File;

/**
 * The <code>FileRotator</code> interface defines the API used to implement log
 * file rotation based on various limits such as the file size (see
 * {@link SizeLimitedFileRotator} or some fixed scheduling (see
 * {@link ScheduledFileRotator}.
 */
interface FileRotator {

    /**
     * The defualt file rotator is unlimited and never rotates the log file.
     * This may for example be used as the rotator for the console.
     */
    static final FileRotator DEFAULT = new FileRotator() {

        // never reaching the limit
        public boolean isRotationDue(File file) {
            return false;
        }

        public void rotate(File file) {
            // no rotation
        }

        @Override
        public String toString() {
            return "NullRotator";
        }
    };

    /**
     * Returns <code>true</code> if the current log <code>file</code> should be
     * rotated.
     */
    boolean isRotationDue(File file);

    /**
     * Since the {@link #isRotationDue(File)} indicated that rotation is due for
     * the given <code>file</code>, this method should actually rotate the log
     * file.
     */
    void rotate(File file);
}
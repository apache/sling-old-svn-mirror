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
package org.apache.sling.commons.testing.sling;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;

import org.apache.sling.api.request.RequestProgressTracker;

public class MockRequestProgressTracker implements RequestProgressTracker {

    public void dump(PrintWriter writer) {
    }

    public Iterator<String> getMessages() {
        return Collections.<String> emptyList().iterator();
    }

    public void log(String message) {
    }

    public void log(String format, Object... args) {
    }

    public void logTimer(String timerName) {
    }

    public void logTimer(String timerName, String format, Object... args) {
    }

    public void startTimer(String timerName) {
    }

    public void done() {
    }
}

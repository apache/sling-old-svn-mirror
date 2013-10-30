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
package org.apache.sling.testing.tools.junit;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Junit rule which exposes the current executing test's description as a thread local instance
 */
public class TestDescriptionRule extends TestWatcher {

    private static final ThreadLocal<Description> currentTestDescription = new ThreadLocal<Description>();

    @Override
    protected void finished(Description description) {
        currentTestDescription.remove();
    }

    @Override
    protected void starting(Description description) {
        currentTestDescription.set(description);
    }

    public static Description getCurrentTestDescription(){
        return currentTestDescription.get();
    }
}

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
package org.apache.sling.testing.rules;

import org.apache.sling.testing.clients.interceptors.TestDescriptionHolder;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Junit rule which exposes the description of the current executing test as a thread local instance
 */
public class TestDescriptionRule extends TestWatcher {


    @Override
    protected void finished(Description description) {
        TestDescriptionHolder.setClassName(description.getClassName());
        TestDescriptionHolder.setMethodName(description.getMethodName());
    }

    @Override
    protected void starting (Description description) {
        TestDescriptionHolder.removeMethodName();
        TestDescriptionHolder.removeClassName();
    }

}

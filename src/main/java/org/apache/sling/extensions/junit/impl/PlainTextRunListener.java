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
package org.apache.sling.extensions.junit.impl;

import java.io.PrintWriter;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

class PlainTextRunListener extends RunListener {
    final PrintWriter pw;
    
    PlainTextRunListener(PrintWriter w) {
        pw = w;
    }
    
    @Override
    public void testFailure(Failure failure) throws Exception {
        super.testFailure(failure);
        pw.println("FAILURE " + failure);
    }

    @Override
    public void testFinished(Description description) throws Exception {
        super.testFinished(description);
        pw.println("FINISHED " + description);
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        super.testIgnored(description);
        pw.println("IGNORED " + description);
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        super.testRunFinished(result);
        pw.println("TEST RUN FINISHED: "
                + "tests:" + result.getRunCount()
                + ", failures:" + result.getFailureCount()
                + ", ignored:" + result.getIgnoreCount()
        );
    }

    @Override
    public void testRunStarted(Description description)
            throws Exception {
        super.testRunStarted(description);
    }

    @Override
    public void testStarted(Description description) throws Exception {
        super.testStarted(description);
    }
}
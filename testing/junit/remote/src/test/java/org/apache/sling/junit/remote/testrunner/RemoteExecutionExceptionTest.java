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
package org.apache.sling.junit.remote.testrunner;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class RemoteExecutionExceptionTest {

    @Test
    public void testGetStackTraceFromString() throws NumberFormatException, IOException {
        String trace = null;
        try {
            throw new IllegalStateException("Some message");
        } catch(Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            trace = writer.toString();
        }
        RemoteExecutionException e = RemoteExecutionException.getExceptionFromTrace(trace);
        Assert.assertThat(e.getMessage(), Matchers.equalTo("java.lang.IllegalStateException: Some message"));
        List<StackTraceElement> stackTraceElements = Arrays.asList(new RemoteExecutionException("some failure", trace).getStackTrace());
        Assert.assertThat(stackTraceElements, Matchers.hasItem(new StackTraceElement("org.apache.sling.junit.remote.testrunner.RemoteExecutionExceptionTest", "testGetStackTraceFromString", "RemoteExecutionExceptionTest.java", 36)));
        // compare original stacktrace with newly generated one from the exception
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        String newTrace = writer.toString();
        Assert.assertEquals(trace, newTrace);
    }

    @Test
    public void testGetStackTraceFromStringWithNestedException() throws NumberFormatException, IOException {
        String trace = null;
        try {
            try {
                throw new IllegalStateException("Some message");
            } catch(Exception e) {
                throw new RuntimeException("Wrapper exception", e);
            }
        } catch (Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            trace = writer.toString();
        }
        
        RemoteExecutionException e = RemoteExecutionException.getExceptionFromTrace(trace);
        Assert.assertThat(e.getMessage(), Matchers.equalTo("java.lang.RuntimeException: Wrapper exception"));
        List<StackTraceElement> stackTraceElements = Arrays.asList(e.getStackTrace());
        Assert.assertThat(stackTraceElements, Matchers.hasItem(new StackTraceElement("org.apache.sling.junit.remote.testrunner.RemoteExecutionExceptionTest", "testGetStackTraceFromStringWithNestedException", "RemoteExecutionExceptionTest.java", 60)));
        // no original exception in the stack trace
        Assert.assertThat(stackTraceElements, Matchers.not(Matchers.hasItem(new StackTraceElement("org.apache.sling.junit.remote.testrunner.RemoteExecutionExceptionTest", "testGetStackTraceFromStringWithNestedException", "RemoteExecutionExceptionTest.java", 58))));
        
        // cause must be set
        Assert.assertNotNull("Cause must be set on the exception", e.getCause());
        Assert.assertThat(e.getCause().getMessage(), Matchers.equalTo("java.lang.IllegalStateException: Some message"));
        stackTraceElements = Arrays.asList(e.getCause().getStackTrace());
        Assert.assertThat(stackTraceElements, Matchers.hasItem(new StackTraceElement("org.apache.sling.junit.remote.testrunner.RemoteExecutionExceptionTest", "testGetStackTraceFromStringWithNestedException", "RemoteExecutionExceptionTest.java", 58)));
    }
}

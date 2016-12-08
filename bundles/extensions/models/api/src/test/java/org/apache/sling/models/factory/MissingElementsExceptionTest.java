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
package org.apache.sling.models.factory;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class MissingElementsExceptionTest {

    @Test
    public void testMissingElementsExceptionStackTraceContainsTracesOfAggregatedExceptions() {
        MissingElementsException wrapperException = new MissingElementsException("Test wrapper");
        try {
            try {
                throw new IllegalStateException("Root exception");
            } catch(IllegalStateException rootException) {
                throw new MissingElementException(null, rootException);
            }
        } catch(MissingElementException e) {
            wrapperException.addMissingElementExceptions(e);
        }

        // now evaluate exception message
        Assert.assertThat(wrapperException.getMessage(), Matchers.not(Matchers.containsString("Root Exception")));
        Assert.assertThat(wrapperException.getMessage(), Matchers.containsString("Test wrapper"));

        // make sure the aggregated exceptions appear in the stack trace
        StringWriter stringWriter = new StringWriter();
        wrapperException.printStackTrace(new PrintWriter(stringWriter));
        Assert.assertThat(stringWriter.toString(), Matchers.containsString("Root exception"));
        Assert.assertThat(stringWriter.toString(), Matchers.containsString("Test wrapper"));
    }
}

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
package org.apache.sling.performance.samples;

import org.apache.sling.performance.PerformanceRunner;
import org.apache.sling.performance.PerformanceRunner.Parameters;
import org.apache.sling.performance.PerformanceRunner.ReportLevel;
import org.apache.sling.performance.annotation.PerformanceTest;
import org.junit.runner.RunWith;

/** Demonstrate the Parameters annotation */
@RunWith(PerformanceRunner.class)
@Parameters(reportLevel=ReportLevel.MethodLevel)
public class WithParametersTest {
    
    @PerformanceTest
    public void PerformanceTestA() throws InterruptedException {
        Thread.sleep((long)(Math.random() * 13));
    }
    
    @PerformanceTest(warmuptime=0,runinvocations=5)
    public void PerformanceTestB() throws InterruptedException {
        Thread.sleep((long)(Math.random() * 11));
    }
}

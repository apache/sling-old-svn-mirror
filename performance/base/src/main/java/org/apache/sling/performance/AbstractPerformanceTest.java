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
package org.apache.sling.performance;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import junit.framework.TestCase;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

public abstract class AbstractPerformanceTest extends TestCase {
    
    private final int runtime = 5;

    private final int warmup = 1;
    
    private void runTest(String name, AbstractTest test) throws Exception {
        DescriptiveStatistics statistics = new DescriptiveStatistics();

        test.setUp();
        try {
            // Run a few iterations to warm up the system
            long warmupEnd = System.currentTimeMillis() + warmup * 1000;
            while (System.currentTimeMillis() < warmupEnd) {
                test.execute();
            }

            // Run test iterations, and capture the execution times
            long runtimeEnd = System.currentTimeMillis() + runtime * 1000;
            while (System.currentTimeMillis() < runtimeEnd) {
                statistics.addValue(test.execute());
            }
        } finally {
            test.tearDown();
        }
 
        if (statistics.getN() > 0) {
            writeReport(test.toString(), name, statistics);
        }
    }

    protected void testPerformance(String name, List <AbstractTest> tests) throws Exception {
        for (AbstractTest test:tests){
            runTest(name,test);
        }
    }
    
    private void writeReport(String test, String name, DescriptiveStatistics statistics)
    throws IOException {
        File report = new File("target", test + ".txt");

        boolean needsPrefix = !report.exists();
        PrintWriter writer = new PrintWriter(
                new FileWriterWithEncoding(report, "UTF-8", true));
        try {
            if (needsPrefix) {
                writer.format(
                        "# %-34.34s     min     10%%     50%%     90%%     max%n",
                        test);
            }

            writer.format(
                    "%-36.36s  %6.0f  %6.0f  %6.0f  %6.0f  %6.0f%n",
                    name,
                    statistics.getMin(),
                    statistics.getPercentile(10.0),
                    statistics.getPercentile(50.0),
                    statistics.getPercentile(90.0),
                    statistics.getMax());
        } finally {
            writer.close();
        }
    }

}

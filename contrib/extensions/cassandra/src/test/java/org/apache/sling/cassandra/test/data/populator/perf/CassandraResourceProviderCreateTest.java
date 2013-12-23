/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.cassandra.test.data.populator.perf;

import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test the PlanetsResourceProvider that the test-services bundles provides
 */
public class CassandraResourceProviderCreateTest extends HttpTestBase {

    private static List<String> exerciseList = new ArrayList<String>();
    private static List<String> testList = new ArrayList<String>();
    private static List<Long> firstRun = new ArrayList<Long>();
    private static List<Long> secondRun = new ArrayList<Long>();

    private static File file = new File("/home/dishara/mystuff/sling/svn/slinglatest/launchpad/integration-tests/CassandraCreateLatencyReport.txt");
    private static FileWriter fileWriter;

    static {
        loadTestLists();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignore) {
            }
        }
        try {
            fileWriter = new FileWriter(file, true);
        } catch (IOException ignore) {
        }

    }

    private static void loadTestLists() {
        int exerciseCount = 0;
        int testCount = 0;

        for (int i = 0; i < 1000; i++) {
            if (i % 2 == 0) {
                if (exerciseCount < 100) {
                    exerciseList.add(String.valueOf(i));
                    System.out.println("Exercise: " + i);
                    exerciseCount++;
                }
            } else {
                if (testCount < 100) {
                    testList.add(String.valueOf(i));
                    System.out.println("Test: " + i);
                    testCount++;
                }
            }
        }

    }

    private void createAllExerciseData(String parentNode) throws IOException {
        Map<String, String> props = new HashMap<String, String>();
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        Map<String, String> props1 = new HashMap<String, String>();
        props1.put("metadata", "resolutionPathInfo=json");
        props1.put("resourceType", "nt:cassandra0");
        props1.put("resourceSuperType", "nt:supercass1");
        props.put(SlingPostConstants.RP_CHECKIN, "true");

        for (String s : exerciseList) {
            testClient.createNode(HTTP_BASE_URL + "/content/cassandra/" + parentNode + "/" + s, props, props1, true);
        }

    }

    private void writeToFile(String s) throws IOException {
        fileWriter.write(s + "\n");
    }

    private void createAllTestData(String parentNode, List<Long> result, Map<String, Long> nodeMap) throws IOException {
        Map<String, String> props = new HashMap<String, String>();
        props.put(SlingPostConstants.RP_CONTENT_TYPE, "json");
        Map<String, String> props1 = new HashMap<String, String>();
        props1.put("metadata", "resolutionPathInfo=json");
        props1.put("resourceType", "nt:cassandra0");
        props1.put("resourceSuperType", "nt:supercass1");
        props.put(SlingPostConstants.RP_CHECKIN, "true");

        writeToFile("#######################################################################################################################");
        writeToFile("############################ HTTP CUD Latency Report on Test Data under " + parentNode + " ################################");
        writeToFile("#######################################################################################################################");

        long subTotal = 0;

        for (String s : testList) {
            long startTime = System.currentTimeMillis();

            String url = HTTP_BASE_URL + "/content/cassandra/" + parentNode + "/" + s;
            testClient.createNode(url, props, props1, true);
            long latency = System.currentTimeMillis() - startTime;
            result.add(latency);
            subTotal = subTotal + latency;
            writeToFile("[TEST] Latency: " + latency + " (ms) HTTP URI " + url);
            System.out.println(url);
        }
        nodeMap.put(parentNode, subTotal);
    }

    public void testMovieResource() throws Exception {
        String[] nodes = new String[]{"LA", "MA", "SA"};
//        String[] nodes = new String[]{"A", "B", "C", "D", "E", "F"};
        Map<String, Long> firstNodeMap = new HashMap<String, Long>();
        Map<String, Long> secondNodeMap = new HashMap<String, Long>();

        writeToFile("============================================ FIRST RUN ============================================================");
        for (String s : nodes) {
            createAllTestData(s, firstRun, firstNodeMap);
        }
        long firstRunTotal = 0;
        long secondRunTotal = 0;

        for (long l1 : firstRun) {
            firstRunTotal = firstRunTotal + l1;
        }

        for (long l2 : secondRun) {
            secondRunTotal = secondRunTotal + l2;
        }

        writeToFile("===========================================================================================================================");
        writeToFile("========================================== FIRST RUN TEST SUMMERY==========================================================");
        writeToFile("[RESULT] Average Latency Under Node A(1K)   = " + firstNodeMap.get("LA") / testList.size() + " (ms)");
        writeToFile("[RESULT] Average Latency Under Node B(10K)  = " + firstNodeMap.get("MA") / testList.size() + " (ms)");
        writeToFile("[RESULT] Average Latency Under Node C(100K) = " + firstNodeMap.get("SA") / testList.size() + " (ms)");
//        writeToFile("[RESULT] Average Latency Under Node D(1M)   = " + firstNodeMap.get("D") / testList.size() + " (ms)");
//        writeToFile("[RESULT] Average Latency Under Node E(10M)  = " + firstNodeMap.get("E") / testList.size() + " (ms)");
//        writeToFile("[RESULT] Average Latency Under Node F(100M) = " + firstNodeMap.get("F") / testList.size() + " (ms)");
        writeToFile("[FIRST RUN] #TOTAL CALLS = " + firstRun.size() + " Total Average Latency = " + firstRunTotal / firstRun.size() + " (ms)");
        writeToFile("===========================================================================================================================");
//        writeToFile("========================================== SECOND RUN TEST SUMMERY==========================================================");
//        writeToFile("[RESULT] Average Latency Under Node A(1K)   = " + secondNodeMap.get("LA") / testList.size() + " (ms)");
//        writeToFile("[RESULT] Average Latency Under Node B(10K)  = " + secondNodeMap.get("MA") / testList.size() + " (ms)");
//        writeToFile("[RESULT] Average Latency Under Node C(100K) = " + secondNodeMap.get("SA") / testList.size() + " (ms)");
//        writeToFile("[RESULT] Average Latency Under Node D(1M)   = " + secondNodeMap.get("D") / testList.size() + " (ms)");
//        writeToFile("[RESULT] Average Latency Under Node E(10M)  = " + secondNodeMap.get("E") / testList.size() + " (ms)");
//        writeToFile("[RESULT] Average Latency Under Node F(100M) = " + secondNodeMap.get("F") / testList.size() + " (ms)");
//        writeToFile("[FIRST RUN] #TOTAL CALLS = " + secondRun.size() + " Total Average Latency = " + secondRunTotal / secondRun.size() + " (ms)");
        writeToFile("===========================================================================================================================");
        fileWriter.close();
    }


}

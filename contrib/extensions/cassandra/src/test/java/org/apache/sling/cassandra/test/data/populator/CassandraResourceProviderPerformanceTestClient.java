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
package org.apache.sling.cassandra.test.data.populator;

import org.apache.sling.commons.testing.integration.HttpTestBase;

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
public class CassandraResourceProviderPerformanceTestClient extends HttpTestBase {

    private static List<String> exerciseList = new ArrayList<String>();
    private static List<String> testList = new ArrayList<String>();
    private static List<Long> firstRun = new ArrayList<Long>();
    private static List<Long> secondRun = new ArrayList<Long>();

    private static File file = new File("/home/dishara/mystuff/sling/svn/slinglatest/launchpad/integration-tests/CassandraLatencyReport.txt");
                     private static FileWriter fileWriter;
    static {
        loadTestLists();
        if(!file.exists()){
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

    private void getAllExerciseData(String parentNode) throws IOException {
        for (String s : exerciseList) {
            getContent(HTTP_BASE_URL + "/content/cassandra/" + parentNode + "/" + s + ".json", CONTENT_TYPE_JSON);
        }
    }
    private void writeToFile(String s) throws IOException {
         fileWriter.write(s+"\n");
    }

    private void getAllTestData(String parentNode, List<Long> result, Map<String, Long> nodeMap) throws IOException {
        writeToFile("#######################################################################################################################");
        writeToFile("############################ HTTP Latency Report on Test Data under " + parentNode + " ################################");
        writeToFile("#######################################################################################################################");

        long subTotal = 0;
        for (String s : testList) {
            long startTime = System.currentTimeMillis();
            String url = HTTP_BASE_URL + "/content/cassandra/" + parentNode + "/" + s + ".json";
            String content = getContent(url, CONTENT_TYPE_JSON);
            long latency = System.currentTimeMillis() - startTime;
            result.add(latency);
            subTotal = subTotal + latency;
            writeToFile("[TEST] Latency: " + latency + " (ms) HTTP URI " + content);
        }
        nodeMap.put(parentNode,subTotal);
    }

    public void testMovieResource() throws Exception {

        String[] nodes = new String[]{"A", "B", "C", "D","E","F"};
        Map<String,Long> firstNodeMap = new HashMap<String,Long>();
        Map<String,Long> secondNodeMap = new HashMap<String,Long>();

        writeToFile("============================================ FIRST RUN ============================================================");
        for (String s : nodes) {
            getAllExerciseData(s);
            getAllTestData(s,firstRun,firstNodeMap);
        }
        writeToFile("============================================ SECOND RUN ============================================================");
        for (String s : nodes) {
            getAllExerciseData(s);
            getAllTestData(s, secondRun, secondNodeMap);
        }

        long firstRunTotal = 0;
        long secondRunTotal = 0;

        for(long l1:firstRun){
            firstRunTotal = firstRunTotal + l1;
        }

        for(long l2:secondRun){
            secondRunTotal = secondRunTotal + l2;
        }

        writeToFile("===========================================================================================================================");
        writeToFile("========================================== FIRST RUN TEST SUMMERY==========================================================");
        writeToFile("[RESULT] Average Latency Under Node A(1K)   = " + firstNodeMap.get("A")/testList.size() +" (ms)");
        writeToFile("[RESULT] Average Latency Under Node B(10K)  = " + firstNodeMap.get("B")/testList.size() +" (ms)");
        writeToFile("[RESULT] Average Latency Under Node C(100K) = " + firstNodeMap.get("C")/testList.size() +" (ms)");
        writeToFile("[RESULT] Average Latency Under Node D(1M)   = " + firstNodeMap.get("D")/testList.size() +" (ms)");
        writeToFile("[RESULT] Average Latency Under Node E(10M)  = " + firstNodeMap.get("E")/testList.size() +" (ms)");
        writeToFile("[RESULT] Average Latency Under Node F(100M) = " + firstNodeMap.get("F")/testList.size() +" (ms)");
        writeToFile("[FIRST RUN] #TOTAL CALLS = " + firstRun.size() + " Total Average Latency = " + firstRunTotal/firstRun.size() + " (ms)");
        writeToFile("===========================================================================================================================");
        writeToFile("========================================== SECOND RUN TEST SUMMERY==========================================================");
        writeToFile("[RESULT] Average Latency Under Node A(1K)   = " + secondNodeMap.get("A")/testList.size() +" (ms)");
        writeToFile("[RESULT] Average Latency Under Node B(10K)  = " + secondNodeMap.get("B")/testList.size() +" (ms)");
        writeToFile("[RESULT] Average Latency Under Node C(100K) = " + secondNodeMap.get("C")/testList.size() +" (ms)");
        writeToFile("[RESULT] Average Latency Under Node D(1M)   = " + secondNodeMap.get("D")/testList.size() +" (ms)");
        writeToFile("[RESULT] Average Latency Under Node E(10M)  = " + secondNodeMap.get("E")/testList.size() +" (ms)");
        writeToFile("[RESULT] Average Latency Under Node F(100M) = " + secondNodeMap.get("F")/testList.size() +" (ms)");
        writeToFile("[FIRST RUN] #TOTAL CALLS = " + secondRun.size() + " Total Average Latency = " + secondRunTotal/secondRun.size() + " (ms)");
        writeToFile("===========================================================================================================================");
        fileWriter.close();
    }


}

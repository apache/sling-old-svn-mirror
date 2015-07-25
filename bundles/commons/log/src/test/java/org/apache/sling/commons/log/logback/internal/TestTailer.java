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

package org.apache.sling.commons.log.logback.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;

public class TestTailer {
    private final Random rnd = new Random();
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private final LineCollector listener = new LineCollector();

    @Test
    public void testEmpty() throws Exception{
        File f1 = tempFolder.newFile();
        Tailer t = new Tailer(listener, 10);
        t.tail(f1);
        assertThat(listener.lines, empty());
    }

    @Test
    public void testLessAndMore() throws Exception{
        File f1 = tempFolder.newFile();
        writeToFile(f1, asList("a", "b", "c", "d"));
        new Tailer(listener, 2).tail(f1);
        assertThat(listener.lines, contains("c", "d"));

        listener.reset();
        new Tailer(listener, 10).tail(f1);
        assertThat(listener.lines, contains("a", "b", "c", "d"));
    }

    @Test
    public void randomTest() throws Exception{
        File f1 = tempFolder.newFile();
        List<String> lines = createRandomLines(Tailer.BUFFER_SIZE * 10);
        int numOfLines = lines.size();
        writeToFile(f1, lines);
        int n = rnd.nextInt(numOfLines/2);
        new Tailer(listener, n).tail(f1);
        assertEquals(listener.lines, lines.subList(numOfLines - n, numOfLines));

    }

    private List<String> createRandomLines(int totalSize){

        List<String> result = new ArrayList<String>();
        int size = 0;
        while(true){
            int l = rnd.nextInt(100);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < l; i++) {
                sb.append('x');
            }
            size += sb.length();
            result.add(sb.toString());

            if (size > totalSize){
                break;
            }
        }
        return result;
    }

    private void writeToFile(File f, List<String> lines) throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean firstLine = true;
        for (String line : lines){
            if (firstLine){
                firstLine = false;
            } else {
                sb.append("\n");
            }
            sb.append(line);

        }
        FileUtils.write(f, sb);
    }

    private static class LineCollector implements Tailer.TailerListener {
        final List<String> lines = new ArrayList<String>();
        @Override
        public void handle(String line) {
            lines.add(line);
        }

        public void reset(){
            lines.clear();
        }
    }
}

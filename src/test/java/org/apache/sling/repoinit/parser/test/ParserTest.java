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

package org.apache.sling.repoinit.parser.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.repoinit.parser.impl.RepoInitParserService;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.apache.sling.repoinit.parser.operations.OperationVisitor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Test the parser using our test-* input/expected output files.
 *  The code of this class doesn't contain any actual tests, it
 *  just looks for test-*.txt files, parses them and verifies the
 *  results according to the test-*-output.txt files.
 */
@RunWith(Parameterized.class)
public class ParserTest {

    public static final String DEFAULT_ENCODING = "UTF-8";

    static class TestCase {
        final Reader input;
        final String inputFilename;
        final InputStream expected;
        final String outputFilename;

        private static final String PREFIX = "/testcases/test-";

        @Override
        public String toString() {
            return inputFilename;
        }

        private TestCase(int index) throws IOException {
            inputFilename = PREFIX + index + ".txt";
            final InputStream is = getClass().getResourceAsStream(inputFilename);
            if ( is != null ) {
                input = new InputStreamReader(is, "UTF-8");
            } else {
                input = null;
            }
            outputFilename = PREFIX + index + "-output.txt";
            expected = getClass().getResourceAsStream(outputFilename);
        }

        static TestCase build(int index) throws IOException {
            final TestCase result = new TestCase(index);
            if(result.input == null || result.expected == null) {
                return null;
            }
            return result;
        }

        void close() {
            try {
                input.close();
            } catch(IOException ignored) {
            }
            try {
                expected.close();
            } catch(IOException ignored) {
            }
        }
    }

    private final TestCase tc;

    @Parameters(name="{0}")
    public static Collection<Object[]> data() throws IOException {
        final List<Object []> result = new ArrayList<>();
        for(int i=0; i < 100; i++) {
            final TestCase tc = TestCase.build(i);
            if(tc != null) {
                result.add(new Object[] { tc });
            }
        }
        return result;

    }

    public ParserTest(TestCase tc) {
        this.tc = tc;
    }

    @Test
    public void checkResult() throws RepoInitParsingException, IOException {
        final String expected = IOUtils.toString(tc.expected, DEFAULT_ENCODING).trim();
        try {
            final StringWriter sw = new StringWriter();
            final OperationVisitor v = new OperationToStringVisitor(new PrintWriter(sw));
            final List<Operation> result = new RepoInitParserService().parse(tc.input);
            for(Operation o : result) {
                o.accept(v);
            }
            sw.flush();
            String actual = sw.toString().trim();

            // normalize line endings to ensure tests run on windows as well
            actual = actual.replaceAll("\r\n", "\n");

            assertEquals(expected, actual);
        } finally {
            tc.close();
        }
    }
}

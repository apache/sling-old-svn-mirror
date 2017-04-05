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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoteExecutionException extends RuntimeException {
    private final String trace;

    private final StackTraceElement[] stackTrace;

    /**
     * Matches on lines in the format {@code at package.class.method(source.java:123)} with 4 (1,2,3,4) or
     * {@code at package.class.method(Native method)} with 3 different groups (1,2,5)
     */
    private static final Pattern TRACE_PATTERN = Pattern
            .compile("\\s*at\\s+([\\w\\.$_]+)\\.([\\w$_]+)(?:\\((.*\\.java):(\\d+)\\)|(\\(.*\\)))");
    /**
     * Matches on lines in the format {@code Caused by: java.io.IOException: Some message} with 1 group containing the
     * part after the first colon
     */
    private static final Pattern CAUSED_BY_PATTERN = Pattern.compile("\\s*Caused by:\\s+(.*)");

    private static final int NATIVE_METHOD_LINE_NUMBER = -2;

    public static RemoteExecutionException getExceptionFromTrace(String trace) throws IOException {
        // first line of trace is something like "java.lang.RuntimeException: Wrapper exception"
        BufferedReader reader = new BufferedReader(new StringReader(trace));
        final String firstLine;
        try {
            firstLine = reader.readLine();
        } finally {
            reader.close();
        }
        return new RemoteExecutionException(firstLine, trace);
    }

    public RemoteExecutionException(String failure, String trace) throws NumberFormatException, IOException {
        super(failure);
        this.trace = trace;
        this.stackTrace = getStackTraceFromString(trace);
    }

    @Override
    public void printStackTrace(PrintStream s) {
        if (trace != null) {
            s.print(trace);
        }
    }
    
    @Override
    public void printStackTrace(PrintWriter s) {
        if (trace != null) {
            s.print(trace);
        }
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }

    /**
     * Returns all StackTraceElement created from the given String. Also evaluates the cause of an exception by setting {@link #initCause(Throwable)}.
     * Example format for given trace:
     * 
     * <pre>
     * {@code
     *  java.lang.RuntimeException: Wrapper exception
     *         at org.apache.sling.junit.remote.testrunner.RemoteExecutionExceptionTest.testGetNestedStackTraceFromString(RemoteExecutionExceptionTest.java:55)
     *         at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
     *         at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
     *         at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
     *         at java.lang.reflect.Method.invoke(Method.java:606)
     *         ...
     * Caused by: java.lang.IllegalStateException: Some message
     *         at org.apache.sling.junit.remote.testrunner.RemoteExecutionExceptionTest.testGetNestedStackTraceFromString(RemoteExecutionExceptionTest.java:53)
     *         ... 23 more
     * }
     * </pre>
     * 
     * @param trace a serialized stack trace.
     * @return an array of {@link StackTraceElement}s.
     * @throws IOException
     * @throws NumberFormatException
     */
    private final StackTraceElement[] getStackTraceFromString(String trace) throws NumberFormatException, IOException {
        if (trace == null) {
            return new StackTraceElement[0];
        }

        List<StackTraceElement> stackTraceElements = new ArrayList<StackTraceElement>();
        BufferedReader reader = new BufferedReader(new StringReader(trace));
        try {
            String line = null;
            while ((line = reader.readLine()) != null) {
                Matcher traceMatcher = TRACE_PATTERN.matcher(line);
                if (traceMatcher.find()) {
                    String className = traceMatcher.group(1);
                    String methodName = traceMatcher.group(2);
                    String sourceFile;
                    int lineNumber;
                    if (traceMatcher.group(3) != null) {
                        // java file with line number
                        sourceFile = traceMatcher.group(3);
                        if (traceMatcher.group(4) != null) {
                            lineNumber = Integer.parseInt(traceMatcher.group(4));
                        } else {
                            lineNumber = -1;
                        }
                    } else {
                        // probably a native method
                        sourceFile = traceMatcher.group(5);
                        lineNumber = NATIVE_METHOD_LINE_NUMBER;
                    }
                    // null checks
                    stackTraceElements.add(new StackTraceElement(className, methodName, sourceFile, lineNumber));
                }
                // is this a caused by
                Matcher causedByMatcher = CAUSED_BY_PATTERN.matcher(line);
                if (causedByMatcher.find()) {
                    // all remaining lines of the trace should be given to the wrapped exception
                    char[] cbuf = new char[trace.length()];
                    if (reader.read(cbuf) > 0) {
                        this.initCause(new RemoteExecutionException(causedByMatcher.group(1), new String(cbuf)));
                    }
                }
            }
        } finally {
            reader.close();
        }
        return stackTraceElements.toArray(new StackTraceElement[stackTraceElements.size()]);
    }
}

/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Stack;

/**
 * Text writing utility which allows stacking of temporary buffers
 */
public final class StackedWriter extends Writer {

    private final PrintWriter baseWriter;
    private final Stack<StringWriter> writerStack = new Stack<StringWriter>();
    private PrintWriter current;

    public StackedWriter(PrintWriter baseWriter) {
        this.baseWriter = baseWriter;
        this.current = baseWriter;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        current.write(cbuf, off, len);
    }

    @Override
    public void write(String text) {
        current.write(text);
    }

    @Override
    public void flush() throws IOException {
        current.flush();
    }

    @Override
    public void close() throws IOException {
        if (writerStack.size() != 1) {
            throw new UnsupportedOperationException("Stack is not empty");
        }
        current.close();
    }

    public void push() {
        StringWriter writer = new StringWriter();
        writerStack.push(writer);
        current = new PrintWriter(writer);
    }

    public String pop() {
        String output = null;
        if (!writerStack.isEmpty()) {
            StringWriter writer = writerStack.pop();
            output = writer.toString();
        }
        if (writerStack.isEmpty()) {
            current = baseWriter;
        } else {
            current = new PrintWriter(writerStack.peek());
        }
        return output;
    }
}

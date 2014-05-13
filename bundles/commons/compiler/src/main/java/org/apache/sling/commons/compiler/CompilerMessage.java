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
package org.apache.sling.commons.compiler;

/**
 * This class encapsulates a message produced the compiler.
 * A message is either a warning or an error.
 * The messages are retrieved from the {@link CompilationResult}.
 *
 * @since 2.0
 */
public class CompilerMessage {

    /**
     * The line number of the offending program text
     */
    private final int line;

    /**
     * The column number of the offending program text
     */
    private final int column;

    /**
     * The name of the file containing the offending program text
     */
    private final String file;

    /**
     * The actual text
     */
    private final String message;

    /**
     * The error message constructor.
     *
     * @param file The name of the file containing the offending program text
     * @param line The line number of the offending program text
     * @param column The column number of the offending program text
     * @param message The actual text
     */
    public CompilerMessage(final String file,
                           final int line,
                           final int column,
                           final String message) {
        this.file = file;
        this.line = line;
        this.column = column;
        this.message = message;
    }

    /**
     * Return the filename associated with this compiler message.
     *
     * @return The filename associated with this compiler message
     */
    public String getFile() {
        return file;
    }

    /**
     * Return the line number of the program text originating this message
     *
     * @return The line number of the program text originating this message
     */
    public int getLine() {
        return this.line;
    }

    /**
     * Return the column number of the program text originating this message
     *
     * @return The column number of the program text originating this message
     */
    public int getColumn() {
        return this.column;
    }

    /**
     * Return the message
     *
     * @return The message
     */
    public String getMessage() {
        return message;
    }
}

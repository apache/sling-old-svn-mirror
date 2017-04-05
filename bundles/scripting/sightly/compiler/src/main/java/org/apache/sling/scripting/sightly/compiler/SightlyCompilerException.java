/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.compiler;

/**
 * Exception thrown by the {@link SightlyCompiler} during various processing operations.
 */
public final class SightlyCompilerException extends RuntimeException {

    private String offendingInput;
    private int line;
    private int column;

    /**
     * Create a simple exception without any other information.
     */
    public SightlyCompilerException() {
        super();
    }

    /**
     * Create an exception with a provided message.
     *
     * @param message the exception's message
     */
    public SightlyCompilerException(String message) {
        super(message);
    }

    /**
     * Create an exception with information about the cause.
     *
     * @param cause the cause
     */
    public SightlyCompilerException(Throwable cause) {
        super(cause);
    }

    /**
     * Create an exception that has information about offending syntax input.
     *
     * @param message        the exception's message
     * @param offendingInput the offending input, as raw text
     */
    public SightlyCompilerException(String message, String offendingInput) {
        super(message);
        this.offendingInput = offendingInput;
    }

    /**
     * Creates an exception that has information about offending syntax input, with additional details about the position of the error.
     *
     * @param message        the exception's message
     * @param offendingInput the offending input, as raw text
     * @param line           the line where the error occurred
     * @param column         the column in the line where the error occurred
     */
    public SightlyCompilerException(String message, String offendingInput, int line, int column) {
        this(message, offendingInput);
        this.column = column;
        this.line = line;
    }

    /**
     * Creates an exception with a provided message and cause.
     *
     * @param message the exception's message
     * @param cause   the cause
     */
    public SightlyCompilerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception that has information about offending syntax input, with additional details about the position of the error.
     *
     * @param message        the exception's message
     * @param offendingInput the offending input, as raw text
     * @param line           the line where the error occurred
     * @param column         the column in the line where the error occurred
     * @param cause          the cause
     */
    public SightlyCompilerException(String message, String offendingInput, int line, int column, Throwable cause) {
        this(message, cause);
        this.offendingInput = offendingInput;
        this.column = column;
        this.line = line;
    }

    /**
     * Returns the offending input, as a raw string.
     *
     * @return the offending input, as a raw string
     */
    public String getOffendingInput() {
        return offendingInput;
    }

    /**
     * Returns the line where the error occurred, if the information is available.
     *
     * @return the line where the error occurred, if the information is available
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the column in the line where the error occurred, if the information is available.
     *
     * @return the column in the line where the error occurred, if the information is available
     */
    public int getColumn() {
        return column;
    }
}
